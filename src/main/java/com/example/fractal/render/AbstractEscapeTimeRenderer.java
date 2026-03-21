package com.example.fractal.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public abstract class AbstractEscapeTimeRenderer implements FractalRenderer {

    private static final int INSIDE_COLOR_RGB = new Color(5, 8, 18).getRGB();

    private final EscapeTimeBackend backend;

    protected AbstractEscapeTimeRenderer() {
        this(new CpuEscapeTimeBackend());
    }

    protected AbstractEscapeTimeRenderer(EscapeTimeBackend backend) {
        this.backend = backend;
    }

    @Override
    public void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        graphics.drawImage(renderImage(width, height, depth, zoom, offsetX, offsetY), 0, 0, null);
    }

    @Override
    public BufferedImage renderImage(int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        EscapeTimeRenderContext context = new EscapeTimeRenderContext(
                width,
                height,
                depth,
                zoom,
                offsetX,
                offsetY,
                60 + depth * 40
        );
        int[] pixels = backend.renderPixels(this, context);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    public double mapPlaneX(int pixelX, int width, double zoom, double offsetX) {
        double scale = getBaseScale() / zoom;
        return (pixelX - width / 2.0 - offsetX) * scale / width + getCenterX();
    }

    public double mapPlaneY(int pixelY, int width, int height, double zoom, double offsetY) {
        double scale = getBaseScale() / zoom;
        return (pixelY - height / 2.0 - offsetY) * scale / width + getCenterY();
    }

    protected abstract double getBaseScale();

    protected abstract double getCenterX();

    protected abstract double getCenterY();

    protected abstract int iteratePoint(double x0, double y0, int maxIterations);

    protected final int insideColorRgb() {
        return INSIDE_COLOR_RGB;
    }

    protected final int computeColorRgb(int iterations, int maxIterations) {
        if (iterations >= maxIterations) {
            return INSIDE_COLOR_RGB;
        }

        float hue = iterations / (float) maxIterations;
        float saturation = 0.85f;
        float brightness = 0.35f + 0.65f * hue;
        return Color.getHSBColor(0.95f - hue * 0.85f, saturation, brightness).getRGB();
    }
}