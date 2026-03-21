package com.example.fractal.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.IntStream;

public abstract class AbstractEscapeTimeRenderer implements FractalRenderer {

    private static final int INSIDE_COLOR_RGB = new Color(5, 8, 18).getRGB();

    @Override
    public void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        graphics.drawImage(renderImage(width, height, depth, zoom, offsetX, offsetY), 0, 0, null);
    }

    @Override
    public BufferedImage renderImage(int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        int maxIterations = 60 + depth * 40;
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, INSIDE_COLOR_RGB);

        IntStream.range(0, height).parallel().forEach(py -> {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            int rowOffset = py * width;
            for (int px = 0; px < width; px++) {
                if ((px & 63) == 0 && Thread.currentThread().isInterrupted()) {
                    return;
                }

                double x0 = mapPlaneX(px, width, zoom, offsetX);
                double y0 = mapPlaneY(py, width, height, zoom, offsetY);
                int iterations = iteratePoint(x0, y0, maxIterations);
                pixels[rowOffset + px] = computeColorRgb(iterations, maxIterations);
            }
        });

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

    protected abstract int iteratePoint(double x0, double y0, int maxIterations);

    protected abstract double getBaseScale();

    protected abstract double getCenterX();

    protected abstract double getCenterY();

    private int computeColorRgb(int iterations, int maxIterations) {
        if (iterations >= maxIterations) {
            return INSIDE_COLOR_RGB;
        }

        float hue = iterations / (float) maxIterations;
        float saturation = 0.85f;
        float brightness = 0.35f + 0.65f * hue;
        return Color.getHSBColor(0.95f - hue * 0.85f, saturation, brightness).getRGB();
    }
}