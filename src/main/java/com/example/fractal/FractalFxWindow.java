package com.example.fractal;

import com.example.fractal.model.FractalDefinition;
import com.example.fractal.model.FractalRegistry;
import com.example.fractal.custom.ComplexFormulaParser;
import com.example.fractal.custom.CurveContourExtractor;
import com.example.fractal.custom.CustomCurveSettings;
import com.example.fractal.custom.CustomFormulaLibrary;
import com.example.fractal.custom.CustomFormulaMode;
import com.example.fractal.custom.CustomFormulaSettings;
import com.example.fractal.custom.CustomFractalManager;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
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
    private final Label palettePresetSectionLabel;
    private final Label palettePreviewSectionLabel;
    private final Label paletteComboSectionLabel;
    private final Label insideColorSectionLabel;
    private final Label curveColorSectionLabel;
    private final Label backgroundColorSectionLabel;
    private final Label paletteSwatchSectionLabel;
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
    private final FlowPane paletteComboPane;
    private final FlowPane paletteSwatchPane;
    private final ComboBox<CustomFormulaMode> customFormulaModeSelector;
    private final ComboBox<CustomFormulaLibrary.Group> customFormulaGroupSelector;
    private final ComboBox<CustomFormulaLibrary.Template> customFormulaTemplateSelector;
    private final TextArea customFormulaTextArea;
    private final TextField customJuliaRealField;
    private final TextField customJuliaImaginaryField;
    private final Button applyCustomFormulaButton;
    private final Button randomCustomFormulaButton;
    private final Button tryTenCustomFormulasButton;
    private final Button importCustomCurveButton;
    private final Slider customCurveThresholdSlider;
    private final Slider customCurveSimplifySlider;
    private final Slider customCurveBranchCountSlider;
    private final Slider customCurveChildScaleSlider;
    private final Label customCurveThresholdValueLabel;
    private final Label customCurveSimplifyValueLabel;
    private final Label customCurveBranchCountValueLabel;
    private final Label customCurveChildScaleValueLabel;
    private final Label customFormulaStatusLabel;
    private final Label customCurveStatusLabel;
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
        this.palettePresetSectionLabel = new Label();
        this.palettePreviewSectionLabel = new Label();
        this.paletteComboSectionLabel = new Label();
        this.insideColorSectionLabel = new Label();
        this.curveColorSectionLabel = new Label();
        this.backgroundColorSectionLabel = new Label();
        this.paletteSwatchSectionLabel = new Label();
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
        this.paletteComboPane = new FlowPane();
        this.paletteSwatchPane = new FlowPane();
        this.customFormulaModeSelector = new ComboBox<CustomFormulaMode>(FXCollections.observableArrayList(CustomFormulaMode.values()));
        this.customFormulaGroupSelector = new ComboBox<CustomFormulaLibrary.Group>(FXCollections.observableArrayList(CustomFormulaLibrary.Group.values()));
        this.customFormulaTemplateSelector = new ComboBox<CustomFormulaLibrary.Template>(FXCollections.observableArrayList(CustomFormulaLibrary.templates()));
        this.customFormulaTextArea = new TextArea();
        this.customJuliaRealField = new TextField();
        this.customJuliaImaginaryField = new TextField();
        this.applyCustomFormulaButton = new Button("Apply Formula");
        this.randomCustomFormulaButton = new Button("Random Formula");
        this.tryTenCustomFormulasButton = new Button("Try 10");
        this.importCustomCurveButton = new Button("Import Curve Image");
        this.customCurveThresholdSlider = new Slider(0, 255, 160);
        this.customCurveSimplifySlider = new Slider(1, 24, 6);
        this.customCurveBranchCountSlider = new Slider(2, 8, 4);
        this.customCurveChildScaleSlider = new Slider(0.2, 0.8, 0.52);
        this.customCurveThresholdValueLabel = createValueBadge();
        this.customCurveSimplifyValueLabel = createValueBadge();
        this.customCurveBranchCountValueLabel = createValueBadge();
        this.customCurveChildScaleValueLabel = createValueBadge();
        this.customFormulaStatusLabel = new Label();
        this.customCurveStatusLabel = new Label();

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
        styleSectionLabel(palettePresetSectionLabel);
        styleSectionLabel(palettePreviewSectionLabel);
        styleSectionLabel(paletteComboSectionLabel);
        styleSectionLabel(insideColorSectionLabel);
        styleSectionLabel(curveColorSectionLabel);
        styleSectionLabel(backgroundColorSectionLabel);
        styleSectionLabel(paletteSwatchSectionLabel);
        viewSizeValueLabel.setStyle("-fx-text-fill: #181e2a; -fx-font-size: 12px; -fx-font-weight: 700;");

        fractalSelector.setMaxWidth(Double.MAX_VALUE);
        fractalSelector.setVisibleRowCount(8);
        depthSpinner.setEditable(true);
        depthSpinner.setMaxWidth(Double.MAX_VALUE);
        styleSpinner(depthSpinner);
        zoomSlider.setMaxWidth(Double.MAX_VALUE);
        zoomSlider.setBlockIncrement(10);
        zoomSlider.setMajorTickUnit(100);
        viewSizePresetSelector.setMaxWidth(Double.MAX_VALUE);
        viewSizePresetSelector.setPromptText("自定义");
        viewportWidthSpinner.setEditable(true);
        viewportHeightSpinner.setEditable(true);
        viewportWidthSpinner.setMaxWidth(Double.MAX_VALUE);
        viewportHeightSpinner.setMaxWidth(Double.MAX_VALUE);
        styleSpinner(viewportWidthSpinner);
        styleSpinner(viewportHeightSpinner);
        palettePresetSelector.setMaxWidth(Double.MAX_VALUE);
        palettePresetSelector.setPromptText("自定义");
        insideColorPicker.setMaxWidth(Double.MAX_VALUE);
        curveColorPicker.setMaxWidth(Double.MAX_VALUE);
        backgroundColorPicker.setMaxWidth(Double.MAX_VALUE);
        styleColorPicker(insideColorPicker);
        styleColorPicker(curveColorPicker);
        styleColorPicker(backgroundColorPicker);
        customFormulaModeSelector.setMaxWidth(Double.MAX_VALUE);
        customFormulaModeSelector.setValue(CustomFractalManager.getFormulaSettings().mode());
        customFormulaGroupSelector.setMaxWidth(Double.MAX_VALUE);
        customFormulaGroupSelector.setValue(CustomFormulaLibrary.Group.STABLE);
        customFormulaTemplateSelector.setMaxWidth(Double.MAX_VALUE);
        customFormulaTemplateSelector.setPromptText("Formula Templates");
        refreshCustomFormulaTemplateChoices();
        if (!customFormulaTemplateSelector.getItems().isEmpty()) {
            customFormulaTemplateSelector.setValue(customFormulaTemplateSelector.getItems().get(0));
        }
        customFormulaTextArea.setPrefRowCount(3);
        customFormulaTextArea.setWrapText(true);
        customFormulaTextArea.setText(CustomFractalManager.getFormulaSettings().formulaText());
        customJuliaRealField.setText(String.format(Locale.US, "%.4f", CustomFractalManager.getFormulaSettings().juliaReal()));
        customJuliaImaginaryField.setText(String.format(Locale.US, "%.4f", CustomFractalManager.getFormulaSettings().juliaImaginary()));
        customFormulaStatusLabel.setWrapText(true);
        customFormulaStatusLabel.setStyle("-fx-text-fill: #5e687a; -fx-font-size: 11px;");
        customCurveStatusLabel.setWrapText(true);
        customCurveStatusLabel.setStyle("-fx-text-fill: #5e687a; -fx-font-size: 11px;");
        styleSecondaryButton(applyCustomFormulaButton);
        styleSecondaryButton(randomCustomFormulaButton);
        styleSecondaryButton(importCustomCurveButton);

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

        paletteComboPane.setHgap(6);
        paletteComboPane.setVgap(6);
        paletteComboPane.setPrefWrapLength(1000);
        paletteComboPane.setMaxWidth(Double.MAX_VALUE);
        paletteComboPane.getChildren().setAll(buildPaletteCombos());

        configureSlider(customCurveThresholdSlider, 64, 7);
        configureSlider(customCurveSimplifySlider, 6, 5);
        configureSlider(customCurveBranchCountSlider, 1, 0);
        configureSlider(customCurveChildScaleSlider, 0.1, 4);
        customCurveThresholdSlider.setSnapToTicks(true);
        customCurveSimplifySlider.setSnapToTicks(true);
        customCurveBranchCountSlider.setSnapToTicks(true);
        updateCustomControlLabels();
        refreshCustomStatusLabels();
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
        applyCustomFormulaButton.setOnAction(event -> applyCustomFormula());
        randomCustomFormulaButton.setOnAction(event -> applyRandomCustomFormula());
        tryTenCustomFormulasButton.setOnAction(event -> tryTenCustomFormulas());
        importCustomCurveButton.setOnAction(event -> importCustomCurve());
        customFormulaModeSelector.setOnAction(event -> applyCustomFormula());
        customFormulaGroupSelector.setOnAction(event -> refreshCustomFormulaTemplateChoices());
        customFormulaTemplateSelector.setOnAction(event -> applySelectedCustomFormulaTemplate());
        customCurveThresholdSlider.valueProperty().addListener((obs, oldValue, newValue) -> updateCustomCurveExtraction(true));
        customCurveSimplifySlider.valueProperty().addListener((obs, oldValue, newValue) -> updateCustomCurveExtraction(true));
        customCurveBranchCountSlider.valueProperty().addListener((obs, oldValue, newValue) -> updateCustomCurveGeometry());
        customCurveChildScaleSlider.valueProperty().addListener((obs, oldValue, newValue) -> updateCustomCurveGeometry());

        viewport.setInteractionListener(new FractalFxViewport.InteractionListener() {
            @Override
            public void onZoomChanged(double zoom, double offsetX, double offsetY, boolean fromWheelAnchor) {
                if (fromWheelAnchor) {
                    setZoomSliderValue(zoom);
                }
                updateDepthSpinnerRange(fractalSelector.getValue(), zoom);
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
                createCustomCard(),
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

    private VBox createCustomCard() {
        HBox juliaRow = new HBox(8,
                new VBox(4, createSectionLabel("Julia c (Re)"), customJuliaRealField),
                new VBox(4, createSectionLabel("Julia c (Im)"), customJuliaImaginaryField)
        );
        HBox.setHgrow(juliaRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(juliaRow.getChildren().get(1), Priority.ALWAYS);
        HBox formulaActionRow = new HBox(8, randomCustomFormulaButton, tryTenCustomFormulasButton, applyCustomFormulaButton);
        HBox.setHgrow(randomCustomFormulaButton, Priority.ALWAYS);
        HBox.setHgrow(applyCustomFormulaButton, Priority.ALWAYS);
        TitledPane formulaPane = new TitledPane("Custom Formula", new VBox(10,
                createSectionLabel("Template Group"),
                customFormulaGroupSelector,
                createSectionLabel("Template Library"),
                customFormulaTemplateSelector,
                createSectionLabel("Mode"),
                customFormulaModeSelector,
                createSectionLabel("Formula"),
                customFormulaTextArea,
                juliaRow,
                formulaActionRow,
                customFormulaStatusLabel,
                wrapLabel("Includes 24 built-in formulas across Stable, Bold, Floral, and Spiral groups. Random uses the current group. Use z and c. Supported operators: + - * / ^. Supported functions: sin, cos, tan, exp, log, abs.")
        ));
        formulaPane.setExpanded(true);
        formulaPane.setCollapsible(false);

        TitledPane curvePane = new TitledPane("Custom Curve", new VBox(10,
                importCustomCurveButton,
                createSliderRow("Threshold", customCurveThresholdSlider, customCurveThresholdValueLabel),
                createSliderRow("Simplify", customCurveSimplifySlider, customCurveSimplifyValueLabel),
                createSliderRow("Branches", customCurveBranchCountSlider, customCurveBranchCountValueLabel),
                createSliderRow("Child Scale", customCurveChildScaleSlider, customCurveChildScaleValueLabel),
                customCurveStatusLabel,
                wrapLabel("Import a high-contrast image. The app extracts the largest contour and recurses it into a pattern.")
        ));
        curvePane.setExpanded(true);
        curvePane.setCollapsible(false);

        VBox box = createCardBox();
        box.getChildren().addAll(
                createCardTitle("Custom"),
                formulaPane,
                curvePane
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
                palettePresetSectionLabel,
                presetRow,
                palettePreviewSectionLabel,
                paletteGradientPreview,
                createSliderRow("对比度", contrastSlider, contrastValueLabel),
                createSliderRow("鲜艳度", vibranceSlider, vibranceValueLabel),
                createSliderRow("明暗", exposureSlider, exposureValueLabel),
                createInlineColorPickerRow(insideColorSectionLabel, insideColorPicker),
                createInlineColorPickerRow(curveColorSectionLabel, curveColorPicker),
                createInlineColorPickerRow(backgroundColorSectionLabel, backgroundColorPicker),
                paletteComboSectionLabel,
                paletteComboPane,
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

    private HBox createInlineColorPickerRow(Label titleLabel, ColorPicker picker) {
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

        setZoomSliderValue(saved.zoom());
        updateDepthSpinnerRange(definition, saved.zoom());
        int depth = clamp(saved.depth(), definition.minDepth(), currentDepthMax(definition, saved.zoom()));
        depthSpinner.getValueFactory().setValue(depth);
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

        updateDepthSpinnerRange(definition, viewport.getCurrentZoom());
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

    private void updateDepthSpinnerRange(FractalDefinition definition, double zoom) {
        if (definition == null) {
            return;
        }
        int currentValue = depthSpinner.getValueFactory() != null ? depthSpinner.getValue() : definition.defaultDepth();
        int maxDepth = currentDepthMax(definition, zoom);
        depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                definition.minDepth(),
                maxDepth,
                clamp(currentValue, definition.minDepth(), maxDepth),
                1
        ));
    }

    private int currentDepthMax(FractalDefinition definition, double zoom) {
        if (definition == null) {
            return 1;
        }
        if (definition.renderer() instanceof AbstractEscapeTimeRenderer) {
            return definition.maxDepth();
        }
        double normalizedZoom = Math.max(1.0, zoom);
        int bonus = (int) Math.floor(Math.log(normalizedZoom) / Math.log(1.8));
        return clamp(definition.maxDepth() + Math.max(0, bonus), definition.minDepth(), definition.maxDepth() + 8);
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

    private void refreshCustomFormulaTemplateChoices() {
        CustomFormulaLibrary.Group group = customFormulaGroupSelector.getValue();
        if (group == null) {
            group = CustomFormulaLibrary.Group.STABLE;
            customFormulaGroupSelector.setValue(group);
        }
        customFormulaTemplateSelector.getItems().setAll(CustomFormulaLibrary.templates(group));
        if (!customFormulaTemplateSelector.getItems().isEmpty()) {
            customFormulaTemplateSelector.setValue(customFormulaTemplateSelector.getItems().get(0));
        }
    }
    private void applySelectedCustomFormulaTemplate() {
        CustomFormulaLibrary.Template template = customFormulaTemplateSelector.getValue();
        if (template == null) {
            return;
        }
        loadCustomFormulaTemplate(template);
    }

    private void tryTenCustomFormulas() {
        for (int i = 0; i < 10; i++) {
            applyRandomCustomFormula();
        }
        customFormulaStatusLabel.setText("Tried 10 formulas | " + customFormulaStatusLabel.getText());
    }

    private void applyRandomCustomFormula() {
        CustomFormulaLibrary.Group group = customFormulaGroupSelector.getValue() == null ? CustomFormulaLibrary.Group.STABLE : customFormulaGroupSelector.getValue();
        CustomFormulaLibrary.Template template = CustomFormulaLibrary.randomTemplate(group);
        customFormulaTemplateSelector.setValue(template);
        loadCustomFormulaTemplate(template);
    }

    private void loadCustomFormulaTemplate(CustomFormulaLibrary.Template template) {
        customFormulaModeSelector.setValue(template.mode());
        customFormulaTextArea.setText(template.formula());
        customJuliaRealField.setText(String.format(Locale.US, "%.5f", template.juliaReal()));
        customJuliaImaginaryField.setText(String.format(Locale.US, "%.5f", template.juliaImaginary()));
        applyCustomFormula();
        customFormulaStatusLabel.setText(template.name() + " | " + customFormulaStatusLabel.getText());
    }
    private void applyCustomFormula() {
        double juliaReal;
        double juliaImaginary;
        try {
            juliaReal = Double.parseDouble(customJuliaRealField.getText().trim());
            juliaImaginary = Double.parseDouble(customJuliaImaginaryField.getText().trim());
        } catch (NumberFormatException ex) {
            customFormulaStatusLabel.setText("Julia constant must be numeric.");
            return;
        }
        CustomFormulaSettings settings = ComplexFormulaParser.compile(
                customFormulaTextArea.getText(),
                customFormulaModeSelector.getValue() == null ? CustomFormulaMode.MANDELBROT_LIKE : customFormulaModeSelector.getValue(),
                juliaReal,
                juliaImaginary
        );
        CustomFractalManager.setFormulaSettings(settings);
        refreshCustomStatusLabels();
        if (isCustomFormulaDefinition(fractalSelector.getValue())) {
            syncControls();
        }
    }

    private void importCustomCurve() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Curve Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        CustomCurveSettings settings = CurveContourExtractor.extract(
                file,
                (int) Math.round(customCurveThresholdSlider.getValue()),
                customCurveSimplifySlider.getValue(),
                (int) Math.round(customCurveBranchCountSlider.getValue()),
                customCurveChildScaleSlider.getValue()
        );
        CustomFractalManager.setCurveSettings(settings);
        refreshCustomStatusLabels();
        if (isCustomCurveDefinition(fractalSelector.getValue())) {
            syncControls();
        }
    }

    private void updateCustomCurveExtraction(boolean reExtract) {
        updateCustomControlLabels();
        CustomCurveSettings current = CustomFractalManager.getCurveSettings();
        if (!reExtract || current.imagePath() == null || current.imagePath().isBlank()) {
            return;
        }
        CustomCurveSettings settings = CurveContourExtractor.extract(
                new File(current.imagePath()),
                (int) Math.round(customCurveThresholdSlider.getValue()),
                customCurveSimplifySlider.getValue(),
                (int) Math.round(customCurveBranchCountSlider.getValue()),
                customCurveChildScaleSlider.getValue()
        );
        CustomFractalManager.setCurveSettings(settings);
        refreshCustomStatusLabels();
        if (isCustomCurveDefinition(fractalSelector.getValue())) {
            syncControls();
        }
    }

    private void updateCustomCurveGeometry() {
        updateCustomControlLabels();
        CustomCurveSettings current = CustomFractalManager.getCurveSettings();
        CustomCurveSettings updated = new CustomCurveSettings(
                current.imagePath(),
                (int) Math.round(customCurveThresholdSlider.getValue()),
                customCurveSimplifySlider.getValue(),
                (int) Math.round(customCurveBranchCountSlider.getValue()),
                customCurveChildScaleSlider.getValue(),
                current.contour(),
                current.validationMessage()
        );
        CustomFractalManager.setCurveSettings(updated);
        refreshCustomStatusLabels();
        if (isCustomCurveDefinition(fractalSelector.getValue()) && current.hasContour()) {
            syncControls();
        }
    }

    private void updateCustomControlLabels() {
        customCurveThresholdValueLabel.setText(Integer.toString((int) Math.round(customCurveThresholdSlider.getValue())));
        customCurveSimplifyValueLabel.setText(String.format(Locale.US, "%.1f px", customCurveSimplifySlider.getValue()));
        customCurveBranchCountValueLabel.setText(Integer.toString((int) Math.round(customCurveBranchCountSlider.getValue())));
        customCurveChildScaleValueLabel.setText(String.format(Locale.US, "%.2f", customCurveChildScaleSlider.getValue()));
    }

    private void refreshCustomStatusLabels() {
        CustomFormulaSettings formulaSettings = CustomFractalManager.getFormulaSettings();
        customFormulaStatusLabel.setText(formulaSettings.validationMessage() == null
                ? "Ready: " + formulaSettings.expression().normalizedFormula()
                : formulaSettings.validationMessage());
        CustomCurveSettings curveSettings = CustomFractalManager.getCurveSettings();
        String curveText = curveSettings.validationMessage();
        if (curveSettings.hasContour()) {
            String sourceName = curveSettings.imagePath() == null ? "curve" : new File(curveSettings.imagePath()).getName();
            curveText = String.format(Locale.US, "%s, %d points", sourceName, curveSettings.contour().size());
        }
        customCurveStatusLabel.setText(curveText == null ? "Import an image to extract a curve." : curveText);
    }

    private void updateCustomSectionState(FractalDefinition definition) {
        boolean formulaActive = isCustomFormulaDefinition(definition);
        boolean curveActive = isCustomCurveDefinition(definition);
        customFormulaModeSelector.setDisable(!formulaActive);
        customFormulaGroupSelector.setDisable(!formulaActive);
        customFormulaTemplateSelector.setDisable(!formulaActive);
        customFormulaTextArea.setDisable(!formulaActive);
        customJuliaRealField.setDisable(!formulaActive);
        customJuliaImaginaryField.setDisable(!formulaActive);
        applyCustomFormulaButton.setDisable(!formulaActive);
        randomCustomFormulaButton.setDisable(!formulaActive);
        tryTenCustomFormulasButton.setDisable(!formulaActive);
        importCustomCurveButton.setDisable(!curveActive);
        customCurveThresholdSlider.setDisable(!curveActive);
        customCurveSimplifySlider.setDisable(!curveActive);
        customCurveBranchCountSlider.setDisable(!curveActive);
        customCurveChildScaleSlider.setDisable(!curveActive);
    }

    private boolean isCustomFormulaDefinition(FractalDefinition definition) {
        return definition != null && "Custom Formula".equals(definition.name());
    }

    private boolean isCustomCurveDefinition(FractalDefinition definition) {
        return definition != null && "Custom Curve".equals(definition.name());
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
        boolean escapeTime = isEscapeTimeDefinition(definition);
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
        paletteComboPane.setDisable(!enabled);
        resetPaletteButton.setDisable(!enabled);

        palettePresetSectionLabel.setText("风格预设");
        palettePreviewSectionLabel.setText("渐变预览");
        paletteComboSectionLabel.setText("推荐组合");
        backgroundColorSectionLabel.setText("背景颜色");
        if (escapeTime) {
            insideColorSectionLabel.setText("内部颜色");
            curveColorSectionLabel.setText("曲线颜色");
            paletteSwatchSectionLabel.setText("主渐变调色盘");
            paletteHintLabel.setText(enabled
                    ? "调色盘用于快速设定主渐变 / 曲线主色，不会覆盖内部颜色。"
                    : "请先选择一种分形。");
        } else {
            insideColorSectionLabel.setText("辅助颜色");
            curveColorSectionLabel.setText("主图形颜色");
            paletteSwatchSectionLabel.setText("主图形调色盘");
            paletteHintLabel.setText(enabled
                    ? "调色盘用于快速设定主图形颜色，推荐组合会一次性协同调整三个颜色槽。"
                    : "请先选择一种分形。");
        }
    }

    private boolean isEscapeTimeDefinition(FractalDefinition definition) {
        return definition != null && definition.renderer() instanceof AbstractEscapeTimeRenderer;
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

    private List<Button> buildPaletteCombos() {
        return Arrays.asList(
                createPaletteComboButton("夜幕青", 0x050812, 0x3BC9DB, 0x081826),
                createPaletteComboButton("纸白黑", 0xF6F4EF, 0x2E3138, 0xFCFBF7),
                createPaletteComboButton("熔岩橙", 0x170605, 0xF97316, 0x2B0E0B),
                createPaletteComboButton("苔藓绿", 0x0E1511, 0x4FAF66, 0x16231B),
                createPaletteComboButton("薄雾紫", 0x17131F, 0xA78BFA, 0x221C30)
        );
    }

    private Button createPaletteComboButton(String label, int insideRgb, int curveRgb, int backgroundRgb) {
        Button button = new Button(label);
        button.setStyle("-fx-background-color: white; -fx-text-fill: #181e2a; -fx-border-color: #dce2eb; -fx-border-radius: 999; -fx-background-radius: 999; -fx-padding: 6 10 6 10;");
        button.setOnAction(event -> applyPaletteCombo(insideRgb, curveRgb, backgroundRgb));
        return button;
    }

    private void applyPaletteCombo(int insideRgb, int curveRgb, int backgroundRgb) {
        insideColorPicker.setValue(fromRgb(insideRgb));
        curveColorPicker.setValue(fromRgb(curveRgb));
        backgroundColorPicker.setValue(fromRgb(backgroundRgb));
        Color swatchColor = fromRgb(curveRgb);
        hueStartSlider.setValue(swatchColor.getHue());
        saturationSlider.setValue(swatchColor.getSaturation() * 100.0);
        brightnessFloorSlider.setValue(Math.max(8.0, Math.min(55.0, swatchColor.getBrightness() * 45.0)));
        brightnessRangeSlider.setValue(Math.max(22.0, swatchColor.getBrightness() * 70.0));
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
        styleSectionLabel(label);
        return label;
    }

    private void styleSectionLabel(Label label) {
        label.setStyle("-fx-text-fill: #181e2a; -fx-font-size: 12px; -fx-font-weight: 700;");
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

    private void styleSpinner(Spinner<Integer> spinner) {
        spinner.setStyle("-fx-background-color: white; -fx-border-color: #c8d0dc; -fx-border-radius: 8; -fx-background-radius: 8; -fx-focus-color: #c8d0dc; -fx-faint-focus-color: transparent;");
        spinner.getEditor().setStyle("-fx-background-color: white; -fx-border-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-text-fill: #181e2a;");
    }

    private void styleColorPicker(ColorPicker picker) {
        picker.setStyle("-fx-background-color: white; -fx-border-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
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
