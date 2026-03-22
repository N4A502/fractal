package com.example.fractal;

import com.example.fractal.model.FractalDefinition;
import com.example.fractal.model.FractalRegistry;
import com.example.fractal.render.AbstractEscapeTimeRenderer;
import com.example.fractal.render.EscapeTimeBackendSelector;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.List;
import java.util.Locale;

public class FractalFxWindow {

    private final Stage stage;
    private final FractalFxViewport viewport;
    private final ComboBox<FractalDefinition> fractalSelector;
    private final Spinner<Integer> depthSpinner;
    private final Slider zoomSlider;
    private final Label summaryTitleLabel;
    private final Label summarySubtitleLabel;
    private final Label categoryValueLabel;
    private final Label descriptionValueLabel;
    private final Label depthValueLabel;
    private final Label zoomValueLabel;
    private final Label backendPillLabel;
    private final Label renderPillLabel;
    private final Label statusLabel;
    private boolean updatingZoomSlider;

    public FractalFxWindow(Stage stage) {
        this.stage = stage;
        List<FractalDefinition> fractals = FractalRegistry.createDefinitions();
        this.viewport = new FractalFxViewport();
        this.fractalSelector = new ComboBox<FractalDefinition>(FXCollections.observableArrayList(fractals));
        this.depthSpinner = new Spinner<Integer>();
        this.zoomSlider = new Slider(10, 400, 100);
        this.summaryTitleLabel = new Label("Fractal Explorer");
        this.summarySubtitleLabel = new Label("Pure JavaFX shell on top of the existing Java render pipeline.");
        this.categoryValueLabel = new Label();
        this.descriptionValueLabel = new Label();
        this.depthValueLabel = new Label();
        this.zoomValueLabel = new Label();
        this.backendPillLabel = createPillLabel();
        this.renderPillLabel = createPillLabel();
        this.statusLabel = new Label("Cursor: -, - | Zoom: 1.00x | Offset: (0, 0)");

        configureStage();
        configureControls();
        configureInteractions();
        syncSelection();
    }

    public void show() {
        stage.show();
    }

    private void configureStage() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #eef2f8;");
        root.setPadding(new Insets(18));
        root.setTop(buildTopBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildViewportShell());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1380, 880, Color.web("#eef2f8"));
        stage.setTitle("Fractal Explorer");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(780);
        stage.setOnCloseRequest(event -> {
            viewport.shutdown();
            EscapeTimeBackendSelector.shutdown();
            Platform.exit();
        });
    }

    private void configureControls() {
        summaryTitleLabel.setFont(Font.font("System", 26));
        summaryTitleLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #181e2a;");
        summarySubtitleLabel.setStyle("-fx-text-fill: #5e687a; -fx-font-size: 14px;");
        statusLabel.setStyle("-fx-text-fill: #343d4e; -fx-font-size: 13px;");
        descriptionValueLabel.setWrapText(true);
        categoryValueLabel.setWrapText(true);
        depthSpinner.setEditable(true);
        zoomSlider.setBlockIncrement(10);
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

    private VBox buildTopBar() {
        VBox wrapper = new VBox();
        wrapper.setPadding(new Insets(0, 0, 18, 0));

        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 18, 16, 18));
        bar.setStyle(cardStyle());

        VBox titles = new VBox(6, summaryTitleLabel, summarySubtitleLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox pills = new HBox(10,
                backendPillLabel,
                renderPillLabel,
                createPrimaryButton("Export PNG", event -> viewport.exportHighResolutionView(stage)),
                createSecondaryButton("Reset View", event -> resetControls())
        );
        pills.setAlignment(Pos.CENTER_RIGHT);

        bar.getChildren().addAll(titles, spacer, pills);
        wrapper.getChildren().add(bar);
        return wrapper;
    }

    private ScrollPane buildSidebar() {
        VBox sidebar = new VBox(14,
                createHeroCard(),
                createControlCard(),
                createExportCard(),
                createInfoCard("Navigation", "Drag to pan, use the mouse wheel to zoom, Shift-drag to box zoom, and double-click to reset the view."),
                createInfoCard("Render pipeline", "Escape-time fractals can use the GPU path when available. Other recursive renderers continue to run on the Java renderer path inside the same JavaFX shell.")
        );
        sidebar.setPadding(new Insets(0, 6, 0, 0));

        ScrollPane scrollPane = new ScrollPane(sidebar);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(372);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setPadding(Insets.EMPTY);
        return scrollPane;
    }

    private VBox createHeroCard() {
        VBox box = createCardBox();
        Label eyebrow = new Label("Pure Java UI");
        eyebrow.setStyle("-fx-text-fill: #1f57c3; -fx-font-weight: 700; -fx-font-size: 13px;");
        Label title = new Label("Modernized desktop shell");
        title.setStyle("-fx-text-fill: #181e2a; -fx-font-weight: 700; -fx-font-size: 22px;");
        Label body = wrapLabel("The Swing shell is gone. JavaFX now drives the window, controls, export flow, and runtime status while the rendering core stays in Java and keeps CPU/GPU fallback behavior.");
        HBox badges = new HBox(8,
                createBadge("JavaFX", "#e3edff", "#1f57c3"),
                createBadge("Tile CPU", "#e3f7ef", "#148c5c"),
                createBadge("Optional GPU", "#fff2e0", "#a1641a")
        );
        badges.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(eyebrow, title, body, badges);
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

    private VBox createExportCard() {
        VBox box = createCardBox();
        HBox buttons = new HBox(8,
                createPrimaryButton("High-res export", event -> viewport.exportHighResolutionView(stage)),
                createSecondaryButton("Current view", event -> viewport.exportCurrentView(stage))
        );
        buttons.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(
                createCardTitle("Export and runtime"),
                createSectionLabel("Export"),
                wrapLabel("The viewport can export the current size, 2x, 4x, or a custom PNG size. Export uses the same backend selection path as preview rendering."),
                buttons,
                createSectionLabel("Backend status"),
                wrapLabel("The top pills report the actual backend in use and the latest render time. If GPU setup fails or is unavailable, the app falls back to CPU automatically.")
        );
        return box;
    }

    private VBox createInfoCard(String title, String text) {
        VBox box = createCardBox();
        box.getChildren().addAll(createCardTitle(title), wrapLabel(text));
        return box;
    }

    private StackPane buildViewportShell() {
        VBox shell = new VBox(12);
        shell.setPadding(new Insets(16));
        shell.setStyle(cardStyle());

        Label title = new Label("Viewport");
        title.setStyle("-fx-text-fill: #181e2a; -fx-font-size: 18px; -fx-font-weight: 700;");
        Label subtitle = new Label("Preview freezes the last completed frame during active navigation so panning and zooming stay responsive while a new image is rendering.");
        subtitle.setStyle("-fx-text-fill: #5e687a; -fx-font-size: 13px;");
        subtitle.setWrapText(true);

        VBox.setVgrow(viewport, Priority.ALWAYS);
        shell.getChildren().addAll(new VBox(4, title, subtitle), viewport);

        return new StackPane(shell);
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(statusLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 14, 10, 14));
        bar.setStyle(cardStyle());
        BorderPane.setMargin(bar, new Insets(18, 0, 0, 0));
        return bar;
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
                "Cursor: %s%s | Zoom: %.2fx | Offset: (%.0f, %.0f) | Backend: %s",
                mouse,
                complex,
                zoom,
                offsetX,
                offsetY,
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
        double real = renderer.mapPlaneX((int) Math.round(mouseX), (int) Math.max(1, Math.round(viewport.getViewportWidth())), zoom, offsetX);
        double imaginary = renderer.mapPlaneY(
                (int) Math.round(mouseY),
                (int) Math.max(1, Math.round(viewport.getViewportWidth())),
                (int) Math.max(1, Math.round(viewport.getViewportHeight())),
                zoom,
                offsetY
        );
        return String.format(Locale.US, " | Complex: %.6f %+.6fi", real, imaginary);
    }

    private VBox createCardBox() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle(cardStyle());
        box.setFillWidth(true);
        return box;
    }

    private Label createCardTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #181e2a; -fx-font-size: 17px; -fx-font-weight: 700;");
        return label;
    }

    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #181e2a; -fx-font-size: 13px; -fx-font-weight: 700;");
        return label;
    }

    private Label wrapLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #5e687a; -fx-font-size: 13px;");
        return label;
    }

    private Label createPillLabel() {
        Label label = new Label();
        label.setStyle(pillStyle("#e3edff", "#1f57c3"));
        return label;
    }

    private Label createBadge(String text, String background, String foreground) {
        Label label = new Label(text);
        label.setStyle(pillStyle(background, foreground));
        return label;
    }

    private Button createPrimaryButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button button = new Button(text);
        button.setOnAction(action);
        button.setStyle("-fx-background-color: #2870ff; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 10 14 10 14;");
        return button;
    }

    private Button createSecondaryButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button button = new Button(text);
        button.setOnAction(action);
        button.setStyle("-fx-background-color: white; -fx-text-fill: #181e2a; -fx-border-color: #dce2eb; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 14 10 14;");
        return button;
    }

    private String cardStyle() {
        return "-fx-background-color: white; -fx-border-color: #dce2eb; -fx-border-radius: 16; -fx-background-radius: 16;";
    }

    private String pillStyle(String background, String foreground) {
        return "-fx-background-color: " + background + "; -fx-text-fill: " + foreground + "; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 999; -fx-padding: 8 12 8 12;";
    }
}