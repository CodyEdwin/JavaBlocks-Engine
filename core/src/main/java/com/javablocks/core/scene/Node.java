/*
 * JavaBlocks Engine - Node Hierarchy System
 * 
 * Godot-inspired node system for scene composition.
 * Nodes are the fundamental building blocks of scenes.
 */
package com.javablocks.core.scene;

import com.javablocks.core.*;
import com.javablocks.core.ecs.*;
import com.javablocks.core.events.*;
import com.javablocks.core.math.*;
import java.util.*;

/**
 * Node is the fundamental building block for scene composition.
 * 
 * Nodes form a hierarchical tree structure where each node can have
 * children nodes. This enables complex scene compositions through
 * composition rather than inheritance.
 * 
 * Features:
 * - Parent-child hierarchy with transform propagation
 * - Signal-based event system
 * - Lifecycle callbacks
 * - Groups for organization
 * - Name and path-based retrieval
 * 
 * Node Lifecycle:
 * 1. Node is created
 * 2. _init() is called for initialization
 * 3. Node is added to parent via addChild()
 * 4. _ready() is called before first _process()
 * 5. _process() is called each frame
 * 6. _exit_tree() is called when removed from tree
 * 
 * @author JavaBlocks Engine Team
 */
public class Node implements Comparable<Node> {
    
    // ==================== Static Fields ====================
    
    /** Global node counter for unique IDs. */
    private static long nextNodeId = 0;
    
    // ==================== Instance Fields ====================
    
    /** Unique node ID. */
    private final long nodeId;
    
    /** Node name. */
    private String name;
    
    /** Node type name for debugging. */
    private final String typeName;
    
    /** Parent node. */
    private Node parent;
    
    /** Children nodes. */
    private final ArrayList<Node> children;
    
    /** Scene this node belongs to (null if not in a scene). */
    private Scene scene;
    
    /** Whether the node is inside the scene tree. */
    private boolean insideTree;
    
    /** Whether the node is processing. */
    private boolean processing;
    
    /** Whether the node is paused. */
    private boolean paused;
    
    /** Processing priority. */
    private int processPriority;
    
    /** Groups this node belongs to. */
    private final HashSet<String> groups;
    
    /** Custom data storage. */
    private Object userData;
    
    // ==================== Signal Registry ====================
    
    /** Signal emitted when node enters the tree. */
    public final Signal<Node> enteredTree;
    
    /** Signal emitted when node exits the tree. */
    public final Signal<Node> exitedTree;
    
    /** Signal emitted when node is ready. */
    public final Signal<Node> ready;
    
    /** Signal emitted when node is renamed. */
    public final Signal<Node> renamed;
    
    /** Signal emitted when parent changes. */
    public final Signal<Node> parentChanged;
    
    // ==================== Lifecycle State ====================
    
    /** Whether _ready has been called. */
    private boolean readyCalled;
    
    // ==================== Constructor ====================
    
    /**
     * Creates a node with the default name.
     */
    public Node() {
        this("Node");
    }
    
    /**
     * Creates a node with a specific name.
     * 
     * @param name The node name
     */
    public Node(String name) {
        this.nodeId = nextNodeId++;
        this.name = name != null ? name : "Node";
        this.typeName = getClass().getSimpleName();
        this.parent = null;
        this.children = new ArrayList<>(4);
        this.scene = null;
        this.insideTree = false;
        this.processing = false;
        this.paused = false;
        this.processPriority = 0;
        this.groups = new HashSet<>();
        this.userData = null;
        this.readyCalled = false;
        
        // Initialize signals
        this.enteredTree = new Signal<>();
        this.exitedTree = new Signal<>();
        this.ready = new Signal<>();
        this.renamed = new Signal<>();
        this.parentChanged = new Signal<>();
        
        // Call initialization
        try {
            _init();
        } catch (Exception e) {
            throw new RuntimeException("Node initialization failed", e);
        }
    }
    
    // ==================== Lifecycle Methods ====================
    
    /**
     * Called when the node is created.
     * Override for initialization that doesn't depend on the tree.
     */
    protected void _init() {
        // Override in subclasses
    }
    
    /**
     * Called when the node enters the scene tree.
     * Override for initialization that depends on the tree.
     */
    protected void _ready() {
        // Override in subclasses
    }
    
    /**
     * Called each frame when processing is enabled.
     * 
     * @param delta Time since last frame in seconds
     */
    protected void _process(float delta) {
        // Override in subclasses
    }
    
    /**
     * Called each physics frame.
     * 
     * @param delta Fixed timestep
     */
    protected void _physicsProcess(float delta) {
        // Override in subclasses
    }
    
    /**
     * Called when the node is about to exit the tree.
     */
    protected void _exitTree() {
        // Override in subclasses
    }
    
    /**
     * Called when the node's transform changes.
     */
    protected void _onTransformChanged() {
        // Override in subclasses
    }
    
    // ==================== Tree Operations ====================
    
    /**
     * Adds a child node.
     * 
     * @param child The child node to add
     * @throws IllegalArgumentException if child is null or this node
     */
    public void addChild(Node child) {
        addChild(child, false);
    }
    
    /**
     * Adds a child node with optional force.
     * 
     * @param child The child node to add
     * @param force True to skip validation
     * @throws IllegalArgumentException if validation fails
     */
    public void addChild(Node child, boolean force) {
        if (child == null) {
            throw new IllegalArgumentException("Child cannot be null");
        }
        
        if (!force) {
            if (child == this) {
                throw new IllegalArgumentException("Node cannot be its own child");
            }
            if (isAncestorOf(child)) {
                throw new IllegalArgumentException("Node is an ancestor of the child");
            }
        }
        
        // Remove from current parent if any
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        
        // Add to children list
        children.add(child);
        child.parent = this;
        
        // Update scene reference
        if (insideTree) {
            child.setScene(scene);
        }
        
        // Emit signal
        child.emitSignal("child_added");
    }
    
    /**
     * Removes a child node.
     * 
     * @param child The child node to remove
     * @return true if the child was found and removed
     */
    public boolean removeChild(Node child) {
        if (child == null) {
            return false;
        }
        
        int index = children.indexOf(child);
        if (index < 0) {
            return false;
        }
        
        removeChildAt(index);
        return true;
    }
    
    /**
     * Removes the child at the specified index.
     * 
     * @param index The index of the child to remove
     * @return The removed child node
     */
    public Node removeChildAt(int index) {
        if (index < 0 || index >= children.size()) {
            throw new IndexOutOfBoundsException("Child index out of bounds: " + index);
        }
        
        Node child = children.remove(index);
        child.parent = null;
        
        // Clear scene reference
        if (insideTree) {
            child.clearScene();
        }
        
        return child;
    }
    
    /**
     * Removes all children.
     */
    public void removeAllChildren() {
        while (!children.isEmpty()) {
            removeChildAt(children.size() - 1);
        }
    }
    
    // ==================== Scene Operations ====================
    
    /**
     * Gets the scene this node belongs to.
     * 
     * @return The scene, or null if not in a scene
     */
    public Scene getScene() {
        return scene;
    }
    
    /**
     * Sets the scene reference (internal).
     * 
     * @param scene The scene to set
     */
    private void setScene(Scene scene) {
        this.scene = scene;
        
        // Propagate to children
        for (Node child : children) {
            child.setScene(scene);
        }
        
        // Enter tree if not already
        if (!insideTree) {
            enterTree();
        }
    }
    
    /**
     * Clears the scene reference (internal).
     */
    private void clearScene() {
        // Exit tree first
        if (insideTree) {
            exitTree();
        }
        
        this.scene = null;
        
        // Propagate to children
        for (Node child : children) {
            child.clearScene();
        }
    }
    
    // ==================== Tree State ====================
    
    /**
     * Called when entering the tree.
     */
    private void enterTree() {
        insideTree = true;
        
        // Call _ready if not called yet
        if (!readyCalled) {
            readyCalled = true;
            try {
                _ready();
            } catch (Exception e) {
                throw new RuntimeException("Node _ready() failed", e);
            }
            ready.dispatch(this);
        }
        
        // Emit entered tree signal
        enteredTree.dispatch(this);
        
        // Call enter tree on children
        for (Node child : children) {
            if (!child.insideTree) {
                child.setScene(scene);
            }
        }
    }
    
    /**
     * Called when exiting the tree.
     */
    private void exitTree() {
        // Call exit tree on children first
        for (Node child : children) {
            if (child.insideTree) {
                child.exitTree();
            }
        }
        
        // Emit exited tree signal
        exitedTree.dispatch(this);
        
        // Call _exitTree override
        try {
            _exitTree();
        } catch (Exception e) {
            // Log but don't throw during cleanup
            System.err.println("Node._exitTree() failed: " + e.getMessage());
        }
        
        insideTree = false;
    }
    
    /**
     * Checks if this node is inside the scene tree.
     * 
     * @return true if inside the tree
     */
    public boolean isInsideTree() {
        return insideTree;
    }
    
    // ==================== Processing ====================
    
    /**
     * Enables processing for this node.
     */
    public void setProcess(boolean enabled) {
        this.processing = enabled;
    }
    
    /**
     * Checks if processing is enabled.
     * 
     * @return true if processing
     */
    public boolean isProcessing() {
        return processing;
    }
    
    /**
     * Sets the process priority.
     * 
     * @param priority The priority (lower = earlier)
     */
    public void setProcessPriority(int priority) {
        this.processPriority = priority;
    }
    
    /**
     * Gets the process priority.
     * 
     * @return The priority
     */
    public int getProcessPriority() {
        return processPriority;
    }
    
    /**
     * Called each frame for processing.
     * 
     * @param delta Time since last frame
     */
    final void process(float delta) {
        if (!processing || paused) {
            return;
        }
        
        try {
            _process(delta);
        } catch (Exception e) {
            // Log but don't stop processing
            System.err.println("Node._process() failed: " + e.getMessage());
        }
    }
    
    // ==================== Pausing ====================
    
    /**
     * Pauses the node and its children.
     */
    public void pause() {
        this.paused = true;
    }
    
    /**
     * Resumes the node.
     */
    public void resume() {
        this.paused = false;
    }
    
    /**
     * Checks if the node is paused.
     * 
     * @return true if paused
     */
    public boolean isPaused() {
        return paused;
    }
    
    /**
     * Checks if the node and its parents are all unpaused.
     * 
     * @return true if not paused by any parent
     */
    public boolean canProcess() {
        if (paused) {
            return false;
        }
        return parent == null || parent.canProcess();
    }
    
    // ==================== Hierarchy Queries ====================
    
    /**
     * Gets the parent node.
     * 
     * @return The parent, or null if root
     */
    public Node getParent() {
        return parent;
    }
    
    /**
     * Gets the root node of the tree.
     * 
     * @return The root node
     */
    public Node getTreeRoot() {
        Node root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        return root;
    }
    
    /**
     * Gets the first child with the specified name.
     * 
     * @param name The name to find
     * @return The child node, or null if not found
     */
    public Node findChild(String name) {
        return findChild(name, true, true);
    }
    
    /**
     * Finds a child by name with options.
     * 
     * @param name The name to find
     * @param recursive Whether to search recursively
     * @param includeSelf Whether to include this node
     * @return The found node, or null
     */
    public Node findChild(String name, boolean recursive, boolean includeSelf) {
        if (includeSelf && this.name.equals(name)) {
            return this;
        }
        
        for (Node child : children) {
            if (recursive) {
                Node found = child.findChild(name, true, true);
                if (found != null) {
                    return found;
                }
            } else if (child.name.equals(name)) {
                return child;
            }
        }
        
        return null;
    }
    
    /**
     * Gets all children with a specific type.
     * 
     * @param <T> The node type
     * @param type The class to match
     * @return List of matching children
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> List<T> getChildren(Class<T> type) {
        List<T> result = new ArrayList<>();
        
        for (Node child : children) {
            if (type.isInstance(child)) {
                result.add((T) child);
            }
        }
        
        return result;
    }
    
    /**
     * Gets all descendants of a specific type.
     * 
     * @param <T> The node type
     * @param type The class to match
     * @return List of matching descendants
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> List<T> getDescendants(Class<T> type) {
        List<T> result = new ArrayList<>();
        collectDescendants(result, type);
        return result;
    }
    
    /**
     * Recursively collects descendants.
     */
    private <T extends Node> void collectDescendants(List<T> result, Class<T> type) {
        for (Node child : children) {
            if (type.isInstance(child)) {
                result.add((T) child);
            }
            if (child.getChildCount() > 0) {
                child.collectDescendants(result, type);
            }
        }
    }
    
    /**
     * Checks if this node is an ancestor of another node.
     * 
     * @param node The node to check
     * @return true if this is an ancestor
     */
    public boolean isAncestorOf(Node node) {
        if (node == null || node.parent == null) {
            return false;
        }
        
        Node current = node.parent;
        while (current != null) {
            if (current == this) {
                return true;
            }
            current = current.parent;
        }
        
        return false;
    }
    
    /**
     * Checks if this node is a descendant of another node.
     * 
     * @param node The potential ancestor
     * @return true if this is a descendant
     */
    public boolean isDescendantOf(Node node) {
        return node != null && node.isAncestorOf(this);
    }
    
    // ==================== Groups ====================
    
    /**
     * Adds this node to a group.
     * 
     * @param group The group name
     */
    public void addToGroup(String group) {
        if (group != null && !group.isEmpty()) {
            groups.add(group);
            if (scene != null) {
                scene.addNodeToGroup(this, group);
            }
        }
    }
    
    /**
     * Removes this node from a group.
     * 
     * @param group The group name
     * @return true if the node was in the group
     */
    public boolean removeFromGroup(String group) {
        boolean removed = groups.remove(group);
        if (removed && scene != null) {
            scene.removeNodeFromGroup(this, group);
        }
        return removed;
    }
    
    /**
     * Checks if this node is in a group.
     * 
     * @param group The group name
     * @return true if in the group
     */
    public boolean isInGroup(String group) {
        return groups.contains(group);
    }
    
    /**
     * Gets all groups this node belongs to.
     * 
     * @return Set of group names
     */
    public Set<String> getGroups() {
        return Collections.unmodifiableSet(groups);
    }
    
    // ==================== Path Operations ====================
    
    /**
     * Gets the path from the root to this node.
     * 
     * @return The node path
     */
    public String getPath() {
        if (parent == null) {
            return "/" + name;
        }
        
        StringBuilder path = new StringBuilder();
        buildPath(path);
        return path.toString();
    }
    
    /**
     * Recursively builds the path.
     */
    private void buildPath(StringBuilder path) {
        if (parent != null) {
            parent.buildPath(path);
        }
        path.append("/").append(name);
    }
    
    /**
     * Gets the relative path from another node.
     * 
     * @param from The node to start from
     * @return The relative path
     */
    public String getRelativePath(Node from) {
        // Simple implementation - could be enhanced
        return getPath();
    }
    
    // ==================== Signal Operations ====================
    
    /**
     * Emits a named signal on this node.
     * 
     * @param signalName The signal name
     */
    public void emitSignal(String signalName) {
        // Placeholder for named signal emission
    }
    
    /**
     * Connects a method to a signal.
     * 
     * @param signalName The signal name
     * @param target The target node
     * @param methodName The method name to call
     * @return Connection object for cleanup
     */
    public Connection connect(String signalName, Node target, String methodName) {
        // Placeholder for signal connection
        return null;
    }
    
    // ==================== Name Operations ====================
    
    /**
     * Gets the node name.
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the node name.
     * 
     * @param name The new name
     */
    public void setName(String name) {
        String oldName = this.name;
        this.name = name != null ? name : "Node";
        
        if (!oldName.equals(this.name)) {
            renamed.dispatch(this);
        }
    }
    
    /**
     * Gets the type name.
     * 
     * @return The type name
     */
    public String getTypeName() {
        return typeName;
    }
    
    // ==================== Child Operations ====================
    
    /**
     * Gets the number of children.
     * 
     * @return The child count
     */
    public int getChildCount() {
        return children.size();
    }
    
    /**
     * Gets a child by index.
     * 
     * @param index The child index
     * @return The child node
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Node getChild(int index) {
        return children.get(index);
    }
    
    /**
     * Gets all children.
     * 
     * @return Unmodifiable list of children
     */
    public List<Node> getChildren() {
        return Collections.unmodifiableList(children);
    }
    
    // ==================== User Data ====================
    
    /**
     * Gets custom user data.
     * 
     * @return The user data, or null if not set
     */
    public Object getUserData() {
        return userData;
    }
    
    /**
     * Sets custom user data.
     * 
     * @param userData The user data to set
     */
    public void setUserData(Object userData) {
        this.userData = userData;
    }
    
    // ==================== Debug Operations ====================
    
    /**
     * Gets the unique node ID.
     * 
     * @return The node ID
     */
    public long getNodeId() {
        return nodeId;
    }
    
    /**
     * Gets debug information about this node.
     * 
     * @return Map of debug information
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("Node ID", nodeId);
        info.put("Name", name);
        info.put("Type", typeName);
        info.put("Child Count", children.size());
        info.put("Inside Tree", insideTree);
        info.put("Processing", processing);
        info.put("Paused", paused);
        info.put("Groups", groups);
        info.put("Path", getPath());
        return info;
    }
    
    // ==================== Comparison ====================
    
    @Override
    public int compareTo(Node other) {
        return Long.compare(nodeId, other.nodeId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Node other)) return false;
        return nodeId == other.nodeId;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(nodeId);
    }
    
    @Override
    public String toString() {
        return typeName + "[" + name + "]";
    }
}
