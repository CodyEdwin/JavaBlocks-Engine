/*
 * JavaBlocks Engine - Resource Callback Interface
 * 
 * Callback interface for async resource loading.
 */
package com.javablocks.core.resource;

/**
 * Callback interface for async resource loading.
 * 
 * @author JavaBlocks Engine Team
 * @param <T> Resource type
 */
public interface ResourceCallback<T extends Resource> {
    
    /**
     * Called when a resource is loaded.
     * 
     * @param resource The loaded resource
     */
    void onLoaded(T resource);
    
    /**
     * Called if resource loading fails.
     * 
     * @param path Resource path
     * @param error Error message
     */
    void onError(String path, String error);
    
    /**
     * Called with loading progress.
     * 
     * @param path Resource path
     * @param progress Progress from 0 to 1
     */
    void onProgress(String path, float progress);
}
