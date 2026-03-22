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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FractalFxWindow {

    private static final double SIDEBAR_AUTO_HIDE_THRESHOLD = 1380.0;

    private final Stage stage;
    private final BorderPane root;
    private final FractalFxViewport viewport;
    private final FractalPreferences preferences;
    private final List<FractalDefinition> fractals;
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
    private final Slider contrastSlider;
    private final Slider vibranceSlider;
    private final Slider exposureSlider;
    private final ColorPicker insideColorPicker;
    private final ColorPicker curveColorPicker;
    private final ColorPicker backgroundColorPicker;
    private final Button resetViewButton;
    private final Button resetPaletteButton;
    private final Label depthValueLabel;
    private final Label zoomValueLabel;
    private final Label viewSizeValueLabel;
    private final Label backendPillLabel;
    private final Label renderPillLabel;
    private final Label paletteHintLabel;
    private final Label statusLabel;
    private final Label hueStartValueLabel;
    private final Label hueRangeValueLabel;
    private final Label saturationValueLabel;
    private final Label brightnessFloorValueLabel;
    private final Label brightnessRangeValueLabel;
    private final Label contrastValueLabel;
    private final Label vibranceValueLabel;
    private final Label exposureValueLabel;
    private final Region paletteGradientPreview;
    private final FlowPane paletteSwatchPane;
    private ScrollPane sidebar;
    private boolean updatingZoomSlider;
    private boolean updatingViewSizeControls;
    private boolean updatingPaletteControls;
    private boolean sidebarVisible = true;
    private boolean sidebarAutoCollapsed;

    public FractalFxWindow(Stage stage) {
        this.stage = stage;
        this.root = new BorderPane();
        this.preferences = new FractalPreferences();
        this.fractals = FractalRegistry.createDefinitions();
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
        this.contrastSlider = new Slider(0, 100, 65);
        this.vibranceSlider = new Slider(0, 100, 85);
        this.exposureSlider = new Slider(0, 100, 68);
        this.insideColorPicker = new ColorPicker(Color.rgb(5, 8, 18));
        this.curveColorPicker = new ColorPicker(Color.rgb(107, 227, 255));
        this.backgroundColorPicker = new ColorPicker(Color.rgb(7, 12, 26));
        this.resetViewButton = new Button("重置视图");
        this.resetPaletteButton = new Button("重置配色");
        this.depthValueLabel = new Label();
        this.zoomValueLabel = new Label();
        this.viewSizeValueLabel = new Label();
        this.backendPillLabel = createPillLabel();
        this.renderPillLabel = createPillLabel();
        this.paletteHintLabel = new Label("先选预设，再用高级调色微调；调色盘用于快速改内部颜色。");
        this.statusLabel = new Label("光标：-, - | 缩放：1.00x | 偏移：(0, 0)");
        this.hueStartValueLabel = createValueBadge();
        this.hueRangeValueLabel = createValueBadge();
        this.saturationValueLabel = createValueBadge();
        this.brightnessFloorValueLabel = createValueBadge();
        this.brightnessRangeValueLabel = createValueBadge();
        this.contrastValueLabel = createValueBadge();
        this.vibranceValueLabel = createValueBadge();
        this.exposureValueLabel = createValueBadge();
        this.paletteGradientPreview = new Region();
        this.paletteSwatchPane = new FlowPane();

        configureStage();
        configureControls();
        restoreSavedConfiguration();
        configureInteractions();
        refreshRuntimeInfo();
    }

    public void show() {
        stage.show();
    }

    private void configureStage() {
        root.setStyle("-fx-background-color: #eef2f8;");
        root.setPadding(new Insets(8));
        root.setTop(buildMenuBar());
        sidebar = buildSidebar();
        root.setLeft(sidebar);
        root.setCenter(buildContentShell());

        Scene scene = new Scene(root, 1520, 940, Color.web("#eef2f8"));
        stage.setTitle("分形浏览器");
        stage.setScene(scene);
        scene.widthProperty().addListener((obs, oldValue, newValue) -> updateResponsiveSidebar(newValue.doubleValue()));
        stage.setMinWidth(1180);
        stage.setMinHeight(760);
        stage.setOnCloseRequest(event -> {
            persistCurrentConfiguration(false);
            viewport.shutdown();
            EscapeTimeBackendSelector.shutdown();
            Platform.exit();
        });
    }

    private void configureControls() {
        statusLabel.setStyle("-fx-text-fill: #343d4e; -fx-font-size: 11px;");
        paletteHintLabel.setStyle("-fx-text-fill: #5e687a; -fx-font-size: 10px;");
        viewSizeValueLabel.setStyle("-fx-text-fill: #181e2a; -fx-font-size: 12px; -fx-font-weight: 700;");

        fractalSelector.setMaxWidth(Double.MAX_VALUE);
        fractalSelector.setVisibleRowCount(8);
        depthSpinner.setEditable(true);
        depthSpinner.setMaxWidth(Double.MAX_VALUE);
        zoomSlider.setMaxWidth(Double.MAX_VALUE);
        zoomSlider.setBlockIncrement(10);
        zoomSlider.setMajorTickUnit(100);
        viewSizePresetSelector.setMaxWidth(Double.MAX_VALUE);
        viewSizePresetSelector.setPromptText("自定义");
        viewportWidthSpinner.setEditable(true);
        viewportHeightSpinner.setEditable(true);
        viewportWidthSpinner.setMaxWidth(Double.MAX_VALUE);
        viewportHeightSpinner.setMaxWidth(Double.MAX_VALUE);
        palettePresetSelector.setMaxWidth(Double.MAX_VALUE);
        palettePresetSelector.setPromptText("自定义");
        insideColorPicker.setMaxWidth(Double.MAX_VALUE);
        curveColorPicker.setMaxWidth(Double.MAX_VALUE);
        backgroundColorPicker.setMaxWidth(Double.MAX_VALUE);

        configureSlider(hueStartSlider, 90, 15);
        configureSlider(hueRangeSlider, 90, 15);
        configureSlider(saturationSlider, 25, 5);
        configureSlider(brightnessFloorSlider, 25, 5);
        configureSlider(brightnessRangeSlider, 25, 5);
        configureSlider(contrastSlider, 25, 5);
        configureSlider(vibranceSlider, 25, 5);
        configureSlider(exposureSlider, 25, 5);

        resetViewButton.setOnAction(event -> resetControls());
        resetPaletteButton.setOnAction(event -> resetPaletteControls());
        styleSecondaryButton(resetViewButton);
        styleSecondaryButton(resetPaletteButton);

        paletteGradientPreview.setMinHeight(18);
        paletteGradientPreview.setPrefHeight(18);
        paletteGradientPreview.setMaxWidth(Double.MAX_VALUE);
        paletteGradientPreview.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #050812, #6BE3FF); -fx-background-radius: 999; -fx-border-color: #dce2eb; -fx-border-radius: 999;");

        paletteSwatchPane.setHgap(4);
        paletteSwatchPane.setVgap(0);
        paletteSwatchPane.setPrefWrapLength(1000);
        paletteSwatchPane.setMaxWidth(Double.MAX_VALUE);
        paletteSwatchPane.getChildren().setAll(buildPaletteSwatches());
        updatePaletteMetricLabels();
    }

    private void configureInteractions() {
        fractalSelector.setOnAction(event -> syncSelection(true));
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
        hueStartSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls(true));
        hueRangeSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls(true));
        saturationSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls(true));
        brightnessFloorSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls(true));
        brightnessRangeSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls(true));
        contrastSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyQuickPaletteControls(true));
        vibranceSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyQuickPaletteControls(true));
        exposureSlider.valueProperty().addListener((obs, oldValue, newValue) -> applyQuickPaletteControls(true));
        insideColorPicker.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls(true));
        curveColorPicker.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls(true));
        backgroundColorPicker.valueProperty().addListener((obs, oldValue, newValue) -> applyPaletteControls(true));

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

    private VBox buildMenuBar() {
        Menu fileMenu = new Menu("文件");
        MenuItem saveConfigItem = new MenuItem("保存当前配置");
        saveConfigItem.setOnAction(event -> persistCurrentConfiguration(true));
        MenuItem exportCurrentItem = new MenuItem("导出当前视图");
        exportCurrentItem.setOnAction(event -> viewport.exportCurrentView(stage));
        MenuItem exportHighResItem = new MenuItem("导出高分辨率 PNG");
        exportHighResItem.setOnAction(event -> viewport.exportHighResolutionView(stage));
        MenuItem exitItem = new MenuItem("退出");
        exitItem.setOnAction(event -> stage.close());
        fileMenu.getItems().addAll(saveConfigItem, new SeparatorMenuItem(), exportCurrentItem, exportHighResItem, new SeparatorMenuItem(), exitItem);

        Menu viewMenu = new Menu("视图");
        MenuItem toggleControlsItem = new MenuItem("显示 / 隐藏控制栏");
        toggleControlsItem.setOnAction(event -> toggleSidebar());
        MenuItem resetViewItem = new MenuItem("重置视图");
        resetViewItem.setOnAction(event -> resetControls());
        viewMenu.getItems().addAll(toggleControlsItem, resetViewItem);

        Menu paletteMenu = new Menu("调色");
        MenuItem resetPaletteItem = new MenuItem("重置配色");
        resetPaletteItem.setOnAction(event -> resetPaletteControls());
        paletteMenu.getItems().add(resetPaletteItem);
        paletteMenu.getItems().add(new SeparatorMenuItem());
        for (EscapeTimeColorPreset preset : EscapeTimeColorPreset.values()) {
            MenuItem item = new MenuItem("应用预设：" + preset);
            item.setOnAction(event -> {
                palettePresetSelector.setValue(preset);
                applyPalettePreset();
            });
            paletteMenu.getItems().add(item);
        }

        Menu infoMenu = new Menu("信息");
        MenuItem fractalInfoItem = new MenuItem("当前分形说明");
        fractalInfoItem.setOnAction(event -> showCurrentFractalInfo());
        MenuItem shortcutsItem = new MenuItem("快捷操作");
        shortcutsItem.setOnAction(event -> showShortcutInfo());
        infoMenu.getItems().addAll(fractalInfoItem, shortcutsItem);

        MenuBar menuBar = new MenuBar(fileMenu, viewMenu, paletteMenu, infoMenu);
        menuBar.setStyle("-fx-background-color: white; -fx-padding: 2 6 2 6;");
        Region separator = createSeparatorRegion(true);
        VBox wrapper = new VBox(menuBar, separator);
        BorderPane.setMargin(wrapper, Insets.EMPTY);
        return wrapper;
    }

    private ScrollPane buildSidebar() {
        VBox sidebarContent = new VBox(10,
                createControlCard(),
                createViewSizeCard(),
                createPaletteCard()
        );
        sidebarContent.setPadding(new Insets(0, 6, 0, 0));

        ScrollPane scrollPane = new ScrollPane(sidebarContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(286);
        scrollPane.setMinViewportWidth(286);
        scrollPane.setMaxWidth(286);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setPadding(Insets.EMPTY);
        BorderPane.setMargin(scrollPane, Insets.EMPTY);
        return scrollPane;
    }

    private VBox createControlCard() {
        VBox box = createCardBox();
        box.getChildren().addAll(
                createSectionLabel("类型"),
                fractalSelector,
                createInlineSpinnerRow("递归层级 / 迭代次数", depthSpinner, depthValueLabel),
                resetViewButton
        );
        return box;
    }

    private VBox createViewSizeCard() {
        VBox box = createCardBox();
        box.getChildren().addAll(
                createCardTitle("渲染视图尺寸"),
                createSectionLabel("预设"),
                viewSizePresetSelector,
                createSectionLabel("宽度"),
                viewportWidthSpinner,
                createSectionLabel("高度"),
                viewportHeightSpinner,
                viewSizeValueLabel,
                wrapLabel("视图会保持当前宽高比随窗口整体缩放，默认导出尺寸使用当前视图大小。")
        );
        return box;
    }

    private VBox createPaletteCard() {
        palettePresetSelector.setPrefHeight(36);
        resetPaletteButton.setMinHeight(36);
        resetPaletteButton.setPrefHeight(36);
        resetPaletteButton.setPrefWidth(112);
        resetPaletteButton.setMaxWidth(112);

        HBox presetRow = new HBox(8, palettePresetSelector, resetPaletteButton);
        presetRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(palettePresetSelector, Priority.ALWAYS);

        TitledPane expertPane = new TitledPane("专家模式", new VBox(10,
                createSliderRow("色相起点", hueStartSlider, hueStartValueLabel),
                createSliderRow("色相范围", hueRangeSlider, hueRangeValueLabel),
                createSliderRow("饱和度", saturationSlider, saturationValueLabel),
                createSliderRow("亮度下限", brightnessFloorSlider, brightnessFloorValueLabel),
                createSliderRow("亮度范围", brightnessRangeSlider, brightnessRangeValueLabel)
        ));
        expertPane.setExpanded(false);
        expertPane.setCollapsible(true);
        expertPane.setAnimated(false);
        expertPane.setStyle("-fx-text-fill: #181e2a; -fx-font-size: 12px; -fx-font-weight: 700;");

        VBox quickColorBox = new VBox(8,
                createSectionLabel("风格预设"),
                presetRow,
                createSectionLabel("渐变预览"),
                paletteGradientPreview,
                createSliderRow("对比度", contrastSlider, contrastValueLabel),
                createSliderRow("鲜艳度", vibranceSlider, vibranceValueLabel),
                createSliderRow("明暗", exposureSlider, exposureValueLabel),
                createInlineColorPickerRow("内部颜色", insideColorPicker),
                createInlineColorPickerRow("曲线颜色", curveColorPicker),
                createInlineColorPickerRow("背景颜色", backgroundColorPicker),
                createSectionLabel("调色盘"),
                paletteSwatchPane,
                paletteHintLabel
        );

        VBox box = createCardBox();
        box.getChildren().addAll(
                createCardTitle("调色"),
                quickColorBox,
                expertPane
        );
        return box;
    }

    private HBox createSliderRow(String title, Slider slider, Label valueLabel) {
        HBox header = new HBox(8, createSectionLabel(title), valueLabel);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        VBox wrapper = new VBox(6, header, slider);
        HBox row = new HBox(wrapper);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return row;
    }

    private HBox createInlineColorPickerRow(String title, ColorPicker picker) {
        Label titleLabel = createSectionLabel(title);
        picker.setPrefWidth(116);
        picker.setMaxWidth(116);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, titleLabel, spacer, picker);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private HBox createInlineSpinnerRow(String title, Spinner<Integer> spinner, Label valueLabel) {
        Label titleLabel = createSectionLabel(title);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spinner.setPrefWidth(110);
        spinner.setMaxWidth(110);
        HBox row = new HBox(8, titleLabel, spacer, valueLabel, spinner);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private HBox createInlineSliderRow(String title, Slider slider, Label valueLabel) {
        Label titleLabel = createSectionLabel(title);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setHgrow(slider, Priority.ALWAYS);
        HBox row = new HBox(8, titleLabel, spacer, valueLabel, slider);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private StackPane buildContentShell() {
        StackPane viewportShell = new StackPane(viewport);
        viewportShell.setPadding(new Insets(0));
        viewportShell.setStyle("-fx-background-color: white;");
        viewport.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Region separator = createSeparatorRegion(false);
        HBox row = new HBox(separator, viewportShell);
        HBox.setHgrow(viewportShell, Priority.ALWAYS);
        return new StackPane(row);
    }

    private void restoreSavedConfiguration() {
        FractalPreferences.SavedConfiguration saved = preferences.load();
        stage.setWidth(Math.max(stage.getMinWidth(), saved.stageWidth()));
        stage.setHeight(Math.max(stage.getMinHeight(), saved.stageHeight()));
        stage.setMaximized(saved.stageMaximized());
        sidebarVisible = saved.sidebarVisible();
        updateResponsiveSidebar(stage.getWidth());

        viewportWidthSpinner.getValueFactory().setValue(clamp(saved.viewWidth(), 320, 4096));
        viewportHeightSpinner.getValueFactory().setValue(clamp(saved.viewHeight(), 240, 4096));
        applyViewSizeControls();

        EscapeTimeColorManager.setSettings(saved.palette());
        refreshPaletteControlsFromManager();
        syncPalettePresetSelection(saved.palette());

        FractalDefinition definition = findDefinition(saved.fractalName());
        fractalSelector.setValue(definition);
        syncSelection(false);

        int depth = clamp(saved.depth(), definition.minDepth(), definition.maxDepth());
        depthSpinner.getValueFactory().setValue(depth);
        setZoomSliderValue(saved.zoom());
        viewport.applyState(definition, depth, saved.zoom(), saved.offsetX(), saved.offsetY());
        depthValueLabel.setText(depth + " 层");
        zoomValueLabel.setText(String.format(Locale.US, "%.2f x", saved.zoom()));
        refreshRuntimeInfo();
    }

    private void syncSelection(boolean resetViewport) {
        FractalDefinition definition = fractalSelector.getValue();
        if (definition == null) {
            fractalSelector.getSelectionModel().selectFirst();
            definition = fractalSelector.getValue();
            if (definition == null) {
                return;
            }
        }

        depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                definition.minDepth(),
                definition.maxDepth(),
                Math.max(definition.defaultDepth(), definition.minDepth()),
                1
        ));
        zoomSlider.setMax(Math.max(400, definition.defaultZoom()));
        updatePaletteSectionState(definition);
        if (resetViewport) {
            viewport.resetView();
            zoomSlider.setValue(definition.defaultZoom());
            syncControls();
        }
        refreshRuntimeInfo();
    }

    private void syncControls() {
        FractalDefinition definition = fractalSelector.getValue();
        if (definition == null) {
            return;
        }

        int depth = depthSpinner.getValue();
        double zoom = zoomSlider.getValue() / 100.0;
        depthValueLabel.setText(depth + " 层");
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
        viewSizeValueLabel.setText(String.format(Locale.US, "当前视图：%d x %d (%.2f:1)", width, height, width / (double) height));
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
        applyPaletteSettings(preset.createSettings(), false);
    }

    private void resetPaletteControls() {
        palettePresetSelector.setValue(EscapeTimeColorPreset.CLASSIC);
        applyPaletteSettings(EscapeTimeColorPreset.CLASSIC.createSettings(), false);
    }

    private void applyQuickPaletteControls(boolean clearPresetSelection) {
        if (updatingPaletteControls) {
            return;
        }
        applyPaletteSettings(createQuickPaletteSettings(), clearPresetSelection);
    }

    private void applyPaletteControls(boolean clearPresetSelection) {
        if (updatingPaletteControls) {
            return;
        }
        EscapeTimeColorSettings settings = new EscapeTimeColorSettings(
                (float) hueStartSlider.getValue(),
                (float) hueRangeSlider.getValue(),
                (float) (saturationSlider.getValue() / 100.0),
                (float) (brightnessFloorSlider.getValue() / 100.0),
                (float) (brightnessRangeSlider.getValue() / 100.0),
                toRgb(insideColorPicker.getValue()),
                toRgb(curveColorPicker.getValue()),
                toRgb(backgroundColorPicker.getValue())
        );
        applyPaletteSettings(settings, clearPresetSelection);
    }

    private EscapeTimeColorSettings createQuickPaletteSettings() {
        float contrast = clampUnit((float) (contrastSlider.getValue() / 100.0));
        float exposure = clampUnit((float) (exposureSlider.getValue() / 100.0));
        float brightnessRange = contrast;
        float brightnessFloor = clampUnit(exposure - brightnessRange * 0.5f);
        return new EscapeTimeColorSettings(
                (float) hueStartSlider.getValue(),
                (float) hueRangeSlider.getValue(),
                clampUnit((float) (vibranceSlider.getValue() / 100.0)),
                brightnessFloor,
                brightnessRange,
                toRgb(insideColorPicker.getValue()),
                toRgb(curveColorPicker.getValue()),
                toRgb(backgroundColorPicker.getValue())
        );
    }

    private void applyPaletteSettings(EscapeTimeColorSettings settings, boolean clearPresetSelection) {
        EscapeTimeColorManager.setSettings(settings);
        refreshPaletteControls(settings);
        if (clearPresetSelection) {
            syncPalettePresetSelection(settings);
        }
        if (isPaletteEnabled()) {
            syncControls();
        }
    }

    private void refreshPaletteControlsFromManager() {
        refreshPaletteControls(EscapeTimeColorManager.getSettings());
    }

    private void refreshPaletteControls(EscapeTimeColorSettings settings) {
        updatingPaletteControls = true;
        try {
            hueStartSlider.setValue(settings.hueStartDegrees());
            hueRangeSlider.setValue(settings.hueRangeDegrees());
            saturationSlider.setValue(settings.saturation() * 100.0);
            brightnessFloorSlider.setValue(settings.brightnessFloor() * 100.0);
            brightnessRangeSlider.setValue(settings.brightnessRange() * 100.0);
            contrastSlider.setValue(settings.brightnessRange() * 100.0);
            vibranceSlider.setValue(settings.saturation() * 100.0);
            exposureSlider.setValue(Math.min(100.0, (settings.brightnessFloor() + settings.brightnessRange() * 0.5) * 100.0));
            insideColorPicker.setValue(fromRgb(settings.insideColorRgb()));
            curveColorPicker.setValue(fromRgb(settings.curveColorRgb()));
            backgroundColorPicker.setValue(fromRgb(settings.backgroundColorRgb()));
            updatePaletteMetricLabels();
            refreshGradientPreview(settings);
        } finally {
            updatingPaletteControls = false;
        }
    }

    private void syncPalettePresetSelection(EscapeTimeColorSettings settings) {
        EscapeTimeColorPreset matched = null;
        for (EscapeTimeColorPreset preset : EscapeTimeColorPreset.values()) {
            if (preset.matches(settings)) {
                matched = preset;
                break;
            }
        }
        palettePresetSelector.setValue(matched);
    }

    private void updatePaletteSectionState(FractalDefinition definition) {
        boolean enabled = definition != null;
        palettePresetSelector.setDisable(!enabled);
        contrastSlider.setDisable(!enabled);
        vibranceSlider.setDisable(!enabled);
        exposureSlider.setDisable(!enabled);
        hueStartSlider.setDisable(!enabled);
        hueRangeSlider.setDisable(!enabled);
        saturationSlider.setDisable(!enabled);
        brightnessFloorSlider.setDisable(!enabled);
        brightnessRangeSlider.setDisable(!enabled);
        insideColorPicker.setDisable(!enabled);
        curveColorPicker.setDisable(!enabled);
        backgroundColorPicker.setDisable(!enabled);
        paletteGradientPreview.setDisable(!enabled);
        paletteSwatchPane.setDisable(!enabled);
        resetPaletteButton.setDisable(!enabled);
        paletteHintLabel.setText(enabled
                ? "预设、调色盘、颜色选择和专家模式现在对所有分形类型都生效。"
                : "请先选择一种分形。");
    }

    private boolean isPaletteEnabled() {
        return fractalSelector.getValue() != null;
    }

    private void updatePaletteMetricLabels() {
        contrastValueLabel.setText(String.format(Locale.US, "%.0f%%", contrastSlider.getValue()));
        vibranceValueLabel.setText(String.format(Locale.US, "%.0f%%", vibranceSlider.getValue()));
        exposureValueLabel.setText(String.format(Locale.US, "%.0f%%", exposureSlider.getValue()));
        hueStartValueLabel.setText(String.format(Locale.US, "%.0f°", hueStartSlider.getValue()));
        hueRangeValueLabel.setText(String.format(Locale.US, "%.0f°", hueRangeSlider.getValue()));
        saturationValueLabel.setText(String.format(Locale.US, "%.0f%%", saturationSlider.getValue()));
        brightnessFloorValueLabel.setText(String.format(Locale.US, "%.0f%%", brightnessFloorSlider.getValue()));
        brightnessRangeValueLabel.setText(String.format(Locale.US, "%.0f%%", brightnessRangeSlider.getValue()));
    }

    private void refreshGradientPreview(EscapeTimeColorSettings settings) {
        String style = String.format(Locale.US,
                "-fx-background-color: linear-gradient(from 0%% 0%% to 100%% 0%%, %s 0%%, %s 22%%, %s 48%%, %s 74%%, %s 100%%); -fx-background-radius: 999; -fx-border-color: #dce2eb; -fx-border-radius: 999;",
                toHex(Color.rgb((settings.backgroundColorRgb() >> 16) & 0xFF, (settings.backgroundColorRgb() >> 8) & 0xFF, settings.backgroundColorRgb() & 0xFF)),
                toHex(computeGradientColor(settings, 0.15)),
                toHex(computeGradientColor(settings, 0.45)),
                toHex(computeGradientColor(settings, 0.75)),
                toHex(Color.rgb((settings.insideColorRgb() >> 16) & 0xFF, (settings.insideColorRgb() >> 8) & 0xFF, settings.insideColorRgb() & 0xFF))
        );
        paletteGradientPreview.setStyle(style);
    }

    private Color computeGradientColor(EscapeTimeColorSettings settings, double progress) {
        double hue = settings.hueStartDegrees() - settings.hueRangeDegrees() * progress;
        double brightness = clampUnit((float) (settings.brightnessFloor() + settings.brightnessRange() * progress));
        return Color.hsb(wrapHueDegrees(hue), settings.saturation(), brightness);
    }

    private String toHex(Color color) {
        return String.format(Locale.US, "#%02X%02X%02X",
                (int) Math.round(color.getRed() * 255.0),
                (int) Math.round(color.getGreen() * 255.0),
                (int) Math.round(color.getBlue() * 255.0));
    }

    private double wrapHueDegrees(double hue) {
        double wrapped = hue % 360.0;
        return wrapped < 0.0 ? wrapped + 360.0 : wrapped;
    }

    private float clampUnit(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
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
        backendPillLabel.setText("渲染后端：" + backendText);
        if (rendering) {
            renderPillLabel.setText("渲染中...");
            renderPillLabel.setStyle(pillStyle("#e3edff", "#1f57c3"));
        } else {
            renderPillLabel.setText("最近渲染：" + viewport.getLastRenderDurationMillis() + " ms");
            renderPillLabel.setStyle(pillStyle("#e3f7ef", "#148c5c"));
        }
    }

    private String buildStatusText(double zoom, double offsetX, double offsetY, double mouseX, double mouseY) {
        String mouse = mouseX >= 0 && mouseY >= 0
                ? String.format(Locale.US, "%.0f, %.0f", mouseX, mouseY)
                : "-, -";
        String complex = buildComplexCoordinateText(mouseX, mouseY, zoom, offsetX, offsetY);
        return String.format(Locale.US,
                "光标：%s%s | 缩放：%.2fx | 偏移：(%.0f, %.0f) | 视图：%d x %d | 后端：%s",
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
        return String.format(Locale.US, " | 复平面：%.6f %+.6fi", real, imaginary);
    }

    private void persistCurrentConfiguration(boolean showFeedback) {
        FractalDefinition definition = fractalSelector.getValue();
        if (definition == null) {
            return;
        }
        FractalPreferences.SavedConfiguration savedConfiguration = new FractalPreferences.SavedConfiguration(
                definition.name(),
                depthSpinner.getValue(),
                viewport.getCurrentZoom(),
                viewport.getCurrentOffsetX(),
                viewport.getCurrentOffsetY(),
                viewport.getViewportWidth(),
                viewport.getViewportHeight(),
                EscapeTimeColorManager.getSettings(),
                sidebarVisible,
                stage.getWidth(),
                stage.getHeight(),
                stage.isMaximized()
        );
        preferences.save(savedConfiguration);
        if (showFeedback) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "当前配置已保存，下次启动会自动恢复。", ButtonType.OK);
            alert.initOwner(stage);
            alert.setTitle("保存配置");
            alert.setHeaderText("保存配置");
            alert.showAndWait();
        }
    }

    private void showCurrentFractalInfo() {
        FractalDefinition definition = fractalSelector.getValue();
        if (definition == null) {
            return;
        }
        String content = String.format(Locale.US,
                "名称：%s%n分类：%s%n说明：%s%n层级范围：%d - %d%n默认层级：%d",
                definition.name(),
                definition.category(),
                definition.description(),
                definition.minDepth(),
                definition.maxDepth(),
                definition.defaultDepth());
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.initOwner(stage);
        alert.setTitle("当前分形说明");
        alert.setHeaderText(definition.name());
        alert.showAndWait();
    }

    private void showShortcutInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "滚轮缩放\n拖拽平移\nShift + 拖拽框选缩放\n双击重置视图\n右键导出",
                ButtonType.OK);
        alert.initOwner(stage);
        alert.setTitle("快捷操作");
        alert.setHeaderText("快捷操作");
        alert.showAndWait();
    }

    private List<Button> buildPaletteSwatches() {
        return Arrays.asList(
                createSwatchButton("\u6df1\u591c\u84dd", 0x050812),
                createSwatchButton("\u6696\u767d", 0xFAFAFA),
                createSwatchButton("\u70ad\u9ed1", 0x040404),
                createSwatchButton("\u6d77\u6d0b\u84dd", 0x0D3B66),
                createSwatchButton("\u73ca\u745a\u6a59", 0xF25F5C),
                createSwatchButton("\u677e\u77f3\u7eff", 0x2EC4B6),
                createSwatchButton("\u7425\u73c0\u9ec4", 0xFFBF69),
                createSwatchButton("\u85b0\u8863\u8349\u7070", 0x6C7A89)
        );
    }

    private Button createSwatchButton(String label, int rgb) {
        Button swatch = new Button();
        swatch.setMinSize(28, 28);
        swatch.setPrefSize(28, 28);
        swatch.setMaxSize(28, 28);
        swatch.setTooltip(new Tooltip(label));
        swatch.setStyle("-fx-background-color: #" + String.format("%06X", rgb) + "; -fx-background-radius: 999; -fx-border-color: #dce2eb; -fx-border-radius: 999;");
        swatch.setOnAction(event -> applyPaletteSwatch(rgb));
        return swatch;
    }

    private void applyPaletteSwatch(int rgb) {
        Color swatchColor = fromRgb(rgb);
        curveColorPicker.setValue(swatchColor);
        if (isPaletteEnabled()) {
            hueStartSlider.setValue(swatchColor.getHue());
            saturationSlider.setValue(swatchColor.getSaturation() * 100.0);
            double brightness = swatchColor.getBrightness() * 100.0;
            brightnessFloorSlider.setValue(Math.max(8.0, Math.min(brightnessFloorSlider.getValue(), brightness)));
            brightnessRangeSlider.setValue(Math.max(18.0, brightnessRangeSlider.getValue()));
        }
        applyPaletteControls(true);
    }

    private FractalDefinition findDefinition(String name) {
        if (name != null) {
            for (FractalDefinition definition : fractals) {
                if (definition.name().equals(name)) {
                    return definition;
                }
            }
        }
        return fractals.get(0);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private VBox createCardBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
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

    private Label createValueBadge() {
        Label label = new Label();
        label.setStyle("-fx-background-color: #f4f7fb; -fx-text-fill: #5e687a; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999; -fx-padding: 4 8 4 8;");
        return label;
    }

    private void styleSecondaryButton(Button button) {
        button.setStyle("-fx-background-color: white; -fx-text-fill: #181e2a; -fx-border-color: #dce2eb; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 9 12 9 12;");
        button.setMaxWidth(Double.MAX_VALUE);
    }

    private void configureSlider(Slider slider, double majorTickUnit, int minorTickCount) {
        slider.setMaxWidth(Double.MAX_VALUE);
        slider.setBlockIncrement(1);
        slider.setMajorTickUnit(majorTickUnit);
        slider.setMinorTickCount(minorTickCount);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
    }

    private Region createSeparatorRegion(boolean horizontal) {
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #dce2eb;");
        if (horizontal) {
            separator.setMinHeight(1);
            separator.setPrefHeight(1);
            separator.setMaxHeight(1);
        } else {
            separator.setMinWidth(1);
            separator.setPrefWidth(1);
            separator.setMaxWidth(1);
        }
        return separator;
    }

    private String cardStyle() {
        return "-fx-background-color: white;";
    }

    private String pillStyle(String background, String foreground) {
        return "-fx-background-color: " + background + "; -fx-text-fill: " + foreground + "; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999; -fx-padding: 6 10 6 10;";
    }

    private void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        applySidebarVisibility();
    }

    private void updateResponsiveSidebar(double stageWidth) {
        sidebarAutoCollapsed = stageWidth > 0.0 && stageWidth < SIDEBAR_AUTO_HIDE_THRESHOLD;
        applySidebarVisibility();
    }

    private void applySidebarVisibility() {
        root.setLeft(sidebarVisible && !sidebarAutoCollapsed ? sidebar : null);
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
                new ViewSizePreset("正方形 1024 x 1024", 1024, 1024, false),
                new ViewSizePreset("竖版 1080 x 1350", 1080, 1350, false),
                new ViewSizePreset("全高清 1920 x 1080", 1920, 1080, false),
                new ViewSizePreset("4K UHD 3840 x 2160", 3840, 2160, false),
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
            return new ViewSizePreset("自定义", width, height, true);
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
            return custom ? String.format(Locale.US, "自定义 (%d x %d)", width, height) : label;
        }
    }
}
