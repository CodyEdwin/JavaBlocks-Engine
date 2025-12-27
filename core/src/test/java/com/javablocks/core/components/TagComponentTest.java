package com.javablocks.core.components;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TagComponent.
 */
class TagComponentTest {
    
    @Test
    @DisplayName("Default constructor should create empty tags")
    void defaultConstructorShouldCreateEmptyTags() {
        TagComponent component = new TagComponent();
        assertEquals("", component.getTag());
        assertFalse(component.hasTag("anything"));
    }
    
    @Test
    @DisplayName("Constructor with tag should set primary tag")
    void constructorWithTagShouldSetPrimaryTag() {
        TagComponent component = new TagComponent("Player");
        assertEquals("Player", component.getTag());
        assertTrue(component.hasTag("Player"));
    }
    
    @Test
    @DisplayName("Constructor with null should create empty")
    void constructorWithNullShouldCreateEmpty() {
        TagComponent component = new TagComponent(null);
        assertEquals("", component.getTag());
    }
    
    @Test
    @DisplayName("addTag should add additional tags")
    void addTagShouldAddAdditionalTags() {
        TagComponent component = new TagComponent("Primary");
        component.addTag("Tag1");
        component.addTag("Tag2");
        
        assertTrue(component.hasTag("Primary"));
        assertTrue(component.hasTag("Tag1"));
        assertTrue(component.hasTag("Tag2"));
    }
    
    @Test
    @DisplayName("addTag should not duplicate existing tags")
    void addTagShouldNotDuplicateExistingTags() {
        TagComponent component = new TagComponent("Player");
        component.addTag("Player");
        
        String[] allTags = component.getAllTags();
        int playerCount = 0;
        for (String tag : allTags) {
            if (tag.equals("Player")) {
                playerCount++;
            }
        }
        assertEquals(1, playerCount);
    }
    
    @Test
    @DisplayName("addTag should ignore null and empty")
    void addTagShouldIgnoreNullAndEmpty() {
        TagComponent component = new TagComponent("Player");
        component.addTag(null);
        component.addTag("");
        
        assertFalse(component.hasTag(null));
        assertFalse(component.hasTag(""));
    }
    
    @Test
    @DisplayName("removeTag should remove tags")
    void removeTagShouldRemoveTags() {
        TagComponent component = new TagComponent("Player");
        component.addTag("Ally");
        
        assertTrue(component.hasTag("Ally"));
        assertTrue(component.removeTag("Ally"));
        assertFalse(component.hasTag("Ally"));
    }
    
    @Test
    @DisplayName("removeTag should return false for non-existent")
    void removeTagShouldReturnFalseForNonExistent() {
        TagComponent component = new TagComponent("Player");
        assertFalse(component.removeTag("NonExistent"));
    }
    
    @Test
    @DisplayName("getAllTags should return all tags")
    void getAllTagsShouldReturnAllTags() {
        TagComponent component = new TagComponent("Primary");
        component.addTag("Tag1");
        component.addTag("Tag2");
        
        String[] allTags = component.getAllTags();
        assertEquals(3, allTags.length);
    }
    
    @Test
    @DisplayName("copy should create independent copy")
    void copyShouldCreateIndependentCopy() {
        TagComponent original = new TagComponent("Player");
        original.addTag("Ally");
        
        TagComponent copy = original.copy();
        assertEquals(original.getTag(), copy.getTag());
        assertArrayEquals(original.getAllTags(), copy.getAllTags());
        
        copy.setTag("Modified");
        assertEquals("Player", original.getTag());
    }
    
    @Test
    @DisplayName("reset should clear all tags")
    void resetShouldClearAllTags() {
        TagComponent component = new TagComponent("Player");
        component.addTag("Ally");
        component.addTag("Enemy");
        
        component.reset();
        
        assertEquals("", component.getTag());
        assertFalse(component.hasTag("Ally"));
        assertFalse(component.hasTag("Enemy"));
    }
}
