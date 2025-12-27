/*
 * JavaBlocks Engine - Game System
 * 
 * Base class for all game systems in the ECS architecture.
 * Systems contain the logic that processes entities and their components.
 */
package com.javablocks.core.ecs;

import com.javablocks.core.*;
import java.util.*;

/**
 * Base class for all game systems.
 * 
 * Systems contain the logic that processes entities and their components.
 * They are updated each frame by the World and can be prioritized for
 * execution order.
 * 
 * System Design Principles:
 * - Systems contain logic, components contain data
 * - Systems should be stateless where possible
 * - Use priority to control execution order
 * - Enable/disable systems for conditional processing
 * 
 * Lifecycle Methods:
 * - initialize(): Called when system is added to world
 * - update(): Called each frame with delta time
 * - fixedUpdate(): Called at fixed timestep for physics
 * - render(): Called for rendering operations
 * - dispose(): Called when system is removed or world is disposed
 * 
 * @author JavaBlocks Engine Team
 */
public abstract class GameSystem {
    
    // ==================== Constants ====================
    
    /** Highest priority (processed first). */
    public static final int PRIORITY_HIGHEST = Integer.MIN_VALUE;
    
    /** High priority. */
    public static final int PRIORITY_HIGH = -100;
    
    /** Normal/default priority. */
    public static final int PRIORITY_NORMAL = 0;
    
    /** Low priority. */
    public static final int PRIORITY_LOW = 100;
    
    /** Lowest priority (processed last). */
    public static final int PRIORITY_LOWEST = Integer.MAX_VALUE;
    
    // ==================== Instance Variables ====================
    
    /** The priority of this system. Lower values are processed first. */
    private int priority;
    
    /** Whether this system is enabled. */
    private boolean enabled;
    
    /** Whether this system has been initialized. */
    private boolean initialized;
    
    /** The world this system belongs to. */
    private World world;
    
    /** System name for debugging. */
    private final String name;
    
    /** Execution statistics. */
    private long totalExecutionTime;
    private int executionCount;
    private float lastExecutionTime;
    
    // ==================== Constructor ====================
    
    /**
     * Creates a new system with default priority.
     */
    public GameSystem() {
        this(PRIORITY_NORMAL);
    }
    
    /**
     * Creates a new system with a specific priority.
     * 
     * @param priority The execution priority (lower = earlier)
     */
    public GameSystem(int priority) {
        this.priority = priority;
        this.enabled = true;
        this.initialized = false;
        this.world = null;
        this.name = getClass().getSimpleName();
        this.totalExecutionTime = 0;
        this.executionCount = 0;
        this.lastExecutionTime = 0;
    }
    
    // ==================== Lifecycle ====================
    
    /**
     * Called when the system is added to a world.
     * Override to perform initialization.
     * 
     * @param world The world this system was added to
     */
    protected void initialize(World world) {
        this.world = world;
        this.initialized = true;
    }
    
    /**
     * Called each frame to update the system.
     * Override to implement system logic.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        long startTime = System.nanoTime();
        
        try {
            onUpdate(deltaTime);
        } finally {
            long endTime = System.nanoTime();
            long executionTime = endTime - startTime;
            
            totalExecutionTime += executionTime;
            executionCount++;
            lastExecutionTime = executionTime / 1_000_000f;
        }
    }
    
    /**
     * Internal update method for subclasses to override.
     * 
     * @param deltaTime Time since last update in seconds
     */
    protected void onUpdate(float deltaTime) {
        // Override in subclasses
    }
    
    /**
     * Called at fixed timestep for time-critical systems like physics.
     * Override for systems requiring deterministic updates.
     * 
     * @param fixedDelta Fixed timestep value
     */
    public void fixedUpdate(float fixedDelta) {
        onFixedUpdate(fixedDelta);
    }
    
    /**
     * Internal fixed update method for subclasses to override.
     * 
     * @param fixedDelta Fixed timestep value
     */
    protected void onFixedUpdate(float fixedDelta) {
        // Override in subclasses
    }
    
    /**
     * Called for rendering operations.
     * Override for systems that need to render something.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void render(float deltaTime) {
        onRender(deltaTime);
    }
    
    /**
     * Internal render method for subclasses to override.
     * 
     * @param deltaTime Time since last update in seconds
     */
    protected void onRender(float deltaTime) {
        // Override in subclasses
    }
    
    /**
     * Called when the system is removed from the world.
     * Override to clean up resources.
     */
    public void dispose() {
        onDispose();
        this.world = null;
        this.initialized = false;
    }
    
    /**
     * Internal dispose method for subclasses to override.
     */
    protected void onDispose() {
        // Override in subclasses
    }
    
    // ==================== Priority ====================
    
    /**
     * Gets the priority of this system.
     * 
     * @return The priority value
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Sets the priority of this system.
     * 
     * @param priority The new priority value
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    // ==================== Enabled State ====================
    
    /**
     * Checks if this system is enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Enables or disables this system.
     * 
     * @param enabled The new enabled state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    // ==================== World Access ====================
    
    /**
     * Gets the world this system belongs to.
     * 
     * @return The world, or null if not added to a world
     */
    public World getWorld() {
        return world;
    }
    
    /**
     * Gets the engine instance from the world.
     * 
     * @return The engine instance
     * @throws IllegalStateException if not added to a world
     */
    protected JavaBlocksEngine getEngine() {
        if (world == null) {
            throw new IllegalStateException(
                "System is not attached to a world. Call addSystem() on World first."
            );
        }
        return JavaBlocksEngine.get();
    }
    
    /**
     * Gets the signal registry from the world.
     * 
     * @return The signal registry
     */
    protected SignalRegistry getSignalRegistry() {
        if (world == null) {
            throw new IllegalStateException(
                "System is not attached to a world. Call addSystem() on World first."
            );
        }
        return world.getSignalRegistry();
    }
    
    // ==================== Entity Queries ====================
    
    /**
     * Gets all entities with specific components.
     * 
     * @param componentClasses The component classes to match
     * @return A collection of matching entities
     */
    protected Collection<Entity> getEntitiesWith(Class<? extends Component>... componentClasses) {
        if (world == null) {
            return Collections.emptyList();
        }
        return world.getEntitiesWith(componentClasses);
    }
    
    /**
     * Gets all active entities.
     * 
     * @return A list of all active entities
     */
    protected List<Entity> getActiveEntities() {
        if (world == null) {
            return Collections.emptyList();
        }
        return world.getActiveEntities();
    }
    
    // ==================== Statistics ====================
    
    /**
     * Gets the total execution time in nanoseconds.
     * 
     * @return Total execution time
     */
    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }
    
    /**
     * Gets the number of times this system has been executed.
     * 
     * @return Execution count
     */
    public int getExecutionCount() {
        return executionCount;
    }
    
    /**
     * Gets the average execution time in milliseconds.
     * 
     * @return Average execution time
     */
    public float getAverageExecutionTime() {
        if (executionCount == 0) {
            return 0;
        }
        return (totalExecutionTime / executionCount) / 1_000_000f;
    }
    
    /**
     * Gets the last execution time in milliseconds.
     * 
     * @return Last execution time
     */
    public float getLastExecutionTime() {
        return lastExecutionTime;
    }
    
    /**
     * Resets execution statistics.
     */
    public void resetStatistics() {
        totalExecutionTime = 0;
        executionCount = 0;
        lastExecutionTime = 0;
    }
    
    // ==================== Debug Information ====================
    
    /**
     * Gets the name of this system.
     * 
     * @return The system name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets debug information about this system.
     * 
     * @return A map of debug information
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("Name", name);
        info.put("Priority", priority);
        info.put("Enabled", enabled);
        info.put("Initialized", initialized);
        info.put("Execution Count", executionCount);
        info.put("Total Time (ms)", totalExecutionTime / 1_000_000.0);
        info.put("Average Time (ms)", getAverageExecutionTime());
        info.put("Last Time (ms)", lastExecutionTime);
        return info;
    }
    
    /**
     * Gets a string representation of this system.
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return name + "(priority=" + priority + ", enabled=" + enabled + ")";
    }
}
