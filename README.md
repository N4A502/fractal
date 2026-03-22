# Fractal Explorer

Fractal Explorer is a desktop fractal viewer built with Java 17.
The current UI is pure JavaFX. Rendering stays in native Java, with a default CPU path and an optional GPU path for escape-time fractals.

## Features

- Browse multiple fractal types
  - Mandelbrot Set
  - Julia Set
  - Burning Ship
  - Koch Snowflake
  - Fractal Tree
  - Sierpinski Carpet
- Mouse-wheel zoom, drag pan, box zoom, double-click reset
- Viewport uses a configurable logical size and scales proportionally in the app window
- Export the current view to PNG
- Export high-resolution PNG at current size, 2x, 4x, or a custom size
- Save the current configuration and restore it on next launch
- Unified color system with presets, color slots, and advanced controls
- Automatic CPU / GPU backend selection with CPU fallback
- Windows app image packaging script

## Stack

- Java 17
- JavaFX 21
- Maven
- LWJGL 3

## Project Layout

- `src/main/java/com/example/fractal/FractalApplication.java`
  - Application entry point
- `src/main/java/com/example/fractal/FractalFxWindow.java`
  - Main JavaFX window
- `src/main/java/com/example/fractal/FractalFxViewport.java`
  - Viewport interaction, preview, and export
- `src/main/java/com/example/fractal/model/`
  - Fractal definitions and view state
- `src/main/java/com/example/fractal/render/`
  - Render backends, backend selection, and color model
- `scripts/package-windows.ps1`
  - Windows app image packaging script
- `docs/DEV_CONTEXT.md`
  - Current development context

## Requirements

- JDK 17 or newer
- Maven 3.9 or newer
- Default Maven local repository: `%USERPROFILE%\\.m2\\repository`

## Development Run

Compile first:

```bash
mvn -DskipTests clean compile
```

Then run this main class from the IDE:

```text
com.example.fractal.FractalApplication
```

Do not rely on `java -jar target/*.jar` as the normal runtime path. JavaFX runtime components are not part of the standard JDK classpath experience, so running the jar directly is error-prone.

## Windows Packaging

Build a Windows app image:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\\package-windows.ps1
```

Output directory:

```text
target\\dist\\FractalExplorer
```

Skip compilation if the project is already compiled:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\\package-windows.ps1 -SkipCompile
```

Optionally override the Maven local repository:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\\package-windows.ps1 -MavenRepo "C:\\path\\to\\repository"
```

## Color System

The palette area currently has three layers of control:

- Style preset
- Three high-frequency color slots
  - Escape-time fractals: `inside color`, `main gradient color`, `background color`
  - Geometry / recursive fractals: `accent color`, `primary shape color`, `background color`
- Advanced controls
  - Hue start
  - Hue range
  - Saturation
  - Brightness floor
  - Brightness range
  - Contrast
  - Vibrance
  - Exposure

## GPU Notes

The GPU path currently only applies to escape-time fractals and depends on local graphics support plus available LWJGL dependencies.

The app automatically falls back to CPU if any of these conditions apply:

- No usable GPU acceleration is detected
- GPU backend initialization fails
- LWJGL jars are missing from the local dependency cache
- Driver or OpenGL support is insufficient

## Export Notes

PNG export always re-renders from the current fractal type, depth, zoom, offset, and logical viewport size. It is not a screenshot.

That means:

- High-resolution export is not limited by the current window resolution
- Export output matches the current view parameters
- Default export size is the current logical viewport size

## Current Limits

- The GPU path only covers escape-time fractals
- The Windows script currently builds an app image, not an installer
- If LWJGL dependencies are missing locally, runtime falls back to CPU