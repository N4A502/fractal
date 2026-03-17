package com.example.fractal;

import com.example.fractal.model.FractalDefinition;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

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
    private InteractionListener interactionListener;

    public FractalCanvas() {
        setPreferredSize(new Dimension(880, 820));
        setBackground(new Color(8, 12, 24));
        bindMouseInteractions();
    }

    public void setInteractionListener(InteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    public void render(FractalDefinition definition, int depth, double zoom) {
        this.definition = definition;
        this.depth = depth;
        this.zoom = zoom;
        repaint();
        notifyViewChanged();
    }

    public void applyZoom(double nextZoom, int anchorX, int anchorY) {
        if (zoom <= 0.0) {
            zoom = nextZoom;
            repaint();
            notifyViewChanged();
            return;
        }

        double centerX = getWidth() / 2.0;
        double centerY = getHeight() / 2.0;
        double zoomFactor = nextZoom / zoom;

        offsetX = anchorX - centerX - zoomFactor * (anchorX - centerX - offsetX);
        offsetY = anchorY - centerY - zoomFactor * (anchorY - centerY - offsetY);
        zoom = nextZoom;
        repaint();
        notifyViewChanged();
    }

    public void resetView() {
        offsetX = 0.0;
        offsetY = 0.0;
        dragAnchor = null;
        selectionStart = null;
        selectionEnd = null;
        selectionMode = false;
        repaint();
        notifyViewChanged();
    }

    private void bindMouseInteractions() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
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
                repaint();
                notifyViewChanged();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
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
            repaint();
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

        if (definition == null) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        if (image == null || image.getWidth() != width || image.getHeight() != height) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D imageGraphics = image.createGraphics();
        imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        imageGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        definition.renderer().render(imageGraphics, width, height, depth, zoom, offsetX, offsetY);
        imageGraphics.dispose();

        Graphics2D graphics = (Graphics2D) g;
        graphics.drawImage(image, 0, 0, null);
        paintOverlay(graphics);
        paintSelection(graphics);
    }

    private void paintOverlay(Graphics2D graphics) {
        graphics.setFont(graphics.getFont().deriveFont(Font.PLAIN, 13f));
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRoundRect(16, 16, 370, 88, 14, 14);
        graphics.setColor(new Color(232, 238, 255));
        graphics.drawString("左键拖拽: 平移视图", 28, 42);
        graphics.drawString("滚轮: 以鼠标为中心无极缩放", 28, 62);
        graphics.drawString("Shift + 拖拽: 框选缩放", 28, 82);
        graphics.drawString("双击: 重置视图", 28, 102);
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

    private void notifyViewChanged() {
        if (interactionListener != null) {
            interactionListener.onViewChanged(zoom, offsetX, offsetY, mouseX, mouseY);
        }
    }

    private double clampMin(double value, double min) {
        return Math.max(min, value);
    }
}