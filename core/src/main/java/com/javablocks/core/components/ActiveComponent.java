/*
 * JavaBlocks Engine - Active Component
 * 
 * Provides entity enable/disable functionality.
 */
package com.javablocks.core.components;

import com.javablocks.core.ecs.Component;

/**
 * Component for enabling and disabling entities.
 * 
 * Disabled entities are not updated or rendered but still exist
 * in the entity system.
 */
public final class ActiveComponent implements Component {
    
    /** Whether the entity is active. */
    private boolean active;
    
    /**
     * Creates an active component.
     */
    public ActiveComponent() {
        this(true);
    }
    
    /**
     * Creates a component with the specified active state.
     * 
     * @param active The active state
     */
    public ActiveComponent(boolean active) {
        this.active = active;
    }
    
    /**
     * Checks if the entity is active.
     * 
     * @return true if active
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Sets the active state.
     * 
     * @param active The new active state
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Toggles the active state.
     * 
     * @return The new active state
     */
    public boolean toggle() {
        active = !active;
        return active;
    }
    
    @Override
    public ActiveComponent copy() {
        return new ActiveComponent(active);
    }
    
    @Override
    public void reset() {
        active = true;
    }
    
    @Override
    public String toString() {
        return "Active(active=" + active + ")";
    }
}
