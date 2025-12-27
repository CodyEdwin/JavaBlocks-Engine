/*
 * JavaBlocks Engine - Core Component Classes
 * 
 * Essential component implementations for the ECS system.
 * Includes naming, tagging, lifetime, and state management components.
 */
package com.javablocks.core.ecs;

/**
 * Component for naming entities.
 * 
 * Provides a human-readable name for entities useful for debugging,
 * editor display, and entity lookup by name.
 * 
 * @author JavaBlocks Engine Team
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

/**
 * Component for tagging entities.
 * 
 * Tags are string identifiers that can be used for quick entity filtering
 * and grouping. Multiple tags can be attached to a single entity.
 * 
 * @author JavaBlocks Engine Team
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

/**
 * Component for enabling and disabling entities.
 * 
 * Disabled entities are not updated or rendered but still exist
 * in the entity system.
 * 
 * @author JavaBlocks Engine Team
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

/**
 * Component for managing entity lifetime.
 * 
 * Provides automatic entity destruction after a specified time.
 * Useful for particles, projectiles, and temporary effects.
 * 
 * @author JavaBlocks Engine Team
 */
public final class LifetimeComponent implements Component {
    
    /** Maximum lifetime in seconds (0 = infinite). */
    private float maxLifetime;
    
    /** Current age in seconds. */
    private float age;
    
    /** Whether the entity is marked for destruction. */
    private boolean markedForDestruction;
    
    /**
     * Creates a component with infinite lifetime.
     */
    public LifetimeComponent() {
        this(0);
    }
    
    /**
     * Creates a component with a specific lifetime.
     * 
     * @param maxLifetime Maximum lifetime in seconds (0 = infinite)
     */
    public LifetimeComponent(float maxLifetime) {
        this.maxLifetime = maxLifetime;
        this.age = 0;
        this.markedForDestruction = false;
    }
    
    /**
     * Updates the lifetime.
     * 
     * @param deltaTime Time since last update in seconds
     * @return true if the entity should be destroyed
     */
    public boolean update(float deltaTime) {
        if (maxLifetime <= 0) {
            return false;
        }
        
        age += deltaTime;
        
        if (age >= maxLifetime) {
            markedForDestruction = true;
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the remaining lifetime.
     * 
     * @return Remaining lifetime in seconds (0 if infinite or expired)
     */
    public float getRemainingLifetime() {
        if (maxLifetime <= 0) {
            return 0;
        }
        return Math.max(0, maxLifetime - age);
    }
    
    /**
     * Gets the lifetime progress (0-1).
     * 
     * @return Progress from 0 to 1 (1 = expired)
     */
    public float getProgress() {
        if (maxLifetime <= 0) {
            return 0;
        }
        return Math.min(1, age / maxLifetime);
    }
    
    /**
     * Gets the maximum lifetime.
     * 
     * @return Maximum lifetime in seconds
     */
    public float getMaxLifetime() {
        return maxLifetime;
    }
    
    /**
     * Sets the maximum lifetime.
     * 
     * @param maxLifetime Maximum lifetime in seconds
     */
    public void setMaxLifetime(float maxLifetime) {
        this.maxLifetime = maxLifetime;
    }
    
    /**
     * Gets the current age.
     * 
     * @return Age in seconds
     */
    public float getAge() {
        return age;
    }
    
    /**
     * Resets the lifetime.
     */
    public void reset() {
        age = 0;
        markedForDestruction = false;
    }
    
    /**
     * Checks if the entity is marked for destruction.
     * 
     * @return true if marked for destruction
     */
    public boolean isMarkedForDestruction() {
        return markedForDestruction;
    }
    
    /**
     * Marks the entity for destruction.
     */
    public void markForDestruction() {
        this.markedForDestruction = true;
    }
    
    /**
     * Checks if the entity has infinite lifetime.
     * 
     * @return true if lifetime is infinite
     */
    public boolean isImmortal() {
        return maxLifetime <= 0;
    }
    
    @Override
    public LifetimeComponent copy() {
        LifetimeComponent copy = new LifetimeComponent(maxLifetime);
        copy.age = age;
        copy.markedForDestruction = markedForDestruction;
        return copy;
    }
    
    @Override
    public void reset() {
        maxLifetime = 0;
        age = 0;
        markedForDestruction = false;
    }
    
    @Override
    public String toString() {
        if (maxLifetime <= 0) {
            return "Lifetime(immortal, age=" + age + ")";
        }
        return "Lifetime(age=" + age + "/" + maxLifetime + ", progress=" + 
               String.format("%.2f", getProgress()) + ")";
    }
}

/**
 * Component for visibility state.
 * 
 * Controls whether entities are rendered and participate
 * in culling calculations.
 * 
 * @author JavaBlocks Engine Team
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

/**
 * Component for tracking parent node relationship.
 * 
 * Used in node hierarchy system to link a node to its parent.
 * 
 * @author JavaBlocks Engine Team
 */
final class ParentNodeComponent implements Component {
    
    /** The parent entity ID. */
    long parentId;
    
    /** Reference to parent's transform component (for convenience). */
    TransformComponent parentTransform;
    
    ParentNodeComponent() {
        this.parentId = Entity.NULL_ID;
        this.parentTransform = null;
    }
    
    @Override
    public ParentNodeComponent copy() {
        ParentNodeComponent copy = new ParentNodeComponent();
        copy.parentId = parentId;
        copy.parentTransform = parentTransform;
        return copy;
    }
    
    @Override
    public void reset() {
        parentId = Entity.NULL_ID;
        parentTransform = null;
    }
}

/**
 * Component for tracking child node relationship.
 * 
 * Used in node hierarchy system to manage child nodes.
 * 
 * @author JavaBlocks Engine Team
 */
final class ChildNodeComponent implements Component {
    
    /** First child in the linked list. */
    long firstChildId;
    
    /** Number of direct children. */
    int childCount;
    
    ChildNodeComponent() {
        this.firstChildId = Entity.NULL_ID;
        this.childCount = 0;
    }
    
    @Override
    public ChildNodeComponent copy() {
        ChildNodeComponent copy = new ChildNodeComponent();
        copy.firstChildId = firstChildId;
        copy.childCount = childCount;
        return copy;
    }
    
    @Override
    public void reset() {
        firstChildId = Entity.NULL_ID;
        childCount = 0;
    }
}
