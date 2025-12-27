/*
 * JavaBlocks Engine - Desktop Launcher
 * 
 * Main entry point for the desktop application.
 */
package com.javablocks.desktop;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.javablocks.core.*;

/**
 * Desktop launcher for JavaBlocks.
 * 
 * Handles window creation, input handling, and platform-specific setup.
 * 
 * @author JavaBlocks Engine Team
 */
public class JavaBlocksDesktop implements ApplicationListener {
    
    /** Engine instance. */
    private JavaBlocksEngine engine;
    
    /** LWJGL3 application. */
    private Lwjgl3Application application;
    
    /** Window title. */
    private static final String WINDOW_TITLE = "JavaBlocks Engine";
    
    /** Default window width. */
    private static final int WINDOW_WIDTH = 1280;
    
    /** Default window height. */
    private static final int WINDOW_HEIGHT = 720;
    
    /** Whether to start in fullscreen. */
    private static boolean fullscreen = false;
    
    /** Whether to enable vsync. */
    private static boolean vsync = true;
    
    /** Target FPS. */
    private static int targetFPS = 60;
    
    // ==================== Main Method ====================
    
    /**
     * Main entry point for the application.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Parse command line arguments
        parseArguments(args);
        
        // Create launcher
        JavaBlocksDesktop launcher = new JavaBlocksDesktop();
        
        // Configure LWJGL3
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(WINDOW_TITLE);
        config.setWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        config.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
        config.setVsync(vsync);
        config.setTargetFrameRate(targetFPS);
        config.setResizable(true);
        
        // Enable OpenGL 3.3 core profile
        config.setOpenGLCoreProfileGLLibraries();
        
        // Audio configuration
        config.setAudioConfig(512, 256, 16);
        
        // Create application
        try {
            launcher.application = new Lwjgl3Application(launcher, config);
            
            if (fullscreen) {
                launcher.setFullscreen(true);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to create application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Parses command line arguments.
     * 
     * @param args Command line arguments
     */
    private static void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            switch (arg) {
                case "--fullscreen":
                case "-f":
                    fullscreen = true;
                    break;
                    
                case "--windowed":
                case "-w":
                    fullscreen = false;
                    break;
                    
                case "--vsync":
                    vsync = true;
                    break;
                    
                case "--novsync":
                    vsync = false;
                    break;
                    
                case "--fps":
                    if (i + 1 < args.length) {
                        try {
                            targetFPS = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid FPS value: " + args[i]);
                        }
                    }
                    break;
                    
                case "--width":
                    if (i + 1 < args.length) {
                        try {
                            // Would update window width
                            i++;
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid width value: " + args[i]);
                        }
                    }
                    break;
                    
                case "--height":
                    if (i + 1 < args.length) {
                        try {
                            // Would update window height
                            i++;
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid height value: " + args[i]);
                        }
                    }
                    break;
                    
                case "--help":
                case "-h":
                    printHelp();
                    System.exit(0);
                    break;
                    
                case "--version":
                case "-v":
                    printVersion();
                    System.exit(0);
                    break;
                    
                default:
                    System.err.println("Unknown argument: " + arg);
                    printHelp();
                    System.exit(1);
            }
        }
    }
    
    /**
     * Prints help information.
     */
    private static void printHelp() {
        System.out.println("JavaBlocks Engine Desktop Launcher");
        System.out.println();
        System.out.println("Usage: java -jar JavaBlocks-Desktop.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --fullscreen, -f    Start in fullscreen mode");
        System.out.println("  --windowed, -w      Start in windowed mode");
        System.out.println("  --vsync             Enable VSync");
        System.out.println("  --novsync           Disable VSync");
        System.out.println("  --fps <value>       Set target FPS");
        System.out.println("  --width <value>     Set window width");
        System.out.println("  --height <value>    Set window height");
        System.out.println("  --help, -h          Show this help");
        System.out.println("  --version, -v       Show version");
    }
    
    /**
     * Prints version information.
     */
    private static void printVersion() {
        System.out.println("JavaBlocks Engine v1.0.0");
        System.out.println("LibGDX v" + com.badlogic.gdx.Version.VERSION);
    }
    
    // ==================== Application Lifecycle ====================
    
    @Override
    public void create() {
        // Configure engine
        JavaBlocksEngine.EngineConfiguration config = new JavaBlocksEngine.EngineConfiguration();
        config.headless = false;
        config.targetFPS = targetFPS;
        config.debugMode = true; // Enable debug mode in development
        
        // Create engine
        engine = JavaBlocksEngine.create(config);
        
        System.out.println("[Desktop] JavaBlocks Engine initialized");
    }
    
    @Override
    public void resize(int width, int height) {
        // Handle window resize
        if (engine != null) {
            engine.resize(width, height);
        }
    }
    
    @Override
    public void render() {
        // Get delta time
        float deltaTime = Gdx.graphics.getDeltaTime();
        
        // Update engine
        engine.update(deltaTime);
        
        // Render
        engine.render(deltaTime);
    }
    
    @Override
    public void pause() {
        // Handle pause
        if (engine != null) {
            engine.pause();
        }
    }
    
    @Override
    public void resume() {
        // Handle resume
        if (engine != null) {
            engine.resume();
        }
    }
    
    @Override
    public void dispose() {
        // Shutdown engine
        if (engine != null) {
            engine.shutdown();
        }
        
        // Dispose application
        if (application != null) {
            application.exit();
        }
        
        System.out.println("[Desktop] Application disposed");
    }
    
    // ==================== Window Management ====================
    
    /**
     * Sets fullscreen mode.
     * 
     * @param fullscreen true for fullscreen
     */
    public void setFullscreen(boolean fullscreen) {
        if (application != null) {
            if (fullscreen) {
                application.getGraphics().setFullscreenMode(
                    Lwjgl3Graphics.getDisplayMode()
                );
            } else {
                application.getGraphics().setWindowedMode(
                    WINDOW_WIDTH, WINDOW_HEIGHT
                );
            }
        }
    }
    
    /**
     * Toggles fullscreen mode.
     */
    public void toggleFullscreen() {
        setFullscreen(!fullscreen);
    }
    
    /**
     * Sets the window title.
     * 
     * @param title New title
     */
    public void setTitle(String title) {
        if (application != null) {
            application.getGraphics().setTitle(title);
        }
    }
    
    /**
     * Minimizes the window.
     */
    public void minimize() {
        if (application != null) {
            application.getGraphics().setMinimized(true);
        }
    }
    
    /**
     * Maximizes the window.
     */
    public void maximize() {
        if (application != null) {
            application.getGraphics().setMaximized(true);
        }
    }
}
