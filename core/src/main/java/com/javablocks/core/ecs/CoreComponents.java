/*
 * JavaBlocks Engine - Core Component Classes (Internal)
 * 
 * Internal component implementations for the ECS system.
 * These are package-private classes used internally by the engine.
 */
package com.javablocks.core.ecs;

/**
 * Component for tracking parent node relationship.
 * 
 * Used in node hierarchy system to link a node to its parent.
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
 */
final class ChildNodeComponent implements Component {
    
    /** First child in the linked list. */
    long firstChildId;
    
    /** Number of direct children. */
    int childCount;
    
    /** Reference to next sibling (linked list). */
    long nextSiblingId;
    
    ChildNodeComponent() {
        this.firstChildId = Entity.NULL_ID;
        this.childCount = 0;
        this.nextSiblingId = Entity.NULL_ID;
    }
    
    @Override
    public ChildNodeComponent copy() {
        ChildNodeComponent copy = new ChildNodeComponent();
        copy.firstChildId = firstChildId;
        copy.childCount = childCount;
        copy.nextSiblingId = nextSiblingId;
        return copy;
    }
    
    @Override
    public void reset() {
        firstChildId = Entity.NULL_ID;
        childCount = 0;
        nextSiblingId = Entity.NULL_ID;
    }
}
