/*
 * JavaBlocks Engine - Scene Manager
 * 
 * Central manager for scene operations and scene tree management.
 */
package com.javablocks.core.scene;

import com.javablocks.core.*;
import com.javablocks.core.ecs.*;
import com.javablocks.core.events.*;
import java.util.*;

/**
 * Central manager for scene operations.
 * 
 * The scene manager handles:
 * - Current scene management
 * - Scene transitions
 * - Scene loading and saving
 * - Multiple scene support (subscenes)
 * 
 * @author JavaBlocks Engine Team
 */
public class SceneManager {
    
    // ==================== Constants ====================
    
    /** Default scene transition time. */
    public static final float DEFAULT_TRANSITION_TIME = 0.5f;
    
    // ==================== Instance Fields ====================
    
    /** Current active scene. */
    private Scene currentScene;
    
    /** Pending scene for transitions. */
    private Scene pendingScene;
    
    /** Scene transition state. */
    private TransitionState transitionState;
    
    /** Transition progress (0-1). */
    private float transitionProgress;
    
    /** Transition duration. */
    private float transitionDuration;
    
    /** Current scene signals. */
    public final Signal<Scene> sceneChanged;
    public final Signal<Scene> sceneLoading;
    public final Signal<Scene> sceneLoaded;
    public final Signal<Scene> sceneUnloading;
    public final Signal<Scene> sceneUnloaded;
    public final Signal<TransitionInfo> sceneTransitionStarted;
    public final Signal<TransitionInfo> sceneTransitionFinished;
    
    /** All loaded scenes. */
    private final HashMap<String, Scene> loadedScenes;
    
    /** Default scene name. */
    private String defaultSceneName;
    
    // ==================== Transition Info ====================
    
    /**
     * Information about a scene transition.
     */
    public record TransitionInfo(
        Scene fromScene,
        Scene toScene,
        float progress,
        float duration
    ) {}
    
    /**
     * Scene transition state.
     */
    private enum TransitionState {
        IDLE,
        TRANSITIONING_IN,
        TRANSITIONING_OUT
    }
    
    // ==================== Constructor ====================
    
    /**
     * Creates a new scene manager.
     */
    public SceneManager() {
        this.currentScene = null;
        this.pendingScene = null;
        this.transitionState = TransitionState.IDLE;
        this.transitionProgress = 0;
        this.transitionDuration = DEFAULT_TRANSITION_TIME;
        
        // Initialize signals
        this.sceneChanged = new Signal<>();
        this.sceneLoading = new Signal<>();
        this.sceneLoaded = new Signal<>();
        this.sceneUnloading = new Signal<>();
        this.sceneUnloaded = new Signal<>();
        this.sceneTransitionStarted = new Signal<>();
        this.sceneTransitionFinished = new Signal<>();
        
        this.loadedScenes = new HashMap<>();
        this.defaultSceneName = "Main";
    }
    
    // ==================== Scene Creation ====================
    
    /**
     * Creates a new scene with the specified name.
     * 
     * @param name The scene name
     * @return The created scene
     */
    public Scene createScene(String name) {
        Scene scene = new Scene(name);
        loadedScenes.put(name, scene);
        sceneLoaded.dispatch(scene);
        return scene;
    }
    
    /**
     * Creates a new scene and sets it as current.
     * 
     * @param name The scene name
     * @return The created scene
     */
    public Scene createAndSetScene(String name) {
        Scene scene = createScene(name);
        setCurrentScene(scene);
        return scene;
    }
    
    /**
     * Creates a default scene if none exists.
     * 
     * @return The default scene
     */
    public Scene createDefaultSceneIfNeeded() {
        if (currentScene == null) {
            return createAndSetScene(defaultSceneName);
        }
        return currentScene;
    }
    
    // ==================== Scene Management ====================
    
    /**
     * Gets the current active scene.
     * 
     * @return The current scene, or null if none
     */
    public Scene getCurrentScene() {
        return currentScene;
    }
    
    /**
     * Sets the current scene.
     * 
     * @param scene The scene to set as current
     */
    public void setCurrentScene(Scene scene) {
        if (scene == currentScene) {
            return;
        }
        
        Scene previousScene = currentScene;
        currentScene = scene;
        
        // Emit signals
        if (previousScene != null) {
            previousScene.deactivate();
            sceneUnloading.dispatch(previousScene);
        }
        
        if (scene != null) {
            sceneLoading.dispatch(scene);
            scene.activate();
            sceneLoaded.dispatch(scene);
        }
        
        sceneChanged.dispatch(scene);
    }
    
    /**
     * Changes to a different scene.
     * 
     * @param sceneName The name of the scene to change to
     * @return The scene, or null if not found
     */
    public Scene changeScene(String sceneName) {
        Scene scene = loadedScenes.get(sceneName);
        if (scene != null) {
            setCurrentScene(scene);
        }
        return scene;
    }
    
    /**
     * Changes to a scene with a transition.
     * 
     * @param scene The scene to change to
     * @param duration Transition duration in seconds
     */
    public void transitionToScene(Scene scene, float duration) {
        if (scene == null) {
            return;
        }
        
        if (transitionState != TransitionState.IDLE) {
            // Cancel current transition
            finishTransition();
        }
        
        pendingScene = scene;
        transitionDuration = duration;
        transitionProgress = 0;
        transitionState = TransitionState.TRANSITIONING_OUT;
        
        // Emit transition started signal
        sceneTransitionStarted.dispatch(new TransitionInfo(
            currentScene, pendingScene, 0, duration
        ));
        
        // Emit unload signal for current scene
        if (currentScene != null) {
            sceneUnloading.dispatch(currentScene);
        }
    }
    
    /**
     * Changes to a scene with default transition.
     * 
     * @param scene The scene to change to
     */
    public void transitionToScene(Scene scene) {
        transitionToScene(scene, DEFAULT_TRANSITION_TIME);
    }
    
    /**
     * Changes to a scene by name with transition.
     * 
     * @param sceneName The name of the scene
     * @param duration Transition duration
     * @return The scene, or null if not found
     */
    public Scene transitionToScene(String sceneName, float duration) {
        Scene scene = loadedScenes.get(sceneName);
        if (scene != null) {
            transitionToScene(scene, duration);
        }
        return scene;
    }
    
    /**
     * Changes to a scene by name with default transition.
     * 
     * @param sceneName The name of the scene
     * @return The scene, or null if not found
     */
    public Scene transitionToScene(String sceneName) {
        return transitionToScene(sceneName, DEFAULT_TRANSITION_TIME);
    }
    
    // ==================== Update ====================
    
    /**
     * Updates the scene manager.
     * 
     * @param delta Time since last update in seconds
     */
    public void update(float delta) {
        // Update current scene
        if (currentScene != null && currentScene.isActive()) {
            currentScene.update(delta);
        }
        
        // Handle transitions
        if (transitionState != TransitionState.IDLE) {
            updateTransition(delta);
        }
    }
    
    /**
     * Updates the scene transition.
     */
    private void updateTransition(float delta) {
        transitionProgress += delta;
        float t = Math.min(1, transitionProgress / transitionDuration);
        
        TransitionInfo info = new TransitionInfo(currentScene, pendingScene, t, transitionDuration);
        
        if (transitionState == TransitionState.TRANSITIONING_OUT) {
            if (t >= 1) {
                // Finish transition out
                finishTransitionOut();
            }
        } else if (transitionState == TransitionState.TRANSITIONING_IN) {
            if (t >= 1) {
                // Finish transition in
                finishTransition();
            }
        }
    }
    
    /**
     * Finishes the transition out phase.
     */
    private void finishTransitionOut() {
        // Deactivate and unload old scene
        if (currentScene != null) {
            currentScene.deactivate();
            sceneUnloaded.dispatch(currentScene);
        }
        
        // Set new scene
        currentScene = pendingScene;
        
        if (currentScene != null) {
            // Emit loading signal
            sceneLoading.dispatch(currentScene);
            
            // Activate new scene
            currentScene.activate();
            sceneLoaded.dispatch(currentScene);
        }
        
        // Start transition in
        transitionState = TransitionState.TRANSITIONING_IN;
        transitionProgress = 0;
    }
    
    /**
     * Finishes the scene transition.
     */
    private void finishTransition() {
        transitionState = TransitionState.IDLE;
        transitionProgress = 0;
        
        // Emit transition finished signal
        sceneTransitionFinished.dispatch(new TransitionInfo(
            currentScene, pendingScene, 1, transitionDuration
        ));
        
        pendingScene = null;
    }
    
    /**
     * Cancels the current transition.
     */
    public void cancelTransition() {
        if (transitionState != TransitionState.IDLE) {
            finishTransition();
        }
    }
    
    // ==================== Scene Loading/Saving ====================
    
    /**
     * Saves a scene to a string representation.
     * 
     * @param scene The scene to save
     * @return JSON or custom format string
     */
    public String saveScene(Scene scene) {
        // Placeholder for scene serialization
        // Would use JSON serialization with reflection or annotations
        return "{}";
    }
    
    /**
     * Loads a scene from a string representation.
     * 
     * @param data The serialized scene data
     * @return The loaded scene
     */
    public Scene loadScene(String data) {
        // Placeholder for scene deserialization
        return createScene("Loaded Scene");
    }
    
    // ==================== Scene Queries ====================
    
    /**
     * Gets a loaded scene by name.
     * 
     * @param name The scene name
     * @return The scene, or null if not found
     */
    public Scene getScene(String name) {
        return loadedScenes.get(name);
    }
    
    /**
     * Checks if a scene is loaded.
     * 
     * @param name The scene name
     * @return true if loaded
     */
    public boolean hasScene(String name) {
        return loadedScenes.containsKey(name);
    }
    
    /**
     * Gets all loaded scenes.
     * 
     * @return Collection of loaded scenes
     */
    public Collection<Scene> getLoadedScenes() {
        return Collections.unmodifiableCollection(loadedScenes.values());
    }
    
    /**
     * Unloads a scene.
     * 
     * @param name The scene name
     * @return true if the scene was unloaded
     */
    public boolean unloadScene(String name) {
        Scene scene = loadedScenes.remove(name);
        if (scene != null) {
            if (scene == currentScene) {
                setCurrentScene(null);
            }
            sceneUnloaded.dispatch(scene);
            return true;
        }
        return false;
    }
    
    /**
     * Unloads all scenes.
     */
    public void unloadAllScenes() {
        List<String> names = new ArrayList<>(loadedScenes.keySet());
        for (String name : names) {
            unloadScene(name);
        }
    }
    
    // ==================== Node Operations ====================
    
    /**
     * Creates a new node in the current scene.
     * 
     * @param name The node name
     * @return The created node
     */
    public Node createNode(String name) {
        Node node = new Node(name);
        
        if (currentScene != null) {
            currentScene.addNode(node);
        }
        
        return node;
    }
    
    /**
     * Adds a node to the current scene.
     * 
     * @param node The node to add
     * @throws IllegalStateException if no current scene
     */
    public void addNode(Node node) {
        if (currentScene == null) {
            throw new IllegalStateException("No current scene. Create or set a scene first.");
        }
        currentScene.addNode(node);
    }
    
    /**
     * Removes a node from the current scene.
     * 
     * @param node The node to remove
     * @return true if removed
     */
    public boolean removeNode(Node node) {
        if (currentScene == null) {
            return false;
        }
        return currentScene.removeNode(node);
    }
    
    /**
     * Adds a child node to a parent.
     * 
     * @param parent The parent node
     * @param child The child node to add
     */
    public void addChild(Node parent, Node child) {
        parent.addChild(child);
        
        if (currentScene != null && !currentScene.getAllNodes().contains(child)) {
            currentScene.addNode(child);
        }
    }
    
    /**
     * Removes a node from its parent.
     * 
     * @param node The node to remove
     * @return The parent node, or null if no parent
     */
    public Node removeChild(Node node) {
        Node parent = node.getParent();
        if (parent != null) {
            parent.removeChild(node);
        }
        return parent;
    }
    
    // ==================== Find Operations ====================
    
    /**
     * Finds a node by name in the current scene.
     * 
     * @param name The node name
     * @return The found node, or null
     */
    public Node findNode(String name) {
        return currentScene != null ? currentScene.findNode(name) : null;
    }
    
    /**
     * Finds a node by path in the current scene.
     * 
     * @param path The node path
     * @return The found node, or null
     */
    public Node findNodeByPath(String path) {
        return currentScene != null ? currentScene.findNodeByPath(path) : null;
    }
    
    /**
     * Finds all nodes of a type in the current scene.
     * 
     * @param <T> The node type
     * @param type The class to match
     * @return List of matching nodes
     */
    public <T extends Node> List<T> findNodesOfType(Class<T> type) {
        return currentScene != null ? currentScene.findNodesOfType(type) : Collections.emptyList();
    }
    
    // ==================== Transition State ====================
    
    /**
     * Checks if a transition is in progress.
     * 
     * @return true if transitioning
     */
    public boolean isTransitioning() {
        return transitionState != TransitionState.IDLE;
    }
    
    /**
     * Gets the current transition progress.
     * 
     * @return Progress from 0 to 1
     */
    public float getTransitionProgress() {
        return transitionProgress / transitionDuration;
    }
    
    /**
     * Gets the transition state.
     * 
     * @return The transition state
     */
    public String getTransitionState() {
        return transitionState.toString();
    }
    
    // ==================== Lifecycle ====================
    
    /**
     * Disposes the scene manager.
     */
    public void dispose() {
        unloadAllScenes();
        
        currentScene = null;
        pendingScene = null;
        
        // Clear signals
        sceneChanged.unsubscribeAll();
        sceneLoading.unsubscribeAll();
        sceneLoaded.unsubscribeAll();
        sceneUnloading.unsubscribeAll();
        sceneUnloaded.unsubscribeAll();
    }
    
    // ==================== Debug Information ====================
    
    /**
     * Gets debug information about the scene manager.
     * 
     * @return Map of debug information
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("Current Scene", currentScene != null ? currentScene.getName() : "None");
        info.put("Loaded Scenes", loadedScenes.size());
        info.put("Transitioning", isTransitioning());
        info.put("Transition State", transitionState.toString());
        info.put("Transition Progress", getTransitionProgress());
        
        if (currentScene != null) {
            info.put("Current Node Count", currentScene.getNodeCount());
        }
        
        return info;
    }
    
    /**
     * Prints debug information to console.
     */
    public void printDebugInfo() {
        if (com.javablocks.core.JavaBlocksEngine.get() != null && 
            com.javablocks.core.JavaBlocksEngine.get().getConfiguration().debugMode) {
            System.out.println("\n=== SceneManager Debug Info ===");
            getDebugInfo().forEach((key, value) -> 
                System.out.println(String.format("  %-25s: %s", key, value)));
            System.out.println("===============================\n");
        }
    }
}
