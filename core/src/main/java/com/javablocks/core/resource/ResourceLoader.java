/*
 * JavaBlocks Engine - Resource Loader Interface
 * 
 * Interface for resource loaders.
 */
package com.javablocks.core.resource;

/**
 * Interface for resource loaders.
 * 
 * Resource loaders are responsible for loading resources from
 * various sources (files, streams, URLs, etc.).
 * 
 * @author JavaBlocks Engine Team
 * @param <T> Resource type
 */
public interface ResourceLoader<T extends Resource> {
    
    /**
     * Checks if this loader can load a resource.
     * 
     * @param path Resource path
     * @return true if this loader can handle the path
     */
    boolean canLoad(String path);
    
    /**
     * Gets the file extensions this loader supports.
     * 
     * @return Array of file extensions (without dot)
     */
    String[] getExtensions();
    
    /**
     * Loads a resource from a path.
     * 
     * @param path Resource path
     * @return The loaded resource
     */
    T load(String path);
    
    /**
     * Loads a resource from binary data.
     * 
     * @param path Resource path
     * @param data Binary data
     * @return The loaded resource
     */
    T load(String path, byte[] data);
    
    /**
     * Gets the loading priority.
     * 
     * @return Priority value (higher = loaded first)
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Checks if this loader supports streaming.
     * 
     * @return true if supports streaming
     */
    default boolean supportsStreaming() {
        return false;
    }
    
    /**
     * Cancels a loading operation.
     * 
     * @param resource The resource being loaded
     */
    default void cancelLoad(T resource) {
        // Override in streaming loaders
    }
}
