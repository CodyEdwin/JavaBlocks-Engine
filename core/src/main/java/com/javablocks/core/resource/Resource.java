/*
 * JavaBlocks Engine - Resource Base Interface
 * 
 * Base interface for all resources.
 */
package com.javablocks.core.resource;

/**
 * Base interface for all resources.
 * 
 * Resources are assets that can be loaded from files or generated.
 * They are managed by the ResourceManager.
 * 
 * @param <T> The type of resource data
 * @author JavaBlocks Engine Team
 */
public interface Resource<T> {
    
    /**
     * Gets the resource path.
     * 
     * @return Resource path
     */
    String getPath();
    
    /**
     * Gets the resource name.
     * 
     * @return Resource name
     */
    String getName();
    
    /**
     * Gets the resource type.
     * 
     * @return Resource type name
     */
    String getType();
    
    /**
     * Checks if the resource is loaded.
     * 
     * @return true if loaded
     */
    boolean isLoaded();
    
    /**
     * Gets the resource size in bytes.
     * 
     * @return Size in bytes
     */
    long getSize();
    
    /**
     * Gets resource dependencies.
     * 
     * @return Array of dependency paths
     */
    String[] getDependencies();
    
    /**
     * Gets the loading progress.
     * 
     * @return Progress from 0 to 1
     */
    float getProgress();
    
    /**
     * Reloads the resource.
     * 
     * @return true if reloaded successfully
     */
    boolean reload();
    
    /**
     * Disposes the resource.
     * Frees all native resources.
     */
    void dispose();
    
    /**
     * Gets a unique ID for this resource.
     * 
     * @return Resource ID
     */
    default String getId() {
        return getPath();
    }
    
    /**
     * Gets the resource data.
     * 
     * @return The resource data
     */
    T getData();
}
