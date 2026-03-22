# Development Context

Last updated: 2026-03-23
Current branch: `dev`
Remote main branch: `origin/main`

## Project State

- Project name: `fractal-explorer`
- Stack: Java 17, JavaFX, Maven, LWJGL
- UI: pure JavaFX, no Swing shell, no JavaFX-in-Swing embedding
- App entry: `com.example.fractal.FractalApplication`
- Main window: `com.example.fractal.FractalFxWindow`
- Viewport and export: `com.example.fractal.FractalFxViewport`
- Working branch for current round: `dev`

## Current Architecture

### UI layer
- `src/main/java/com/example/fractal/FractalApplication.java`
  - JavaFX startup entry
- `src/main/java/com/example/fractal/FractalFxWindow.java`
  - Main window shell
  - Menu bar + left control rail + right viewport layout
  - Custom formula panel
  - Custom curve panel
- `src/main/java/com/example/fractal/FractalFxViewport.java`
  - Viewport interaction, preview, box zoom, export, context menu
  - Preview uses a configured logical viewport size and scales proportionally
- `src/main/java/com/example/fractal/custom/`
  - `ComplexFormulaParser.java`
    - Restricted custom formula parser
  - `CustomFormulaLibrary.java`
    - Built-in custom formula templates
    - Grouped as `Stable`, `Bold`, `Floral`, `Spiral`
  - `CurveContourExtractor.java`
    - Largest-contour extraction from imported images
  - `CustomFractalManager.java`
    - Shared in-memory custom formula / curve settings

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
- `src/main/java/com/example/fractal/render/CustomFormulaRenderer.java`
  - CPU renderer for restricted user-defined formulas
- `src/main/java/com/example/fractal/render/CustomCurveFractalRenderer.java`
  - CPU renderer for recursive contour-based custom curve fractals

### Model layer
- `src/main/java/com/example/fractal/model/FractalRegistry.java`
  - Fractal definition registry
  - Includes built-in fractals plus `Custom Formula` and `Custom Curve`
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

### Custom formula behavior
- Two custom modes exist in the registry:
  - `Custom Formula`
  - `Custom Curve`
- `Custom Formula` supports:
  - Restricted math expressions only
  - Variables `z` and `c`
  - Operators `+ - * / ^`
  - Functions `sin`, `cos`, `tan`, `exp`, `log`, `abs`
  - `Mandelbrot-like` and `Julia-like` modes
- Formula template support exists in the UI:
  - 24 built-in templates
  - Group selector: `Stable`, `Bold`, `Floral`, `Spiral`
  - Single random button
  - `Try 10` button that samples 10 formulas from the current group and leaves the last one active

### Custom curve behavior
- User can import an image for `Custom Curve`
- Extraction keeps the largest single contour only
- The contour is simplified and normalized before rendering
- Rendering recursively reuses that contour as a generated pattern
- This is contour-based recursive generation, not equation inference from the image

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
- Custom formula rendering does not currently use the GPU backend
- Custom curve extraction assumes one dominant contour and may not behave well on noisy inputs
- `CustomFractalManager` is currently in-memory only; custom formula / curve settings are not yet persisted to `Preferences`

## Latest Work In This Round

- Created `dev` branch and switched work there
- Added `Custom Formula` fractal support
- Added restricted formula parsing via `ComplexFormulaParser`
- Added `Custom Curve` fractal support
- Added image contour extraction via `CurveContourExtractor`
- Added `CustomFormulaRenderer` and `CustomCurveFractalRenderer`
- Registered new fractal types in `FractalRegistry`
- Added 24 built-in custom formula templates
- Added custom formula grouping: `Stable`, `Bold`, `Floral`, `Spiral`
- Added `Random Formula` and `Try 10` actions in the custom formula UI
- Updated `README.md`
- Updated this context file

## Read First Next Time

1. `docs/DEV_CONTEXT.md`
2. `README.md`
3. `src/main/java/com/example/fractal/FractalApplication.java`
4. `src/main/java/com/example/fractal/FractalFxWindow.java`
5. `src/main/java/com/example/fractal/FractalFxViewport.java`
6. `src/main/java/com/example/fractal/custom/CustomFormulaLibrary.java`
7. `src/main/java/com/example/fractal/custom/ComplexFormulaParser.java`
8. `src/main/java/com/example/fractal/custom/CurveContourExtractor.java`
9. `src/main/java/com/example/fractal/render/CustomFormulaRenderer.java`
10. `src/main/java/com/example/fractal/render/CustomCurveFractalRenderer.java`
11. `src/main/java/com/example/fractal/render/AbstractEscapeTimeRenderer.java`
12. `src/main/java/com/example/fractal/render/LwjglGpuEscapeTimeBackend.java`
