/*
 * JavaBlocks Engine - Scene Management
 * 
 * Scene class for organizing nodes and managing the scene tree.
 */
package com.javablocks.core.scene;

import com.javablocks.core.*;
import com.javablocks.core.ecs.*;
import com.javablocks.core.events.*;
import java.util.*;

/**
 * Scene is a container for nodes that form a scene tree.
 * 
 * Scenes are the top-level containers for game content. Each scene
 * has a single root node, and all other nodes in the scene are
 * descendants of this root.
 * 
 * Features:
 * - Hierarchical node management
 * - Group-based node organization
 * - Node finding by name or path
 * - Processing management
 * 
 * @author JavaBlocks Engine Team
 */
public class Scene {
    
    // ==================== Constants ====================
    
    /** Default scene name. */
    public static final String DEFAULT_NAME = "New Scene";
    
    // ==================== Instance Fields ====================
    
    /** Scene name. */
    private String name;
    
    /** Root node of the scene. */
    private Node root;
    
    /** Nodes indexed by name (for fast lookup). */
    private final HashMap<String, List<Node>> nodesByName;
    
    /** Nodes indexed by group. */
    private final HashMap<String, Set<Node>> nodesByGroup;
    
    /** All nodes in the scene. */
    private final HashSet<Node> allNodes;
    
    /** Number of nodes in the scene. */
    private int nodeCount;
    
    /** Whether the scene is currently active. */
    private boolean active;
    
    /** Scene signals. */
    public final Signal<Scene> sceneLoaded;
    public final Signal<Scene> sceneUnloaded;
    public final Signal<Node> nodeAdded;
    public final Signal<Node> nodeRemoved;
    
    // ==================== Constructor ====================
    
    /**
     * Creates a new unnamed scene with a default root node.
     */
    public Scene() {
        this(DEFAULT_NAME);
    }
    
    /**
     * Creates a new scene with a name.
     * 
     * @param name The scene name
     */
    public Scene(String name) {
        this.name = name != null ? name : DEFAULT_NAME;
        this.nodesByName = new HashMap<>();
        this.nodesByGroup = new HashMap<>();
        this.allNodes = new HashSet<>();
        this.nodeCount = 0;
        this.active = false;
        
        // Initialize signals
        this.sceneLoaded = new Signal<>();
        this.sceneUnloaded = new Signal<>();
        this.nodeAdded = new Signal<>();
        this.nodeRemoved = new Signal<>();
        
        // Create root node
        this.root = new Node("Root");
        addNode(root);
    }
    
    // ==================== Node Management ====================
    
    /**
     * Adds a node to the scene.
     * 
     * @param node The node to add
     * @throws IllegalArgumentException if node is null or already in scene
     */
    public void addNode(Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        
        if (allNodes.contains(node)) {
            throw new IllegalArgumentException("Node is already in this scene");
        }
        
        // Add to internal structures
        allNodes.add(node);
        nodeCount++;
        
        // Index by name
        List<Node> nameList = nodesByName.computeIfAbsent(node.getName(), k -> new ArrayList<>());
        nameList.add(node);
        
        // Index by groups
        for (String group : node.getGroups()) {
            addNodeToGroup(node, group);
        }
        
        // Set scene reference
        node.setScene(this);
        
        // Emit signal
        nodeAdded.dispatch(node);
    }
    
    /**
     * Removes a node from the scene.
     * 
     * @param node The node to remove
     * @return true if the node was found and removed
     */
    public boolean removeNode(Node node) {
        if (node == null || !allNodes.contains(node)) {
            return false;
        }
        
        // Remove from groups
        for (String group : node.getGroups()) {
            removeNodeFromGroup(node, group);
        }
        
        // Remove from name index
        List<Node> nameList = nodesByName.get(node.getName());
        if (nameList != null) {
            nameList.remove(node);
            if (nameList.isEmpty()) {
                nodesByName.remove(node.getName());
            }
        }
        
        // Remove from scene
        allNodes.remove(node);
        nodeCount--;
        
        // Clear scene reference
        node.clearScene();
        
        // Emit signal
        nodeRemoved.dispatch(node);
        
        return true;
    }
    
    /**
     * Removes all nodes from the scene except the root.
     */
    public void clear() {
        List<Node> nodesToRemove = new ArrayList<>(allNodes);
        nodesToRemove.remove(root);
        
        for (Node node : nodesToRemove) {
            removeNode(node);
        }
    }
    
    // ==================== Group Management ====================
    
    /**
     * Adds a node to a group.
     * 
     * @param node The node to add
     * @param group The group name
     */
    public void addNodeToGroup(Node node, String group) {
        if (group == null || group.isEmpty()) {
            return;
        }
        
        Set<Node> groupSet = nodesByGroup.computeIfAbsent(group, k -> new HashSet<>());
        groupSet.add(node);
    }
    
    /**
     * Removes a node from a group.
     * 
     * @param node The node to remove
     * @param group The group name
     */
    public void removeNodeFromGroup(Node node, String group) {
        if (group == null || group.isEmpty()) {
            return;
        }
        
        Set<Node> groupSet = nodesByGroup.get(group);
        if (groupSet != null) {
            groupSet.remove(node);
            if (groupSet.isEmpty()) {
                nodesByGroup.remove(group);
            }
        }
    }
    
    /**
     * Gets all nodes in a group.
     * 
     * @param group The group name
     * @return Set of nodes in the group
     */
    public Set<Node> getNodesInGroup(String group) {
        Set<Node> nodes = nodesByGroup.get(group);
        return nodes != null ? Collections.unmodifiableSet(nodes) : Collections.emptySet();
    }
    
    /**
     * Checks if a group exists.
     * 
     * @param group The group name
     * @return true if the group exists
     */
    public boolean hasGroup(String group) {
        return nodesByGroup.containsKey(group);
    }
    
    /**
     * Gets all groups in the scene.
     * 
     * @return Set of group names
     */
    public Set<String> getAllGroups() {
        return Collections.unmodifiableSet(nodesByGroup.keySet());
    }
    
    // ==================== Node Finding ====================
    
    /**
     * Finds a node by name.
     * 
     * @param name The node name
     * @return The first node with that name, or null
     */
    public Node findNode(String name) {
        List<Node> nodes = nodesByName.get(name);
        return nodes != null && !nodes.isEmpty() ? nodes.get(0) : null;
    }
    
    /**
     * Finds all nodes with a name.
     * 
     * @param name The node name
     * @return List of nodes with that name
     */
    public List<Node> findNodes(String name) {
        List<Node> nodes = nodesByName.get(name);
        return nodes != null ? Collections.unmodifiableList(nodes) : Collections.emptyList();
    }
    
    /**
     * Finds a node by path.
     * 
     * @param path The node path (e.g., "/Root/Child/Grandchild")
     * @return The found node, or null
     */
    public Node findNodeByPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        // Remove leading slash
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        String[] parts = path.split("/");
        if (parts.length == 0) {
            return null;
        }
        
        Node current = root;
        
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            
            Node found = null;
            for (Node child : current.getChildren()) {
                if (child.getName().equals(part)) {
                    found = child;
                    break;
                }
            }
            
            if (found == null) {
                return null;
            }
            
            current = found;
        }
        
        return current;
    }
    
    /**
     * Finds all nodes of a specific type.
     * 
     * @param <T> The node type
     * @param type The class to match
     * @return List of matching nodes
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> List<T> findNodesOfType(Class<T> type) {
        List<T> result = new ArrayList<>();
        
        for (Node node : allNodes) {
            if (type.isInstance(node)) {
                result.add((T) node);
            }
        }
        
        return result;
    }
    
    // ==================== Processing ====================
    
    /**
     * Updates all processing nodes in the scene.
     * 
     * @param delta Time since last frame in seconds
     */
    public void update(float delta) {
        // Collect processing nodes and sort by priority
        List<Node> processingNodes = new ArrayList<>();
        
        for (Node node : allNodes) {
            if (node.isInsideTree() && node.isProcessing() && node.canProcess()) {
                processingNodes.add(node);
            }
        }
        
        // Sort by priority
        processingNodes.sort(Comparator.comparingInt(Node::getProcessPriority));
        
        // Update all
        for (Node node : processingNodes) {
            node.process(delta);
        }
    }
    
    // ==================== Lifecycle ====================
    
    /**
     * Activates the scene.
     */
    public void activate() {
        if (!active) {
            active = true;
            sceneLoaded.dispatch(this);
        }
    }
    
    /**
     * Deactivates the scene.
     */
    public void deactivate() {
        if (active) {
            active = false;
            sceneUnloaded.dispatch(this);
        }
    }
    
    /**
     * Checks if the scene is active.
     * 
     * @return true if active
     */
    public boolean isActive() {
        return active;
    }
    
    // ==================== Property Accessors ====================
    
    /**
     * Gets the scene name.
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the scene name.
     * 
     * @param name The new name
     */
    public void setName(String name) {
        this.name = name != null ? name : DEFAULT_NAME;
    }
    
    /**
     * Gets the root node.
     * 
     * @return The root node
     */
    public Node getRoot() {
        return root;
    }
    
    /**
     * Gets the number of nodes in the scene.
     * 
     * @return The node count
     */
    public int getNodeCount() {
        return nodeCount;
    }
    
    /**
     * Gets all nodes in the scene.
     * 
     * @return Unmodifiable set of all nodes
     */
    public Set<Node> getAllNodes() {
        return Collections.unmodifiableSet(allNodes);
    }
    
    // ==================== Debug Information ====================
    
    /**
     * Gets debug information about the scene.
     * 
     * @return Map of debug information
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("Name", name);
        info.put("Node Count", nodeCount);
        info.put("Active", active);
        info.put("Group Count", nodesByGroup.size());
        
        Map<String, Integer> groupStats = new LinkedHashMap<>();
        for (Map.Entry<String, Set<Node>> entry : nodesByGroup.entrySet()) {
            groupStats.put(entry.getKey(), entry.getValue().size());
        }
        info.put("Groups", groupStats);
        
        return info;
    }
    
    // ==================== Object Methods ====================
    
    @Override
    public String toString() {
        return "Scene[" + name + ", nodes=" + nodeCount + "]";
    }
}
