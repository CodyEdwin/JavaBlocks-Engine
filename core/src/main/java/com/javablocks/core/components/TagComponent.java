/*
 * JavaBlocks Engine - Tag Component
 * 
 * Provides tagging functionality for entity filtering and grouping.
 */
package com.javablocks.core.components;

import com.javablocks.core.ecs.Component;

/**
 * Component for tagging entities.
 * 
 * Tags are string identifiers that can be used for quick entity filtering
 * and grouping. Multiple tags can be attached to a single entity.
 */
public final class TagComponent implements Component {
    
    /** The primary tag. */
    private String tag;
    
    /** Additional tags. */
    private String[] additionalTags;
    
    /** Number of additional tags. */
    private int tagCount;
    
    /**
     * Creates a component with no tags.
     */
    public TagComponent() {
        this.tag = "";
        this.additionalTags = new String[4];
        this.tagCount = 0;
    }
    
    /**
     * Creates a component with a primary tag.
     * 
     * @param tag The primary tag
     */
    public TagComponent(String tag) {
        this.tag = tag != null ? tag : "";
        this.additionalTags = new String[4];
        this.tagCount = 0;
    }
    
    /**
     * Gets the primary tag.
     * 
     * @return The primary tag
     */
    public String getTag() {
        return tag;
    }
    
    /**
     * Sets the primary tag.
     * 
     * @param tag The new primary tag
     */
    public void setTag(String tag) {
        this.tag = tag != null ? tag : "";
    }
    
    /**
     * Adds an additional tag.
     * 
     * @param tag The tag to add
     */
    public void addTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }
        
        // Check if already added
        if (tag.equals(this.tag)) {
            return;
        }
        
        for (int i = 0; i < tagCount; i++) {
            if (tag.equals(additionalTags[i])) {
                return;
            }
        }
        
        // Grow array if needed
        if (tagCount >= additionalTags.length) {
            String[] newTags = new String[additionalTags.length * 2];
            System.arraycopy(additionalTags, 0, newTags, 0, additionalTags.length);
            additionalTags = newTags;
        }
        
        additionalTags[tagCount++] = tag;
    }
    
    /**
     * Removes a tag.
     * 
     * @param tag The tag to remove
     * @return true if the tag was removed
     */
    public boolean removeTag(String tag) {
        if (tag == null) {
            return false;
        }
        
        // Check primary tag
        if (tag.equals(this.tag)) {
            this.tag = "";
            return true;
        }
        
        // Check additional tags
        for (int i = 0; i < tagCount; i++) {
            if (tag.equals(additionalTags[i])) {
                additionalTags[i] = additionalTags[--tagCount];
                additionalTags[tagCount] = null;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if this entity has a specific tag.
     * 
     * @param tag The tag to check
     * @return true if the entity has the tag
     */
    public boolean hasTag(String tag) {
        if (tag == null) {
            return false;
        }
        
        if (tag.equals(this.tag)) {
            return true;
        }
        
        for (int i = 0; i < tagCount; i++) {
            if (tag.equals(additionalTags[i])) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets all tags as an array.
     * 
     * @return Array of all tags
     */
    public String[] getAllTags() {
        String[] allTags = new String[tagCount + (tag.isEmpty() ? 0 : 1)];
        int index = 0;
        
        if (!tag.isEmpty()) {
            allTags[index++] = tag;
        }
        
        for (int i = 0; i < tagCount; i++) {
            allTags[index++] = additionalTags[i];
        }
        
        return allTags;
    }
    
    @Override
    public TagComponent copy() {
        TagComponent copy = new TagComponent(tag);
        for (int i = 0; i < tagCount; i++) {
            copy.additionalTags[i] = additionalTags[i];
        }
        copy.tagCount = tagCount;
        return copy;
    }
    
    @Override
    public void reset() {
        tag = "";
        tagCount = 0;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Tag(tags=");
        boolean first = true;
        
        if (!tag.isEmpty()) {
            sb.append(tag);
            first = false;
        }
        
        for (int i = 0; i < tagCount; i++) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(additionalTags[i]);
            first = false;
        }
        
        sb.append(")");
        return sb.toString();
    }
}
