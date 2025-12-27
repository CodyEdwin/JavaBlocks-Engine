/*
 * JavaBlocks Engine - Component System
 * 
 * Marker interface and annotations for the component system.
 * Components are data containers that can be attached to entities.
 */
package com.javablocks.core.ecs;

import java.lang.annotation.*;

/**
 * Marker interface for all components.
 * 
 * Components are pure data containers that hold state for entities.
 * They should not contain any logic - all behavior belongs in systems.
 * 
 * Design Principles:
 * - Zero-allocation in hot paths
 * - Cache-friendly memory layout
 * - Type-safe component queries
 * 
 * Best Practices:
 * - Use records for immutable components
 * - Use final classes for mutable components
 * - Keep components small and focused
 * - Use component tags for quick filtering
 * 
 * @author JavaBlocks Engine Team
 */
public interface Component {
    
    // ==================== Component Type Information ====================
    
    /**
     * Gets the unique ID for this component type.
     * Used for efficient component storage and lookup.
     * 
     * @return The component type ID
     */
    default int getTypeId() {
        return ComponentRegistry.getTypeId(getClass());
    }
    
    /**
     * Gets the name of this component type.
     * Useful for debugging and serialization.
     * 
     * @return The component type name
     */
    default String getTypeName() {
        return getClass().getSimpleName();
    }
    
    /**
     * Creates a copy of this component.
     * Used for cloning and serialization.
     * 
     * @return A copy of this component
     */
    Component copy();
    
    /**
     * Resets this component to its default state.
     * Called when a component is reused from a pool.
     */
    void reset();
    
    // ==================== Component Pool Information ====================
    
    /**
     * Gets the size of the component pool.
     * Override for components with variable-sized data.
     * 
     * @return The component size in bytes
     */
    default int getSize() {
        return -1; // -1 means default pool allocation
    }
    
    /**
     * Gets the alignment requirement for this component.
     * Override for components with specific alignment needs.
     * 
     * @return The alignment in bytes, or -1 for default
     */
    default int getAlignment() {
        return -1;
    }
}
