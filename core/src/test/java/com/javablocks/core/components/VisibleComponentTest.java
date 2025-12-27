package com.javablocks.core.components;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VisibleComponent.
 */
class VisibleComponentTest {
    
    @Test
    @DisplayName("Default constructor should create visible component on layer 0")
    void defaultConstructorShouldCreateVisibleOnLayer0() {
        VisibleComponent component = new VisibleComponent();
        assertTrue(component.isVisible());
        assertEquals(0, component.getLayer());
        assertEquals(0, component.getOrder());
    }
    
    @Test
    @DisplayName("Constructor with visibility should set visibility")
    void constructorWithVisibilityShouldSetVisibility() {
        VisibleComponent visible = new VisibleComponent(true);
        VisibleComponent invisible = new VisibleComponent(false);
        
        assertTrue(visible.isVisible());
        assertFalse(invisible.isVisible());
    }
    
    @Test
    @DisplayName("Constructor with visibility and layer should set both")
    void constructorWithVisibilityAndLayerShouldSetBoth() {
        VisibleComponent component = new VisibleComponent(true, 5);
        assertTrue(component.isVisible());
        assertEquals(5, component.getLayer());
    }
    
    @Test
    @DisplayName("Constructor with all parameters should set all")
    void constructorWithAllParametersShouldSetAll() {
        VisibleComponent component = new VisibleComponent(true, 3, 7);
        assertTrue(component.isVisible());
        assertEquals(3, component.getLayer());
        assertEquals(7, component.getOrder());
    }
    
    @Test
    @DisplayName("setVisible should update visibility")
    void setVisibleShouldUpdateVisibility() {
        VisibleComponent component = new VisibleComponent(true);
        component.setVisible(false);
        assertFalse(component.isVisible());
    }
    
    @Test
    @DisplayName("setLayer should update layer")
    void setLayerShouldUpdateLayer() {
        VisibleComponent component = new VisibleComponent();
        component.setLayer(10);
        assertEquals(10, component.getLayer());
    }
    
    @Test
    @DisplayName("setOrder should update order")
    void setOrderShouldUpdateOrder() {
        VisibleComponent component = new VisibleComponent();
        component.setOrder(15);
        assertEquals(15, component.getOrder());
    }
    
    @Test
    @DisplayName("getSortKey should combine layer and order")
    void getSortKeyShouldCombineLayerAndOrder() {
        VisibleComponent component = new VisibleComponent(true, 5, 10);
        
        int sortKey = component.getSortKey();
        // Sort key = (layer << 16) | (order & 0xFFFF)
        assertEquals((5 << 16) | 10, sortKey);
    }
    
    @Test
    @DisplayName("getSortKey should handle negative order")
    void getSortKeyShouldHandleNegativeOrder() {
        VisibleComponent component = new VisibleComponent(true, 1, -1);
        
        int sortKey = component.getSortKey();
        // order & 0xFFFF converts -1 to 65535
        assertEquals((1 << 16) | 0xFFFF, sortKey);
    }
    
    @Test
    @DisplayName("copy should create independent copy")
    void copyShouldCreateIndependentCopy() {
        VisibleComponent original = new VisibleComponent(true, 5, 10);
        VisibleComponent copy = original.copy();
        
        assertEquals(original.isVisible(), copy.isVisible());
        assertEquals(original.getLayer(), copy.getLayer());
        assertEquals(original.getOrder(), copy.getOrder());
        
        copy.setVisible(false);
        assertTrue(original.isVisible());
    }
    
    @Test
    @DisplayName("reset should reset to defaults")
    void resetShouldResetToDefaults() {
        VisibleComponent component = new VisibleComponent(false, 100, 200);
        component.reset();
        
        assertTrue(component.isVisible());
        assertEquals(0, component.getLayer());
        assertEquals(0, component.getOrder());
    }
    
    @Test
    @DisplayName("toString should contain all properties")
    void toStringShouldContainAllProperties() {
        VisibleComponent component = new VisibleComponent(true, 5, 10);
        String str = component.toString();
        
        assertTrue(str.contains("visible=true"));
        assertTrue(str.contains("layer=5"));
        assertTrue(str.contains("order=10"));
    }
}
