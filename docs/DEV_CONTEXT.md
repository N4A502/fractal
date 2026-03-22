# Development Context

Last updated: 2026-03-22
Current branch: `main`
Remote main branch: `origin/main`

## Project State

- Project name: `fractal-explorer`
- Stack: Java 17, JavaFX, Maven, LWJGL
- UI: pure JavaFX, no Swing shell, no JavaFX-in-Swing embedding
- App entry: `com.example.fractal.FractalApplication`
- Main window: `com.example.fractal.FractalFxWindow`
- Viewport and export: `com.example.fractal.FractalFxViewport`

## Current Architecture

### UI layer
- `src/main/java/com/example/fractal/FractalApplication.java`
  - JavaFX startup entry
- `src/main/java/com/example/fractal/FractalFxWindow.java`
  - Main window shell
  - Menu bar + left control rail + right viewport layout
- `src/main/java/com/example/fractal/FractalFxViewport.java`
  - Viewport interaction, preview, box zoom, export, context menu
  - Preview uses a configured logical viewport size and scales proportionally

### Render layer
- `src/main/java/com/example/fractal/render/FractalRenderService.java`
  - Async render service
  - New render requests cancel stale work
- `src/main/java/com/example/fractal/render/AbstractEscapeTimeRenderer.java`
  - Shared escape-time fractal logic
- `src/main/java/com/example/fractal/render/CpuEscapeTimeBackend.java`
  - Default CPU path
  - Tile-based parallel rendering
- `src/main/java/com/example/fractal/render/EscapeTimeBackendSelector.java`
  - Automatic GPU / CPU selection
  - Automatic CPU fallback
- `src/main/java/com/example/fractal/render/LwjglGpuEscapeTimeBackend.java`
  - Optional GPU path
  - LWJGL + OpenGL shader
- `src/main/java/com/example/fractal/render/EscapeTimeColorSettings.java`
  - Palette settings model
- `src/main/java/com/example/fractal/render/EscapeTimeColorPreset.java`
  - Style presets
- `src/main/java/com/example/fractal/render/EscapeTimeColorManager.java`
  - Global palette state

### Model layer
- `src/main/java/com/example/fractal/model/FractalRegistry.java`
  - Fractal definition registry
- `src/main/java/com/example/fractal/model/FractalViewState.java`
  - Current view state: fractal type, depth, zoom, offset

## Current Behavior

### View size
- Left panel supports viewport size presets and custom width / height
- Logical viewport size drives:
  - Preview render size
  - Proportional display scaling in the app window
  - Default export size
- `3840 x 2160` is supported

### Interaction policy
- Pan: drag shows a frozen-frame transform, then re-renders on release
- Zoom: mouse wheel uses a short debounce, then re-renders
- Box zoom: renders immediately when the interaction ends
- Double click: resets the view
- New render requests discard stale work

### Palette semantics
- Escape-time fractals currently use:
  - `inside color`
  - `main gradient color`
  - `background color`
- Geometry / recursive fractals currently use:
  - `accent color`
  - `primary shape color`
  - `background color`
- Palette presets, palette swatches, and combo buttons still exist in the UI
- CPU and GPU paths share the same palette settings

### Build and packaging
- `mvn -DskipTests clean compile` passes
- `scripts/package-windows.ps1` is still the Windows packaging entry point
- The project no longer creates `.m2/` inside the repo

## Current Limits

- GPU only covers escape-time fractals
- Missing OpenGL support or LWJGL jars causes automatic CPU fallback
- Windows packaging currently produces an app image, not an installer

## Latest Work In This Round

- Restored `FractalFxWindow.java` to a clean UTF-8 state after local encoding corruption in the working tree
- Updated escape-time palette wording from `curve color` to `main gradient color`
- Updated `README.md`
- Updated this context file

## Read First Next Time

1. `docs/DEV_CONTEXT.md`
2. `README.md`
3. `src/main/java/com/example/fractal/FractalApplication.java`
4. `src/main/java/com/example/fractal/FractalFxWindow.java`
5. `src/main/java/com/example/fractal/FractalFxViewport.java`
6. `src/main/java/com/example/fractal/render/AbstractEscapeTimeRenderer.java`
7. `src/main/java/com/example/fractal/render/LwjglGpuEscapeTimeBackend.java`