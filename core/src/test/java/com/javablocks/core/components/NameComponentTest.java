package com.javablocks.core.components;

import com.javablocks.core.ecs.Component;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NameComponent.
 */
class NameComponentTest {
    
    @Test
    @DisplayName("Default constructor should create empty name")
    void defaultConstructorShouldCreateEmptyName() {
        NameComponent component = new NameComponent();
        assertEquals("", component.getName());
    }
    
    @Test
    @DisplayName("Constructor with name should set name")
    void constructorWithNameShouldSetName() {
        NameComponent component = new NameComponent("TestEntity");
        assertEquals("TestEntity", component.getName());
    }
    
    @Test
    @DisplayName("Constructor with null should convert to empty")
    void constructorWithNullShouldConvertToEmpty() {
        NameComponent component = new NameComponent(null);
        assertEquals("", component.getName());
    }
    
    @Test
    @DisplayName("setName should update name")
    void setNameShouldUpdateName() {
        NameComponent component = new NameComponent();
        component.setName("UpdatedName");
        assertEquals("UpdatedName", component.getName());
    }
    
    @Test
    @DisplayName("setName with null should convert to empty")
    void setNameWithNullShouldConvertToEmpty() {
        NameComponent component = new NameComponent("Test");
        component.setName(null);
        assertEquals("", component.getName());
    }
    
    @Test
    @DisplayName("copy should create independent copy")
    void copyShouldCreateIndependentCopy() {
        NameComponent original = new NameComponent("Original");
        NameComponent copy = original.copy();
        
        assertEquals(original.getName(), copy.getName());
        copy.setName("Modified");
        assertEquals("Original", original.getName());
    }
    
    @Test
    @DisplayName("reset should clear name")
    void resetShouldClearName() {
        NameComponent component = new NameComponent("TestName");
        component.reset();
        assertEquals("", component.getName());
    }
    
    @Test
    @DisplayName("toString should contain name")
    void toStringShouldContainName() {
        NameComponent component = new NameComponent("MyEntity");
        String str = component.toString();
        assertTrue(str.contains("MyEntity"));
    }
}
