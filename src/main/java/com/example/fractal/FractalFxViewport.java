package com.example.fractal;

import com.example.fractal.model.FractalDefinition;
import com.example.fractal.model.FractalViewState;
import com.example.fractal.render.FractalRenderService;
import com.example.fractal.render.RenderRequest;
import com.example.fractal.render.RenderResult;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class FractalFxViewport extends StackPane {

    public interface InteractionListener {
        void onZoomChanged(double zoom, double offsetX, double offsetY, boolean fromWheelAnchor);

        void onViewChanged(double zoom, double offsetX, double offsetY, double mouseX, double mouseY);

        void onResetRequested();
    }

    private static final double MIN_ZOOM = 0.1;
    private static final int MAX_EXPORT_DIMENSION = 12000;
    private static final double PREVIEW_OVERSCAN_FACTOR = 1.35;

    private final FractalRenderService renderService;
    private final PauseTransition interactionRenderDelay;
    private final ImageView frozenImageView;
    private final ImageView imageView;
    private final Canvas overlayCanvas;
    private final Label renderStatusLabel;
    private final ContextMenu contextMenu;
    private final StackPane renderPane;

    private FractalViewState viewState;
    private FractalViewState frozenViewState;
    private WritableImage currentImage;
    private Rectangle2D currentViewportRect;
    private InteractionListener interactionListener;
    private double dragAnchorX = Double.NaN;
    private double dragAnchorY = Double.NaN;
    private double selectionStartX = Double.NaN;
    private double selectionStartY = Double.NaN;
    private double selectionEndX = Double.NaN;
    private double selectionEndY = Double.NaN;
    private double mouseX = -1;
    private double mouseY = -1;
    private boolean selectionMode;
    private boolean renderInProgress;
    private long lastRenderDurationMillis;
    private int viewWidth = 1280;
    private int viewHeight = 720;

    public FractalFxViewport() {
        this.renderService = new FractalRenderService();
        this.interactionRenderDelay = new PauseTransition(Duration.millis(140));
        this.frozenImageView = new ImageView();
        this.imageView = new ImageView();
        this.overlayCanvas = new Canvas(viewWidth, viewHeight);
        this.renderStatusLabel = new Label();
        this.contextMenu = buildContextMenu();
        this.renderPane = new StackPane();
        this.viewState = new FractalViewState(null, 0, 1.0, 0.0, 0.0);

        setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        setMinSize(400, 320);
        setPrefSize(900, 760);

        Rectangle containerClip = new Rectangle();
        containerClip.setArcWidth(24);
        containerClip.setArcHeight(24);
        containerClip.widthProperty().bind(widthProperty());
        containerClip.heightProperty().bind(heightProperty());
        setClip(containerClip);

        renderPane.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        renderPane.setPickOnBounds(true);
        renderPane.setMinSize(viewWidth, viewHeight);
        renderPane.setPrefSize(viewWidth, viewHeight);
        renderPane.setMaxSize(viewWidth, viewHeight);
        Rectangle viewportClip = new Rectangle(viewWidth, viewHeight);
        viewportClip.setArcWidth(24);
        viewportClip.setArcHeight(24);
        viewportClip.widthProperty().bind(renderPane.widthProperty());
        viewportClip.heightProperty().bind(renderPane.heightProperty());
        renderPane.setClip(viewportClip);
        StackPane.setAlignment(renderStatusLabel, Pos.TOP_RIGHT);
        renderStatusLabel.setStyle("-fx-background-color: rgba(0,0,0,0.65); -fx-text-fill: #f0f6ff; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 10 14 10 14;");

        renderPane.getChildren().addAll(frozenImageView, imageView, overlayCanvas, renderStatusLabel);
        imageView.fitWidthProperty().bind(renderPane.widthProperty());
        imageView.fitHeightProperty().bind(renderPane.heightProperty());
        imageView.setPreserveRatio(false);
        frozenImageView.fitWidthProperty().bind(renderPane.widthProperty());
        frozenImageView.fitHeightProperty().bind(renderPane.heightProperty());
        frozenImageView.setPreserveRatio(false);
        overlayCanvas.widthProperty().bind(renderPane.widthProperty());
        overlayCanvas.heightProperty().bind(renderPane.heightProperty());

        getChildren().add(renderPane);
        StackPane.setAlignment(renderPane, Pos.CENTER);

        interactionRenderDelay.setOnFinished(event -> scheduleRender());
        widthProperty().addListener((obs, oldValue, newValue) -> updateRenderPaneScale());
        heightProperty().addListener((obs, oldValue, newValue) -> updateRenderPaneScale());
        bindInteractions();
        updateRenderPaneScale();
        refreshOverlay();
    }

    public void setInteractionListener(InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    public void setViewSize(int width, int height) {
        this.viewWidth = Math.max(320, width);
        this.viewHeight = Math.max(240, height);
        renderPane.setMinSize(viewWidth, viewHeight);
        renderPane.setPrefSize(viewWidth, viewHeight);
        renderPane.setMaxSize(viewWidth, viewHeight);
        updateRenderPaneScale();
        scheduleRender();
    }

    public void render(FractalDefinition definition, int depth, double zoom) {
        viewState = viewState.withDefinition(definition, depth, zoom);
        scheduleRender();
        notifyViewChanged();
    }

    public void resetView() {
        dragAnchorX = Double.NaN;
        dragAnchorY = Double.NaN;
        selectionMode = false;
        clearSelection();
        interactionRenderDelay.stop();
        viewState = viewState.resetOffset();
        scheduleRender();
        notifyViewChanged();
    }

    public void exportCurrentView(Stage owner) {
        if (!viewState.hasDefinition()) {
            return;
        }
        exportImageWithSize(owner, viewWidth, viewHeight, "导出当前视图");
    }

    public void exportHighResolutionView(Stage owner) {
        if (!viewState.hasDefinition()) {
            return;
        }
        ExportSize exportSize = promptForExportSize(owner);
        if (exportSize != null) {
            exportImageWithSize(owner, exportSize.width, exportSize.height, "导出高分辨率 PNG");
        }
    }

    public boolean isRenderInProgress() {
        return renderInProgress;
    }

    public long getLastRenderDurationMillis() {
        return lastRenderDurationMillis;
    }

    public String getBackendDescription() {
        if (!viewState.hasDefinition()) {
            return "-";
        }
        return viewState.definition().renderer().backendDescription();
    }

    public int getViewportWidth() {
        return viewWidth;
    }

    public int getViewportHeight() {
        return viewHeight;
    }

    public double getCurrentZoom() {
        return viewState.zoom();
    }

    public double getCurrentOffsetX() {
        return viewState.offsetX();
    }

    public double getCurrentOffsetY() {
        return viewState.offsetY();
    }

    public void applyState(FractalDefinition definition, int depth, double zoom, double offsetX, double offsetY) {
        viewState = new FractalViewState(definition, depth, zoom, offsetX, offsetY);
        scheduleRender();
        notifyViewChanged();
    }

    public void shutdown() {
        renderService.shutdown();
    }

    private ContextMenu buildContextMenu() {
        MenuItem exportCurrent = new MenuItem("导出当前视图");
        exportCurrent.setOnAction(event -> exportCurrentView(resolveOwner()));
        MenuItem exportHighRes = new MenuItem("导出高分辨率 PNG");
        exportHighRes.setOnAction(event -> exportHighResolutionView(resolveOwner()));
        MenuItem reset = new MenuItem("重置视图");
        reset.setOnAction(event -> {
            if (interactionListener != null) {
                interactionListener.onResetRequested();
            }
        });
        return new ContextMenu(exportCurrent, exportHighRes, reset);
    }

    private Stage resolveOwner() {
        return getScene() != null && getScene().getWindow() instanceof Stage ? (Stage) getScene().getWindow() : null;
    }

    private void updateRenderPaneScale() {
        if (viewWidth <= 0 || viewHeight <= 0 || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        double scale = Math.max(getWidth() / viewWidth, getHeight() / viewHeight);
        if (!Double.isFinite(scale) || scale <= 0.0) {
            scale = 1.0;
        }
        renderPane.setScaleX(scale);
        renderPane.setScaleY(scale);
    }

    private void scheduleRender() {
        if (!viewState.hasDefinition()) {
            renderService.cancelActiveRender();
            currentImage = null;
            renderInProgress = false;
            imageView.setImage(null);
            imageView.setViewport(null);
            frozenImageView.setImage(null);
            frozenImageView.setViewport(null);
            currentViewportRect = null;
            Platform.runLater(this::refreshOverlay);
            return;
        }

        freezeCurrentFrame();
        PreviewRenderConfig previewConfig = buildPreviewRenderConfig();
        RenderRequest request = previewConfig.request();
        renderService.renderAsync(request, new FractalRenderService.Listener() {
            @Override
            public void onRenderStarted(RenderRequest ignored) {
                Platform.runLater(() -> {
                    renderInProgress = true;
                    refreshOverlay();
                });
            }

            @Override
            public void onRenderCompleted(RenderResult result) {
                Platform.runLater(() -> {
                    currentImage = toWritableImage(result.image());
                    currentViewportRect = previewConfig.viewportRect();
                    imageView.setImage(currentImage);
                    imageView.setViewport(currentViewportRect);
                    frozenImageView.setImage(null);
                    frozenImageView.setOpacity(0.0);
                    renderInProgress = false;
                    lastRenderDurationMillis = result.durationMillis();
                    refreshOverlay();
                });
            }

            @Override
            public void onRenderFailed(RenderRequest failedRequest, Exception exception) {
                Platform.runLater(() -> {
                    renderInProgress = false;
                    refreshOverlay();
                    showError("渲染失败", exception);
                });
            }
        });
    }

    private void freezeCurrentFrame() {
        if (currentImage == null) {
            frozenViewState = null;
            frozenImageView.setImage(null);
            frozenImageView.setViewport(null);
            return;
        }
        frozenViewState = viewState;
        frozenImageView.setImage(currentImage);
        frozenImageView.setViewport(currentViewportRect);
        frozenImageView.setOpacity(1.0);
    }

    private PreviewRenderConfig buildPreviewRenderConfig() {
        int overscanWidth = clampExportDimension((int) Math.ceil(viewWidth * PREVIEW_OVERSCAN_FACTOR));
        int overscanHeight = clampExportDimension((int) Math.ceil(viewHeight * PREVIEW_OVERSCAN_FACTOR));
        double zoomScale = (double) viewWidth / overscanWidth;
        FractalViewState previewState = viewState.withZoomAndOffset(viewState.zoom() * zoomScale, viewState.offsetX(), viewState.offsetY());
        double viewportX = Math.max(0.0, (overscanWidth - viewWidth) / 2.0);
        double viewportY = Math.max(0.0, (overscanHeight - viewHeight) / 2.0);
        return new PreviewRenderConfig(
                RenderRequest.of(previewState, overscanWidth, overscanHeight),
                new Rectangle2D(viewportX, viewportY, viewWidth, viewHeight)
        );
    }

    private void bindInteractions() {
        renderPane.setOnMousePressed(this::handleMousePressed);
        renderPane.setOnMouseDragged(this::handleMouseDragged);
        renderPane.setOnMouseReleased(this::handleMouseReleased);
        renderPane.setOnMouseMoved(event -> {
            mouseX = logicalX(event);
            mouseY = logicalY(event);
            notifyViewChanged();
        });
        renderPane.setOnMouseExited(event -> {
            mouseX = -1;
            mouseY = -1;
            notifyViewChanged();
        });
        renderPane.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(renderPane, event.getScreenX(), event.getScreenY());
                return;
            }
            contextMenu.hide();
            if (event.getClickCount() >= 2 && event.getButton() == MouseButton.PRIMARY && interactionListener != null) {
                interactionListener.onResetRequested();
            }
        });
        renderPane.setOnScroll(this::handleScroll);
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            return;
        }

        contextMenu.hide();
        mouseX = logicalX(event);
        mouseY = logicalY(event);
        if (event.isShiftDown()) {
            selectionMode = true;
            selectionStartX = logicalX(event);
            selectionStartY = logicalY(event);
            selectionEndX = logicalX(event);
            selectionEndY = logicalY(event);
            dragAnchorX = Double.NaN;
            dragAnchorY = Double.NaN;
        } else {
            selectionMode = false;
            clearSelection();
            dragAnchorX = logicalX(event);
            dragAnchorY = logicalY(event);
        }
        refreshOverlay();
        notifyViewChanged();
    }

    private void handleMouseDragged(MouseEvent event) {
        mouseX = logicalX(event);
        mouseY = logicalY(event);
        if (selectionMode) {
            selectionEndX = logicalX(event);
            selectionEndY = logicalY(event);
            refreshOverlay();
            notifyViewChanged();
            return;
        }

        if (Double.isNaN(dragAnchorX) || Double.isNaN(dragAnchorY)) {
            dragAnchorX = logicalX(event);
            dragAnchorY = logicalY(event);
            return;
        }

        double currentX = logicalX(event);
        double currentY = logicalY(event);
        double nextOffsetX = viewState.offsetX() + currentX - dragAnchorX;
        double nextOffsetY = viewState.offsetY() + currentY - dragAnchorY;
        viewState = viewState.withOffset(nextOffsetX, nextOffsetY);
        dragAnchorX = currentX;
        dragAnchorY = currentY;
        updateFrozenTransform();
        refreshOverlay();
        notifyViewChanged();
    }

    private void handleMouseReleased(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            return;
        }

        mouseX = logicalX(event);
        mouseY = logicalY(event);
        if (selectionMode) {
            selectionEndX = logicalX(event);
            selectionEndY = logicalY(event);
            applySelectionZoom();
        } else {
            interactionRenderDelay.stop();
            scheduleRender();
        }
        dragAnchorX = Double.NaN;
        dragAnchorY = Double.NaN;
        selectionMode = false;
        refreshOverlay();
        notifyViewChanged();
    }

    private void handleScroll(ScrollEvent event) {
        double factor = event.getDeltaY() > 0 ? 1.12 : 1.0 / 1.12;
        double nextZoom = clampMin(viewState.zoom() * factor, MIN_ZOOM);
        applyZoom(nextZoom, logicalX(event), logicalY(event));
        interactionRenderDelay.playFromStart();
        if (interactionListener != null) {
            interactionListener.onZoomChanged(nextZoom, viewState.offsetX(), viewState.offsetY(), true);
        }
    }

    private void applyZoom(double nextZoom, double anchorX, double anchorY) {
        if (viewState.zoom() <= 0.0) {
            viewState = viewState.withZoomAndOffset(nextZoom, viewState.offsetX(), viewState.offsetY());
            notifyViewChanged();
            return;
        }

        double centerX = viewWidth / 2.0;
        double centerY = viewHeight / 2.0;
        double zoomFactor = nextZoom / viewState.zoom();
        double nextOffsetX = anchorX - centerX - zoomFactor * (anchorX - centerX - viewState.offsetX());
        double nextOffsetY = anchorY - centerY - zoomFactor * (anchorY - centerY - viewState.offsetY());
        viewState = viewState.withZoomAndOffset(nextZoom, nextOffsetX, nextOffsetY);
        updateFrozenTransform();
        notifyViewChanged();
    }

    private void applySelectionZoom() {
        if (Double.isNaN(selectionStartX) || Double.isNaN(selectionStartY) || Double.isNaN(selectionEndX) || Double.isNaN(selectionEndY)) {
            clearSelection();
            return;
        }

        double x = Math.min(selectionStartX, selectionEndX);
        double y = Math.min(selectionStartY, selectionEndY);
        double width = Math.abs(selectionStartX - selectionEndX);
        double height = Math.abs(selectionStartY - selectionEndY);
        if (width < 20 || height < 20) {
            clearSelection();
            return;
        }

        double factor = Math.min(viewWidth / width, viewHeight / height);
        double nextZoom = clampMin(viewState.zoom() * factor, MIN_ZOOM);
        double zoomFactor = nextZoom / viewState.zoom();
        double centerX = viewWidth / 2.0;
        double centerY = viewHeight / 2.0;
        double rectCenterX = x + width / 2.0;
        double rectCenterY = y + height / 2.0;
        double nextOffsetX = (viewState.offsetX() + centerX - rectCenterX) * zoomFactor;
        double nextOffsetY = (viewState.offsetY() + centerY - rectCenterY) * zoomFactor;

        viewState = viewState.withZoomAndOffset(nextZoom, nextOffsetX, nextOffsetY);
        clearSelection();
        scheduleRender();
        notifyViewChanged();
        if (interactionListener != null) {
            interactionListener.onZoomChanged(nextZoom, viewState.offsetX(), viewState.offsetY(), false);
        }
    }

    private void updateFrozenTransform() {
        if (currentImage == null || frozenViewState == null) {
            return;
        }
        double zoomFactor = frozenViewState.zoom() == 0.0 ? 1.0 : viewState.zoom() / frozenViewState.zoom();
        double deltaX = viewState.offsetX() - frozenViewState.offsetX();
        double deltaY = viewState.offsetY() - frozenViewState.offsetY();
        frozenImageView.setTranslateX(deltaX);
        frozenImageView.setTranslateY(deltaY);
        frozenImageView.setScaleX(zoomFactor);
        frozenImageView.setScaleY(zoomFactor);
    }

    private void refreshOverlay() {
        GraphicsContext graphics = overlayCanvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());

        if (!Double.isNaN(selectionStartX) && !Double.isNaN(selectionEndX)) {
            double x = Math.min(selectionStartX, selectionEndX);
            double y = Math.min(selectionStartY, selectionEndY);
            double width = Math.abs(selectionStartX - selectionEndX);
            double height = Math.abs(selectionStartY - selectionEndY);
            graphics.setFill(Color.rgb(6, 10, 18, 0.42));
            graphics.fillRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
            graphics.clearRect(x, y, width, height);
            graphics.setFill(Color.rgb(86, 174, 255, 0.22));
            graphics.fillRect(x, y, width, height);
            graphics.setStroke(Color.rgb(86, 174, 255, 0.9));
            graphics.setLineWidth(1.5);
            graphics.strokeRect(x, y, width, height);
        }

        renderStatusLabel.setText(renderInProgress ? "渲染中..." : "最近渲染：" + lastRenderDurationMillis + " ms");
        if (!renderInProgress) {
            frozenImageView.setOpacity(0.0);
            frozenImageView.setViewport(currentViewportRect);
            frozenImageView.setTranslateX(0.0);
            frozenImageView.setTranslateY(0.0);
            frozenImageView.setScaleX(1.0);
            frozenImageView.setScaleY(1.0);
        }
    }

    private void exportImageWithSize(Stage owner, int width, int height, String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG 图片", "*.png"));
        chooser.setInitialFileName(buildDefaultFileName(width, height));
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }

        try {
            RenderResult exportResult = renderService.renderMeasured(RenderRequest.of(viewState, width, height));
            ImageIO.write(exportResult.image(), "png", file);
        } catch (IOException ex) {
            showError("导出失败", ex);
        }
    }

    private ExportSize promptForExportSize(Stage owner) {
        List<String> choices = Arrays.asList("当前视图尺寸", "2x", "4x", "自定义");
        ChoiceDialog<String> dialog = new ChoiceDialog<String>(choices.get(0), choices);
        dialog.setTitle("导出高分辨率 PNG");
        dialog.setHeaderText("选择导出尺寸");
        dialog.initOwner(owner);
        Optional<String> selected = dialog.showAndWait();
        if (!selected.isPresent()) {
            return null;
        }

        String choice = selected.get();
        if (choice.startsWith("当前")) {
            return new ExportSize(viewWidth, viewHeight);
        }
        if ("2x".equals(choice)) {
            return new ExportSize(clampExportDimension(viewWidth * 2), clampExportDimension(viewHeight * 2));
        }
        if ("4x".equals(choice)) {
            return new ExportSize(clampExportDimension(viewWidth * 4), clampExportDimension(viewHeight * 4));
        }

        Dialog<ExportSize> customDialog = new Dialog<ExportSize>();
        customDialog.setTitle("自定义导出尺寸");
        customDialog.initOwner(owner);
        ButtonType okButton = new ButtonType("导出", ButtonBar.ButtonData.OK_DONE);
        customDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);
        Spinner<Integer> widthSpinner = new Spinner<Integer>(1, MAX_EXPORT_DIMENSION, clampExportDimension(viewWidth), 10);
        Spinner<Integer> heightSpinner = new Spinner<Integer>(1, MAX_EXPORT_DIMENSION, clampExportDimension(viewHeight), 10);
        VBox content = new VBox(10, new Label("宽度"), widthSpinner, new Label("高度"), heightSpinner);
        customDialog.getDialogPane().setContent(content);
        customDialog.setResultConverter(button -> button == okButton ? new ExportSize(widthSpinner.getValue(), heightSpinner.getValue()) : null);
        return customDialog.showAndWait().orElse(null);
    }

    private int clampExportDimension(int value) {
        return Math.max(1, Math.min(MAX_EXPORT_DIMENSION, value));
    }

    private String buildDefaultFileName(int width, int height) {
        String name = viewState.definition() != null ? viewState.definition().name() : "fractal";
        String normalized = name.replaceAll("[^a-zA-Z0-9-_]", "_");
        return normalized + "-" + width + "x" + height + "-zoom-" + String.format(Locale.US, "%.2f", viewState.zoom()).replace('.', '_') + ".png";
    }

    private WritableImage toWritableImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter writer = writableImage.getPixelWriter();
        int[] pixels = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
        writer.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return writableImage;
    }

    private void showError(String title, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName());
        Stage owner = resolveOwner();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.show();
    }

    private void clearSelection() {
        selectionStartX = Double.NaN;
        selectionStartY = Double.NaN;
        selectionEndX = Double.NaN;
        selectionEndY = Double.NaN;
    }

    private double logicalX(MouseEvent event) {
        return clampLogical(sceneToLogical(event.getSceneX(), event.getSceneY()).getX(), viewWidth);
    }

    private double logicalY(MouseEvent event) {
        return clampLogical(sceneToLogical(event.getSceneX(), event.getSceneY()).getY(), viewHeight);
    }

    private double logicalX(ScrollEvent event) {
        return clampLogical(sceneToLogical(event.getSceneX(), event.getSceneY()).getX(), viewWidth);
    }

    private double logicalY(ScrollEvent event) {
        return clampLogical(sceneToLogical(event.getSceneX(), event.getSceneY()).getY(), viewHeight);
    }

    private Point2D sceneToLogical(double sceneX, double sceneY) {
        return renderPane.sceneToLocal(sceneX, sceneY);
    }

    private double clampLogical(double value, double max) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(max, value));
    }

    private void notifyViewChanged() {
        if (interactionListener != null) {
            interactionListener.onViewChanged(viewState.zoom(), viewState.offsetX(), viewState.offsetY(), mouseX, mouseY);
        }
    }

    private double clampMin(double value, double min) {
        return Math.max(min, value);
    }

    private static class ExportSize {
        private final int width;
        private final int height;

        private ExportSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    private static class PreviewRenderConfig {
        private final RenderRequest request;
        private final Rectangle2D viewportRect;

        private PreviewRenderConfig(RenderRequest request, Rectangle2D viewportRect) {
            this.request = request;
            this.viewportRect = viewportRect;
        }

        private RenderRequest request() {
            return request;
        }

        private Rectangle2D viewportRect() {
            return viewportRect;
        }
    }
}
