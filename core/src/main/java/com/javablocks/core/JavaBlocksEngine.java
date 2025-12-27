/*
 * JavaBlocks Engine - Core Module
 * 
 * This is the central hub of the JavaBlocks game engine, containing all shared
 * engine logic including ECS, node hierarchies, physics, audio, rendering interfaces,
 * and networking utilities. All other modules depend on this module.
 * 
 * License: MIT License
 */
package com.javablocks.core;

import com.javablocks.core.components.*;
import com.javablocks.core.ecs.*;
import com.javablocks.core.events.*;
import com.javablocks.core.math.*;
import com.javablocks.core.scene.*;
import com.javablocks.core.plugin.*;
import com.javablocks.core.resource.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * The main entry point and central coordinator for the JavaBlocks engine.
 * This class manages the entire engine lifecycle, including initialization,
 * update loops, rendering, and shutdown procedures.
 * 
 * Design Principles:
 * - Pure Java implementation with no native dependencies
 * - Modular architecture for easy extension
 * - Thread-safe operations where required
 * - Zero-GC allocation in hot paths
 * 
 * @author JavaBlocks Engine Team
 * @version 1.0.0
 */
public final class JavaBlocksEngine {
    
    // ==================== Singleton Instance ====================
    
    /**
     * Singleton instance of the engine for global access.
     * Using volatile for thread-safe lazy initialization.
     */
    private static volatile JavaBlocksEngine instance;
    
    /**
     * Lock object for synchronization during initialization.
     */
    private static final Object LOCK = new Object();
    
    // ==================== Engine State ====================
    
    /**
     * Current state of the engine lifecycle.
     */
    private EngineState state = EngineState.CREATED;
    
    /**
     * Engine configuration loaded at startup.
     */
    private EngineConfiguration configuration;
    
    // ==================== Core Subsystems ====================
    
    /**
     * The world containing all entities, components, and systems.
     * This is the heart of the ECS architecture.
     */
    private World world;
    
    /**
     * Scene graph manager for node hierarchies.
     */
    private SceneManager sceneManager;
    
    /**
     * Signal registry for event communication.
     */
    private SignalRegistry signalRegistry;
    
    /**
     * Resource manager for asset loading and caching.
     */
    private ResourceManager resourceManager;
    
    /**
     * Plugin manager for extensibility.
     */
    private PluginManager pluginManager;
    
    // ==================== Threading ====================
    
    /**
     * Virtual thread executor for I/O operations (Java 21 feature).
     * Virtual threads are lightweight threads that can be created in millions.
     */
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    /**
     * Fork-join pool for parallel game logic processing.
     */
    private final ForkJoinPool gameLogicPool = new ForkJoinPool(
        Runtime.getRuntime().availableProcessors(),
        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
        null,
        false
    );
    
    /**
     * Background task scheduler for delayed operations.
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 4)
    );
    
    // ==================== Lifecycle Timestamps ====================
    
    /**
     * Time when the engine was created (nanoseconds).
     */
    private long creationTime;
    
    /**
     * Time when the engine was initialized (nanoseconds).
     */
    private long initializationTime;
    
    /**
     * Total running time in nanoseconds.
     */
    private long totalRunningTime;
    
    // ==================== Engine Configuration ====================
    
    /**
     * Engine configuration holder class.
     */
    public static final class EngineConfiguration {
        /** Whether to run in headless mode (no rendering). */
        public boolean headless = false;
        
        /** Target FPS for the game loop. */
        public int targetFPS = 60;
        
        /** Whether to enable debug mode with additional logging. */
        public boolean debugMode = false;
        
        /** Maximum number of entities the engine can handle. */
        public int maxEntities = 100000;
        
        /** Initial capacity for entity pools. */
        public int initialEntityPoolSize = 10000;
        
        /** Whether to enable hot-reloading for development. */
        public boolean hotReloadEnabled = false;
        
        /** Physics simulation timestep in seconds. */
        public float physicsTimestep = 1f / 60f;
        
        /** Whether to enable multi-threaded rendering. */
        public boolean multiThreadedRendering = false;
        
        /** Audio buffer size for streaming. */
        public int audioBufferSize = 512;
        
        /** Network tick rate in Hz. */
        public int networkTickRate = 20;
        
        /**
         * Create a default configuration.
         */
        public EngineConfiguration() {}
        
        /**
         * Create a configuration with custom settings.
         */
        public EngineConfiguration(boolean headless, int targetFPS, boolean debugMode) {
            this.headless = headless;
            this.targetFPS = targetFPS;
            this.debugMode = debugMode;
        }
    }
    
    // ==================== Lifecycle States ====================
    
    /**
     * Enumeration of possible engine lifecycle states.
     */
    public enum EngineState {
        /** Engine object created but not initialized. */
        CREATED,
        
        /** Engine is initializing subsystems. */
        INITIALIZING,
        
        /** Engine is fully initialized and running. */
        RUNNING,
        
        /** Engine is paused (e.g., game lost focus). */
        PAUSED,
        
        /** Engine is being shutdown. */
        SHUTTING_DOWN,
        
        /** Engine has been fully shutdown. */
        TERMINATED
    }
    
    // ==================== Singleton Access ====================
    
    /**
     * Gets the singleton instance of the engine.
     * Thread-safe lazy initialization pattern.
     * 
     * @return The singleton JavaBlocksEngine instance
     * @throws IllegalStateException if the engine hasn't been created yet
     */
    public static JavaBlocksEngine get() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    throw new IllegalStateException(
                        "JavaBlocksEngine has not been created. Call create() first."
                    );
                }
            }
        }
        return instance;
    }
    
    /**
     * Creates the singleton instance of the engine.
     * This must be called before any other engine operations.
     * 
     * @param config Optional configuration for the engine
     * @return The created JavaBlocksEngine instance
     */
    public static JavaBlocksEngine create(EngineConfiguration config) {
        synchronized (LOCK) {
            if (instance != null) {
                throw new IllegalStateException("JavaBlocksEngine already created");
            }
            instance = new JavaBlocksEngine(config);
            return instance;
        }
    }
    
    /**
     * Creates the engine with default configuration.
     * 
     * @return The created JavaBlocksEngine instance
     */
    public static JavaBlocksEngine create() {
        return create(new EngineConfiguration());
    }
    
    // ==================== Private Constructor ====================
    
    /**
     * Private constructor to enforce singleton pattern.
     * Use JavaBlocksEngine.create() factory methods instead.
     * 
     * @param config The engine configuration to use
     */
    private JavaBlocksEngine(EngineConfiguration config) {
        this.configuration = Objects.requireNonNull(config, "Configuration cannot be null");
        this.creationTime = System.nanoTime();
        this.state = EngineState.CREATED;
        
        // Validate configuration
        validateConfiguration();
        
        // Initialize core subsystems
        initializeSubsystems();
    }
    
    // ==================== Initialization ====================
    
    /**
     * Validates the engine configuration for any invalid settings.
     */
    private void validateConfiguration() {
        if (configuration.targetFPS <= 0) {
            throw new IllegalArgumentException("Target FPS must be positive");
        }
        if (configuration.maxEntities <= 0) {
            throw new IllegalArgumentException("Max entities must be positive");
        }
        if (configuration.physicsTimestep <= 0) {
            throw new IllegalArgumentException("Physics timestep must be positive");
        }
    }
    
    /**
     * Initializes all core subsystems in the correct order.
     * Subsystems must be initialized in dependency order.
     */
    private void initializeSubsystems() {
        state = EngineState.INITIALIZING;
        long startTime = System.nanoTime();
        
        try {
            // Phase 1: Core infrastructure
            this.signalRegistry = new SignalRegistry();
            this.resourceManager = new ResourceManager();
            this.pluginManager = new PluginManager();
            
            // Phase 2: ECS and Scene
            this.world = new World(configuration);
            this.sceneManager = new SceneManager();
            
            // Phase 3: Register engine-level signals
            registerEngineSignals();
            
            // Phase 4: Load plugins if enabled
            if (!configuration.hotReloadEnabled) {
                pluginManager.loadPlugins();
            }
            
            // Phase 5: Create default scene
            sceneManager.createDefaultScene();
            
            initializationTime = System.nanoTime() - startTime;
            state = EngineState.RUNNING;
            
            if (configuration.debugMode) {
                System.out.println("[JavaBlocks] Engine initialized in " + 
                    (initializationTime / 1_000_000.0) + "ms");
            }
            
        } catch (Exception e) {
            state = EngineState.TERMINATED;
            throw new RuntimeException("Failed to initialize engine subsystems", e);
        }
    }
    
    /**
     * Registers all built-in engine signals.
     */
    private void registerEngineSignals() {
        // Core engine signals
        signalRegistry.register(EngineSignals.ENGINE_STARTED);
        signalRegistry.register(EngineSignals.ENGINE_UPDATE);
        signalRegistry.register(EngineSignals.ENGINE_PAUSED);
        signalRegistry.register(EngineSignals.ENGINE_RESUMED);
        signalRegistry.register(EngineSignals.ENGINE_STOPPED);
        signalRegistry.register(EngineSignals.ENTITY_CREATED);
        signalRegistry.register(EngineSignals.ENTITY_DESTROYED);
        signalRegistry.register(EngineSignals.SCENE_CHANGED);
    }
    
    // ==================== Update Loop ====================
    
    /**
     * Main update method called each frame.
     * Updates all systems in priority order.
     * 
     * @param deltaTime Time since last frame in seconds
     */
    public void update(float deltaTime) {
        if (state != EngineState.RUNNING && state != EngineState.PAUSED) {
            return;
        }
        
        // Update total running time
        totalRunningTime += (long)(deltaTime * 1_000_000_000);
        
        // Dispatch update signal
        signalRegistry.dispatch(EngineSignals.ENGINE_UPDATE, new UpdateEvent(deltaTime, totalRunningTime));
        
        // Update ECS world
        world.update(deltaTime);
        
        // Update scene manager
        sceneManager.update(deltaTime);
    }
    
    /**
     * Fixed timestep update for physics and other time-critical systems.
     * This ensures deterministic simulation regardless of frame rate.
     * 
     * @param fixedDelta Fixed timestep value
     */
    public void fixedUpdate(float fixedDelta) {
        world.fixedUpdate(fixedDelta);
    }
    
    /**
     * Late update for post-processing and final frame preparations.
     * 
     * @param deltaTime Time since last frame in seconds
     */
    public void lateUpdate(float deltaTime) {
        // Handle any pending operations
        processPendingOperations();
    }
    
    // ==================== Rendering ====================
    
    /**
     * Main rendering method called each frame.
     * Implementations should override this for actual rendering.
     * 
     * @param deltaTime Time since last frame in seconds
     */
    public void render(float deltaTime) {
        // Override in platform-specific implementations
    }
    
    // ==================== Lifecycle Management ====================
    
    /**
     * Pauses the engine, typically when the application loses focus.
     */
    public void pause() {
        if (state == EngineState.RUNNING) {
            state = EngineState.PAUSED;
            signalRegistry.dispatch(EngineSignals.ENGINE_PAUSED, null);
            
            if (configuration.debugMode) {
                System.out.println("[JavaBlocks] Engine paused");
            }
        }
    }
    
    /**
     * Resumes the engine after being paused.
     */
    public void resume() {
        if (state == EngineState.PAUSED) {
            state = EngineState.RUNNING;
            signalRegistry.dispatch(EngineSignals.ENGINE_RESUMED, null);
            
            if (configuration.debugMode) {
                System.out.println("[JavaBlocks] Engine resumed");
            }
        }
    }
    
    /**
     * Shuts down the engine and releases all resources.
     * This method blocks until all resources are released.
     */
    public void shutdown() {
        if (state == EngineState.SHUTTING_DOWN || state == EngineState.TERMINATED) {
            return;
        }
        
        state = EngineState.SHUTTING_DOWN;
        
        try {
            // Dispatch shutdown signal
            signalRegistry.dispatch(EngineSignals.ENGINE_STOPPED, null);
            
            // Shutdown subsystems in reverse order
            sceneManager.dispose();
            world.dispose();
            pluginManager.unloadPlugins();
            resourceManager.dispose();
            signalRegistry.dispose();
            
            // Shutdown executors
            scheduler.shutdown();
            gameLogicPool.shutdown();
            ioExecutor.shutdown();
            
            // Wait for executors to finish
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!gameLogicPool.awaitTermination(5, TimeUnit.SECONDS)) {
                gameLogicPool.shutdownNow();
            }
            if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
            
            state = EngineState.TERMINATED;
            
            if (configuration.debugMode) {
                System.out.println("[JavaBlocks] Engine shutdown complete. Total running time: " + 
                    (totalRunningTime / 1_000_000_000.0) + "s");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            state = EngineState.TERMINATED;
            throw new RuntimeException("Engine shutdown interrupted", e);
        }
    }
    
    // ==================== Entity Management ====================
    
    /**
     * Creates a new entity in the current scene.
     * 
     * @return The created entity
     */
    public Entity createEntity() {
        return world.createEntity();
    }
    
    /**
     * Creates a new entity with a name.
     * 
     * @param name Name for the entity
     * @return The created entity
     */
    public Entity createEntity(String name) {
        Entity entity = world.createEntity();
        world.addComponent(entity, new NameComponent(name));
        return entity;
    }
    
    /**
     * Destroys an entity and all its components.
     * 
     * @param entity The entity to destroy
     */
    public void destroyEntity(Entity entity) {
        world.destroyEntity(entity);
    }
    
    // ==================== Component Management ====================
    
    /**
     * Adds a component to an entity.
     * 
     * @param entity The entity to add the component to
     * @param component The component to add
     * @param <T> The component type
     * @return The entity for method chaining
     */
    public <T extends Component> Entity addComponent(Entity entity, T component) {
        world.addComponent(entity, component);
        return entity;
    }
    
    /**
     * Removes a component from an entity.
     * 
     * @param entity The entity to remove the component from
     * @param componentClass The class of the component to remove
     * @return true if the component was removed
     */
    public boolean removeComponent(Entity entity, Class<? extends Component> componentClass) {
        return world.removeComponent(entity, componentClass);
    }
    
    /**
     * Gets a component from an entity.
     * 
     * @param entity The entity to get the component from
     * @param componentClass The class of the component to get
     * @param <T> The component type
     * @return The component if found, null otherwise
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Entity entity, Class<T> componentClass) {
        return world.getComponent(entity, componentClass);
    }
    
    /**
     * Checks if an entity has a specific component.
     * 
     * @param entity The entity to check
     * @param componentClass The component class to check for
     * @return true if the entity has the component
     */
    public boolean hasComponent(Entity entity, Class<? extends Component> componentClass) {
        return world.hasComponent(entity, componentClass);
    }
    
    // ==================== System Management ====================
    
    /**
     * Adds a system to the engine.
     * 
     * @param system The system to add
     */
    public void addSystem(GameSystem system) {
        world.addSystem(system);
    }
    
    /**
     * Removes a system from the engine.
     * 
     * @param systemClass The class of the system to remove
     * @return true if the system was removed
     */
    public boolean removeSystem(Class<? extends GameSystem> systemClass) {
        return world.removeSystem(systemClass);
    }
    
    /**
     * Gets a system by class.
     * 
     * @param systemClass The class of the system to get
     * @param <T> The system type
     * @return The system if found, null otherwise
     */
    @SuppressWarnings("unchecked")
    public <T extends GameSystem> T getSystem(Class<T> systemClass) {
        return world.getSystem(systemClass);
    }
    
    // ==================== Node/Scene Management ====================
    
    /**
     * Creates a new node in the current scene.
     * 
     * @param name Name for the node
     * @return The created node
     */
    public Node createNode(String name) {
        return sceneManager.createNode(name);
    }
    
    /**
     * Adds a node as a child of another node.
     * 
     * @param parent The parent node
     * @param child The child node to add
     */
    public void addChild(Node parent, Node child) {
        sceneManager.addChild(parent, child);
    }
    
    /**
     * Removes a node from its parent.
     * 
     * @param node The node to remove
     * @return The parent node, or null if the node had no parent
     */
    public Node removeChild(Node node) {
        return sceneManager.removeChild(node);
    }
    
    // ==================== Signal/Event System ====================
    
    /**
     * Gets the signal registry for event management.
     * 
     * @return The signal registry
     */
    public SignalRegistry getSignalRegistry() {
        return signalRegistry;
    }
    
    /**
     * Gets a typed signal for event subscription.
     * 
     * @param signalType The type of signal to get
     * @param <T> The event type
     * @return The signal instance
     */
    @SuppressWarnings("unchecked")
    public <T> Signal<T> getSignal(Class<?> signalType) {
        return signalRegistry.getSignal(signalType);
    }
    
    /**
     * Connects a listener to a signal.
     * 
     * @param signalType The signal type to connect to
     * @param listener The listener to connect
     * @param <T> The event type
     */
    @SuppressWarnings("unchecked")
    public <T> void connect(Class<?> signalType, Consumer<T> listener) {
        Signal<T> signal = (Signal<T>) signalRegistry.getSignal(signalType);
        if (signal != null) {
            signal.subscribe(listener);
        }
    }
    
    /**
     * Disconnects a listener from a signal.
     * 
     * @param signalType The signal type to disconnect from
     * @param listener The listener to disconnect
     * @param <T> The event type
     */
    @SuppressWarnings("unchecked")
    public <T> void disconnect(Class<?> signalType, Consumer<T> listener) {
        Signal<T> signal = (Signal<T>) signalRegistry.getSignal(signalType);
        if (signal != null) {
            signal.unsubscribe(listener);
        }
    }
    
    // ==================== Resource Management ====================
    
    /**
     * Gets the resource manager for asset loading.
     * 
     * @return The resource manager
     */
    public ResourceManager getResourceManager() {
        return resourceManager;
    }
    
    /**
     * Loads a resource asynchronously.
     * 
     * @param path Path to the resource
     * @param type Expected type of the resource
     * @param <T> The resource type
     * @return A future containing the loaded resource
     */
    public <T extends Resource> CompletableFuture<T> loadAsync(String path, Class<T> type) {
        return resourceManager.loadAsync(path, type);
    }
    
    /**
     * Loads a resource synchronously.
     * 
     * @param path Path to the resource
     * @param type Expected type of the resource
     * @param <T> The resource type
     * @return The loaded resource
     */
    public <T extends Resource> T load(String path, Class<T> type) {
        return resourceManager.load(path, type);
    }
    
    // ==================== Plugin Management ====================
    
    /**
     * Gets the plugin manager.
     * 
     * @return The plugin manager
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }
    
    // ==================== Background Operations ====================
    
    /**
     * Submits a task to the I/O executor for asynchronous processing.
     * 
     * @param task The task to execute
     * @return A future for tracking completion
     */
    public CompletableFuture<Void> submitIoTask(Runnable task) {
        return CompletableFuture.runAsync(task, ioExecutor);
    }
    
    /**
     * Submits a task to the game logic pool for parallel processing.
     * 
     * @param task The task to execute
     * @return A future for tracking completion
     */
    public CompletableFuture<Void> submitGameTask(Runnable task) {
        return CompletableFuture.runAsync(task, gameLogicPool);
    }
    
    /**
     * Schedules a task to run after a delay.
     * 
     * @param task The task to schedule
     * @param delay Delay before execution
     * @param unit Time unit for the delay
     * @return A scheduled future
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduler.schedule(task, delay, unit);
    }
    
    /**
     * Schedules a repeating task.
     * 
     * @param task The task to schedule
     * @param initialDelay Delay before first execution
     * @param period Period between executions
     * @param unit Time unit for delays
     * @return A scheduled future
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(task, initialDelay, period, unit);
    }
    
    // ==================== Pending Operations ====================
    
    /**
     * Queue of operations to be processed at the end of the frame.
     * Thread-safe for multi-threaded operation.
     */
    private final Queue<Runnable> pendingOperations = new ConcurrentLinkedQueue<>();
    
    /**
     * Submits an operation to be processed at the end of the current frame.
     * Useful for operations that shouldn't happen during the update loop.
     * 
     * @param operation The operation to queue
     */
    public void submitPendingOperation(Runnable operation) {
        pendingOperations.offer(operation);
    }
    
    /**
     * Processes all pending operations queued for this frame.
     */
    private void processPendingOperations() {
        Runnable operation;
        while ((operation = pendingOperations.poll()) != null) {
            try {
                operation.run();
            } catch (Exception e) {
                if (configuration.debugMode) {
                    System.err.println("[JavaBlocks] Error processing pending operation: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    // ==================== State Queries ====================
    
    /**
     * Gets the current state of the engine.
     * 
     * @return The current engine state
     */
    public EngineState getState() {
        return state;
    }
    
    /**
     * Checks if the engine is running.
     * 
     * @return true if the engine is in RUNNING state
     */
    public boolean isRunning() {
        return state == EngineState.RUNNING;
    }
    
    /**
     * Checks if the engine is paused.
     * 
     * @return true if the engine is in PAUSED state
     */
    public boolean isPaused() {
        return state == EngineState.PAUSED;
    }
    
    /**
     * Checks if the engine has been shut down.
     * 
     * @return true if the engine is in TERMINATED state
     */
    public boolean isTerminated() {
        return state == EngineState.TERMINATED;
    }
    
    /**
     * Gets the time since the engine was created.
     * 
     * @return Time in nanoseconds
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Gets the total running time of the engine.
     * 
     * @return Time in nanoseconds
     */
    public long getTotalRunningTime() {
        return totalRunningTime;
    }
    
    /**
     * Gets the configuration used by this engine.
     * 
     * @return The engine configuration
     */
    public EngineConfiguration getConfiguration() {
        return configuration;
    }
    
    // ==================== Debug Information ====================
    
    /**
     * Gets debug information about the engine state.
     * Useful for debugging and profiling.
     * 
     * @return A map of debug information
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("State", state);
        info.put("Total Running Time (s)", totalRunningTime / 1_000_000_000.0);
        info.put("Entity Count", world.getEntityCount());
        info.put("System Count", world.getSystemCount());
        info.put("Virtual Thread Count", ioExecutor instanceof ThreadPoolExecutor ? 
            ((ThreadPoolExecutor) ioExecutor).getActiveCount() : "N/A");
        info.put("Pending Operations", pendingOperations.size());
        return info;
    }
    
    /**
     * Prints debug information to the console.
     */
    public void printDebugInfo() {
        if (!configuration.debugMode) {
            return;
        }
        
        System.out.println("\n=== JavaBlocks Engine Debug Info ===");
        getDebugInfo().forEach((key, value) -> 
            System.out.println(String.format("  %-25s: %s", key, value)));
        System.out.println("====================================\n");
    }
    
    // ==================== Internal Classes ====================
    
    /**
     * Event data for engine update signals.
     */
    public record UpdateEvent(float deltaTime, long totalTime) {}
    
    /**
     * Built-in engine signals for core events.
     */
    @SuppressWarnings("rawtypes")
    public static final class EngineSignals {
        /** Signal dispatched when the engine starts. */
        public static final Class<Signal> ENGINE_STARTED = Signal.class;
        
        /** Signal dispatched each update frame. */
        public static final Class<Signal> ENGINE_UPDATE = Signal.class;
        
        /** Signal dispatched when the engine pauses. */
        public static final Class<Signal> ENGINE_PAUSED = Signal.class;
        
        /** Signal dispatched when the engine resumes. */
        public static final Class<Signal> ENGINE_RESUMED = Signal.class;
        
        /** Signal dispatched when the engine stops. */
        public static final Class<Signal> ENGINE_STOPPED = Signal.class;
        
        /** Signal dispatched when an entity is created. */
        public static final Class<Signal> ENTITY_CREATED = Signal.class;
        
        /** Signal dispatched when an entity is destroyed. */
        public static final Class<Signal> ENTITY_DESTROYED = Signal.class;
        
        /** Signal dispatched when the scene changes. */
        public static final Class<Signal> SCENE_CHANGED = Signal.class;
        
        private EngineSignals() {} // Prevent instantiation
    }
}
