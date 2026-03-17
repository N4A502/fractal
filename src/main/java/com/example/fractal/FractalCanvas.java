package com.example.fractal;

import com.example.fractal.model.FractalDefinition;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
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

    private BufferedImage image;
    private FractalDefinition definition;
    private int depth;
    private double zoom;
    private double offsetX;
    private double offsetY;
    private Point dragAnchor;
    private Point selectionStart;
    private Point selectionEnd;
    private int mouseX = -1;
    private int mouseY = -1;
    private boolean selectionMode;
    private boolean renderInProgress;
    private long renderSequence;
    private SwingWorker<BufferedImage, Void> renderWorker;
    private InteractionListener interactionListener;

    public FractalCanvas() {
        setPreferredSize(new Dimension(880, 820));
        setBackground(new Color(8, 12, 24));
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
        this.definition = definition;
        this.depth = depth;
        this.zoom = zoom;
        scheduleRender();
        notifyViewChanged();
    }

    public void applyZoom(double nextZoom, int anchorX, int anchorY) {
        if (zoom <= 0.0) {
            zoom = nextZoom;
            scheduleRender();
            notifyViewChanged();
            return;
        }

        double centerX = getWidth() / 2.0;
        double centerY = getHeight() / 2.0;
        double zoomFactor = nextZoom / zoom;

        offsetX = anchorX - centerX - zoomFactor * (anchorX - centerX - offsetX);
        offsetY = anchorY - centerY - zoomFactor * (anchorY - centerY - offsetY);
        zoom = nextZoom;
        scheduleRender();
        notifyViewChanged();
    }

    public void resetView() {
        offsetX = 0.0;
        offsetY = 0.0;
        dragAnchor = null;
        selectionStart = null;
        selectionEnd = null;
        selectionMode = false;
        scheduleRender();
        notifyViewChanged();
    }

    private void scheduleRender() {
        if (definition == null) {
            image = null;
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

        if (renderWorker != null && !renderWorker.isDone()) {
            renderWorker.cancel(true);
        }

        final FractalDefinition currentDefinition = definition;
        final int currentDepth = depth;
        final double currentZoom = zoom;
        final double currentOffsetX = offsetX;
        final double currentOffsetY = offsetY;
        final long currentSequence = ++renderSequence;

        renderInProgress = true;
        repaint();

        renderWorker = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                return renderImage(width, height, currentDefinition, currentDepth, currentZoom, currentOffsetX, currentOffsetY);
            }

            @Override
            protected void done() {
                if (isCancelled() || currentSequence != renderSequence) {
                    return;
                }

                try {
                    image = get();
                } catch (Exception ignored) {
                    return;
                } finally {
                    if (currentSequence == renderSequence) {
                        renderInProgress = false;
                    }
                }
                repaint();
            }
        };
        renderWorker.execute();
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
                offsetX += point.getX() - dragAnchor.getX();
                offsetY += point.getY() - dragAnchor.getY();
                dragAnchor = point;
                scheduleRender();
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
                double nextZoom = clampMin(zoom * factor, MIN_ZOOM);
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
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
        return true;
    }

    private void exportCurrentView() {
        if (definition == null || getWidth() <= 0 || getHeight() <= 0) {
            JOptionPane.showMessageDialog(this, "当前没有可导出的分形视图。", "导出失败", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出当前视图");
        chooser.setSelectedFile(new File(buildDefaultFileName()));
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
            BufferedImage exportImage = renderImage(getWidth(), getHeight(), definition, depth, zoom, offsetX, offsetY);
            ImageIO.write(exportImage, "png", file);
            JOptionPane.showMessageDialog(this, "导出成功:\n" + file.getAbsolutePath(), "导出完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "导出失败:\n" + ex.getMessage(), "导出失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildDefaultFileName() {
        String name = definition != null ? definition.name() : "fractal";
        String normalized = name.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5-_]", "_");
        return normalized + "-zoom-" + String.format("%.2f", zoom).replace('.', '_') + ".png";
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
        double nextZoom = clampMin(zoom * factor, MIN_ZOOM);
        double zoomFactor = nextZoom / zoom;
        double rectCenterX = area.getCenterX();
        double rectCenterY = area.getCenterY();
        double centerX = getWidth() / 2.0;
        double centerY = getHeight() / 2.0;

        offsetX = (offsetX + centerX - rectCenterX) * zoomFactor;
        offsetY = (offsetY + centerY - rectCenterY) * zoomFactor;

        clearSelection();
        if (interactionListener != null) {
            interactionListener.onZoomChanged(nextZoom, (int) Math.round(centerX), (int) Math.round(centerY));
        } else {
            zoom = nextZoom;
            scheduleRender();
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

        if (image != null) {
            graphics.drawImage(image, 0, 0, null);
        }

        if (definition != null) {
            paintOverlay(graphics);
            paintSelection(graphics);
            paintRenderStatus(graphics);
        }
    }

    private BufferedImage renderImage(int width,
                                      int height,
                                      FractalDefinition definition,
                                      int depth,
                                      double zoom,
                                      double offsetX,
                                      double offsetY) {
        BufferedImage rendered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D imageGraphics = rendered.createGraphics();
        imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        imageGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        definition.renderer().render(imageGraphics, width, height, depth, zoom, offsetX, offsetY);
        imageGraphics.dispose();
        return rendered;
    }

    private void paintOverlay(Graphics2D graphics) {
        graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN, 13f));
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRoundRect(16, 16, 420, 106, 14, 14);
        graphics.setColor(new Color(232, 238, 255));
        graphics.drawString("左键拖拽: 平移视图", 28, 42);
        graphics.drawString("滚轮: 以鼠标为中心无极缩放", 28, 62);
        graphics.drawString("Shift + 拖拽: 框选缩放", 28, 82);
        graphics.drawString("双击: 重置视图", 28, 102);
        graphics.drawString("右键: 导出当前视图 PNG", 28, 122);
    }

    private void paintSelection(Graphics2D graphics) {
        if (selectionStart == null || selectionEnd == null) {
            return;
        }

        Rectangle area = toRectangle(selectionStart, selectionEnd);
        graphics.setColor(new Color(86, 174, 255, 50));
        graphics.fill(area);
        graphics.setColor(new Color(86, 174, 255, 220));
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.draw(area);
    }

    private void paintRenderStatus(Graphics2D graphics) {
        if (!renderInProgress) {
            return;
        }

        graphics.setColor(new Color(0, 0, 0, 160));
        graphics.fillRoundRect(getWidth() - 180, 16, 156, 40, 12, 12);
        graphics.setColor(new Color(240, 246, 255));
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 14f));
        graphics.drawString("正在渲染...", getWidth() - 156, 41);
    }

    private void notifyViewChanged() {
        if (interactionListener != null) {
            interactionListener.onViewChanged(zoom, offsetX, offsetY, mouseX, mouseY);
        }
    }

    private double clampMin(double value, double min) {
        return Math.max(min, value);
    }
}