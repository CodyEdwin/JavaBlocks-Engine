/*
 * JavaBlocks Engine - World Class
 * 
 * Central hub for the Entity Component System.
 * Manages entities, components, and systems in a cohesive package.
 */
package com.javablocks.core.ecs;

import com.javablocks.core.*;
import com.javablocks.core.events.*;
import com.javablocks.core.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * The World class is the central hub of the ECS architecture.
 * It manages entities, their components, and the systems that process them.
 * 
 * Features:
 * - High-performance entity creation and destruction
 * - Component storage with cache-friendly memory layout
 * - System execution with priority ordering
 * - Thread-safe operations where needed
 * - Zero-GC hot paths for performance
 * 
 * @author JavaBlocks Engine Team
 */
public final class World {
    
    // ==================== Constants ====================
    
    /** Default number of entities to pre-allocate. */
    private static final int DEFAULT_INITIAL_ENTITIES = 10000;
    
    /** Default number of component pools. */
    private static final int DEFAULT_COMPONENT_POOLS = 32;
    
    /** Number of milliseconds to wait for system updates. */
    private static final long SYSTEM_UPDATE_TIMEOUT_MS = 100;
    
    // ==================== Instance Variables ====================
    
    /** Engine configuration. */
    private final JavaBlocksEngine.EngineConfiguration config;
    
    /** Entity pool for efficient entity ID management. */
    private final Entity.EntityPool entityPool;
    
    /** Component manager for component storage. */
    private final ComponentManager componentManager;
    
    /** System manager for system execution. */
    private final SystemManager systemManager;
    
    /** Signal registry for event communication. */
    private final SignalRegistry signalRegistry;
    
    /** Entity sets for fast entity iteration. */
    private final EntitySet entitySet;
    
    /** Active entities list for fast iteration. */
    private final LongList activeEntities;
    
    /** Pending entity operations (thread-safe queue). */
    private final ConcurrentLinkedQueue<EntityOperation> pendingOperations;
    
    /** Number of entities currently in the world. */
    private int entityCount;
    
    /** Whether the world is updating (for safety checks). */
    private volatile boolean isUpdating;
    
    /** Whether the world has been disposed. */
    private volatile boolean disposed;
    
    // ==================== Entity Operations ====================
    
    /**
     * Internal structure for pending entity operations.
     */
    private static final class EntityOperation {
        final enum Type { CREATE, DESTROY, ADD_COMPONENT, REMOVE_COMPONENT }
        
        final Type type;
        final Entity entity;
        final Component component;
        final Class<? extends Component> componentClass;
        
        EntityOperation(Type type, Entity entity) {
            this.type = type;
            this.entity = entity;
            this.component = null;
            this.componentClass = null;
        }
        
        EntityOperation(Type type, Entity entity, Component component) {
            this.type = type;
            this.entity = entity;
            this.component = component;
            this.componentClass = null;
        }
        
        EntityOperation(Type type, Entity entity, Class<? extends Component> componentClass) {
            this.type = type;
            this.entity = entity;
            this.component = null;
            this.componentClass = componentClass;
        }
    }
    
    // ==================== Constructor ====================
    
    /**
     * Creates a new World with default configuration.
     */
    public World() {
        this(new JavaBlocksEngine.EngineConfiguration());
    }
    
    /**
     * Creates a new World with custom configuration.
     * 
     * @param config The engine configuration
     */
    public World(JavaBlocksEngine.EngineConfiguration config) {
        this.config = config;
        
        // Initialize core structures
        int initialEntities = config.initialEntityPoolSize > 0 ? 
            config.initialEntityPoolSize : DEFAULT_INITIAL_ENTITIES;
        
        this.entityPool = new Entity.EntityPool(initialEntities);
        this.componentManager = new ComponentManager(DEFAULT_COMPONENT_POOLS);
        this.systemManager = new SystemManager();
        this.signalRegistry = new SignalRegistry();
        this.entitySet = new EntitySet();
        this.activeEntities = new LongList(initialEntities);
        this.pendingOperations = new ConcurrentLinkedQueue<>();
        this.entityCount = 0;
        this.isUpdating = false;
        
        // Create special entities
        createSpecialEntities();
        
        if (config.debugMode) {
            System.out.println("[World] Initialized with capacity for " + initialEntities + " entities");
        }
    }
    
    /**
     * Creates special entities for world state.
     */
    private void createSpecialEntities() {
        // Create world root entity (hidden root of scene hierarchy)
        Entity root = createEntityInternal();
        addComponent(root, new NameComponent("World Root"));
        addComponent(root, new ActiveComponent(true));
    }
    
    // ==================== Entity Management ====================
    
    /**
     * Creates a new entity in the world.
     * 
     * @return The new entity
     */
    public Entity createEntity() {
        Entity entity = createEntityInternal();
        
        // Add default components
        addComponent(entity, new NameComponent("Entity " + entity.getIndex()));
        addComponent(entity, new ActiveComponent(true));
        addComponent(entity, new LifetimeComponent());
        
        // Queue creation operation for thread safety
        pendingOperations.offer(new EntityOperation(
            EntityOperation.Type.CREATE, entity
        ));
        
        // Dispatch creation signal
        signalRegistry.dispatch(JavaBlocksEngine.EngineSignals.ENTITY_CREATED, entity);
        
        return entity;
    }
    
    /**
     * Internal entity creation without default components.
     * 
     * @return The new entity
     */
    private Entity createEntityInternal() {
        int index = entityPool.obtain();
        int generation = entityPool.getGeneration(index);
        Entity entity = Entity.create(index, generation);
        
        entitySet.add(entity);
        activeEntities.add(Entity.pack(index, generation));
        entityCount++;
        
        return entity;
    }
    
    /**
     * Destroys an entity and all its components.
     * 
     * @param entity The entity to destroy
     */
    public void destroyEntity(Entity entity) {
        if (!entity.isValid()) {
            return;
        }
        
        // Queue destruction operation
        pendingOperations.offer(new EntityOperation(
            EntityOperation.Type.DESTROY, entity
        ));
    }
    
    /**
     * Internal entity destruction.
     * 
     * @param entity The entity to destroy
     */
    private void destroyEntityInternal(Entity entity) {
        // Remove all components
        componentManager.removeAllComponents(entity);
        
        // Remove from entity set
        entitySet.remove(entity);
        activeEntities.removeValue(Entity.pack(entity.getIndex(), entity.getGeneration()));
        
        // Release entity ID
        entityPool.release(entity.getIndex());
        entityCount--;
        
        // Dispatch destruction signal
        signalRegistry.dispatch(JavaBlocksEngine.EngineSignals.ENTITY_DESTROYED, entity);
    }
    
    /**
     * Checks if an entity is valid (exists in this world).
     * 
     * @param entity The entity to check
     * @return true if the entity exists in this world
     */
    public boolean isValid(Entity entity) {
        if (entity.isNull()) {
            return false;
        }
        
        // Check if the entity exists in our pool with matching generation
        int currentGeneration = entityPool.getGeneration(entity.getIndex());
        return currentGeneration == entity.getGeneration() && entitySet.contains(entity);
    }
    
    // ==================== Component Management ====================
    
    /**
     * Adds a component to an entity.
     * 
     * @param entity The entity to add the component to
     * @param component The component to add
     * @param <T> The component type
     * @return The component that was added
     */
    public <T extends Component> T addComponent(Entity entity, T component) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(component, "Component cannot be null");
        
        if (!entity.isValid()) {
            throw new IllegalArgumentException("Invalid entity: " + entity);
        }
        
        // Store the component
        componentManager.setComponent(entity, component);
        
        // Queue the operation
        pendingOperations.offer(new EntityOperation(
            EntityOperation.Type.ADD_COMPONENT, entity, component
        ));
        
        return component;
    }
    
    /**
     * Removes a component from an entity.
     * 
     * @param entity The entity to remove the component from
     * @param componentClass The class of the component to remove
     * @return true if the component was removed
     */
    public boolean removeComponent(Entity entity, Class<? extends Component> componentClass) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(componentClass, "Component class cannot be null");
        
        Component component = componentManager.removeComponent(entity, componentClass);
        
        if (component != null) {
            pendingOperations.offer(new EntityOperation(
                EntityOperation.Type.REMOVE_COMPONENT, entity, componentClass
            ));
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets a component from an entity.
     * 
     * @param entity The entity to get the component from
     * @param componentClass The class of the component to get
     * @param <T> The component type
     * @return The component, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Entity entity, Class<T> componentClass) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(componentClass, "Component class cannot be null");
        
        return (T) componentManager.getComponent(entity, componentClass);
    }
    
    /**
     * Checks if an entity has a specific component.
     * 
     * @param entity The entity to check
     * @param componentClass The component class to check for
     * @return true if the entity has the component
     */
    public boolean hasComponent(Entity entity, Class<? extends Component> componentClass) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        Objects.requireNonNull(componentClass, "Component class cannot be null");
        
        return componentManager.hasComponent(entity, componentClass);
    }
    
    /**
     * Gets all components attached to an entity.
     * 
     * @param entity The entity to get components from
     * @return An iterable of all components on the entity
     */
    public Iterable<Component> getComponents(Entity entity) {
        return componentManager.getComponents(entity);
    }
    
    /**
     * Gets the number of components on an entity.
     * 
     * @param entity The entity to check
     * @return The number of components
     */
    public int getComponentCount(Entity entity) {
        return componentManager.getComponentCount(entity);
    }
    
    // ==================== System Management ====================
    
    /**
     * Adds a system to the world.
     * 
     * @param system The system to add
     */
    public void addSystem(GameSystem system) {
        systemManager.addSystem(system);
    }
    
    /**
     * Removes a system from the world.
     * 
     * @param systemClass The class of the system to remove
     * @return true if the system was found and removed
     */
    public boolean removeSystem(Class<? extends GameSystem> systemClass) {
        return systemManager.removeSystem(systemClass);
    }
    
    /**
     * Gets a system by class.
     * 
     * @param systemClass The class of the system to get
     * @param <T> The system type
     * @return The system instance, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends GameSystem> T getSystem(Class<T> systemClass) {
        return (T) systemManager.getSystem(systemClass);
    }
    
    /**
     * Gets all systems in the world.
     * 
     * @return A list of all systems
     */
    public List<GameSystem> getSystems() {
        return systemManager.getSystems();
    }
    
    /**
     * Gets the number of systems in the world.
     * 
     * @return The system count
     */
    public int getSystemCount() {
        return systemManager.getSystemCount();
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Gets all entities that have a specific set of components.
     * 
     * @param componentClasses The component classes to match
     * @return A collection of matching entities
     */
    public Collection<Entity> getEntitiesWith(Class<? extends Component>... componentClasses) {
        return entitySet.getEntitiesWith(componentClasses);
    }
    
    /**
     * Gets all active entities in the world.
     * 
     * @return A list of all active entities
     */
    public List<Entity> getActiveEntities() {
        List<Entity> entities = new ArrayList<>(activeEntities.size());
        for (int i = 0; i < activeEntities.size(); i++) {
            long packed = activeEntities.get(i);
            entities.add(Entity.create(Entity.unpackIndex(packed), Entity.unpackGeneration(packed)));
        }
        return entities;
    }
    
    /**
     * Gets the number of active entities.
     * 
     * @return The entity count
     */
    public int getEntityCount() {
        return entityCount;
    }
    
    // ==================== Update Loop ====================
    
    /**
     * Updates all systems in the world.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        if (disposed) {
            return;
        }
        
        isUpdating = true;
        
        try {
            // Process pending operations
            processPendingOperations();
            
            // Update all systems
            systemManager.update(deltaTime);
            
        } finally {
            isUpdating = false;
        }
    }
    
    /**
     * Fixed timestep update for physics and time-critical systems.
     * 
     * @param fixedDelta Fixed timestep value
     */
    public void fixedUpdate(float fixedDelta) {
        if (disposed) {
            return;
        }
        
        systemManager.fixedUpdate(fixedDelta);
    }
    
    /**
     * Processes all pending entity operations.
     * This is called at the end of each update.
     */
    private void processPendingOperations() {
        EntityOperation operation;
        while ((operation = pendingOperations.poll()) != null) {
            switch (operation.type) {
                case DESTROY:
                    destroyEntityInternal(operation.entity);
                    break;
                case ADD_COMPONENT:
                    // Component already added to manager
                    break;
                case REMOVE_COMPONENT:
                    // Component already removed from manager
                    break;
                case CREATE:
                    // Entity already created
                    break;
            }
        }
    }
    
    // ==================== Lifecycle ====================
    
    /**
     * Disposes the world and releases all resources.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        
        disposed = true;
        
        // Destroy all entities
        List<Entity> entities = getActiveEntities();
        for (Entity entity : entities) {
            destroyEntityInternal(entity);
        }
        
        // Dispose systems
        systemManager.dispose();
        
        // Clear component manager
        componentManager.clear();
        
        if (config.debugMode) {
            System.out.println("[World] Disposed. Final entity count: " + entityCount);
        }
    }
    
    /**
     * Checks if the world has been disposed.
     * 
     * @return true if disposed
     */
    public boolean isDisposed() {
        return disposed;
    }
    
    // ==================== Signal Access ====================
    
    /**
     * Gets the signal registry for this world.
     * 
     * @return The signal registry
     */
    public SignalRegistry getSignalRegistry() {
        return signalRegistry;
    }
    
    /**
     * Gets a signal for event communication.
     * 
     * @param signalType The type of signal
     * @param <T> The event type
     * @return The signal instance
     */
    @SuppressWarnings("unchecked")
    public <T> Signal<T> getSignal(Class<?> signalType) {
        return signalRegistry.getSignal(signalType);
    }
    
    // ==================== Debug Information ====================
    
    /**
     * Gets debug information about the world state.
     * 
     * @return A map of debug information
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("Entity Count", entityCount);
        info.put("Entity Capacity", entityPool.capacity());
        info.put("System Count", systemManager.getSystemCount());
        info.put("Component Types", componentManager.getComponentTypeCount());
        info.put("Pending Operations", pendingOperations.size());
        info.put("Is Updating", isUpdating);
        info.put("Is Disposed", disposed);
        return info;
    }
    
    /**
     * Prints debug information to the console.
     */
    public void printDebugInfo() {
        if (!config.debugMode) {
            return;
        }
        
        System.out.println("\n=== World Debug Info ===");
        getDebugInfo().forEach((key, value) -> 
            System.out.println(String.format("  %-25s: %s", key, value)));
        componentManager.printComponentStats();
        System.out.println("========================\n");
    }
    
    // ==================== Component Manager ====================
    
    /**
     * Internal component manager for efficient component storage.
     */
    private static final class ComponentManager {
        private static final int INITIAL_POOL_SIZE = 1000;
        private static final int GROWTH_FACTOR = 2;
        
        private final SparseArray<ComponentPool<?>> componentPools;
        
        ComponentManager(int initialPoolCount) {
            this.componentPools = new SparseArray<>(initialPoolCount);
        }
        
        <T extends Component> void setComponent(Entity entity, T component) {
            int typeId = component.getTypeId();
            
            ComponentPool<T> pool = getOrCreatePool(typeId, component.getClass());
            pool.set(entity.getIndex(), component);
        }
        
        @SuppressWarnings("unchecked")
        <T extends Component> T getComponent(Entity entity, Class<T> componentClass) {
            int typeId = ComponentRegistry.getTypeId(componentClass);
            ComponentPool<T> pool = (ComponentPool<T>) componentPools.get(typeId);
            
            if (pool == null) {
                return null;
            }
            
            return pool.get(entity.getIndex());
        }
        
        @SuppressWarnings("unchecked")
        <T extends Component> T removeComponent(Entity entity, Class<T> componentClass) {
            int typeId = ComponentRegistry.getTypeId(componentClass);
            ComponentPool<T> pool = (ComponentPool<T>) componentPools.get(typeId);
            
            if (pool == null) {
                return null;
            }
            
            return pool.remove(entity.getIndex());
        }
        
        boolean hasComponent(Entity entity, Class<? extends Component> componentClass) {
            int typeId = ComponentRegistry.getTypeId(componentClass);
            ComponentPool<?> pool = componentPools.get(typeId);
            
            if (pool == null) {
                return false;
            }
            
            return pool.has(entity.getIndex());
        }
        
        void removeAllComponents(Entity entity) {
            for (int i = 0; i < componentPools.size(); i++) {
                ComponentPool<?> pool = componentPools.valueAt(i);
                pool.remove(entity.getIndex());
            }
        }
        
        Iterable<Component> getComponents(Entity entity) {
            List<Component> components = new ArrayList<>();
            
            for (int i = 0; i < componentPools.size(); i++) {
                ComponentPool<?> pool = componentPools.valueAt(i);
                Component component = pool.get(entity.getIndex());
                if (component != null) {
                    components.add(component);
                }
            }
            
            return components;
        }
        
        int getComponentCount(Entity entity) {
            int count = 0;
            
            for (int i = 0; i < componentPools.size(); i++) {
                ComponentPool<?> pool = componentPools.valueAt(i);
                if (pool.has(entity.getIndex())) {
                    count++;
                }
            }
            
            return count;
        }
        
        @SuppressWarnings("unchecked")
        private <T extends Component> ComponentPool<T> getOrCreatePool(
                int typeId, Class<? extends Component> componentClass) {
            ComponentPool<?> existing = componentPools.get(typeId);
            
            if (existing != null) {
                return (ComponentPool<T>) existing;
            }
            
            ComponentPool<T> pool = new ComponentPool<>(INITIAL_POOL_SIZE);
            componentPools.put(typeId, pool);
            
            return pool;
        }
        
        void clear() {
            componentPools.clear();
        }
        
        int getComponentTypeCount() {
            return componentPools.size();
        }
        
        void printComponentStats() {
            System.out.println("  Component Pools:");
            for (int i = 0; i < componentPools.size(); i++) {
                int typeId = componentPools.keyAt(i);
                ComponentPool<?> pool = componentPools.valueAt(i);
                Class<?> componentClass = ComponentRegistry.getClassOrNull(typeId);
                String name = componentClass != null ? componentClass.getSimpleName() : "Unknown";
                System.out.println(String.format("    %-30s: %d active", 
                    name + " (ID: " + typeId + ")", pool.getActiveCount()));
            }
        }
        
        // ==================== Component Pool ====================
        
        private static final class ComponentPool<T extends Component> {
            private static final float GROWTH_FACTOR = 1.5f;
            
            private T[] components;
            private int[] generations;
            private int size;
            private int capacity;
            
            @SuppressWarnings("unchecked")
            ComponentPool(int initialCapacity) {
                this.capacity = initialCapacity;
                this.components = (T[]) new Component[initialCapacity];
                this.generations = new int[initialCapacity];
                this.size = 0;
            }
            
            void set(int index, T component) {
                ensureCapacity(index);
                
                T oldComponent = components[index];
                if (oldComponent == null) {
                    size++;
                }
                
                components[index] = component;
                generations[index]++;
            }
            
            T get(int index) {
                if (index < 0 || index >= components.length) {
                    return null;
                }
                return components[index];
            }
            
            T remove(int index) {
                if (index < 0 || index >= components.length) {
                    return null;
                }
                
                T component = components[index];
                if (component != null) {
                    components[index] = null;
                    size--;
                }
                
                return component;
            }
            
            boolean has(int index) {
                return index >= 0 && index < components.length && components[index] != null;
            }
            
            int getActiveCount() {
                return size;
            }
            
            private void ensureCapacity(int index) {
                if (index < capacity) {
                    return;
                }
                
                @SuppressWarnings("unchecked")
                T[] newComponents = (T[]) new Component[(int)(capacity * GROWTH_FACTOR)];
                int[] newGenerations = new int[(int)(capacity * GROWTH_FACTOR)];
                
                System.arraycopy(components, 0, newComponents, 0, capacity);
                System.arraycopy(generations, 0, newGenerations, 0, capacity);
                
                components = newComponents;
                generations = newGenerations;
                capacity = components.length;
            }
        }
    }
    
    // ==================== System Manager ====================
    
    /**
     * Internal system manager for system execution.
     */
    private static final class SystemManager {
        private final PriorityQueue<GameSystem> systems;
        private final HashMap<Class<? extends GameSystem>, GameSystem> systemMap;
        private final ArrayList<GameSystem> updateList;
        
        SystemManager() {
            this.systems = new PriorityQueue<>(
                (a, b) -> Integer.compare(a.getPriority(), b.getPriority())
            );
            this.systemMap = new HashMap<>();
            this.updateList = new ArrayList<>();
        }
        
        void addSystem(GameSystem system) {
            if (systemMap.containsKey(system.getClass())) {
                throw new IllegalArgumentException(
                    "System already registered: " + system.getClass().getName()
                );
            }
            
            systems.offer(system);
            systemMap.put(system.getClass(), system);
            rebuildUpdateList();
        }
        
        boolean removeSystem(Class<? extends GameSystem> systemClass) {
            GameSystem removed = systemMap.remove(systemClass);
            if (removed != null) {
                systems.remove(removed);
                rebuildUpdateList();
                return true;
            }
            return false;
        }
        
        GameSystem getSystem(Class<? extends GameSystem> systemClass) {
            return systemMap.get(systemClass);
        }
        
        List<GameSystem> getSystems() {
            return Collections.unmodifiableList(updateList);
        }
        
        int getSystemCount() {
            return systemMap.size();
        }
        
        void update(float deltaTime) {
            for (GameSystem system : updateList) {
                if (system.isEnabled()) {
                    system.update(deltaTime);
                }
            }
        }
        
        void fixedUpdate(float fixedDelta) {
            for (GameSystem system : updateList) {
                if (system.isEnabled()) {
                    system.fixedUpdate(fixedDelta);
                }
            }
        }
        
        void dispose() {
            for (GameSystem system : updateList) {
                system.dispose();
            }
            systems.clear();
            systemMap.clear();
            updateList.clear();
        }
        
        private void rebuildUpdateList() {
            updateList.clear();
            updateList.addAll(systems);
        }
    }
    
    // ==================== Entity Set ====================
    
    /**
     * Efficient data structure for tracking entities with specific components.
     */
    private static final class EntitySet {
        private final LongHashSet entities;
        private final LongHashSet[] componentMasks;
        private final int componentCount;
        
        @SuppressWarnings("unchecked")
        EntitySet() {
            this.entities = new LongHashSet();
            this.componentCount = ComponentRegistry.getRegisteredCount();
            this.componentMasks = new LongHashSet[componentCount];
            
            for (int i = 0; i < componentCount; i++) {
                componentMasks[i] = new LongHashSet();
            }
        }
        
        void add(Entity entity) {
            long packed = Entity.pack(entity.getIndex(), entity.getGeneration());
            entities.add(packed);
        }
        
        void remove(Entity entity) {
            long packed = Entity.pack(entity.getIndex(), entity.getGeneration());
            entities.remove(packed);
            
            for (int i = 0; i < componentCount; i++) {
                componentMasks[i].remove(packed);
            }
        }
        
        boolean contains(Entity entity) {
            return entities.contains(Entity.pack(entity.getIndex(), entity.getGeneration()));
        }
        
        Collection<Entity> getEntitiesWith(Class<? extends Component>... componentClasses) {
            LongHashSet result = new LongHashSet(entities);
            
            for (Class<? extends Component> componentClass : componentClasses) {
                int typeId = ComponentRegistry.getTypeId(componentClass);
                result.retainAll(componentMasks[typeId]);
            }
            
            List<Entity> matching = new ArrayList<>(result.size());
            for (long packed : result) {
                matching.add(Entity.create(Entity.unpackIndex(packed), Entity.unpackGeneration(packed)));
            }
            
            return matching;
        }
    }
    
    // ==================== Utility Classes ====================
    
    /**
     * Simple hash set for long values.
     */
    private static final class LongHashSet {
        private static final float LOAD_FACTOR = 0.75f;
        
        private long[] table;
        private int size;
        private int capacity;
        
        LongHashSet() {
            this.capacity = 16;
            this.table = new long[capacity];
            this.size = 0;
            Arrays.fill(table, -1L);
        }
        
        void add(long value) {
            if (size >= capacity * LOAD_FACTOR) {
                rehash();
            }
            
            int index = hash(value) & (capacity - 1);
            while (table[index] != -1L) {
                if (table[index] == value) {
                    return;
                }
                index = (index + 1) & (capacity - 1);
            }
            
            table[index] = value;
            size++;
        }
        
        void remove(long value) {
            int index = hash(value) & (capacity - 1);
            
            while (true) {
                if (table[index] == value) {
                    table[index] = -2L; // Tombstone
                    size--;
                    return;
                }
                if (table[index] == -1L) {
                    return;
                }
                index = (index + 1) & (capacity - 1);
            }
        }
        
        boolean contains(long value) {
            int index = hash(value) & (capacity - 1);
            
            while (table[index] != -1L) {
                if (table[index] == value) {
                    return true;
                }
                if (table[index] == -2L) {
                    return false;
                }
                index = (index + 1) & (capacity - 1);
            }
            
            return false;
        }
        
        void retainAll(LongHashSet other) {
            for (int i = 0; i < capacity; i++) {
                if (table[i] >= 0 && !other.contains(table[i])) {
                    table[i] = -2L;
                    size--;
                }
            }
        }
        
        int size() {
            return size;
        }
        
        private void rehash() {
            long[] oldTable = table;
            capacity *= 2;
            table = new long[capacity];
            Arrays.fill(table, -1L);
            size = 0;
            
            for (long value : oldTable) {
                if (value >= 0) {
                    add(value);
                }
            }
        }
        
        private int hash(long value) {
            return (int)(value ^ (value >>> 32));
        }
    }
    
    /**
     * Simple array list for long values with no boxing.
     */
    private static final class LongList {
        private long[] items;
        private int size;
        
        LongList(int initialCapacity) {
            items = new long[initialCapacity];
        }
        
        void add(long value) {
            ensureCapacity(size + 1);
            items[size++] = value;
        }
        
        void removeValue(long value) {
            for (int i = 0; i < size; i++) {
                if (items[i] == value) {
                    System.arraycopy(items, i + 1, items, i, size - i - 1);
                    size--;
                    return;
                }
            }
        }
        
        long get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException();
            }
            return items[index];
        }
        
        int size() {
            return size;
        }
        
        private void ensureCapacity(int minCapacity) {
            if (minCapacity > items.length) {
                items = Arrays.copyOf(items, Math.max(items.length * 2, minCapacity));
            }
        }
    }
    
    /**
     * Simple stack for int values with no boxing.
     */
    private static final class IntStack {
        private int[] items;
        private int top;
        
        IntStack(int initialCapacity) {
            items = new int[initialCapacity];
            top = 0;
        }
        
        void push(int value) {
            if (top >= items.length) {
                items = Arrays.copyOf(items, items.length * 2);
            }
            items[top++] = value;
        }
        
        int pop() {
            if (top == 0) {
                throw new IllegalStateException("Stack is empty");
            }
            return items[--top];
        }
        
        boolean isEmpty() {
            return top == 0;
        }
        
        int size() {
            return top;
        }
    }
    
    /**
     * LibGDX-style sparse array for integer keys.
     */
    private static final class SparseArray<V> {
        private static final float LOAD_FACTOR = 0.75f;
        
        private int[] keys;
        private V[] values;
        private int size;
        private int capacity;
        
        @SuppressWarnings("unchecked")
        SparseArray(int initialCapacity) {
            capacity = Math.max(16, initialCapacity);
            keys = new int[capacity];
            values = (V[]) new Object[capacity];
            size = 0;
        }
        
        V get(int key) {
            int index = findIndex(key);
            return index >= 0 ? values[index] : null;
        }
        
        void put(int key, V value) {
            int index = findIndex(key);
            if (index >= 0) {
                values[index] = value;
                return;
            }
            
            if (size >= capacity * LOAD_FACTOR) {
                rehash();
            }
            
            keys[size] = key;
            values[size] = value;
            size++;
        }
        
        V remove(int key) {
            int index = findIndex(key);
            if (index < 0) {
                return null;
            }
            
            V value = values[index];
            size--;
            keys[index] = keys[size];
            values[index] = values[size];
            return value;
        }
        
        int size() {
            return size;
        }
        
        int keyAt(int index) {
            return keys[index];
        }
        
        V valueAt(int index) {
            return values[index];
        }
        
        void clear() {
            size = 0;
        }
        
        private int findIndex(int key) {
            for (int i = 0; i < size; i++) {
                if (keys[i] == key) {
                    return i;
                }
            }
            return -1;
        }
        
        @SuppressWarnings("unchecked")
        private void rehash() {
            int oldCapacity = capacity;
            capacity *= 2;
            int[] oldKeys = keys;
            V[] oldValues = values;
            
            keys = new int[capacity];
            values = (V[]) new Object[capacity];
            size = 0;
            
            for (int i = 0; i < oldCapacity; i++) {
                if (oldValues[i] != null) {
                    keys[size] = oldKeys[i];
                    values[size] = oldValues[i];
                    size++;
                }
            }
        }
    }
}
