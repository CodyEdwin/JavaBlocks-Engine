/*
 * JavaBlocks Engine - Lifetime Component
 * 
 * Provides automatic entity destruction after a specified time.
 */
package com.javablocks.core.components;

import com.javablocks.core.ecs.Component;

/**
 * Component for managing entity lifetime.
 * 
 * Provides automatic entity destruction after a specified time.
 * Useful for particles, projectiles, and temporary effects.
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
