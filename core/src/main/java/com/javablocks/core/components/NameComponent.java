/*
 * JavaBlocks Engine - Name Component
 * 
 * Provides a human-readable name for entities.
 */
package com.javablocks.core.components;

import com.javablocks.core.ecs.Component;

/**
 * Component for naming entities.
 * 
 * Provides a human-readable name for entities useful for debugging,
 * editor display, and entity lookup by name.
 */
public final class NameComponent implements Component {
    
    /** The entity name. */
    private String name;
    
    /**
     * Creates a component with an empty name.
     */
    public NameComponent() {
        this("");
    }
    
    /**
     * Creates a component with the specified name.
     * 
     * @param name The entity name
     */
    public NameComponent(String name) {
        this.name = name != null ? name : "";
    }
    
    /**
     * Gets the entity name.
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the entity name.
     * 
     * @param name The new name
     */
    public void setName(String name) {
        this.name = name != null ? name : "";
    }
    
    @Override
    public NameComponent copy() {
        return new NameComponent(name);
    }
    
    @Override
    public void reset() {
        name = "";
    }
    
    @Override
    public String toString() {
        return "Name(name=" + name + ")";
    }
}
