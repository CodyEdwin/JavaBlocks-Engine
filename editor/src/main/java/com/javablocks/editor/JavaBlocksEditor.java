/*
 * JavaBlocks Engine - Editor Launcher
 * 
 * Main entry point for the visual editor.
 */
package com.javablocks.editor;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.javablocks.core.*;
import com.javablocks.desktop.*;

/**
 * Visual editor for JavaBlocks.
 * 
 * Provides a complete game development environment with:
 * - Scene view
 * - Hierarchy panel
 * - Inspector panel
 * - Asset browser
 * - Console output
 * 
 * @author JavaBlocks Engine Team
 */
public class JavaBlocksEditor implements ApplicationListener {
    
    /** Engine instance. */
    private JavaBlocksEngine engine;
    
    /** Editor UI stage. */
    private Stage uiStage;
    
    /** Main editor screen. */
    private EditorScreen editorScreen;
    
    /** Window title. */
    private static final String WINDOW_TITLE = "JavaBlocks Editor";
    
    /** Default window width. */
    private static final int WINDOW_WIDTH = 1600;
    
    /** Default window height. */
    private static final int WINDOW_HEIGHT = 900;
    
    // ==================== Main Method ====================
    
    /**
     * Main entry point for the editor.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Configure LWJGL3
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(WINDOW_TITLE);
        config.setWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        config.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
        config.setVsync(true);
        config.setTargetFrameRate(60);
        config.setResizable(true);
        config.setOpenGLCoreProfileGLLibraries();
        
        // Create application
        try {
            new Lwjgl3Application(new JavaBlocksEditor(), config);
        } catch (Exception e) {
            System.err.println("Failed to create editor: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    // ==================== Application Lifecycle ====================
    
    @Override
    public void create() {
        // Initialize engine in editor mode
        JavaBlocksEngine.EngineConfiguration config = new JavaBlocksEngine.EngineConfiguration();
        config.headless = false;
        config.targetFPS = 60;
        config.debugMode = true;
        
        engine = JavaBlocksEngine.create(config);
        
        // Create UI
        createEditorUI();
        
        System.out.println("[Editor] JavaBlocks Editor initialized");
    }
    
    /**
     * Creates the editor UI.
     */
    private void createEditorUI() {
        // Create stage
        uiStage = new Stage(new ScreenViewport());
        
        // Set up input
        Gdx.input.setInputProcessor(uiStage);
        
        // Create editor screen
        editorScreen = new EditorScreen();
        editorScreen.setStage(uiStage);
        
        // Add to stage
        uiStage.addActor(editorScreen.getRoot());
    }
    
    @Override
    public void resize(int width, int height) {
        if (uiStage != null) {
            uiStage.getViewport().update(width, height, true);
        }
    }
    
    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        
        // Update engine
        engine.update(deltaTime);
        
        // Render game view
        engine.render(deltaTime);
        
        // Render UI
        if (uiStage != null) {
            uiStage.act(deltaTime);
            uiStage.draw();
        }
    }
    
    @Override
    public void pause() {
        engine.pause();
    }
    
    @Override
    public void resume() {
        engine.resume();
    }
    
    @Override
    public void dispose() {
        if (uiStage != null) {
            uiStage.dispose();
        }
        
        engine.shutdown();
        
        System.out.println("[Editor] Editor disposed");
    }
    
    // ==================== Editor Operations ====================
    
    /**
     * Opens a scene in the editor.
     * 
     * @param scenePath Path to the scene file
     */
    public void openScene(String scenePath) {
        // Would load and display the scene
        System.out.println("[Editor] Opening scene: " + scenePath);
    }
    
    /**
     * Saves the current scene.
     * 
     * @param scenePath Path to save the scene
     */
    public void saveScene(String scenePath) {
        // Would save the current scene
        System.out.println("[Editor] Saving scene: " + scenePath);
    }
    
    /**
     * Creates a new scene.
     * 
     * @param sceneName Name for the new scene
     */
    public void newScene(String sceneName) {
        // Would create a new scene
        System.out.println("[Editor] Creating new scene: " + sceneName);
    }
    
    /**
     * Exits the editor.
     */
    public void exit() {
        Gdx.app.exit();
    }
}
