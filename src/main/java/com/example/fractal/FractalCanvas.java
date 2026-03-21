package com.example.fractal;

import com.example.fractal.model.FractalDefinition;
import com.example.fractal.model.FractalViewState;
import com.example.fractal.render.FractalRenderService;
import com.example.fractal.render.RenderRequest;
import com.example.fractal.render.RenderResult;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class FractalCanvas extends JPanel {

    public interface InteractionListener {
        void onZoomChanged(double zoom, int anchorX, int anchorY);

        void onViewChanged(double zoom, double offsetX, double offsetY, int mouseX, int mouseY);

        void onResetRequested();
    }

    private static final double MIN_ZOOM = 0.1;
    private static final int DRAG_RENDER_DELAY_MS = 40;
    private static final int MAX_EXPORT_DIMENSION = 12000;

    private final Timer dragRenderTimer;
    private final FractalRenderService renderService;

    private BufferedImage image;
    private BufferedImage frozenFrame;
    private FractalViewState viewState;
    private FractalViewState frozenViewState;
    private Point dragAnchor;
    private Point selectionStart;
    private Point selectionEnd;
    private int mouseX = -1;
    private int mouseY = -1;
    private boolean selectionMode;
    private boolean renderInProgress;
    private long lastRenderDurationMillis;
    private InteractionListener interactionListener;

    public FractalCanvas() {
        setPreferredSize(new Dimension(880, 820));
        setBackground(new Color(8, 12, 24));
        this.renderService = new FractalRenderService();
        this.viewState = new FractalViewState(null, 0, 1.0, 0.0, 0.0);
        this.dragRenderTimer = new Timer(DRAG_RENDER_DELAY_MS, e -> scheduleRender());
        this.dragRenderTimer.setRepeats(false);
        bindMouseInteractions();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                scheduleRender();
            }
        });
    }

    public void setInteractionListener(InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    public void render(FractalDefinition definition, int depth, double zoom) {
        viewState = viewState.withDefinition(definition, depth, zoom);
        scheduleRender();
        notifyViewChanged();
    }

    public void applyZoom(double nextZoom, int anchorX, int anchorY) {
        if (viewState.zoom() <= 0.0) {
            viewState = viewState.withZoomAndOffset(nextZoom, viewState.offsetX(), viewState.offsetY());
            scheduleRender();
            notifyViewChanged();
            return;
        }

        double centerX = getWidth() / 2.0;
        double centerY = getHeight() / 2.0;
        double zoomFactor = nextZoom / viewState.zoom();
        double nextOffsetX = anchorX - centerX - zoomFactor * (anchorX - centerX - viewState.offsetX());
        double nextOffsetY = anchorY - centerY - zoomFactor * (anchorY - centerY - viewState.offsetY());

        viewState = viewState.withZoomAndOffset(nextZoom, nextOffsetX, nextOffsetY);
        scheduleRender();
        notifyViewChanged();
    }

    public void resetView() {
        dragAnchor = null;
        selectionStart = null;
        selectionEnd = null;
        selectionMode = false;
        dragRenderTimer.stop();
        viewState = viewState.resetOffset();
        scheduleRender();
        notifyViewChanged();
    }

    public void exportCurrentView() {
        if (!canExport()) {
            return;
        }

        exportImageWithSize(getWidth(), getHeight(), "导出当前视图");
    }

    public void exportHighResolutionView() {
        if (!canExport()) {
            return;
        }

        ExportSize exportSize = promptForExportSize();
        if (exportSize == null) {
            return;
        }

        exportImageWithSize(exportSize.width, exportSize.height, "导出高分辨率 PNG");
    }

    private boolean canExport() {
        if (viewState.hasDefinition() && getWidth() > 0 && getHeight() > 0) {
            return true;
        }

        JOptionPane.showMessageDialog(this, "当前没有可导出的分形视图。", "导出失败", JOptionPane.WARNING_MESSAGE);
        return false;
    }

    private void scheduleRender() {
        if (!viewState.hasDefinition()) {
            renderService.cancelActiveRender();
            image = null;
            frozenFrame = null;
            frozenViewState = null;
            renderInProgress = false;
            repaint();
            return;
        }

        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) {
            repaint();
            return;
        }

        freezeCurrentFrame(width, height);
        RenderRequest request = RenderRequest.of(viewState, width, height);
        renderService.renderAsync(request, new FractalRenderService.Listener() {
            @Override
            public void onRenderStarted(RenderRequest ignored) {
                renderInProgress = true;
                repaint();
            }

            @Override
            public void onRenderCompleted(RenderResult result) {
                image = result.image();
                frozenFrame = null;
                frozenViewState = null;
                renderInProgress = false;
                lastRenderDurationMillis = result.durationMillis();
                repaint();
            }

            @Override
            public void onRenderFailed(RenderRequest failedRequest, Exception exception) {
                renderInProgress = false;
                repaint();
            }
        });
    }

    private void scheduleDragRender() {
        dragRenderTimer.restart();
        repaint();
    }

    private void freezeCurrentFrame(int width, int height) {
        if (image == null) {
            frozenFrame = null;
            frozenViewState = null;
            return;
        }

        frozenViewState = viewState;
        if (image.getWidth() == width && image.getHeight() == height) {
            frozenFrame = copyImage(image);
            return;
        }

        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        frozenFrame = scaled;
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private void bindMouseInteractions() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (showPopupIfNeeded(e)) {
                    return;
                }

                mouseX = e.getX();
                mouseY = e.getY();
                if (e.isShiftDown()) {
                    selectionMode = true;
                    selectionStart = e.getPoint();
                    selectionEnd = e.getPoint();
                    dragAnchor = null;
                } else {
                    selectionMode = false;
                    selectionStart = null;
                    selectionEnd = null;
                    dragAnchor = e.getPoint();
                }
                repaint();
                notifyViewChanged();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                if (selectionMode) {
                    selectionEnd = e.getPoint();
                    repaint();
                    notifyViewChanged();
                    return;
                }

                if (dragAnchor == null) {
                    dragAnchor = e.getPoint();
                    return;
                }

                Point point = e.getPoint();
                double nextOffsetX = viewState.offsetX() + point.getX() - dragAnchor.getX();
                double nextOffsetY = viewState.offsetY() + point.getY() - dragAnchor.getY();
                viewState = viewState.withOffset(nextOffsetX, nextOffsetY);
                dragAnchor = point;
                scheduleDragRender();
                notifyViewChanged();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (showPopupIfNeeded(e)) {
                    return;
                }

                mouseX = e.getX();
                mouseY = e.getY();
                if (selectionMode) {
                    selectionEnd = e.getPoint();
                    applySelectionZoom();
                } else {
                    dragRenderTimer.stop();
                    scheduleRender();
                }
                dragAnchor = null;
                selectionMode = false;
                repaint();
                notifyViewChanged();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                notifyViewChanged();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseX = -1;
                mouseY = -1;
                notifyViewChanged();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2 && interactionListener != null) {
                    interactionListener.onResetRequested();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double factor = e.getWheelRotation() < 0 ? 1.12 : 1.0 / 1.12;
                double nextZoom = clampMin(viewState.zoom() * factor, MIN_ZOOM);
                if (interactionListener != null) {
                    interactionListener.onZoomChanged(nextZoom, e.getX(), e.getY());
                }
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
        addMouseWheelListener(adapter);
    }

    private boolean showPopupIfNeeded(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return false;
        }

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem exportItem = new JMenuItem("导出当前视图 PNG");
        exportItem.addActionListener(event -> exportCurrentView());
        popupMenu.add(exportItem);

        JMenuItem exportHighResItem = new JMenuItem("导出高分辨率 PNG");
        exportHighResItem.addActionListener(event -> exportHighResolutionView());
        popupMenu.add(exportHighResItem);

        popupMenu.show(e.getComponent(), e.getX(), e.getY());
        return true;
    }

    private void exportImageWithSize(int width, int height, String dialogTitle) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogTitle);
        chooser.setSelectedFile(new File(buildDefaultFileName(width, height)));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG 图片", "png"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }

        try {
            RenderResult exportResult = renderService.renderMeasured(RenderRequest.of(viewState, width, height));
            ImageIO.write(exportResult.image(), "png", file);
            JOptionPane.showMessageDialog(
                    this,
                    String.format("导出成功 (%d x %d, %d ms):\n%s", width, height, exportResult.durationMillis(), file.getAbsolutePath()),
                    "导出完成",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "导出失败:\n" + ex.getMessage(), "导出失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ExportSize promptForExportSize() {
        final int baseWidth = Math.max(1, getWidth());
        final int baseHeight = Math.max(1, getHeight());
        final JComboBox<String> modeBox = new JComboBox<String>(new String[]{"当前尺寸 (1x)", "双倍尺寸 (2x)", "四倍尺寸 (4x)", "自定义"});
        final JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(clampExportDimension(baseWidth * 2), 1, MAX_EXPORT_DIMENSION, 1));
        final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(clampExportDimension(baseHeight * 2), 1, MAX_EXPORT_DIMENSION, 1));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 0, 4, 10);
        panel.add(new JLabel("导出尺寸"), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        panel.add(modeBox, constraints);

        constraints.gridy = 1;
        constraints.gridx = 0;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("宽度"), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(widthSpinner, constraints);

        constraints.gridy = 2;
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("高度"), constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(heightSpinner, constraints);

        constraints.gridy = 3;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(8, 0, 0, 0);
        panel.add(new JLabel(String.format("当前画布: %d x %d", baseWidth, baseHeight)), constraints);

        modeBox.addActionListener(event -> syncExportDimensions(modeBox, widthSpinner, heightSpinner, baseWidth, baseHeight));
        syncExportDimensions(modeBox, widthSpinner, heightSpinner, baseWidth, baseHeight);

        int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                "导出高分辨率 PNG",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (option != JOptionPane.OK_OPTION) {
            return null;
        }

        int width = ((Number) widthSpinner.getValue()).intValue();
        int height = ((Number) heightSpinner.getValue()).intValue();
        if (width <= 0 || height <= 0) {
            JOptionPane.showMessageDialog(this, "导出尺寸必须大于 0。", "导出失败", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return new ExportSize(width, height);
    }

    private void syncExportDimensions(JComboBox<String> modeBox,
                                      JSpinner widthSpinner,
                                      JSpinner heightSpinner,
                                      int baseWidth,
                                      int baseHeight) {
        int multiplier = 0;
        if (modeBox.getSelectedIndex() == 0) {
            multiplier = 1;
        } else if (modeBox.getSelectedIndex() == 1) {
            multiplier = 2;
        } else if (modeBox.getSelectedIndex() == 2) {
            multiplier = 4;
        }

        boolean custom = multiplier == 0;
        widthSpinner.setEnabled(custom);
        heightSpinner.setEnabled(custom);
        if (!custom) {
            widthSpinner.setValue(clampExportDimension(baseWidth * multiplier));
            heightSpinner.setValue(clampExportDimension(baseHeight * multiplier));
        }
    }

    private int clampExportDimension(int value) {
        return Math.max(1, Math.min(MAX_EXPORT_DIMENSION, value));
    }

    private String buildDefaultFileName(int width, int height) {
        String name = viewState.definition() != null ? viewState.definition().name() : "fractal";
        String normalized = name.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5-_]", "_");
        return normalized
                + "-zoom-"
                + String.format("%.2f", viewState.zoom()).replace('.', '_')
                + "-"
                + width
                + "x"
                + height
                + ".png";
    }

    private void applySelectionZoom() {
        if (selectionStart == null || selectionEnd == null) {
            clearSelection();
            return;
        }

        Rectangle area = toRectangle(selectionStart, selectionEnd);
        if (area.width < 20 || area.height < 20) {
            clearSelection();
            return;
        }

        double factor = Math.min(getWidth() / (double) area.width, getHeight() / (double) area.height);
        double nextZoom = clampMin(viewState.zoom() * factor, MIN_ZOOM);
        double zoomFactor = nextZoom / viewState.zoom();
        double rectCenterX = area.getCenterX();
        double rectCenterY = area.getCenterY();
        double centerX = getWidth() / 2.0;
        double centerY = getHeight() / 2.0;
        double nextOffsetX = (viewState.offsetX() + centerX - rectCenterX) * zoomFactor;
        double nextOffsetY = (viewState.offsetY() + centerY - rectCenterY) * zoomFactor;

        viewState = viewState.withZoomAndOffset(nextZoom, nextOffsetX, nextOffsetY);
        clearSelection();
        scheduleRender();
        notifyViewChanged();
        if (interactionListener != null) {
            interactionListener.onZoomChanged(nextZoom, -1, -1);
        }
    }

    private Rectangle toRectangle(Point start, Point end) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(start.x - end.x);
        int height = Math.abs(start.y - end.y);
        return new Rectangle(x, y, width, height);
    }

    private void clearSelection() {
        selectionStart = null;
        selectionEnd = null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D graphics = (Graphics2D) g;
        graphics.setColor(getBackground());
        graphics.fillRect(0, 0, getWidth(), getHeight());

        if (renderInProgress && frozenFrame != null) {
            paintPreviewFrame(graphics);
        } else if (image != null) {
            graphics.drawImage(image, 0, 0, null);
        }

        if (viewState.hasDefinition()) {
            paintOverlay(graphics);
            paintSelection(graphics);
            paintRenderStatus(graphics);
        }
    }

    private void paintPreviewFrame(Graphics2D graphics) {
        if (frozenViewState == null) {
            graphics.drawImage(frozenFrame, 0, 0, null);
            return;
        }

        double zoomFactor = frozenViewState.zoom() == 0.0 ? 1.0 : viewState.zoom() / frozenViewState.zoom();
        double centerX = getWidth() / 2.0;
        double centerY = getHeight() / 2.0;
        double deltaX = viewState.offsetX() - frozenViewState.offsetX();
        double deltaY = viewState.offsetY() - frozenViewState.offsetY();

        AffineTransform original = graphics.getTransform();
        graphics.translate(centerX + deltaX, centerY + deltaY);
        graphics.scale(zoomFactor, zoomFactor);
        graphics.translate(-centerX, -centerY);
        graphics.drawImage(frozenFrame, 0, 0, null);
        graphics.setTransform(original);
    }

    private void paintOverlay(Graphics2D graphics) {
        graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN, 13f));
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRoundRect(16, 16, 420, 126, 14, 14);
        graphics.setColor(new Color(232, 238, 255));
        graphics.drawString("左键拖拽: 平移视图", 28, 42);
        graphics.drawString("滚轮: 以鼠标为中心无极缩放", 28, 62);
        graphics.drawString("Shift + 拖拽: 框选缩放", 28, 82);
        graphics.drawString("双击: 重置视图", 28, 102);
        graphics.drawString("右键: 导出当前视图 PNG", 28, 122);
        graphics.drawString("右键: 导出高分辨率 PNG", 28, 142);
    }

    private void paintSelection(Graphics2D graphics) {
        if (selectionStart == null || selectionEnd == null) {
            return;
        }

        Rectangle area = toRectangle(selectionStart, selectionEnd);
        Area dimArea = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
        dimArea.subtract(new Area(area));

        graphics.setColor(new Color(6, 10, 18, 110));
        graphics.fill(dimArea);
        graphics.setColor(new Color(86, 174, 255, 55));
        graphics.fill(area);
        graphics.setColor(new Color(86, 174, 255, 230));
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.draw(area);
    }

    private void paintRenderStatus(Graphics2D graphics) {
        String statusText = renderInProgress ? "正在渲染..." : String.format("最近渲染: %d ms", lastRenderDurationMillis);
        graphics.setColor(new Color(0, 0, 0, 160));
        graphics.fillRoundRect(getWidth() - 210, 16, 186, 40, 12, 12);
        graphics.setColor(new Color(240, 246, 255));
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 14f));
        graphics.drawString(statusText, getWidth() - 186, 41);
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