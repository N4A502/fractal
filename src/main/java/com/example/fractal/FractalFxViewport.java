package com.example.fractal;

import com.example.fractal.model.FractalDefinition;
import com.example.fractal.model.FractalViewState;
import com.example.fractal.render.FractalRenderService;
import com.example.fractal.render.RenderRequest;
import com.example.fractal.render.RenderResult;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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

    private final FractalRenderService renderService;
    private final PauseTransition interactionRenderDelay;
    private final ImageView frozenImageView;
    private final ImageView imageView;
    private final Canvas overlayCanvas;
    private final Label renderStatusLabel;
    private final ContextMenu contextMenu;

    private FractalViewState viewState;
    private FractalViewState frozenViewState;
    private WritableImage currentImage;
    private WritableImage frozenImage;
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

    public FractalFxViewport() {
        this.renderService = new FractalRenderService();
        this.interactionRenderDelay = new PauseTransition(Duration.millis(140));
        this.frozenImageView = new ImageView();
        this.imageView = new ImageView();
        this.overlayCanvas = new Canvas(880, 820);
        this.renderStatusLabel = new Label();
        this.contextMenu = buildContextMenu();
        this.viewState = new FractalViewState(null, 0, 1.0, 0.0, 0.0);

        setStyle("-fx-background-color: #080c18; -fx-background-radius: 12;");
        setMinSize(400, 320);
        setPrefSize(880, 820);
        StackPane.setAlignment(renderStatusLabel, Pos.TOP_RIGHT);
        renderStatusLabel.setStyle("-fx-background-color: rgba(0,0,0,0.65); -fx-text-fill: #f0f6ff; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 10 14 10 14;");

        getChildren().addAll(frozenImageView, imageView, overlayCanvas, renderStatusLabel);
        imageView.fitWidthProperty().bind(widthProperty());
        imageView.fitHeightProperty().bind(heightProperty());
        frozenImageView.fitWidthProperty().bind(widthProperty());
        frozenImageView.fitHeightProperty().bind(heightProperty());
        overlayCanvas.widthProperty().bind(widthProperty());
        overlayCanvas.heightProperty().bind(heightProperty());

        interactionRenderDelay.setOnFinished(event -> scheduleRender());
        widthProperty().addListener((obs, oldValue, newValue) -> scheduleRender());
        heightProperty().addListener((obs, oldValue, newValue) -> scheduleRender());
        bindInteractions();
        refreshOverlay();
    }

    public void setInteractionListener(InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
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
        exportImageWithSize(owner, (int) Math.max(1, Math.round(getWidth())), (int) Math.max(1, Math.round(getHeight())), "Export Current View");
    }

    public void exportHighResolutionView(Stage owner) {
        if (!viewState.hasDefinition()) {
            return;
        }
        ExportSize exportSize = promptForExportSize(owner);
        if (exportSize != null) {
            exportImageWithSize(owner, exportSize.width, exportSize.height, "Export High Resolution PNG");
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

    public double getViewportWidth() {
        return getWidth();
    }

    public double getViewportHeight() {
        return getHeight();
    }

    public void shutdown() {
        renderService.shutdown();
    }

    private ContextMenu buildContextMenu() {
        MenuItem exportCurrent = new MenuItem("Export current view");
        exportCurrent.setOnAction(event -> exportCurrentView(resolveOwner()));
        MenuItem exportHighRes = new MenuItem("Export high resolution PNG");
        exportHighRes.setOnAction(event -> exportHighResolutionView(resolveOwner()));
        MenuItem reset = new MenuItem("Reset view");
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

    private void scheduleRender() {
        if (!viewState.hasDefinition()) {
            renderService.cancelActiveRender();
            currentImage = null;
            frozenImage = null;
            frozenViewState = null;
            renderInProgress = false;
            Platform.runLater(this::refreshOverlay);
            return;
        }

        int width = (int) Math.max(1, Math.round(getWidth()));
        int height = (int) Math.max(1, Math.round(getHeight()));
        if (width <= 1 || height <= 1) {
            return;
        }

        freezeCurrentFrame();
        RenderRequest request = RenderRequest.of(viewState, width, height);
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
                    imageView.setImage(currentImage);
                    frozenImage = null;
                    frozenViewState = null;
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
                    showError("Render failed", exception);
                });
            }
        });
    }

    private void freezeCurrentFrame() {
        if (currentImage == null) {
            frozenImage = null;
            frozenViewState = null;
            frozenImageView.setImage(null);
            return;
        }

        frozenImage = currentImage;
        frozenViewState = viewState;
        frozenImageView.setImage(frozenImage);
        frozenImageView.setOpacity(1.0);
    }

    private void bindInteractions() {
        setOnMousePressed(this::handleMousePressed);
        setOnMouseDragged(this::handleMouseDragged);
        setOnMouseReleased(this::handleMouseReleased);
        setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
            notifyViewChanged();
        });
        setOnMouseExited(event -> {
            mouseX = -1;
            mouseY = -1;
            notifyViewChanged();
        });
        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(this, event.getScreenX(), event.getScreenY());
                return;
            }
            contextMenu.hide();
            if (event.getClickCount() >= 2 && event.getButton() == MouseButton.PRIMARY && interactionListener != null) {
                interactionListener.onResetRequested();
            }
        });
        setOnScroll(this::handleScroll);
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            return;
        }

        contextMenu.hide();
        mouseX = event.getX();
        mouseY = event.getY();
        if (event.isShiftDown()) {
            selectionMode = true;
            selectionStartX = event.getX();
            selectionStartY = event.getY();
            selectionEndX = event.getX();
            selectionEndY = event.getY();
            dragAnchorX = Double.NaN;
            dragAnchorY = Double.NaN;
        } else {
            selectionMode = false;
            clearSelection();
            dragAnchorX = event.getX();
            dragAnchorY = event.getY();
        }
        refreshOverlay();
        notifyViewChanged();
    }

    private void handleMouseDragged(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();
        if (selectionMode) {
            selectionEndX = event.getX();
            selectionEndY = event.getY();
            refreshOverlay();
            notifyViewChanged();
            return;
        }

        if (Double.isNaN(dragAnchorX) || Double.isNaN(dragAnchorY)) {
            dragAnchorX = event.getX();
            dragAnchorY = event.getY();
            return;
        }

        double nextOffsetX = viewState.offsetX() + event.getX() - dragAnchorX;
        double nextOffsetY = viewState.offsetY() + event.getY() - dragAnchorY;
        viewState = viewState.withOffset(nextOffsetX, nextOffsetY);
        dragAnchorX = event.getX();
        dragAnchorY = event.getY();
        updateFrozenTransform();
        refreshOverlay();
        notifyViewChanged();
    }

    private void handleMouseReleased(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            return;
        }

        mouseX = event.getX();
        mouseY = event.getY();
        if (selectionMode) {
            selectionEndX = event.getX();
            selectionEndY = event.getY();
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
        applyZoom(nextZoom, event.getX(), event.getY());
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

        double centerX = getWidth() / 2.0;
        double centerY = getHeight() / 2.0;
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

        double factor = Math.min(getWidth() / width, getHeight() / height);
        double nextZoom = clampMin(viewState.zoom() * factor, MIN_ZOOM);
        double zoomFactor = nextZoom / viewState.zoom();
        double centerX = getWidth() / 2.0;
        double centerY = getHeight() / 2.0;
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
        if (frozenImage == null || frozenViewState == null) {
            return;
        }
        double zoomFactor = frozenViewState.zoom() == 0.0 ? 1.0 : viewState.zoom() / frozenViewState.zoom();
        double deltaX = viewState.offsetX() - frozenViewState.offsetX();
        double deltaY = viewState.offsetY() - frozenViewState.offsetY();
        frozenImageView.setTranslateX(deltaX);
        frozenImageView.setTranslateY(deltaY);
        frozenImageView.setScaleX(zoomFactor);
        frozenImageView.setScaleY(zoomFactor);
        imageView.setImage(currentImage);
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

        renderStatusLabel.setText(renderInProgress ? "Rendering..." : "Last render: " + lastRenderDurationMillis + " ms");
        if (!renderInProgress) {
            frozenImageView.setOpacity(0.0);
            frozenImageView.setTranslateX(0.0);
            frozenImageView.setTranslateY(0.0);
            frozenImageView.setScaleX(1.0);
            frozenImageView.setScaleY(1.0);
        }
    }

    private void exportImageWithSize(Stage owner, int width, int height, String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
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
            showError("Export failed", ex);
        }
    }

    private ExportSize promptForExportSize(Stage owner) {
        List<String> choices = Arrays.asList("Current size (1x)", "2x", "4x", "Custom");
        ChoiceDialog<String> dialog = new ChoiceDialog<String>(choices.get(1), choices);
        dialog.setTitle("Export High Resolution PNG");
        dialog.setHeaderText("Choose export size");
        dialog.initOwner(owner);
        Optional<String> selected = dialog.showAndWait();
        if (!selected.isPresent()) {
            return null;
        }

        int baseWidth = (int) Math.max(1, Math.round(getWidth()));
        int baseHeight = (int) Math.max(1, Math.round(getHeight()));
        String choice = selected.get();
        if (choice.startsWith("Current")) {
            return new ExportSize(baseWidth, baseHeight);
        }
        if ("2x".equals(choice)) {
            return new ExportSize(clampExportDimension(baseWidth * 2), clampExportDimension(baseHeight * 2));
        }
        if ("4x".equals(choice)) {
            return new ExportSize(clampExportDimension(baseWidth * 4), clampExportDimension(baseHeight * 4));
        }

        Dialog<ExportSize> customDialog = new Dialog<ExportSize>();
        customDialog.setTitle("Custom export size");
        customDialog.initOwner(owner);
        ButtonType okButton = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        customDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);
        Spinner<Integer> widthSpinner = new Spinner<Integer>(1, MAX_EXPORT_DIMENSION, clampExportDimension(baseWidth * 2), 1);
        Spinner<Integer> heightSpinner = new Spinner<Integer>(1, MAX_EXPORT_DIMENSION, clampExportDimension(baseHeight * 2), 1);
        VBox content = new VBox(10, new Label("Width"), widthSpinner, new Label("Height"), heightSpinner);
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
        return normalized + "-zoom-" + String.format(Locale.US, "%.2f", viewState.zoom()).replace('.', '_') + "-" + width + "x" + height + ".png";
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
}