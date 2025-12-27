/*
 * JavaBlocks Engine - Plugin System
 * 
 * Extensible plugin architecture for adding functionality.
 */
package com.javablocks.core.plugin;

import com.javablocks.core.*;
import com.javablocks.core.ecs.*;
import java.util.*;

/**
 * Plugin manager for extensibility.
 * 
 * The plugin system allows third-party developers to extend the engine
 * without modifying the core codebase.
 * 
 * Features:
 * - Dynamic plugin loading
 * - Plugin lifecycle management
 * - Dependency management
 * - Plugin sandboxing
 * 
 * @author JavaBlocks Engine Team
 */
public final class PluginManager {
    
    // ==================== Constants ====================
    
    /** Plugin interface version. */
    public static final String PLUGIN_API_VERSION = "1.0.0";
    
    // ==================== Instance Fields ====================
    
    /** Loaded plugins. */
    private final HashMap<String, Plugin> plugins;
    
    /** Plugin class loader. */
    private final PluginClassLoader classLoader;
    
    /** Whether plugins are loaded. */
    private boolean pluginsLoaded;
    
    /** Engine reference. */
    private JavaBlocksEngine engine;
    
    // ==================== Constructor ====================
    
    /**
     * Creates a new plugin manager.
     */
    public PluginManager() {
        this.plugins = new HashMap<>();
        this.classLoader = new PluginClassLoader();
        this.pluginsLoaded = false;
    }
    
    // ==================== Plugin Loading ====================
    
    /**
     * Initializes the plugin manager with the engine.
     * 
     * @param engine The engine instance
     */
    void initialize(JavaBlocksEngine engine) {
        this.engine = engine;
    }
    
    /**
     * Loads all discoverable plugins.
     */
    public void loadPlugins() {
        if (pluginsLoaded) {
            return;
        }
        
        // Discover plugins from service loader
        ServiceLoader<Plugin> serviceLoader = ServiceLoader.load(Plugin.class, classLoader);
        
        for (Plugin plugin : serviceLoader) {
            loadPlugin(plugin);
        }
        
        pluginsLoaded = true;
        
        if (engine.getConfiguration().debugMode) {
            System.out.println("[PluginManager] Loaded " + plugins.size() + " plugins");
        }
    }
    
    /**
     * Loads a single plugin.
     * 
     * @param plugin The plugin to load
     */
    private void loadPlugin(Plugin plugin) {
        String pluginId = plugin.getId();
        
        if (plugins.containsKey(pluginId)) {
            if (engine.getConfiguration().debugMode) {
                System.out.println("[PluginManager] Plugin already loaded: " + pluginId);
            }
            return;
        }
        
        try {
            // Initialize plugin
            plugin.initialize(engine);
            
            // Store plugin
            plugins.put(pluginId, plugin);
            
            if (engine.getConfiguration().debugMode) {
                System.out.println("[PluginManager] Loaded plugin: " + pluginId + " v" + plugin.getVersion());
            }
            
        } catch (Exception e) {
            System.err.println("[PluginManager] Failed to load plugin: " + pluginId);
            e.printStackTrace();
        }
    }
    
    /**
     * Loads a plugin from a class.
     * 
     * @param pluginClass The plugin class
     * @return The loaded plugin
     */
    public Plugin loadPlugin(Class<? extends Plugin> pluginClass) {
        try {
            Plugin plugin = pluginClass.getDeclaredConstructor().newInstance();
            loadPlugin(plugin);
            return plugin;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate plugin: " + pluginClass.getName(), e);
        }
    }
    
    /**
     * Loads a plugin from a JAR file.
     * 
     * @param jarPath Path to the JAR file
     * @return The loaded plugin
     */
    public Plugin loadPluginFromJar(String jarPath) {
        try {
            Class<? extends Plugin> pluginClass = classLoader.loadClassFromJar(jarPath);
            return loadPlugin(pluginClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load plugin from JAR: " + jarPath, e);
        }
    }
    
    // ==================== Plugin Unloading ====================
    
    /**
     * Unloads a plugin.
     * 
     * @param pluginId The plugin ID
     * @return true if the plugin was unloaded
     */
    public boolean unloadPlugin(String pluginId) {
        Plugin plugin = plugins.remove(pluginId);
        
        if (plugin != null) {
            try {
                plugin.dispose();
                
                if (engine.getConfiguration().debugMode) {
                    System.out.println("[PluginManager] Unloaded plugin: " + pluginId);
                }
                
                return true;
            } catch (Exception e) {
                System.err.println("[PluginManager] Error unloading plugin: " + pluginId);
                e.printStackTrace();
            }
        }
        
        return false;
    }
    
    /**
     * Unloads all plugins.
     */
    public void unloadPlugins() {
        for (Plugin plugin : plugins.values()) {
            try {
                plugin.dispose();
            } catch (Exception e) {
                System.err.println("[PluginManager] Error disposing plugin: " + plugin.getId());
                e.printStackTrace();
            }
        }
        
        plugins.clear();
        pluginsLoaded = false;
        
        if (engine.getConfiguration().debugMode) {
            System.out.println("[PluginManager] Unloaded all plugins");
        }
    }
    
    /**
     * Reloads all plugins.
     */
    public void reloadPlugins() {
        unloadPlugins();
        loadPlugins();
    }
    
    // ==================== Plugin Access ====================
    
    /**
     * Gets a loaded plugin by ID.
     * 
     * @param pluginId The plugin ID
     * @return The plugin, or null if not found
     */
    public Plugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }
    
    /**
     * Gets a plugin cast to a specific type.
     * 
     * @param pluginId The plugin ID
     * @param <T> Plugin type
     * @param pluginClass Plugin class
     * @return The plugin cast to type
     */
    @SuppressWarnings("unchecked")
    public <T extends Plugin> T getPlugin(String pluginId, Class<T> pluginClass) {
        Plugin plugin = getPlugin(pluginId);
        if (plugin != null && pluginClass.isInstance(plugin)) {
            return (T) plugin;
        }
        return null;
    }
    
    /**
     * Gets all loaded plugins.
     * 
     * @return Collection of plugins
     */
    public Collection<Plugin> getPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }
    
    /**
     * Gets plugins of a specific type.
     * 
     * @param <T> Plugin type
     * @param pluginClass Plugin class
     * @return List of plugins
     */
    @SuppressWarnings("unchecked")
    public <T extends Plugin> List<T> getPlugins(Class<T> pluginClass) {
        List<T> result = new ArrayList<>();
        
        for (Plugin plugin : plugins.values()) {
            if (pluginClass.isInstance(plugin)) {
                result.add((T) plugin);
            }
        }
        
        return result;
    }
    
    /**
     * Checks if a plugin is loaded.
     * 
     * @param pluginId The plugin ID
     * @return true if loaded
     */
    public boolean hasPlugin(String pluginId) {
        return plugins.containsKey(pluginId);
    }
    
    /**
     * Gets the number of loaded plugins.
     * 
     * @return Plugin count
     */
    public int getPluginCount() {
        return plugins.size();
    }
    
    // ==================== Plugin Lifecycle ====================
    
    /**
     * Called when the engine starts.
     */
    void onEngineStart() {
        for (Plugin plugin : plugins.values()) {
            try {
                plugin.onEngineStart();
            } catch (Exception e) {
                System.err.println("[PluginManager] Error in onEngineStart for: " + plugin.getId());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Called when the engine updates.
     * 
     * @param delta Time since last update
     */
    void onUpdate(float delta) {
        for (Plugin plugin : plugins.values()) {
            try {
                plugin.onUpdate(delta);
            } catch (Exception e) {
                System.err.println("[PluginManager] Error in onUpdate for: " + plugin.getId());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Called when the engine pauses.
     */
    void onPause() {
        for (Plugin plugin : plugins.values()) {
            try {
                plugin.onPause();
            } catch (Exception e) {
                System.err.println("[PluginManager] Error in onPause for: " + plugin.getId());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Called when the engine resumes.
     */
    void onResume() {
        for (Plugin plugin : plugins.values()) {
            try {
                plugin.onResume();
            } catch (Exception e) {
                System.err.println("[PluginManager] Error in onResume for: " + plugin.getId());
                e.printStackTrace();
            }
        }
    }
    
    // ==================== Debug Information ====================
    
    /**
     * Gets debug information about the plugin manager.
     * 
     * @return Map of debug information
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("Plugin Count", plugins.size());
        info.put("Plugins Loaded", pluginsLoaded);
        
        List<String> pluginList = new ArrayList<>();
        for (Plugin plugin : plugins.values()) {
            pluginList.add(plugin.getId() + " v" + plugin.getVersion());
        }
        info.put("Plugins", pluginList);
        
        return info;
    }
    
    /**
     * Prints debug information to console.
     */
    public void printDebugInfo() {
        if (engine.getConfiguration().debugMode) {
            System.out.println("\n=== PluginManager Debug Info ===");
            getDebugInfo().forEach((key, value) -> 
                System.out.println(String.format("  %-25s: %s", key, value)));
            System.out.println("================================\n");
        }
    }
}
