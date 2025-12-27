package com.javablocks.core.components;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ActiveComponent.
 */
class ActiveComponentTest {
    
    @Test
    @DisplayName("Default constructor should create active component")
    void defaultConstructorShouldCreateActiveComponent() {
        ActiveComponent component = new ActiveComponent();
        assertTrue(component.isActive());
    }
    
    @Test
    @DisplayName("Constructor with false should create inactive")
    void constructorWithFalseShouldCreateInactive() {
        ActiveComponent component = new ActiveComponent(false);
        assertFalse(component.isActive());
    }
    
    @Test
    @DisplayName("setActive should update state")
    void setActiveShouldUpdateState() {
        ActiveComponent component = new ActiveComponent(true);
        component.setActive(false);
        assertFalse(component.isActive());
        
        component.setActive(true);
        assertTrue(component.isActive());
    }
    
    @Test
    @DisplayName("toggle should switch state")
    void toggleShouldSwitchState() {
        ActiveComponent component = new ActiveComponent(true);
        assertTrue(component.toggle());
        assertFalse(component.isActive());
        
        assertFalse(component.toggle());
        assertTrue(component.isActive());
    }
    
    @Test
    @DisplayName("copy should preserve active state")
    void copyShouldPreserveActiveState() {
        ActiveComponent original = new ActiveComponent(false);
        ActiveComponent copy = original.copy();
        
        assertEquals(original.isActive(), copy.isActive());
        assertFalse(copy.isActive());
    }
    
    @Test
    @DisplayName("reset should set to active")
    void resetShouldSetToActive() {
        ActiveComponent component = new ActiveComponent(false);
        component.reset();
        assertTrue(component.isActive());
    }
    
    @Test
    @DisplayName("toString should show active state")
    void toStringShouldShowActiveState() {
        ActiveComponent active = new ActiveComponent(true);
        ActiveComponent inactive = new ActiveComponent(false);
        
        assertTrue(active.toString().contains("active=true"));
        assertTrue(inactive.toString().contains("active=false"));
    }
}

/**
 * Tests for LifetimeComponent.
 */
class LifetimeComponentTest {
    
    @Test
    @DisplayName("Default constructor should create immortal component")
    void defaultConstructorShouldCreateImmortal() {
        LifetimeComponent component = new LifetimeComponent();
        assertTrue(component.isImmortal());
        assertEquals(0, component.getMaxLifetime());
    }
    
    @Test
    @DisplayName("Constructor with lifetime should set max lifetime")
    void constructorWithLifetimeShouldSetMaxLifetime() {
        LifetimeComponent component = new LifetimeComponent(5.0f);
        assertEquals(5.0f, component.getMaxLifetime());
        assertFalse(component.isImmortal());
    }
    
    @Test
    @DisplayName("update should increase age")
    void updateShouldIncreaseAge() {
        LifetimeComponent component = new LifetimeComponent(10.0f);
        component.update(2.0f);
        assertEquals(2.0f, component.getAge(), 0.001f);
        
        component.update(3.0f);
        assertEquals(5.0f, component.getAge(), 0.001f);
    }
    
    @Test
    @DisplayName("update should not affect immortal component")
    void updateShouldNotAffectImmortal() {
        LifetimeComponent component = new LifetimeComponent();
        component.update(100.0f);
        assertEquals(0, component.getAge());
        assertFalse(component.isMarkedForDestruction());
    }
    
    @Test
    @DisplayName("update should mark for destruction when expired")
    void updateShouldMarkForDestructionWhenExpired() {
        LifetimeComponent component = new LifetimeComponent(5.0f);
        
        boolean shouldDestroy = component.update(3.0f);
        assertFalse(shouldDestroy);
        assertFalse(component.isMarkedForDestruction());
        
        shouldDestroy = component.update(3.0f);
        assertTrue(shouldDestroy);
        assertTrue(component.isMarkedForDestruction());
    }
    
    @Test
    @DisplayName("getRemainingLifetime should return correct value")
    void getRemainingLifetimeShouldReturnCorrectValue() {
        LifetimeComponent component = new LifetimeComponent(10.0f);
        component.update(3.0f);
        
        assertEquals(7.0f, component.getRemainingLifetime(), 0.001f);
    }
    
    @Test
    @DisplayName("getRemainingLifetime should return 0 for immortal")
    void getRemainingLifetimeShouldReturn0ForImmortal() {
        LifetimeComponent component = new LifetimeComponent();
        assertEquals(0, component.getRemainingLifetime());
    }
    
    @Test
    @DisplayName("getProgress should return correct value")
    void getProgressShouldReturnCorrectValue() {
        LifetimeComponent component = new LifetimeComponent(10.0f);
        component.update(2.5f);
        
        assertEquals(0.25f, component.getProgress(), 0.001f);
    }
    
    @Test
    @DisplayName("getProgress should cap at 1")
    void getProgressShouldCapAt1() {
        LifetimeComponent component = new LifetimeComponent(5.0f);
        component.update(10.0f); // Over time
        
        assertEquals(1.0f, component.getProgress(), 0.001f);
    }
    
    @Test
    @DisplayName("markForDestruction should set flag")
    void markForDestructionShouldSetFlag() {
        LifetimeComponent component = new LifetimeComponent(10.0f);
        component.markForDestruction();
        assertTrue(component.isMarkedForDestruction());
    }
    
    @Test
    @DisplayName("reset should reset all values")
    void resetShouldResetAllValues() {
        LifetimeComponent component = new LifetimeComponent(10.0f);
        component.update(5.0f);
        component.markForDestruction();
        
        component.reset();
        
        assertEquals(0, component.getMaxLifetime());
        assertEquals(0, component.getAge());
        assertFalse(component.isMarkedForDestruction());
    }
    
    @Test
    @DisplayName("copy should create independent copy")
    void copyShouldCreateIndependentCopy() {
        LifetimeComponent original = new LifetimeComponent(10.0f);
        original.update(5.0f);
        
        LifetimeComponent copy = original.copy();
        
        assertEquals(original.getMaxLifetime(), copy.getMaxLifetime());
        assertEquals(original.getAge(), copy.getAge(), 0.001f);
    }
}
