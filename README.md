# Planet

Planet is a work-in-progress procedural planet simulator built with Godot and Kotlin/JVM. It models tectonic plates, geology, erosion, climate, ocean currents, biomes, and related world-generation systems on a distorted spherical grid.

## Requirements

- Godot 4.4.1 with Godot Kotlin/JVM 0.13.1-4.4.1
- JDK 17
- Git LFS 3.x

## Setup

Clone the repository, fetch the save fixtures, and build the Kotlin project:

```powershell
git lfs install
git lfs pull
cd planet
.\gradlew.bat build
```

Open `planet/project.godot` in the matching Godot Kotlin/JVM editor. The application starts with a seed prompt and generates the planet after the seed is submitted.

## Tests

Run the JVM tests from the `planet` directory:

```powershell
.\gradlew.bat test
```

## Configuration

- `planet/tectonics_config.json` contains reloadable tectonic and erosion parameters.
- `planet/climate_config.json` contains reloadable climate simulation parameters.
- The climate menu also exposes runtime-only modifiers that are intentionally not written to either file.

Use the in-game refresh button after editing a configuration file.

To calibrate `climate_config.json` against the bundled Earth Hersfeldt reference,
use the headless, resumable tuner described in
[`planet/CLIMATE_TUNING.md`](planet/CLIMATE_TUNING.md).

## Project Layout

- `planet/src/main/kotlin/dev/biserman/planet/planet` contains the world model and simulations.
- `planet/src/main/kotlin/dev/biserman/planet/geometry` and `topology` contain spherical mesh and adjacency code.
- `planet/src/main/kotlin/dev/biserman/planet/rendering` contains color modes and debug renderers.
- `planet/src/main/kotlin/dev/biserman/planet/gui` contains Godot-facing controls and tools.
- `planet/save` contains large sample worlds managed by Git LFS.

OpenSimplex2 is vendored under `planet/src/main/java/opensimplex2`. Other JVM dependencies are declared in `planet/build.gradle.kts`.
