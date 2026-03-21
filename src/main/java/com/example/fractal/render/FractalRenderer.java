package com.example.fractal.render;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public interface FractalRenderer {
    void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY);

    default BufferedImage renderImage(int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        BufferedImage rendered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D imageGraphics = rendered.createGraphics();
        imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        imageGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        render(imageGraphics, width, height, depth, zoom, offsetX, offsetY);
        imageGraphics.dispose();
        return rendered;
    }

    default String backendDescription() {
        return "Java2D";
    }
}