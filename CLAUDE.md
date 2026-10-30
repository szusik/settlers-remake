# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JSettlers is a Java remake of the 1998 strategy game "The Settlers 3" (Blue Byte). Cross-platform: Windows, Linux, macOS, Android. Currently in alpha. Requires original Settlers 3 game files (GFX, SND, MAP folders) to run.

## Build & Run Commands

```bash
./gradlew build            # Build all modules
./gradlew run              # Run desktop (Swing) version
./gradlew runMapCreator    # Run map creator tool
./gradlew runTools         # Run dev tools
./gradlew release          # Create release packages (JSettlers.zip, Android APK, DedicatedServer jar)
```

## Testing

```bash
./gradlew test                                 # All tests
./gradlew :jsettlers.logic:unitTest            # Unit tests only (fast)
./gradlew :jsettlers.logic:aiDifficultiesIT    # AI difficulty integration tests
./gradlew :jsettlers.logic:autoReplayIT        # Auto replay integration tests
./gradlew :jsettlers.logic:replayValidationIT  # Replay validation integration tests
```

Tests use up to 4GB heap and run in parallel (half available processors). Set `JSETTLERS_RESOURCES` environment variable to point at common resources if needed.

## Module Architecture

The project is a 13-module Gradle build. Key modules:

| Module | Role |
|--------|------|
| `jsettlers.logic` | Core game logic: map, buildings, units, AI, game state, timer |
| `jsettlers.common` | Shared data structures, constants, interfaces |
| `jsettlers.graphics` | Game rendering, UI components, layout engine |
| `jsettlers.network` | Multiplayer networking, dedicated server protocol |
| `jsettlers.main.swing` | Desktop entry point (`SwingManagedJSettlers`) |
| `jsettlers.main.android` | Android entry point |
| `jsettlers.mapcreator` | Map editor application |
| `go.graphics` | Graphics abstraction layer (backend-agnostic) |
| `go.graphics.swing` | LWJGL 3 / Swing desktop graphics backend |
| `go.graphics.android` | Android graphics backend |
| `jsettlers.testutils` | Shared test helpers |
| `jsettlers.tests` | Integration and replay validation tests |
| `buildSrc` | Custom Gradle tasks: texture generation, GLSL shader compilation, git revision detection |

## Key Architectural Patterns

**Game Loop & Replay**: `jsettlers.logic.main` owns the simulation loop and a complete replay recording system. Replays are written to `resources/logs/<date>/` alongside `_out.log`. The replay system is used for debugging and regression testing.

**Graphics Abstraction**: `go.graphics` defines backend-agnostic interfaces; `go.graphics.swing` (LWJGL) and `go.graphics.android` provide implementations. GLSL shaders in `go.graphics` are compiled to Java source by a `buildSrc` task at build time.

**Texture Pipeline**: Raw image assets are processed by a `buildSrc` Gradle task into generated Java source during compilation. The outputs are tracked as source directories.

**UI Layouts**: XML layout descriptors are compiled to Java code by a Gradle task in `buildSrc` (similar to Android's layout inflation, but custom).

**AI System**: Located in `jsettlers.logic` under `jsettlers.ai`. Multiple difficulty levels built from composable strategies: army, construction, economy, and high-level orchestration.

**Networking**: `jsettlers.network` handles both client-server multiplayer and dedicated server (`DedicatedServerApp`). Separates game logic from network transport.

## Entry Points

- **Desktop game**: `jsettlers.main.swing.SwingManagedJSettlers`
- **Dedicated server**: `jsettlers.network.server.DedicatedServerApp`
- **Map creator**: `jsettlers.mapcreator.main.MapCreatorApp`

## Gradle Notes

- Gradle daemon is disabled (`org.gradle.daemon=false`)
- Configuration on demand is disabled
- Each module has a custom resource-copying task that places resources alongside compiled classes
- `buildSrc` tasks (shader/texture/layout generation) must complete before compilation; outputs are registered as generated source directories