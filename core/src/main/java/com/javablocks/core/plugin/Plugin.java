/*
 * JavaBlocks Engine - Plugin Interface
 * 
 * Base interface for all plugins.
 */
package com.javablocks.core.plugin;

import com.javablocks.core.*;

/**
 * Base interface for all plugins.
 * 
 * Plugins can extend the engine's functionality without modifying
 * the core codebase.
 * 
 * @author JavaBlocks Engine Team
 */
public interface Plugin {
    
    /**
     * Gets the unique plugin ID.
     * 
     * @return Plugin ID
     */
    String getId();
    
    /**
     * Gets the plugin name.
     * 
     * @return Plugin name
     */
    String getName();
    
    /**
     * Gets the plugin version.
     * 
     * @return Plugin version
     */
    String getVersion();
    
    /**
     * Gets the minimum engine version required.
     * 
     * @return Minimum engine version
     */
    String getMinimumEngineVersion();
    
    /**
     * Gets the plugin description.
     * 
     * @return Plugin description
     */
    String getDescription();
    
    /**
     * Gets the plugin author.
     * 
     * @return Plugin author
     */
    String getAuthor();
    
    /**
     * Gets the plugin website.
     * 
     * @return Plugin website URL
     */
    String getWebsite();
    
    /**
     * Initializes the plugin.
     * 
     * @param engine The engine instance
     */
    void initialize(JavaBlocksEngine engine);
    
    /**
     * Called when the engine starts.
     */
    void onEngineStart();
    
    /**
     * Called when the engine updates.
     * 
     * @param delta Time since last update in seconds
     */
    void onUpdate(float delta);
    
    /**
     * Called when the engine pauses.
     */
    void onPause();
    
    /**
     * Called when the engine resumes.
     */
    void onResume();
    
    /**
     * Disposes the plugin.
     * Called when unloading the plugin or shutting down.
     */
    void dispose();
    
    // ==================== Default Methods ====================
    
    /**
     * Checks if the plugin is compatible with the current engine version.
     * 
     * @param engineVersion Current engine version
     * @return true if compatible
     */
    default boolean isCompatible(String engineVersion) {
        String minVersion = getMinimumEngineVersion();
        if (minVersion == null || minVersion.isEmpty()) {
            return true;
        }
        return compareVersions(engineVersion, minVersion) >= 0;
    }
    
    /**
     * Compares two version strings.
     * 
     * @param v1 First version
     * @param v2 Second version
     * @return -1 if v1 < v2, 0 if equal, 1 if v1 > v2
     */
    default int compareVersions(String v1, String v2) {
        if (v1 == null) return -1;
        if (v2 == null) return 1;
        
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int length = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            
            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }
        
        return 0;
    }
    
    /**
     * Parses a version part to a number.
     * 
     * @param part Version part string
     * @return Parsed number
     */
    private int parseVersionPart(String part) {
        try {
            // Remove any non-numeric suffix (like "beta", "alpha")
            String numeric = part.split("[^0-9]")[0];
            return Integer.parseInt(numeric);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Gets the plugin icon resource path.
     * 
     * @return Icon path, or null if none
     */
    default String getIconPath() {
        return null;
    }
    
    /**
     * Checks if the plugin requires initialization before other plugins.
     * 
     * @return true if early initialization required
     */
    default boolean requiresEarlyInit() {
        return false;
    }
    
    /**
     * Gets plugin dependencies.
     * 
     * @return Array of dependency IDs
     */
    default String[] getDependencies() {
        return new String[0];
    }
}
