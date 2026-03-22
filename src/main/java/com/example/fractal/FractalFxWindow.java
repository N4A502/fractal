package com.example.fractal;

import com.example.fractal.model.FractalDefinition;
import com.example.fractal.model.FractalRegistry;
import com.example.fractal.render.AbstractEscapeTimeRenderer;
import com.example.fractal.render.EscapeTimeBackendSelector;
import com.example.fractal.render.EscapeTimeColorManager;
import com.example.fractal.render.EscapeTimeColorPreset;
import com.example.fractal.render.EscapeTimeColorSettings;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FractalFxWindow {

    private final Stage stage;
    private final BorderPane root;
    private final FractalFxViewport viewport;
    private final ComboBox<FractalDefinition> fractalSelector;
    private final Spinner<Integer> depthSpinner;
    private final Slider zoomSlider;
    private final ComboBox<ViewSizePreset> viewSizePresetSelector;
    private final Spinner<Integer> viewportWidthSpinner;
    private final Spinner<Integer> viewportHeightSpinner;
    private final ComboBox<EscapeTimeColorPreset> palettePresetSelector;
    private final Slider hueStartSlider;
    private final Slider hueRangeSlider;
    private final Slider saturationSlider;
    private final Slider brightnessFloorSlider;
    private final Slider brightnessRangeSlider;
    private final ColorPicker insideColorPicker;
    private final Button exportButton;
    private final Button exportCurrentButton;
    private final Button resetViewButton;
    private final Button sidebarToggleButton;
    private final Label summaryTitleLabel;
    private final Label summarySubtitleLabel;
    private final Label categoryValueLabel;
    private final Label descriptionValueLabel;
    private final Label depthValueLabel;
    private final Label zoomValueLabel;
    private final Label viewSizeValueLabel;
    private final Label backendPillLabel;
    private final Label renderPillLabel;
    private final Label paletteHintLabel;
    private final Label statusLabel;
    private ScrollPane sidebar;
    private boolean updatingZoomSlider;
    private boolean updatingViewSizeControls;
    private boolean updatingPaletteControls;
    private boolean sidebarVisible = true;

    public FractalFxWindow(Stage stage) {
        this.stage = stage;
        this.root = new BorderPane();
        List<FractalDefinition> fractals = FractalRegistry.createDefinitions();
        this.viewport = new FractalFxViewport();
        this.fractalSelector = new ComboBox<FractalDefinition>(FXCollections.observableArrayList(fractals));
        this.depthSpinner = new Spinner<Integer>();
        this.zoomSlider = new Slider(10, 400, 100);
        this.viewSizePresetSelector = new ComboBox<ViewSizePreset>(FXCollections.observableArrayList(createViewSizePresets()));
        this.viewportWidthSpinner = new Spinner<Integer>(320, 4096, 1280, 10);
        this.viewportHeightSpinner = new Spinner<Integer>(240, 4096, 720, 10);
        this.palettePresetSelector = new ComboBox<EscapeTimeColorPreset>(FXCollections.observableArrayList(EscapeTimeColorPreset.values()));
        this.hueStartSlider = new Slider(0, 360, 342);
        this.hueRangeSlider = new Slider(0, 360, 306);
        this.saturationSlider = new Slider(0, 100, 85);
        this.brightnessFloorSlider = new Slider(0, 100, 35);
        this.brightnessRangeSlider = new Slider(0, 100, 65);
        this.insideColorPicker = new ColorPicker(Color.rgb(5, 8, 18));
        this.exportButton = new Button("High-res export");
        this.exportCurrentButton = new Button("Current view");
        this.resetViewButton = new Button("Reset view");
        this.sidebarToggleButton = new Button();
        this.summaryTitleLabel = new Label("Fractal Explorer");
        this.summarySubtitleLabel = new Label("JavaFX workspace with configurable viewport and palette.");
        this.categoryValueLabel = new Label();
        this.descriptionValueLabel = new Label();
        this.depthValueLabel = new Label();
        this.zoomValueLabel = new Label();
        this.viewSizeValueLabel = new Label();
        this.backendPillLabel = createPillLabel();
        this.renderPillLabel = createPillLabel();
        this.paletteHintLabel = new Label("Palette controls affect escape-time fractals.");
        this.statusLabel = new Label("Cursor: -, - | Zoom: 1.00x | Offset: (0, 0)");

        configureStage();
        configureControls();
        configureInteractions();
        syncSelection();
        applyViewSizeControls();
        palettePresetSelector.setValue(EscapeTimeColorPreset.CLASSIC);
        refreshPaletteControlsFromManager();
    }

    public void show() {
        stage.show();
    }

    private void configureStage() {
        root.setStyle("-fx-background-color: #eef2f8;");
        root.setPadding(new Insets(10));
        root.setTop(buildMenuBar());
        sidebar = buildSidebar();
        root.setLeft(sidebar);
        root.setCenter(buildViewportShell());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1520, 940, Color.web("#eef2f8"));
        stage.setTitle("Fractal Explorer");
        stage.setScene(scene);
        stage.setMinWidth(1180);
        stage.setMinHeight(760);
        stage.setOnCloseRequest(event -> {
            viewport.shutdown();
            EscapeTimeBackendSelector.shutdown();
            Platform.exit();
        });
    }

    private void configureControls() {
        summaryTitleLabel.setFont(Font.font("System", 24));
        summaryTitleLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #181e2a;");
        summarySubtitleLabel.setStyle("-fx-text-fill: #5e687a; -fx-font-size: 13px;");
        statusLabel.setStyle("-fx-text-fill: #343d4e; -fx-font-size: 12px;");
        paletteHintLabel.setStyle("-fx-text-fill: #5e687a; -fx-font-size: 11px;");
        descriptionValueLabel.setWrapText(true);
        categoryValueLabel.setWrapText(true);
        viewSizeValueLabel.setStyle("-fx-text-fill: #181e2a; -fx-font-size: 12px; -fx-font-weight: 700;");

        depthSpinner.setEditable(true);
        depthSpinner.setMaxWidth(Double.MAX_VALUE);
        fractalSelector.setMaxWidth(Double.MAX_VALUE);
        zoomSlider.setMaxWidth(Double.MAX_VALUE);
        zoomSlider.setBlockIncrement(10);
        viewSizePresetSelector.setMaxWidth(Double.MAX_VALUE);
        viewportWidthSpinner.setEditable(true);
        viewportHeightSpinner.setEditable(true);
        viewportWidthSpinner.setMaxWidth(Double.MAX_VALUE);
        viewportHeightSpinner.setMaxWidth(Double.MAX_VALUE);
        palettePresetSelector.setMaxWidth(Double.MAX_VALUE);
        insideColorPicker.setMaxWidth(Double.MAX_VALUE);

        configureSlider(hueStartSlider);
        configureSlider(hueRangeSlider);
        configureSlider(saturationSlider);
        configureSlider(brightnessFloorSlider);
        configureSlider(brightnessRangeSlider);

        exportButton.setOnAction(event -> viewport.exportHighResolutionView(stage));
        exportCurrentButton.setOnAction(event -> viewport.exportCurrentView(stage));
        resetViewButton.setOnAction(event -> resetControls());
        stylePrimaryButton(exportButton);
        styleSecondaryButton(exportCurrentButton);
        styleSecondaryButton(resetViewButton);
        styleSecondaryButton(sidebarToggleButton);
        sidebarToggleButton.setOnAction(event -> toggleSidebar());
        refreshSidebarToggleLabel();
        refreshRuntimeInfo();
    }

    private void configureInteractions() {
        fractalSelector.setOnAction(event -> syncSelection());
        depthSpinner.valueProperty().addListener((obs, oldValue, newValue) -> syncControls());
        zoomSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingZoomSlider) {
                syncControls();
            }
        });

        viewSizePresetSelector.setOnAction(event -> applyViewSizePreset());
        viewportWidthSpinner.valueProperty().addListener((obs, oldValue, newValue) -> applyViewSizeControls());
        viewportHeightSpinner.valueProperty().addListener((obs, oldValue, newValue) -> applyViewSizeControls());

        palettePresetSelector.setOnAction(event -> applyPalettePreset());
        hueStartSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls());
        hueRangeSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls());
        saturationSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls());
        brightnessFloorSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls());
        brightnessRangeSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls());
        insideColorPicker.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls());

        viewport.setInteractionListener(new FractalFxViewport.InteractionListener() {
            @Override
            public void onZoomChanged(double zoom, double offsetX, double offsetY, boolean fromWheelAnchor) {
                if (fromWheelAnchor) {
                    setZoomSliderValue(zoom);
                }
                refreshRuntimeInfo();
            }

            @Override
            public void onViewChanged(double zoom, double offsetX, double offsetY, double mouseX, double mouseY) {
                statusLabel.setText(buildStatusText(zoom, offsetX, offsetY, mouseX, mouseY));
                refreshRuntimeInfo();
            }

            @Override
            public void onResetRequested() {
                resetControls();
            }
        });
    }

    private MenuBar buildMenuBar() {
        Menu fileMenu = new Menu("File");
        MenuItem exportCurrentItem = new MenuItem("Export Current View");
        exportCurrentItem.setOnAction(event -> viewport.exportCurrentView(stage));
        MenuItem exportHighResItem = new MenuItem("Export High Resolution PNG");
        exportHighResItem.setOnAction(event -> viewport.exportHighResolutionView(stage));
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(event -> stage.close());
        fileMenu.getItems().addAll(exportCurrentItem, exportHighResItem, new SeparatorMenuItem(), exitItem);

        Menu viewMenu = new Menu("View");
        MenuItem toggleControlsItem = new MenuItem("Show / Hide Controls");
        toggleControlsItem.setOnAction(event -> toggleSidebar());
        MenuItem resetViewItem = new MenuItem("Reset View");
        resetViewItem.setOnAction(event -> resetControls());
        viewMenu.getItems().addAll(toggleControlsItem, resetViewItem);

        Menu paletteMenu = new Menu("Palette");
        for (EscapeTimeColorPreset preset : EscapeTimeColorPreset.values()) {
            MenuItem item = new MenuItem(preset.toString());
            item.setOnAction(event -> {
                palettePresetSelector.setValue(preset);
                applyPalettePreset();
            });
            paletteMenu.getItems().add(item);
        }

        MenuBar menuBar = new MenuBar(fileMenu, viewMenu, paletteMenu);
        menuBar.setStyle("-fx-background-color: white; -fx-border-color: #dce2eb; -fx-border-radius: 16; -fx-background-radius: 16;");
        BorderPane.setMargin(menuBar, new Insets(0, 0, 10, 0));
        return menuBar;
    }

    private ScrollPane buildSidebar() {
        VBox sidebarContent = new VBox(10,
                createHeaderCard(),
                createControlCard(),
                createViewSizeCard(),
                createPaletteCard(),
                createExportCard(),
                createInfoStrip()
        );
        sidebarContent.setPadding(new Insets(0, 6, 0, 0));

        ScrollPane scrollPane = new ScrollPane(sidebarContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(330);
        scrollPane.setMinViewportWidth(330);
        scrollPane.setMaxWidth(330);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setPadding(Insets.EMPTY);
        BorderPane.setMargin(scrollPane, new Insets(0, 10, 0, 0));
        return scrollPane;
    }

    private VBox createHeaderCard() {
        VBox box = createCardBox();
        box.getChildren().addAll(
                summaryTitleLabel,
                summarySubtitleLabel,
                backendPillLabel,
                renderPillLabel
        );
        return box;
    }

    private VBox createControlCard() {
        VBox box = createCardBox();
        box.getChildren().addAll(
                createCardTitle("Controls"),
                createSectionLabel("Fractal"),
                fractalSelector,
                createSectionLabel("Category"),
                categoryValueLabel,
                createSectionLabel("Description"),
                descriptionValueLabel,
                createSectionLabel("Depth / iterations"),
                depthValueLabel,
                depthSpinner,
                createSectionLabel("Zoom"),
                zoomValueLabel,
                zoomSlider
        );
        return box;
    }

    private VBox createViewSizeCard() {
        VBox box = createCardBox();
        box.getChildren().addAll(
                createCardTitle("Viewport size"),
                createSectionLabel("Preset"),
                viewSizePresetSelector,
                createSectionLabel("Width"),
                viewportWidthSpinner,
                createSectionLabel("Height"),
                viewportHeightSpinner,
                viewSizeValueLabel,
                wrapLabel("The render view preserves this aspect ratio while the application window rescales. Default export uses the current view size.")
        );
        return box;
    }

    private VBox createPaletteCard() {
        VBox box = createCardBox();
        box.getChildren().addAll(
                createCardTitle("Palette"),
                createSectionLabel("Quick style"),
                palettePresetSelector,
                createSectionLabel("Hue start"),
                hueStartSlider,
                createSectionLabel("Hue range"),
                hueRangeSlider,
                createSectionLabel("Saturation"),
                saturationSlider,
                createSectionLabel("Brightness floor"),
                brightnessFloorSlider,
                createSectionLabel("Brightness range"),
                brightnessRangeSlider,
                createSectionLabel("Inside color"),
                insideColorPicker,
                paletteHintLabel
        );
        return box;
    }

    private VBox createExportCard() {
        VBox box = createCardBox();
        box.getChildren().addAll(
                createCardTitle("Actions"),
                sidebarToggleButton,
                exportButton,
                exportCurrentButton,
                resetViewButton,
                wrapLabel("High-resolution export re-renders with the current viewport size unless you choose another size in the export dialog.")
        );
        return box;
    }

    private VBox createInfoStrip() {
        VBox box = createCardBox();
        box.getChildren().addAll(
                createCardTitle("Shortcuts"),
                wrapLabel("Wheel zoom, drag pan, Shift-drag box zoom, double-click reset, right-click export.")
        );
        return box;
    }

    private StackPane buildViewportShell() {
        StackPane shell = new StackPane(viewport);
        shell.setPadding(new Insets(8));
        shell.setStyle(cardStyle());
        viewport.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return shell;
    }

    private VBox buildStatusBar() {
        VBox wrapper = new VBox(statusLabel);
        wrapper.setPadding(new Insets(8, 10, 8, 10));
        wrapper.setStyle(cardStyle());
        BorderPane.setMargin(wrapper, new Insets(10, 0, 0, 0));
        return wrapper;
    }

    private void syncSelection() {
        FractalDefinition definition = fractalSelector.getValue();
        if (definition == null) {
            fractalSelector.getSelectionModel().selectFirst();
            definition = fractalSelector.getValue();
            if (definition == null) {
                return;
            }
        }

        categoryValueLabel.setText(definition.category());
        descriptionValueLabel.setText(definition.description());
        summaryTitleLabel.setText(definition.name());
        summarySubtitleLabel.setText(definition.description());
        depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                definition.minDepth(),
                definition.maxDepth(),
                Math.max(definition.defaultDepth(), definition.minDepth()),
                1
        ));
        zoomSlider.setMax(Math.max(400, definition.defaultZoom()));
        zoomSlider.setValue(definition.defaultZoom());
        updatePaletteSectionState(definition);
        viewport.resetView();
        refreshRuntimeInfo();
        syncControls();
    }

    private void syncControls() {
        FractalDefinition definition = fractalSelector.getValue();
        if (definition == null) {
            return;
        }

        int depth = depthSpinner.getValue();
        double zoom = zoomSlider.getValue() / 100.0;
        depthValueLabel.setText(depth + " levels");
        zoomValueLabel.setText(String.format(Locale.US, "%.2f x", zoom));
        viewport.render(definition, depth, zoom);
        refreshRuntimeInfo();
    }

    private void resetControls() {
        FractalDefinition definition = fractalSelector.getValue();
        if (definition == null) {
            return;
        }

        viewport.resetView();
        zoomSlider.setMax(Math.max(400, definition.defaultZoom()));
        depthSpinner.getValueFactory().setValue(Math.max(definition.defaultDepth(), definition.minDepth()));
        zoomSlider.setValue(definition.defaultZoom());
        syncControls();
    }

    private void applyViewSizePreset() {
        if (updatingViewSizeControls) {
            return;
        }
        ViewSizePreset preset = viewSizePresetSelector.getValue();
        if (preset == null || preset.custom()) {
            return;
        }
        updatingViewSizeControls = true;
        try {
            viewportWidthSpinner.getValueFactory().setValue(preset.width());
            viewportHeightSpinner.getValueFactory().setValue(preset.height());
        } finally {
            updatingViewSizeControls = false;
        }
        applyViewSizeControls();
    }

    private void applyViewSizeControls() {
        if (updatingViewSizeControls) {
            return;
        }
        int width = viewportWidthSpinner.getValue();
        int height = viewportHeightSpinner.getValue();
        ViewSizePreset preset = findPreset(width, height);
        updatingViewSizeControls = true;
        try {
            viewSizePresetSelector.setValue(preset);
        } finally {
            updatingViewSizeControls = false;
        }
        viewSizeValueLabel.setText(String.format(Locale.US, "Current view: %d x %d (%.2f:1)", width, height, width / (double) height));
        viewport.setViewSize(width, height);
        refreshRuntimeInfo();
    }

    private ViewSizePreset findPreset(int width, int height) {
        for (ViewSizePreset preset : viewSizePresetSelector.getItems()) {
            if (!preset.custom() && preset.width() == width && preset.height() == height) {
                return preset;
            }
        }
        return ViewSizePreset.custom(width, height);
    }

    private void applyPalettePreset() {
        if (updatingPaletteControls) {
            return;
        }
        EscapeTimeColorPreset preset = palettePresetSelector.getValue();
        if (preset == null) {
            return;
        }
        EscapeTimeColorManager.setSettings(preset.createSettings());
        refreshPaletteControlsFromManager();
        syncControls();
    }

    private void applyPaletteControls() {
        if (updatingPaletteControls) {
            return;
        }
        EscapeTimeColorSettings settings = new EscapeTimeColorSettings(
                (float) hueStartSlider.getValue(),
                (float) hueRangeSlider.getValue(),
                (float) (saturationSlider.getValue() / 100.0),
                (float) (brightnessFloorSlider.getValue() / 100.0),
                (float) (brightnessRangeSlider.getValue() / 100.0),
                toRgb(insideColorPicker.getValue())
        );
        EscapeTimeColorManager.setSettings(settings);
        syncControls();
    }

    private void refreshPaletteControlsFromManager() {
        EscapeTimeColorSettings settings = EscapeTimeColorManager.getSettings();
        updatingPaletteControls = true;
        try {
            hueStartSlider.setValue(settings.hueStartDegrees());
            hueRangeSlider.setValue(settings.hueRangeDegrees());
            saturationSlider.setValue(settings.saturation() * 100.0);
            brightnessFloorSlider.setValue(settings.brightnessFloor() * 100.0);
            brightnessRangeSlider.setValue(settings.brightnessRange() * 100.0);
            insideColorPicker.setValue(fromRgb(settings.insideColorRgb()));
        } finally {
            updatingPaletteControls = false;
        }
    }

    private void updatePaletteSectionState(FractalDefinition definition) {
        boolean enabled = definition != null && definition.renderer() instanceof AbstractEscapeTimeRenderer;
        palettePresetSelector.setDisable(!enabled);
        hueStartSlider.setDisable(!enabled);
        hueRangeSlider.setDisable(!enabled);
        saturationSlider.setDisable(!enabled);
        brightnessFloorSlider.setDisable(!enabled);
        brightnessRangeSlider.setDisable(!enabled);
        insideColorPicker.setDisable(!enabled);
        paletteHintLabel.setText(enabled
                ? "Palette controls affect the current escape-time fractal."
                : "Palette controls apply to Mandelbrot, Julia, and Burning Ship.");
    }

    private void setZoomSliderValue(double zoom) {
        updatingZoomSlider = true;
        try {
            double sliderValue = Math.max(10, Math.round(zoom * 100.0));
            if (sliderValue > zoomSlider.getMax()) {
                double nextMax = zoomSlider.getMax();
                while (sliderValue > nextMax) {
                    nextMax *= 2.0;
                }
                zoomSlider.setMax(nextMax);
            }
            zoomSlider.setValue(sliderValue);
        } finally {
            updatingZoomSlider = false;
        }
    }

    private void refreshRuntimeInfo() {
        String backendText = viewport.getBackendDescription();
        boolean rendering = viewport.isRenderInProgress();
        backendPillLabel.setText("Backend: " + backendText);
        if (rendering) {
            renderPillLabel.setText("Rendering...");
            renderPillLabel.setStyle(pillStyle("#e3edff", "#1f57c3"));
        } else {
            renderPillLabel.setText("Last render: " + viewport.getLastRenderDurationMillis() + " ms");
            renderPillLabel.setStyle(pillStyle("#e3f7ef", "#148c5c"));
        }
    }

    private String buildStatusText(double zoom, double offsetX, double offsetY, double mouseX, double mouseY) {
        String mouse = mouseX >= 0 && mouseY >= 0
                ? String.format(Locale.US, "%.0f, %.0f", mouseX, mouseY)
                : "-, -";
        String complex = buildComplexCoordinateText(mouseX, mouseY, zoom, offsetX, offsetY);
        return String.format(Locale.US,
                "Cursor: %s%s | Zoom: %.2fx | Offset: (%.0f, %.0f) | View: %d x %d | Backend: %s",
                mouse,
                complex,
                zoom,
                offsetX,
                offsetY,
                viewport.getViewportWidth(),
                viewport.getViewportHeight(),
                viewport.getBackendDescription());
    }

    private String buildComplexCoordinateText(double mouseX, double mouseY, double zoom, double offsetX, double offsetY) {
        if (mouseX < 0 || mouseY < 0) {
            return "";
        }

        FractalDefinition definition = fractalSelector.getValue();
        if (definition == null || !(definition.renderer() instanceof AbstractEscapeTimeRenderer)) {
            return "";
        }

        AbstractEscapeTimeRenderer renderer = (AbstractEscapeTimeRenderer) definition.renderer();
        double real = renderer.mapPlaneX((int) Math.round(mouseX), viewport.getViewportWidth(), zoom, offsetX);
        double imaginary = renderer.mapPlaneY(
                (int) Math.round(mouseY),
                viewport.getViewportWidth(),
                viewport.getViewportHeight(),
                zoom,
                offsetY
        );
        return String.format(Locale.US, " | Complex: %.6f %+.6fi", real, imaginary);
    }

    private VBox createCardBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(12));
        box.setStyle(cardStyle());
        box.setFillWidth(true);
        return box;
    }

    private Label createCardTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #181e2a; -fx-font-size: 16px; -fx-font-weight: 700;");
        return label;
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #181e2a; -fx-font-size: 12px; -fx-font-weight: 700;");
        return label;
    }

    private Label wrapLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #5e687a; -fx-font-size: 12px;");
        return label;
    }

    private Label createPillLabel() {
        Label label = new Label();
        label.setStyle(pillStyle("#e3edff", "#1f57c3"));
        return label;
    }

    private void stylePrimaryButton(Button button) {
        button.setStyle("-fx-background-color: #2870ff; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 9 12 9 12;");
        button.setMaxWidth(Double.MAX_VALUE);
    }

    private void styleSecondaryButton(Button button) {
        button.setStyle("-fx-background-color: white; -fx-text-fill: #181e2a; -fx-border-color: #dce2eb; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 9 12 9 12;");
        button.setMaxWidth(Double.MAX_VALUE);
    }

    private void configureSlider(Slider slider) {
        slider.setMaxWidth(Double.MAX_VALUE);
        slider.setBlockIncrement(1);
    }

    private String cardStyle() {
        return "-fx-background-color: white; -fx-border-color: #dce2eb; -fx-border-radius: 16; -fx-background-radius: 16;";
    }

    private String pillStyle(String background, String foreground) {
        return "-fx-background-color: " + background + "; -fx-text-fill: " + foreground + "; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 999; -fx-padding: 8 12 8 12;";
    }

    private void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        root.setLeft(sidebarVisible ? sidebar : null);
        refreshSidebarToggleLabel();
    }

    private void refreshSidebarToggleLabel() {
        sidebarToggleButton.setText(sidebarVisible ? "Hide controls" : "Show controls");
    }

    private int toRgb(Color color) {
        int r = (int) Math.round(color.getRed() * 255.0);
        int g = (int) Math.round(color.getGreen() * 255.0);
        int b = (int) Math.round(color.getBlue() * 255.0);
        return (r << 16) | (g << 8) | b;
    }

    private Color fromRgb(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return Color.rgb(r, g, b);
    }

    private List<ViewSizePreset> createViewSizePresets() {
        return Arrays.asList(
                new ViewSizePreset("HD 1280 x 720", 1280, 720, false),
                new ViewSizePreset("Square 1024 x 1024", 1024, 1024, false),
                new ViewSizePreset("Portrait 1080 x 1350", 1080, 1350, false),
                new ViewSizePreset("Full HD 1920 x 1080", 1920, 1080, false),
                ViewSizePreset.custom(1280, 720)
        );
    }

    private static final class ViewSizePreset {
        private final String label;
        private final int width;
        private final int height;
        private final boolean custom;

        private ViewSizePreset(String label, int width, int height, boolean custom) {
            this.label = label;
            this.width = width;
            this.height = height;
            this.custom = custom;
        }

        private static ViewSizePreset custom(int width, int height) {
            return new ViewSizePreset("Custom", width, height, true);
        }

        private int width() {
            return width;
        }

        private int height() {
            return height;
        }

        private boolean custom() {
            return custom;
        }

        @Override
        public String toString() {
            return custom ? String.format(Locale.US, "Custom (%d x %d)", width, height) : label;
        }
    }
}
