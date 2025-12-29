/*
 * JavaBlocks Engine - Editor Screen
 * 
 * Main editor UI screen implementation.
 */
package com.javablocks.editor;

import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.*;
import com.kotcrab.vis.ui.widget.*;

/**
 * Main editor screen layout.
 * 
 * Implements a VS Code-like layout with:
 * - Menu bar at top
 * - Scene hierarchy on left
 * - Main viewport in center
 * - Inspector on right
 * - Console/asset browser at bottom
 * 
 * @author JavaBlocks Engine Team
 */
public class EditorScreen {
    
    /** The stage this screen is on. */
    private Stage stage;
    
    /** Root table for layout. */
    private VisTable root;
    
    /** Menu bar. */
    private MenuBar menuBar;
    
    /** Hierarchy panel. */
    private VisTable hierarchyPanel;
    
    /** Viewport panel. */
    private VisTable viewportPanel;
    
    /** Inspector panel. */
    private VisTable inspectorPanel;
    
    /** Bottom panel (console/assets). */
    VisTable bottomPanel;
    
    // ==================== Constants ====================
    
    /** Left panel width. */
    private static final int LEFT_PANEL_WIDTH = 250;
    
    /** Right panel width. */
    private static final int RIGHT_PANEL_WIDTH = 300;
    
    /** Bottom panel height. */
    private static final int BOTTOM_PANEL_HEIGHT = 200;
    
    // ==================== Constructor ====================
    
    /**
     * Creates a new editor screen.
     */
    public EditorScreen() {
        // Load VisUI skin
        VisUI.load();
        
        // Create root table
        root = new VisTable();
        root.setFillParent(true);
    }
    
    /**
     * Sets the stage for this screen.
     * 
     * @param stage The stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;
        setupLayout();
    }
    
    /**
     * Gets the root table.
     * 
     * @return The root table
     */
    public Table getRoot() {
        return root;
    }
    
    // ==================== Layout Setup ====================
    
    /**
     * Sets up the editor layout.
     */
    private void setupLayout() {
        // Create panels
        createMenuBar();
        createHierarchyPanel();
        createViewportPanel();
        createInspectorPanel();
        createBottomPanel();
        
        // Add panels to root
        // Row 1: Menu bar (full width)
        root.add(menuBar.getTable()).colspan(3).top().growX().height(30).row();
        
        // Row 2: Left, Center, Right panels
        root.add(hierarchyPanel).width(LEFT_PANEL_WIDTH).growY();
        root.add(viewportPanel).grow();
        root.add(inspectorPanel).width(RIGHT_PANEL_WIDTH).growY().row();
        
        // Row 3: Bottom panel (full width)
        root.add(bottomPanel).colspan(3).height(BOTTOM_PANEL_HEIGHT).growX();
    }
    
    /**
     * Creates the menu bar.
     */
    private void createMenuBar() {
        menuBar = new MenuBar();
        
        // File menu
        Menu fileMenu = new Menu("File");
        fileMenu.addItem(new MenuItem("New Project"));
        fileMenu.addItem(new MenuItem("Open Project"));
        fileMenu.addSeparator();
        fileMenu.addItem(new MenuItem("Save"));
        fileMenu.addItem(new MenuItem("Save As..."));
        fileMenu.addSeparator();
        fileMenu.addItem(new MenuItem("Exit"));
        menuBar.addMenu(fileMenu);
        
        // Edit menu
        Menu editMenu = new Menu("Edit");
        editMenu.addItem(new MenuItem("Undo"));
        editMenu.addItem(new MenuItem("Redo"));
        editMenu.addSeparator();
        editMenu.addItem(new MenuItem("Cut"));
        editMenu.addItem(new MenuItem("Copy"));
        editMenu.addItem(new MenuItem("Paste"));
        menuBar.addMenu(editMenu);
        
        // View menu
        Menu viewMenu = new Menu("View");
        viewMenu.addItem(new MenuItem("Hierarchy"));
        viewMenu.addItem(new MenuItem("Inspector"));
        viewMenu.addItem(new MenuItem("Console"));
        menuBar.addMenu(viewMenu);
        
        // Game menu
        Menu gameMenu = new Menu("Game");
        gameMenu.addItem(new MenuItem("Play"));
        gameMenu.addItem(new MenuItem("Pause"));
        gameMenu.addItem(new MenuItem("Stop"));
        gameMenu.addSeparator();
        gameMenu.addItem(new MenuItem("Settings..."));
        menuBar.addMenu(gameMenu);
        
        // Help menu
        Menu helpMenu = new Menu("Help");
        helpMenu.addItem(new MenuItem("Documentation"));
        helpMenu.addItem(new MenuItem("API Reference"));
        helpMenu.addSeparator();
        helpMenu.addItem(new MenuItem("About"));
        menuBar.addMenu(helpMenu);
    }
    
    /**
     * Creates the hierarchy panel.
     */
    private void createHierarchyPanel() {
        hierarchyPanel = new VisTable(true);
        hierarchyPanel.setBackground("window-bg");
        
        // Panel title
        VisLabel title = new VisLabel("Hierarchy");
        title.setStyle(VisUI.getSkin().get("title", Label.LabelStyle.class));
        hierarchyPanel.add(title).top().left().pad(5);
        hierarchyPanel.row();
        
        // Simple list for hierarchy
        VisList<String> list = new VisList<>();
        list.setItems("Root", "Camera", "Light", "Player", "Enemy");
        
        hierarchyPanel.add(list).grow().pad(5);
    }
    
    /**
     * Creates the viewport panel.
     */
    private void createViewportPanel() {
        viewportPanel = new VisTable(true);
        viewportPanel.setBackground("window-bg");
        
        // Viewport title
        VisLabel title = new VisLabel("Scene View");
        title.setStyle(VisUI.getSkin().get("title", Label.LabelStyle.class));
        viewportPanel.add(title).top().left().pad(5);
        viewportPanel.row();
        
        // Placeholder for game view
        VisLabel placeholder = new VisLabel("Game Viewport\n\n(Drag game window here)");
        placeholder.setAlignment(Align.center);
        viewportPanel.add(placeholder).grow().center();
    }
    
    /**
     * Creates the inspector panel.
     */
    private void createInspectorPanel() {
        inspectorPanel = new VisTable(true);
        inspectorPanel.setBackground("window-bg");
        
        // Panel title
        VisLabel title = new VisLabel("Inspector");
        title.setStyle(VisUI.getSkin().get("title", Label.LabelStyle.class));
        inspectorPanel.add(title).top().left().pad(5);
        inspectorPanel.row();
        
        // Property grid (placeholder)
        VisTable propertyGrid = new VisTable();
        propertyGrid.defaults().left().pad(2);
        
        // Add some sample properties
        addProperty(propertyGrid, "Name", "Root");
        addProperty(propertyGrid, "Position", "(0, 0, 0)");
        addProperty(propertyGrid, "Rotation", "(0, 0, 0)");
        addProperty(propertyGrid, "Scale", "(1, 1, 1)");
        addProperty(propertyGrid, "Active", "true");
        addProperty(propertyGrid, "Visible", "true");
        
        ScrollPane scrollPane = new ScrollPane(propertyGrid);
        scrollPane.setFadeScrollBars(false);
        inspectorPanel.add(scrollPane).grow().pad(5);
    }
    
    /**
     * Adds a property to the inspector.
     * 
     * @param parent Parent table
     * @param name Property name
     * @param value Property value
     */
    private void addProperty(VisTable parent, String name, String value) {
        VisLabel nameLabel = new VisLabel(name);
        VisLabel valueLabel = new VisLabel(value);
        
        parent.add(nameLabel).width(80);
        parent.add(valueLabel).grow().row();
    }
    
    /**
     * Creates the bottom panel.
     */
    private void createBottomPanel() {
        bottomPanel = new VisTable(true);
        bottomPanel.setBackground("window-bg");
        
        // Title
        VisLabel title = new VisLabel("Output");
        title.setStyle(VisUI.getSkin().get("title", Label.LabelStyle.class));
        bottomPanel.add(title).top().left().pad(5);
        bottomPanel.row();
        
        // Console area
        VisTextArea consoleArea = new VisTextArea();
        consoleArea.setText("Welcome to JavaBlocks Editor v1.0.0\n>");
        bottomPanel.add(consoleArea).grow().pad(5);
    }
    
    // ==================== Update ====================
    
    /**
     * Updates the editor screen.
     * 
     * @param delta Time since last update
     */
    public void update(float delta) {
        if (stage != null) {
            stage.act(delta);
        }
    }
}
