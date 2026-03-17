package com.example.fractal.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public abstract class AbstractEscapeTimeRenderer implements FractalRenderer {

    @Override
    public void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int maxIterations = 60 + depth * 40;
        double scale = getBaseScale() / zoom;
        double centerX = getCenterX();
        double centerY = getCenterY();

        for (int px = 0; px < width; px++) {
            for (int py = 0; py < height; py++) {
                double x0 = (px - width / 2.0 - offsetX) * scale / width + centerX;
                double y0 = (py - height / 2.0 - offsetY) * scale / width + centerY;
                int iterations = iteratePoint(x0, y0, maxIterations);
                image.setRGB(px, py, computeColor(iterations, maxIterations).getRGB());
            }
        }

        graphics.drawImage(image, 0, 0, null);
    }

    protected abstract int iteratePoint(double x0, double y0, int maxIterations);

    protected abstract double getBaseScale();

    protected abstract double getCenterX();

    protected abstract double getCenterY();

    private Color computeColor(int iterations, int maxIterations) {
        if (iterations >= maxIterations) {
            return new Color(5, 8, 18);
        }

        float hue = iterations / (float) maxIterations;
        float saturation = 0.85f;
        float brightness = 0.35f + 0.65f * hue;
        return Color.getHSBColor(0.95f - hue * 0.85f, saturation, brightness);
    }
}