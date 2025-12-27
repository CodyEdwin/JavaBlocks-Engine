/*
 * JavaBlocks Engine - Resource Management
 * 
 * Central resource manager for loading and caching assets.
 */
package com.javablocks.core.resource;

import com.javablocks.core.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Central manager for resource loading and caching.
 * 
 * Features:
 * - Asynchronous loading
 * - Automatic caching
 * - Reference counting
 * - Dependency handling
 * - Resource groups
 * 
 * @author JavaBlocks Engine Team
 */
public final class ResourceManager {
    
    // ==================== Constants ====================
    
    /** Maximum concurrent async loads. */
    public static final int MAX_CONCURRENT_LOADS = 4;
    
    /** Default cache size. */
    public static final int DEFAULT_CACHE_SIZE = 100;
    
    // ==================== Instance Fields ====================
    
    /** Resource cache by path. */
    private final HashMap<String, Resource<?>> resourceCache;
    
    /** Resource loaders by type. */
    private final HashMap<Class<?>, ResourceLoader<?>> loaders;
    
    /** Loading tasks. */
    private final LinkedBlockingQueue<ResourceLoadTask<?>> loadingTasks;
    
    /** Active loading tasks. */
    private final HashMap<String, ResourceLoadTask<?>> activeTasks;
    
    /** Reference counts for resources. */
    private final HashMap<String, Integer> referenceCounts;
    
    /** Resource groups. */
    private final HashMap<String, HashSet<String>> resourceGroups;
    
    /** Default group name. */
    private static final String DEFAULT_GROUP = "default";
    
    /** Whether the manager is disposed. */
    private volatile boolean disposed;
    
    /** Engine reference. */
    private JavaBlocksEngine engine;
    
    // ==================== Constructor ====================
    
    /**
     * Creates a new resource manager.
     */
    public ResourceManager() {
        this.resourceCache = new HashMap<>();
        this.loaders = new HashMap<>();
        this.loadingTasks = new LinkedBlockingQueue<>();
        this.activeTasks = new HashMap<>();
        this.referenceCounts = new HashMap<>();
        this.resourceGroups = new HashMap<>();
        this.disposed = false;
        
        // Initialize default group
        resourceGroups.put(DEFAULT_GROUP, new HashSet<>());
        
        // Register default loaders
        registerDefaultLoaders();
    }
    
    /**
     * Initializes with engine reference.
     * 
     * @param engine The engine
     */
    void initialize(JavaBlocksEngine engine) {
        this.engine = engine;
    }
    
    // ==================== Default Loaders ====================
    
    /**
     * Registers the default resource loaders.
     */
    private void registerDefaultLoaders() {
        // Texture loader would be registered here
        // Sound loader would be registered here
        // Model loader would be registered here
    }
    
    // ==================== Loader Registration ====================
    
    /**
     * Registers a resource loader.
     * 
     * @param <T> Resource type
     * @param resourceClass Resource class
     * @param loader Loader for the resource type
     */
    public <T extends Resource> void registerLoader(
            Class<T> resourceClass, ResourceLoader<T> loader) {
        loaders.put(resourceClass, loader);
    }
    
    /**
     * Gets the loader for a resource type.
     * 
     * @param <T> Resource type
     * @param resourceClass Resource class
     * @return The loader, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends Resource> ResourceLoader<T> getLoader(Class<T> resourceClass) {
        return (ResourceLoader<T>) loaders.get(resourceClass);
    }
    
    // ==================== Resource Loading ====================
    
    /**
     * Synchronously loads a resource.
     * 
     * @param <T> Resource type
     * @param path Resource path
     * @param type Resource type class
     * @return The loaded resource
     */
    public <T extends Resource> T load(String path, Class<T> type) {
        // Check cache first
        @SuppressWarnings("unchecked")
        T cached = (T) resourceCache.get(path);
        if (cached != null) {
            incrementReference(path);
            return cached;
        }
        
        // Get loader
        ResourceLoader<T> loader = getLoader(type);
        if (loader == null) {
            throw new IllegalArgumentException(
                "No loader registered for type: " + type.getName()
            );
        }
        
        // Load resource
        T resource = loader.load(path);
        
        if (resource != null) {
            // Cache and reference
            resourceCache.put(path, resource);
            incrementReference(path);
            
            // Add to default group
            addToGroup(path, DEFAULT_GROUP);
            
            // Load dependencies
            String[] dependencies = resource.getDependencies();
            if (dependencies != null) {
                for (String dep : dependencies) {
                    load(dep, Resource.class);
                }
            }
        }
        
        return resource;
    }
    
    /**
     * Asynchronously loads a resource.
     * 
     * @param <T> Resource type
     * @param path Resource path
     * @param type Resource type class
     * @return Future for the loaded resource
     */
    public <T extends Resource> CompletableFuture<T> loadAsync(String path, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> load(path, type), r -> {
            // Would use engine's thread pool
            new Thread(r).start();
        });
    }
    
    /**
     * Loads a resource with a callback.
     * 
     * @param <T> Resource type
     * @param path Resource path
     * @param type Resource type class
     * @param callback Callback when loaded
     */
    public <T extends Resource> void loadAsync(String path, Class<T> type, 
            ResourceCallback<T> callback) {
        loadAsync(path, type).thenAccept(callback::onLoaded);
    }
    
    /**
     * Queues a resource for loading.
     * 
     * @param <T> Resource type
     * @param path Resource path
     * @param type Resource type class
     */
    public <T extends Resource> void queueLoad(String path, Class<T> type) {
        @SuppressWarnings("unchecked")
        ResourceLoadTask<T> task = new ResourceLoadTask<>(path, type, this);
        loadingTasks.offer(task);
    }
    
    // ==================== Resource Unloading ====================
    
    /**
     * Unloads a resource.
     * 
     * @param path Resource path
     * @return true if unloaded
     */
    public boolean unload(String path) {
        decrementReference(path);
        
        if (referenceCounts.getOrDefault(path, 0) <= 0) {
            // Remove from cache
            Resource<?> resource = resourceCache.remove(path);
            
            if (resource != null) {
                // Dispose resource
                resource.dispose();
                
                // Remove from groups
                for (HashSet<String> group : resourceGroups.values()) {
                    group.remove(path);
                }
                
                // Remove reference count
                referenceCounts.remove(path);
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Unloads all resources in a group.
     * 
     * @param groupName Group name
     */
    public void unloadGroup(String groupName) {
        HashSet<String> group = resourceGroups.get(groupName);
        if (group != null) {
            List<String> toUnload = new ArrayList<>(group);
            for (String path : toUnload) {
                unload(path);
            }
            group.clear();
        }
    }
    
    /**
     * Unloads all resources.
     */
    public void unloadAll() {
        List<String> paths = new ArrayList<>(resourceCache.keySet());
        for (String path : paths) {
            unload(path);
        }
    }
    
    // ==================== Reference Counting ====================
    
    /**
     * Increments the reference count for a resource.
     * 
     * @param path Resource path
     */
    private void incrementReference(String path) {
        referenceCounts.merge(path, 1, Integer::sum);
    }
    
    /**
     * Decrements the reference count for a resource.
     * 
     * @param path Resource path
     */
    private void decrementReference(String path) {
        referenceCounts.computeIfPresent(path, (k, v) -> Math.max(0, v - 1));
    }
    
    /**
     * Gets the reference count for a resource.
     * 
     * @param path Resource path
     * @return Reference count
     */
    public int getReferenceCount(String path) {
        return referenceCounts.getOrDefault(path, 0);
    }
    
    // ==================== Resource Groups ====================
    
    /**
     * Adds a resource to a group.
     * 
     * @param path Resource path
     * @param groupName Group name
     */
    public void addToGroup(String path, String groupName) {
        HashSet<String> group = resourceGroups.computeIfAbsent(
            groupName, k -> new HashSet<>()
        );
        group.add(path);
    }
    
    /**
     * Removes a resource from a group.
     * 
     * @param path Resource path
     * @param groupName Group name
     * @return true if removed
     */
    public boolean removeFromGroup(String path, String groupName) {
        HashSet<String> group = resourceGroups.get(groupName);
        return group != null && group.remove(path);
    }
    
    /**
     * Gets all resources in a group.
     * 
     * @param groupName Group name
     * @return Set of resource paths
     */
    public Set<String> getGroupResources(String groupName) {
        HashSet<String> group = resourceGroups.get(groupName);
        return group != null ? Collections.unmodifiableSet(group) : Collections.emptySet();
    }
    
    /**
     * Gets all group names.
     * 
     * @return Set of group names
     */
    public Set<String> getGroupNames() {
        return Collections.unmodifiableSet(resourceGroups.keySet());
    }
    
    // ==================== Resource Queries ====================
    
    /**
     * Checks if a resource is loaded.
     * 
     * @param path Resource path
     * @return true if loaded
     */
    public boolean isLoaded(String path) {
        return resourceCache.containsKey(path);
    }
    
    /**
     * Gets a loaded resource without loading.
     * 
     * @param <T> Resource type
     * @param path Resource path
     * @param type Resource type class
     * @return The resource, or null if not loaded
     */
    @SuppressWarnings("unchecked")
    public <T extends Resource> T get(String path, Class<T> type) {
        Resource<?> resource = resourceCache.get(path);
        if (resource != null && type.isInstance(resource)) {
            return (T) resource;
        }
        return null;
    }
    
    /**
     * Gets all loaded resources.
     * 
     * @return Map of paths to resources
     */
    public Map<String, Resource<?>> getAllResources() {
        return Collections.unmodifiableMap(resourceCache);
    }
    
    /**
     * Gets resources of a specific type.
     * 
     * @param <T> Resource type
     * @param type Resource type class
     * @return List of resources
     */
    @SuppressWarnings("unchecked")
    public <T extends Resource> List<T> getResourcesOfType(Class<T> type) {
        List<T> result = new ArrayList<>();
        
        for (Resource<?> resource : resourceCache.values()) {
            if (type.isInstance(resource)) {
                result.add((T) resource);
            }
        }
        
        return result;
    }
    
    // ==================== Update ====================
    
    /**
     * Processes queued resource loading tasks.
     * 
     * @param maxTasks Maximum tasks to process per frame
     */
    public void update(int maxTasks) {
        int processed = 0;
        
        while (processed < maxTasks) {
            ResourceLoadTask<?> task = loadingTasks.poll();
            if (task == null) {
                break;
            }
            
            try {
                task.load();
                processed++;
            } catch (Exception e) {
                System.err.println("[ResourceManager] Failed to load: " + task.path);
                e.printStackTrace();
            }
        }
    }
    
    // ==================== Lifecycle ====================
    
    /**
     * Disposes the resource manager.
     */
    public void dispose() {
        disposed = true;
        unloadAll();
        loaders.clear();
        resourceGroups.clear();
    }
    
    /**
     * Checks if disposed.
     * 
     * @return true if disposed
     */
    public boolean isDisposed() {
        return disposed;
    }
    
    // ==================== Internal Classes ====================
    
    /**
     * Resource load task for async loading.
     */
    private static final class ResourceLoadTask<T extends Resource> {
        final String path;
        final Class<T> type;
        final ResourceManager manager;
        
        ResourceLoadTask(String path, Class<T> type, ResourceManager manager) {
            this.path = path;
            this.type = type;
            this.manager = manager;
        }
        
        void load() {
            manager.load(path, type);
        }
    }
}
