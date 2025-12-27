/*
 * JavaBlocks Engine - Server Launcher
 * 
 * Main entry point for the dedicated game server.
 */
package com.javablocks.server;

import com.javablocks.core.*;

/**
 * Main server entry point.
 * 
 * Provides:
 * - HTTP API server
 * - WebSocket server for multiplayer
 * - Game state management
 * - Player authentication
 * 
 * @author JavaBlocks Engine Team
 */
public class JavaBlocksServer {
    
    /** Server configuration. */
    private ServerConfig config;
    
    /** HTTP server. */
    private com.javablocks.server.http.HttpServer httpServer;
    
    /** WebSocket server. */
    private com.javablocks.server.ws.WebSocketServer wsServer;
    
    /** Game state manager. */
    private com.javablocks.server.state.GameStateManager stateManager;
    
    /** Whether the server is running. */
    private volatile boolean running;
    
    // ==================== Main Method ====================
    
    /**
     * Main entry point for the server.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Parse arguments
        int port = 8080;
        int wsPort = 8765;
        String configFile = null;
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                    if (i + 1 < args.length) {
                        port = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--ws-port":
                    if (i + 1 < args.length) {
                        wsPort = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--config":
                    if (i + 1 < args.length) {
                        configFile = args[++i];
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
            }
        }
        
        // Create and start server
        JavaBlocksServer server = new JavaBlocksServer();
        
        try {
            server.start(port, wsPort, configFile);
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Prints help information.
     */
    private static void printHelp() {
        System.out.println("JavaBlocks Game Server");
        System.out.println();
        System.out.println("Usage: java -jar JavaBlocks-Server.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --port <value>     HTTP API port (default: 8080)");
        System.out.println("  --ws-port <value>  WebSocket port (default: 8765)");
        System.out.println("  --config <file>    Configuration file path");
        System.out.println("  --help, -h         Show this help");
        System.out.println("  --version, -v      Show version");
    }
    
    /**
     * Prints version information.
     */
    private static void printVersion() {
        System.out.println("JavaBlocks Server v1.0.0");
    }
    
    // ==================== Server Lifecycle ====================
    
    /**
     * Starts the server.
     * 
     * @param httpPort HTTP API port
     * @param wsPort WebSocket port
     * @param configFile Configuration file path
     */
    public void start(int httpPort, int wsPort, String configFile) throws Exception {
        System.out.println("[Server] Starting JavaBlocks Server...");
        
        // Load configuration
        loadConfiguration(configFile);
        
        // Initialize engine in headless mode
        JavaBlocksEngine.EngineConfiguration engineConfig = new JavaBlocksEngine.EngineConfiguration();
        engineConfig.headless = true;
        engineConfig.targetFPS = 30;
        
        JavaBlocksEngine.create(engineConfig);
        
        // Initialize subsystems
        initializeSubsystems(httpPort, wsPort);
        
        running = true;
        
        System.out.println("[Server] Server started successfully");
        System.out.println("[Server] HTTP API: http://localhost:" + httpPort);
        System.out.println("[Server] WebSocket: ws://localhost:" + wsPort);
        
        // Main server loop
        mainLoop();
    }
    
    /**
     * Loads server configuration.
     * 
     * @param configFile Configuration file path
     */
    private void loadConfiguration(String configFile) {
        config = new ServerConfig();
        
        if (configFile != null) {
            // Load from file
            // Would parse YAML/JSON configuration
        }
        
        System.out.println("[Server] Configuration loaded");
    }
    
    /**
     * Initializes server subsystems.
     * 
     * @param httpPort HTTP API port
     * @param wsPort WebSocket port
     */
    private void initializeSubsystems(int httpPort, int wsPort) throws Exception {
        // Initialize game state manager
        stateManager = new com.javablocks.server.state.GameStateManager();
        
        // Initialize HTTP server
        httpServer = new com.javablocks.server.http.HttpServer(httpPort, stateManager);
        httpServer.start();
        
        // Initialize WebSocket server
        wsServer = new com.javablocks.server.ws.WebSocketServer(wsPort, stateManager);
        wsServer.start();
    }
    
    /**
     * Main server loop.
     */
    private void mainLoop() {
        while (running) {
            try {
                Thread.sleep(1000); // 1 second tick
                
                // Update server state
                update();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[Server] Update error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Updates server state.
     */
    private void update() {
        // Update game state
        stateManager.update(1f / 30f);
        
        // Update connected players
        stateManager.updatePlayers();
        
        // Clean up disconnected players
        stateManager.cleanupDisconnected();
    }
    
    /**
     * Stops the server.
     */
    public void stop() {
        System.out.println("[Server] Stopping server...");
        
        running = false;
        
        // Stop subsystems
        if (wsServer != null) {
            wsServer.stop();
        }
        
        if (httpServer != null) {
            httpServer.stop();
        }
        
        // Shutdown engine
        JavaBlocksEngine.get().shutdown();
        
        System.out.println("[Server] Server stopped");
    }
    
    // ==================== Configuration ====================
    
    /**
     * Server configuration holder.
     */
    public static class ServerConfig {
        /** Maximum concurrent connections. */
        public int maxConnections = 1000;
        
        /** Connection timeout in seconds. */
        public int connectionTimeout = 60;
        
        /** Heartbeat interval in seconds. */
        public int heartbeatInterval = 30;
        
        /** Enable player authentication. */
        public boolean authenticationEnabled = false;
        
        /** Database connection string. */
        public String databaseUrl = "jdbc:h2:mem:javablocks";
        
        /** Log level. */
        public String logLevel = "INFO";
    }
}
