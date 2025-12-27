# JavaBlocks Engine

A pure Java game engine and creation suite platform, inspired by Roblox, Unity, Godot, and Unreal Engine.

## Overview

JavaBlocks is a comprehensive, pure Java-based game engine designed for:
- **Beginners**: Intuitive drag-and-drop interfaces with guided tutorials
- **Hobbyists**: Low-code and no-code options for creating games
- **Educators**: Curriculum-aligned tools for classroom settings
- **Professional Developers**: Powerful, extensible tools for complex 3D and 2D experiences

## Key Features

### Core Engine
- **Pure Java Implementation**: No native dependencies beyond LibGDX
- **Multi-Platform Support**: Windows, macOS, Linux, Android, iOS, Web
- **Entity Component System (ECS)**: High-performance game architecture
- **Node Hierarchy System**: Godot-inspired scene graph
- **Signal-Based Events**: Decoupled communication between systems

### Rendering
- **2D Sprite Batching**: Optimized for 2D games
- **3D Model Instancing**: Efficient rendering of many objects
- **PBR Materials**: Physically-based rendering support
- **Multi-Pass Rendering**: Advanced rendering techniques

### Physics
- **2D Physics**: Box2D integration for rigid body dynamics
- **3D Physics**: Custom pure Java implementation
- **Collision Detection**: Raycasting, bounding volumes
- **Joint Constraints**: Complex physical simulations

### Audio
- **Spatial Audio**: 3D positional sound
- **Audio Mixing**: Bus-based mixing and effects
- **Streaming**: Efficient audio playback

### Networking
- **Multiplayer Support**: WebSocket-based multiplayer
- **Server Authority**: Secure game state management
- **Delta Compression**: Efficient state synchronization

### Editor
- **Visual Interface**: Studio-like development environment
- **Hierarchy Inspector**: Tree view of scene objects
- **Drag-and-Drop**: Intuitive asset management
- **Real-time Preview**: See changes instantly

## Architecture

```
JavaBlocks/
├── core/           # Core engine logic (ECS, physics, audio, etc.)
├── desktop/        # Desktop launcher (LWJGL3)
├── android/        # Android launcher
├── editor/         # Visual editor
├── server/         # Dedicated game server
├── assets/         # Asset management tools
├── plugins/        # Plugin system
├── marketplace/    # Content marketplace
└── tools/          # Development tools
```

## Requirements

- **Java**: JDK 21 or higher
- **Gradle**: 8.7 or higher
- **Android**: SDK 35 (for Android builds)
- **Memory**: 2GB minimum (4GB recommended)

## Building

### Prerequisites

1. Install JDK 21
2. Install Gradle 8.7 or use wrapper
3. For Android: Install Android SDK

### Quick Start

```bash
# Clone the repository
git clone https://github.com/javablocks/engine.git
cd engine

# Build all modules
./gradlew build

# Run desktop version
./gradlew :desktop:run

# Run editor
./gradlew :editor:run

# Build distribution
./gradlew dist
```

### Building for Specific Platforms

```bash
# Desktop (JAR)
./gradlew :desktop:shadowJar

# Android (APK)
./gradlew :android:assembleRelease

# Web (HTML)
./gradlew :html:dist

# iOS (requires macOS)
./gradlew :ios:createIPA
```

## Project Structure

### Core Module

The core module contains all engine logic:

```java
// Create the engine
JavaBlocksEngine engine = JavaBlocksEngine.create();

// Create an entity
Entity player = engine.createEntity("Player");
engine.addComponent(player, new TransformComponent(0, 0, 0));
engine.addComponent(player, new RigidBodyComponent());

// Create a node
Node camera = engine.createNode("MainCamera");
engine.addChild(rootNode, camera);

// Subscribe to events
engine.connect(EngineSignals.ENGINE_UPDATE, event -> {
    // Handle update
});
```

### ECS Architecture

```java
// Define a component
public class HealthComponent implements Component {
    public float health = 100;
    public float maxHealth = 100;
}

// Create a system
public class HealthSystem extends GameSystem {
    @Override
    public void update(float deltaTime) {
        for (Entity entity : getEntitiesWith(HealthComponent.class)) {
            HealthComponent health = entity.getComponent(HealthComponent.class);
            // Update health logic
        }
    }
}

// Register the system
engine.addSystem(new HealthSystem());
```

### Node System

```java
// Create a scene
Scene scene = new Scene("MyGame");
scene.getRoot().addChild(playerNode);
scene.getRoot().addChild(cameraNode);
scene.getRoot().addChild(lightNode);

// Use signals
playerNode.ready.subscribe(node -> {
    System.out.println("Player ready!");
});

cameraNode.enteredTree.subscribe(node -> {
    System.out.println("Camera added to scene!");
});
```

## Editor Features

The JavaBlocks Editor provides:

- **Scene View**: Visual scene composition
- **Hierarchy Panel**: Scene tree navigation
- **Inspector Panel**: Property editing
- **Asset Browser**: Manage game assets
- **Console**: Debug output
- **Game Preview**: Test without leaving editor

## Multiplayer

```java
// Connect to server
NetworkManager.connect("localhost", 8765);

// Handle events
NetworkManager.onPlayerJoin(player -> {
    System.out.println("Player joined: " + player.getName());
});

NetworkManager.onPlayerLeave(player -> {
    System.out.println("Player left: " + player.getName());
});

// Sync entities
NetworkManager.syncEntity(playerEntity);
```

## Plugin Development

Create plugins by implementing the Plugin interface:

```java
public class MyPlugin implements Plugin {
    @Override
    public void initialize(JavaBlocksEngine engine) {
        // Initialize plugin
    }
    
    @Override
    public void onUpdate(float deltaTime) {
        // Update logic
    }
}
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

MIT License - see LICENSE file for details.

## Support

- **Documentation**: [docs.javablocks.io](https://docs.javablocks.io)
- **Discord**: [discord.javablocks.io](https://discord.javablocks.io)
- **GitHub**: [github.com/javablocks/engine](https://github.com/javablocks/engine)

## Acknowledgments

- **LibGDX**: Cross-platform game framework
- **Godot Engine**: Node-based architecture inspiration
- **Unity**: Component system inspiration
- **Unreal Engine**: High-performance rendering inspiration
- **Roblox**: User-friendly creation suite inspiration
