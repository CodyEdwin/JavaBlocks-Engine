/*
 * JavaBlocks Engine - Component Registry
 * 
 * Registry for component types with automatic ID assignment.
 * Thread-safe for parallel component registration.
 */
package com.javablocks.core.ecs;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.javablocks.core.components.*;

/**
 * Registry that maps component classes to unique type IDs.
 * This enables efficient component storage and lookup in the ECS.
 * 
 * Features:
 * - Automatic ID assignment for new component types
 * - Thread-safe registration
 * - ID validation
 * - Component class lookup
 * 
 * @author JavaBlocks Engine Team
 */
public final class ComponentRegistry {
    
    // ==================== Constants ====================
    
    /** Maximum number of component types. */
    public static final int MAX_COMPONENT_TYPES = 1024;
    
    /** Reserved ID for the base Component interface. */
    public static final int COMPONENT_ID = -1;
    
    // ==================== Static State ====================
    
    /** Map from component class to type ID. */
    private static final ConcurrentHashMap<Class<? extends Component>, Integer> classToId = 
        new ConcurrentHashMap<>();
    
    /** Map from type ID to component class. */
    private static final ConcurrentHashMap<Integer, Class<? extends Component>> idToClass = 
        new ConcurrentHashMap<>();
    
    /** Next available type ID. */
    private static final AtomicInteger nextId = new AtomicInteger(0);
    
    /** Registration lock for initial component registration. */
    private static final Object registrationLock = new Object();
    
    /** Whether the registry has been sealed (no more registrations). */
    private static volatile boolean sealed = false;
    
    // ==================== Static Initialization ====================
    
    static {
        // Register core component types
        registerInternal(TransformComponent.class);
        registerInternal(ChildNodeComponent.class);
        registerInternal(ParentNodeComponent.class);
        registerInternal(NameComponent.class);
        registerInternal(TagComponent.class);
        registerInternal(LifetimeComponent.class);
        registerInternal(ActiveComponent.class);
        registerInternal(VisibleComponent.class);
    }
    
    // ==================== Registration Methods ====================
    
    /**
     * Registers a component class and assigns it a unique ID.
     * Thread-safe for parallel registration.
     * 
     * @param componentClass The component class to register
     * @return The assigned type ID
     * @throws IllegalStateException if the registry is sealed
     * @throws IllegalArgumentException if the component is already registered
     */
    public static int register(Class<? extends Component> componentClass) {
        Objects.requireNonNull(componentClass, "Component class cannot be null");
        
        // Fast path: already registered
        Integer existingId = classToId.get(componentClass);
        if (existingId != null) {
            return existingId;
        }
        
        // Check if sealed
        if (sealed) {
            throw new IllegalStateException(
                "Component registry is sealed. Cannot register new component types."
            );
        }
        
        synchronized (registrationLock) {
            // Double-check inside synchronized block
            existingId = classToId.get(componentClass);
            if (existingId != null) {
                return existingId;
            }
            
            // Check for registration limit
            int id = nextId.getAndIncrement();
            if (id >= MAX_COMPONENT_TYPES) {
                throw new IllegalStateException(
                    "Maximum component types (" + MAX_COMPONENT_TYPES + ") exceeded"
                );
            }
            
            // Register the component
            classToId.put(componentClass, id);
            idToClass.put(id, componentClass);
            
            if (com.javablocks.core.JavaBlocksEngine.get() != null && 
                com.javablocks.core.JavaBlocksEngine.get().getConfiguration().debugMode) {
                System.out.println("[ECS] Registered component: " + 
                    componentClass.getSimpleName() + " (ID: " + id + ")");
            }
            
            return id;
        }
    }
    
    /**
     * Internal registration that doesn't trigger debug output.
     * 
     * @param componentClass The component class to register
     * @return The assigned type ID
     */
    private static int registerInternal(Class<? extends Component> componentClass) {
        Objects.requireNonNull(componentClass, "Component class cannot be null");
        
        Integer existingId = classToId.get(componentClass);
        if (existingId != null) {
            return existingId;
        }
        
        synchronized (registrationLock) {
            existingId = classToId.get(componentClass);
            if (existingId != null) {
                return existingId;
            }
            
            int id = nextId.getAndIncrement();
            if (id >= MAX_COMPONENT_TYPES) {
                throw new IllegalStateException(
                    "Maximum component types (" + MAX_COMPONENT_TYPES + ") exceeded"
                );
            }
            
            classToId.put(componentClass, id);
            idToClass.put(id, componentClass);
            
            return id;
        }
    }
    
    // ==================== Lookup Methods ====================
    
    /**
     * Gets the type ID for a component class.
     * 
     * @param componentClass The component class
     * @return The type ID
     * @throws IllegalArgumentException if the component is not registered
     */
    public static int getTypeId(Class<? extends Component> componentClass) {
        Integer id = classToId.get(componentClass);
        if (id == null) {
            throw new IllegalArgumentException(
                "Component class not registered: " + componentClass.getName()
            );
        }
        return id;
    }
    
    /**
     * Gets the type ID for a component class, returning a default if not registered.
     * 
     * @param componentClass The component class
     * @param defaultId The default ID to return if not registered
     * @return The type ID, or defaultId if not registered
     */
    public static int getTypeIdOrDefault(Class<? extends Component> componentClass, int defaultId) {
        Integer id = classToId.get(componentClass);
        return id != null ? id : defaultId;
    }
    
    /**
     * Checks if a component class is registered.
     * 
     * @param componentClass The component class
     * @return true if registered
     */
    public static boolean isRegistered(Class<? extends Component> componentClass) {
        return classToId.containsKey(componentClass);
    }
    
    /**
     * Gets the component class for a type ID.
     * 
     * @param typeId The type ID
     * @return The component class
     * @throws IllegalArgumentException if the type ID is not registered
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Component> getClass(int typeId) {
        Class<? extends Component> clazz = idToClass.get(typeId);
        if (clazz == null) {
            throw new IllegalArgumentException(
                "No component class registered for type ID: " + typeId
            );
        }
        return clazz;
    }
    
    /**
     * Gets the component class for a type ID, returning null if not found.
     * 
     * @param typeId The type ID
     * @return The component class, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Component> getClassOrNull(int typeId) {
        return idToClass.get(typeId);
    }
    
    // ==================== Registry Management ====================
    
    /**
     * Seals the registry to prevent new component registrations.
     * This should be called after all components are registered.
     */
    public static void seal() {
        sealed = true;
    }
    
    /**
     * Checks if the registry is sealed.
     * 
     * @return true if sealed
     */
    public static boolean isSealed() {
        return sealed;
    }
    
    /**
     * Gets the total number of registered components.
     * 
     * @return The number of registered components
     */
    public static int getRegisteredCount() {
        return classToId.size();
    }
    
    /**
     * Gets all registered component classes.
     * 
     * @return Collection of registered component classes
     */
    public static Collection<Class<? extends Component>> getRegisteredClasses() {
        return Collections.unmodifiableCollection(classToId.keySet());
    }
    
    /**
     * Clears all registered components.
     * WARNING: This should only be used for testing.
     */
    public static void clear() {
        synchronized (registrationLock) {
            classToId.clear();
            idToClass.clear();
            nextId.set(0);
            sealed = false;
        }
    }
    
    // ==================== Component Creation ====================
    
    /**
     * Creates a new instance of a component class.
     * 
     * @param typeId The type ID of the component
     * @return A new component instance
     * @throws RuntimeException if instantiation fails
     */
    @SuppressWarnings("unchecked")
    public static Component create(int typeId) {
        Class<? extends Component> clazz = getClass(typeId);
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create component instance for type ID: " + typeId, e
            );
        }
    }
    
    /**
     * Creates a new instance of a component class.
     * 
     * @param componentClass The component class
     * @return A new component instance
     * @throws RuntimeException if instantiation fails
     */
    public static Component create(Class<? extends Component> componentClass) {
        try {
            return componentClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create component instance: " + componentClass.getName(), e
            );
        }
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Gets a bitmask for a single component type.
     * Useful for component filtering operations.
     * 
     * @param typeId The component type ID
     * @return A bitmask with the type ID bit set
     */
    public static long getTypeMask(int typeId) {
        return 1L << typeId;
    }
    
    /**
     * Gets a bitmask for multiple component types.
     * 
     * @param typeIds The component type IDs
     * @return A bitmask with all type ID bits set
     */
    public static long getTypeMask(int... typeIds) {
        long mask = 0;
        for (int id : typeIds) {
            mask |= 1L << id;
        }
        return mask;
    }
}
