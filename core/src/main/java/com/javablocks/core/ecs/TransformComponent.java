/*
 * JavaBlocks Engine - Transform Component
 * 
 * Core component for node positioning, rotation, and scale.
 * Includes parent-child transform propagation and dirty flag optimization.
 */
package com.javablocks.core.ecs;

import com.badlogic.gdx.math.*;
import com.javablocks.core.math.*;

/**
 * Transform component for spatial positioning.
 * 
 * This component stores the position, rotation, and scale of a node.
 * It also manages the parent-child relationship for hierarchical transforms.
 * 
 * Features:
 * - Position, rotation, and scale in 3D space
 * - Dirty flag for optimization of matrix recalculation
 * - Parent-child relationship for hierarchical transforms
 * - World transform calculation with parent propagation
 * 
 * @author JavaBlocks Engine Team
 */
public final class TransformComponent implements Component {
    
    // ==================== Local Transform ====================
    
    /** Local position in world units. */
    public final Vector3 position;
    
    /** Local rotation as quaternion for smooth interpolation. */
    public final Quaternion rotation;
    
    /** Local scale factors. */
    public final Vector3 scale;
    
    // ==================== World Transform ====================
    
    /** World position (computed from local and parent). */
    public final Vector3 worldPosition;
    
    /** World rotation quaternion. */
    public final Quaternion worldRotation;
    
    /** World scale. */
    public final Vector3 worldScale;
    
    /** Local to world transformation matrix. */
    public final Matrix4 localToWorldMatrix;
    
    /** World to local transformation matrix. */
    public final Matrix4 worldToLocalMatrix;
    
    // ==================== Hierarchy ====================
    
    /** Parent entity ID (-1 for no parent). */
    public long parentId;
    
    /** First child entity ID (-1 for no children). */
    public long firstChildId;
    
    /** Previous sibling entity ID. */
    public long prevSiblingId;
    
    /** Next sibling entity ID. */
    public long nextSiblingId;
    
    // ==================== State ====================
    
    /** Whether this transform needs recalculation. */
    public boolean isDirty;
    
    /** Whether this node has changed since last frame. */
    public boolean hasChanged;
    
    // ==================== Constructor ====================
    
    /**
     * Creates a transform at the origin with identity rotation and unit scale.
     */
    public TransformComponent() {
        this(0, 0, 0);
    }
    
    /**
     * Creates a transform at the specified position.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public TransformComponent(float x, float y, float z) {
        this.position = new Vector3(x, y, z);
        this.rotation = new Quaternion();
        this.scale = new Vector3(1, 1, 1);
        
        this.worldPosition = new Vector3();
        this.worldRotation = new Quaternion();
        this.worldScale = new Vector3(1, 1, 1);
        this.localToWorldMatrix = new Matrix4();
        this.worldToLocalMatrix = new Matrix4();
        
        this.parentId = Entity.NULL_ID;
        this.firstChildId = Entity.NULL_ID;
        this.prevSiblingId = Entity.NULL_ID;
        this.nextSiblingId = Entity.NULL_ID;
        
        this.isDirty = true;
        this.hasChanged = true;
    }
    
    /**
     * Creates a transform at the specified position with scale.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param scale Uniform scale factor
     */
    public TransformComponent(float x, float y, float z, float scale) {
        this(x, y, z);
        this.scale.set(scale, scale, scale);
    }
    
    /**
     * Creates a transform from a vector position.
     * 
     * @param position The position vector
     */
    public TransformComponent(Vector3 position) {
        this(position.x, position.y, position.z);
    }
    
    // ==================== Position Methods ====================
    
    /**
     * Sets the local position.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return This transform for chaining
     */
    public TransformComponent setPosition(float x, float y, float z) {
        position.set(x, y, z);
        markDirty();
        return this;
    }
    
    /**
     * Sets the local position from a vector.
     * 
     * @param position The position vector
     * @return This transform for chaining
     */
    public TransformComponent setPosition(Vector3 position) {
        this.position.set(position);
        markDirty();
        return this;
    }
    
    /**
     * Translates the local position.
     * 
     * @param x X translation
     * @param y Y translation
     * @param z Z translation
     * @return This transform for chaining
     */
    public TransformComponent translate(float x, float y, float z) {
        position.add(x, y, z);
        markDirty();
        return this;
    }
    
    /**
     * Translates the local position from a vector.
     * 
     * @param translation The translation vector
     * @return This transform for chaining
     */
    public TransformComponent translate(Vector3 translation) {
        position.add(translation);
        markDirty();
        return this;
    }
    
    // ==================== Rotation Methods ====================
    
    /**
     * Sets the local rotation from a quaternion.
     * 
     * @param quaternion The rotation quaternion
     * @return This transform for chaining
     */
    public TransformComponent setRotation(Quaternion quaternion) {
        this.rotation.set(quaternion);
        markDirty();
        return this;
    }
    
    /**
     * Sets the local rotation from Euler angles.
     * 
     * @param pitch X-axis rotation in degrees
     * @param yaw Y-axis rotation in degrees
     * @param roll Z-axis rotation in degrees
     * @return This transform for chaining
     */
    public TransformComponent setRotation(float pitch, float yaw, float roll) {
        this.rotation.setEulerAngles(pitch, yaw, roll);
        markDirty();
        return this;
    }
    
    /**
     * Rotates around an axis.
     * 
     * @param axis The rotation axis
     * @param degrees Rotation angle in degrees
     * @return This transform for chaining
     */
    public TransformComponent rotate(Vector3 axis, float degrees) {
        Quaternion q = new Quaternion();
        q.setFromAxis(axis.nor(), degrees);
        rotation.mul(q);
        markDirty();
        return this;
    }
    
    /**
     * Rotates around the X axis.
     * 
     * @param degrees Rotation angle in degrees
     * @return This transform for chaining
     */
    public TransformComponent rotateX(float degrees) {
        Quaternion temp = new Quaternion();
        temp.setFromAxis(1, 0, 0, degrees);
        rotation.mul(temp);
        markDirty();
        return this;
    }
    
    /**
     * Rotates around the Y axis.
     * 
     * @param degrees Rotation angle in degrees
     * @return This transform for chaining
     */
    public TransformComponent rotateY(float degrees) {
        Quaternion temp = new Quaternion();
        temp.setFromAxis(0, 1, 0, degrees);
        rotation.mul(temp);
        markDirty();
        return this;
    }
    
    /**
     * Rotates around the Z axis.
     * 
     * @param degrees Rotation angle in degrees
     * @return This transform for chaining
     */
    public TransformComponent rotateZ(float degrees) {
        Quaternion temp = new Quaternion();
        temp.setFromAxis(0, 0, 1, degrees);
        rotation.mul(temp);
        markDirty();
        return this;
    }
    
    // ==================== Scale Methods ====================
    
    /**
     * Sets the local scale.
     * 
     * @param scale Uniform scale factor
     * @return This transform for chaining
     */
    public TransformComponent setScale(float scale) {
        this.scale.set(scale, scale, scale);
        markDirty();
        return this;
    }
    
    /**
     * Sets the local scale from individual factors.
     * 
     * @param x X scale
     * @param y Y scale
     * @param z Z scale
     * @return This transform for chaining
     */
    public TransformComponent setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
        markDirty();
        return this;
    }
    
    /**
     * Sets the local scale from a vector.
     * 
     * @param scale The scale vector
     * @return This transform for chaining
     */
    public TransformComponent setScale(Vector3 scale) {
        this.scale.set(scale);
        markDirty();
        return this;
    }
    
    /**
     * Scales the local transform.
     * 
     * @param factor Uniform scale factor
     * @return This transform for chaining
     */
    public TransformComponent scaleBy(float factor) {
        this.scale.scl(factor);
        markDirty();
        return this;
    }
    
    // ==================== Transform Calculation ====================
    
    /**
     * Marks this transform as dirty, requiring recalculation.
     */
    public void markDirty() {
        isDirty = true;
        hasChanged = true;
    }
    
    /**
     * Updates the world transform from the local transform and parent.
     * 
     * @param parentTransform Parent's world transform (null for root)
     */
    public void updateWorldTransform(TransformComponent parentTransform) {
        if (parentTransform == null) {
            // Root node - world transform is local transform
            worldPosition.set(position);
            worldRotation.set(rotation);
            worldScale.set(scale);
        } else {
            // Combine with parent transform
            // Position = parentPosition + parentRotation * (parentScale * localPosition)
            Vector3 scaledLocalPos = new Vector3(position);
            scaledLocalPos.scl(parentTransform.worldScale);
            worldPosition.set(parentTransform.worldPosition);
            worldPosition.mul(parentTransform.worldRotation);
            worldPosition.add(scaledLocalPos);
            
            // Rotation = parentRotation * localRotation
            worldRotation.set(parentTransform.worldRotation);
            worldRotation.mul(rotation);
            
            // Scale = parentScale * localScale
            worldScale.set(parentTransform.worldScale);
            worldScale.scl(scale);
        }
        
        // Update matrices
        localToWorldMatrix.setTranslation(worldPosition);
        localToWorldMatrix.rotate(worldRotation);
        localToWorldMatrix.scale(worldScale.x, worldScale.y, worldScale.z);
        worldToLocalMatrix.set(localToWorldMatrix);
        worldToLocalMatrix.inv();
        
        isDirty = false;
    }
    
    /**
     * Recursively updates all child transforms.
     * 
     * @param world World instance for child lookup
     * @param firstChildId ID of the first child
     */
    public void updateChildTransforms(World world, long firstChildId) {
        long childId = firstChildId;
        
        while (childId != Entity.NULL_ID) {
            Entity childEntity = Entity.create(
                Entity.unpackIndex(childId),
                Entity.unpackGeneration(childId)
            );
            
            TransformComponent childTransform = world.getComponent(childEntity, TransformComponent.class);
            
            if (childTransform != null) {
                childTransform.updateWorldTransform(this);
                childTransform.updateChildTransforms(world, childTransform.firstChildId);
            }
            
            childId = childTransform != null ? childTransform.nextSiblingId : Entity.NULL_ID;
        }
    }
    
    // ==================== World Position Methods ====================
    
    /**
     * Gets the world position.
     * 
     * @return World position vector
     */
    public Vector3 getWorldPosition() {
        return worldPosition;
    }
    
    /**
     * Gets the world rotation as a quaternion.
     * 
     * @return World rotation quaternion
     */
    public Quaternion getWorldRotation() {
        return worldRotation;
    }
    
    /**
     * Gets the world scale.
     * 
     * @return World scale vector
     */
    public Vector3 getWorldScale() {
        return worldScale;
    }
    
    /**
     * Transforms a point from local to world space.
     * 
     * @param localPoint Point in local space
     * @return Point in world space
     */
    public Vector3 localToWorld(Vector3 localPoint) {
        Vector3 result = new Vector3(localPoint);
        return result.mul(localToWorldMatrix);
    }
    
    /**
     * Transforms a point from world to local space.
     * 
     * @param worldPoint Point in world space
     * @return Point in local space
     */
    public Vector3 worldToLocal(Vector3 worldPoint) {
        Vector3 result = new Vector3(worldPoint);
        return result.mul(worldToLocalMatrix);
    }
    
    /**
     * Transforms a direction from local to world space.
     * 
     * @param localDirection Direction in local space
     * @return Direction in world space
     */
    public Vector3 localToWorldDirection(Vector3 localDirection) {
        Vector3 result = new Vector3(localDirection);
        return worldRotation.transform(result);
    }
    
    // ==================== Hierarchy Methods ====================
    
    /**
     * Checks if this node has a parent.
     * 
     * @return true if has parent
     */
    public boolean hasParent() {
        return parentId != Entity.NULL_ID;
    }
    
    /**
     * Checks if this node has children.
     * 
     * @return true if has children
     */
    public boolean hasChildren() {
        return firstChildId != Entity.NULL_ID;
    }
    
    /**
     * Gets the number of direct children.
     * 
     * @return Child count
     */
    public int getChildCount() {
        int count = 0;
        long childId = firstChildId;
        
        while (childId != Entity.NULL_ID) {
            count++;
            TransformComponent child = null;
            // Would need world reference to get child transform
            childId = nextSiblingId;
        }
        
        return count;
    }
    
    // ==================== Component Interface ====================
    
    /**
     * Creates a copy of this component.
     * 
     * @return A new transform component with copied values
     */
    @Override
    public TransformComponent copy() {
        TransformComponent copy = new TransformComponent();
        copy.position.set(position);
        copy.rotation.set(rotation);
        copy.scale.set(scale);
        copy.worldPosition.set(worldPosition);
        copy.worldRotation.set(worldRotation);
        copy.worldScale.set(worldScale);
        return copy;
    }
    
    /**
     * Resets this component to default values.
     */
    @Override
    public void reset() {
        position.set(0, 0, 0);
        rotation.idt();
        scale.set(1, 1, 1);
        
        worldPosition.set(0, 0, 0);
        worldRotation.idt();
        worldScale.set(1, 1, 1);
        
        localToWorldMatrix.idt();
        worldToLocalMatrix.idt();
        
        parentId = Entity.NULL_ID;
        firstChildId = Entity.NULL_ID;
        prevSiblingId = Entity.NULL_ID;
        nextSiblingId = Entity.NULL_ID;
        
        isDirty = true;
        hasChanged = false;
    }
    
    /**
     * Gets the type name for this component.
     * 
     * @return Component type name
     */
    @Override
    public String toString() {
        return "Transform(position=" + position + ", rotation=" + rotation + 
               ", scale=" + scale + ")";
    }
}
