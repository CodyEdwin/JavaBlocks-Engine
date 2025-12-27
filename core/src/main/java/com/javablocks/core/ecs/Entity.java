/*
 * JavaBlocks Engine - Entity Component System
 * 
 * Core ECS classes including Entity, Component, World, and related utilities.
 * Designed for high-performance with object pooling and zero-GC hot paths.
 */
package com.javablocks.core.ecs;

import com.javablocks.core.*;
import com.javablocks.core.events.*;
import com.javablocks.core.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * An Entity is a lightweight identifier that represents a game object.
 * Entities do not contain any data themselves - they are simply IDs that
 * components are attached to. This separation allows for flexible composition.
 * 
 * Design Principles:
 * - Immutable value object for entity IDs
 * - Pooled internally by World for performance
 * - Comparison-optimized for fast lookups
 * 
 * @author JavaBlocks Engine Team
 */
public final class Entity implements Comparable<Entity> {
    
    // ==================== Constants ====================
    
    /** Maximum number of entities the system can handle. */
    public static final long MAX_ENTITIES = 1L << 32;
    
    /** Entity ID for null/invalid entities. */
    public static final int NULL_ID = -1;
    
    /** Index bits for entity ID (lower 32 bits). */
    public static final int INDEX_BITS = 32;
    
    /** Generation bits for entity ID (upper 32 bits). */
    public static final int GENERATION_BITS = 32;
    
    // ==================== Entity Pool ====================
    
    /**
     * Internal entity pool for recycling entity IDs.
     * This reduces allocation pressure in hot paths.
     */
    static final class EntityPool {
        private final IntStack freeIndices = new IntStack(10000);
        private int nextIndex = 0;
        private int[] generations;
        private int poolSize;
        
        EntityPool(int initialSize) {
            generations = new int[initialSize];
            poolSize = initialSize;
        }
        
        synchronized int obtain() {
            int index;
            if (freeIndices.isEmpty()) {
                if (nextIndex >= MAX_ENTITIES) {
                    throw new IllegalStateException("Entity limit exceeded");
                }
                index = nextIndex++;
                if (index >= poolSize) {
                    growPool();
                }
            } else {
                index = freeIndices.pop();
            }
            return index;
        }
        
        synchronized void release(int index) {
            if (index < 0 || index >= poolSize) {
                throw new IllegalArgumentException("Invalid entity index");
            }
            generations[index]++;
            freeIndices.push(index);
        }
        
        int getGeneration(int index) {
            if (index < 0 || index >= poolSize) {
                return -1;
            }
            return generations[index];
        }
        
        private void growPool() {
            int oldSize = poolSize;
            poolSize = poolSize * 2;
            generations = Arrays.copyOf(generations, poolSize);
            if (JavaBlocksEngine.get().getConfiguration().debugMode) {
                System.out.println("[ECS] Entity pool grown to " + poolSize);
            }
        }
        
        int size() {
            return nextIndex - freeIndices.size();
        }
        
        int capacity() {
            return poolSize;
        }
    }
    
    // ==================== Instance Variables ====================
    
    /** The entity index in the pool. */
    private final int index;
    
    /** The generation counter for this entity. */
    private final int generation;
    
    /** Cached hash code for performance. */
    private int hashCode;
    
    // ==================== Private Constructor ====================
    
    /**
     * Private constructor for entity factory pattern.
     * 
     * @param index The entity index
     * @param generation The entity generation
     */
    Entity(int index, int generation) {
        this.index = index;
        this.generation = generation;
        this.hashCode = Objects.hash(index, generation);
    }
    
    // ==================== Factory Methods ====================
    
    /**
     * Creates a new entity ID.
     * 
     * @param index The entity index
     * @param generation The entity generation
     * @return The new entity
     */
    static Entity create(int index, int generation) {
        return new Entity(index, generation);
    }
    
    /**
     * Gets the null entity.
     * 
     * @return A null entity
     */
    public static Entity nullEntity() {
        return new Entity(NULL_ID, 0);
    }
    
    // ==================== Accessors ====================
    
    /**
     * Gets the entity index.
     * 
     * @return The entity index
     */
    public int getIndex() {
        return index;
    }
    
    /**
     * Gets the entity generation.
     * 
     * @return The entity generation
     */
    public int getGeneration() {
        return generation;
    }
    
    /**
     * Checks if this entity is valid (not null).
     * 
     * @return true if this entity is valid
     */
    public boolean isValid() {
        return index != NULL_ID;
    }
    
    /**
     * Checks if this entity is the null entity.
     * 
     * @return true if this entity is null
     */
    public boolean isNull() {
        return index == NULL_ID;
    }
    
    // ==================== Comparison & Equality ====================
    
    /**
     * Checks if this entity equals another entity.
     * Entities are equal if they have the same index and generation.
     * 
     * @param obj The object to compare with
     * @return true if equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Entity other)) return false;
        return index == other.index && generation == other.generation;
    }
    
    /**
     * Compares this entity to another for ordering.
     * 
     * @param other The entity to compare with
     * @return Comparison result
     */
    @Override
    public int compareTo(Entity other) {
        int indexCompare = Integer.compare(index, other.index);
        if (indexCompare != 0) return indexCompare;
        return Integer.compare(generation, other.generation);
    }
    
    /**
     * Gets the hash code for this entity.
     * 
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    /**
     * Gets a string representation of this entity.
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        if (isNull()) {
            return "Entity[NULL]";
        }
        return "Entity[" + index + ":" + generation + "]";
    }
    
    // ==================== Packed Integer Operations ====================
    
    /**
     * Creates a packed integer from index and generation.
     * This is useful for compact storage.
     * 
     * @param index The entity index
     * @param generation The entity generation
     * @return The packed integer
     */
    public static long pack(int index, int generation) {
        return ((long) generation << INDEX_BITS) | (index & 0xFFFFFFFFL);
    }
    
    /**
     * Unpacks the index from a packed integer.
     * 
     * @param packed The packed integer
     * @return The index
     */
    public static int unpackIndex(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }
    
    /**
     * Unpacks the generation from a packed integer.
     * 
     * @param packed The packed integer
     * @return The generation
     */
    public static int unpackGeneration(long packed) {
        return (int) (packed >>> INDEX_BITS);
    }
}
