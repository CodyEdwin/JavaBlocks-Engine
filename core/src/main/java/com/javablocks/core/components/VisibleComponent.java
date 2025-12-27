/*
 * JavaBlocks Engine - Visible Component
 * 
 * Provides visibility state and layer management for rendering.
 */
package com.javablocks.core.components;

import com.javablocks.core.ecs.Component;

/**
 * Component for visibility state.
 * 
 * Controls whether entities are rendered and participate
 * in culling calculations.
 */
public final class VisibleComponent implements Component {
    
    /** Whether the entity is visible. */
    private boolean visible;
    
    /** Layer for sorting (lower = rendered first). */
    private int layer;
    
    /** Order within layer. */
    private int order;
    
    /**
     * Creates a visible component on the default layer.
     */
    public VisibleComponent() {
        this(true, 0, 0);
    }
    
    /**
     * Creates a component with visibility state.
     * 
     * @param visible Whether the entity is visible
     */
    public VisibleComponent(boolean visible) {
        this(visible, 0, 0);
    }
    
    /**
     * Creates a component with visibility and layer.
     * 
     * @param visible Whether the entity is visible
     * @param layer Render layer
     */
    public VisibleComponent(boolean visible, int layer) {
        this(visible, layer, 0);
    }
    
    /**
     * Creates a component with full visibility settings.
     * 
     * @param visible Whether the entity is visible
     * @param layer Render layer
     * @param order Order within layer
     */
    public VisibleComponent(boolean visible, int layer, int order) {
        this.visible = visible;
        this.layer = layer;
        this.order = order;
    }
    
    /**
     * Checks if the entity is visible.
     * 
     * @return true if visible
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Sets the visibility state.
     * 
     * @param visible The new visibility state
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * Gets the render layer.
     * 
     * @return The layer
     */
    public int getLayer() {
        return layer;
    }
    
    /**
     * Sets the render layer.
     * 
     * @param layer The new layer
     */
    public void setLayer(int layer) {
        this.layer = layer;
    }
    
    /**
     * Gets the render order.
     * 
     * @return The order
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * Sets the render order.
     * 
     * @param order The new order
     */
    public void setOrder(int order) {
        this.order = order;
    }
    
    /**
     * Gets the combined sort key.
     * 
     * @return Sort key (lower = rendered first)
     */
    public int getSortKey() {
        return (layer << 16) | (order & 0xFFFF);
    }
    
    @Override
    public VisibleComponent copy() {
        return new VisibleComponent(visible, layer, order);
    }
    
    @Override
    public void reset() {
        visible = true;
        layer = 0;
        order = 0;
    }
    
    @Override
    public String toString() {
        return "Visible(visible=" + visible + ", layer=" + layer + ", order=" + order + ")";
    }
}
