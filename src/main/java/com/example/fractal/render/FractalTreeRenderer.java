package com.example.fractal.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class FractalTreeRenderer implements FractalRenderer {

    private static final double MIN_BRANCH_PIXELS = 1.2;

    @Override
    public void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        EscapeTimeColorSettings settings = EscapeTimeColorManager.getSettings();
        graphics.setColor(settings.backgroundColor());
        graphics.fillRect(0, 0, width, height);
        drawBranch(graphics, settings, width / 2.0 + offsetX, height - 60 + offsetY, -Math.PI / 2, Math.min(width, height) * 0.20 * zoom, depth, depth);
    }

    private void drawBranch(Graphics2D graphics, EscapeTimeColorSettings settings, double x1, double y1, double angle, double length, int depth, int maxDepth) {
        if (depth <= 0 || length < MIN_BRANCH_PIXELS) {
            return;
        }

        double x2 = x1 + Math.cos(angle) * length;
        double y2 = y1 + Math.sin(angle) * length;

        float progress = maxDepth <= 1 ? 1.0f : (maxDepth - depth) / (float) (maxDepth - 1);
        Color start = settings.gradientColor(progress);
        Color end = settings.insideColor();
        graphics.setColor(blend(start, end, progress * 0.30f));
        graphics.setStroke(new BasicStroke(Math.max(1.0f, depth * 0.75f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));

        double nextLength = length * 0.72;
        drawBranch(graphics, settings, x2, y2, angle - Math.PI / 6, nextLength, depth - 1, maxDepth);
        drawBranch(graphics, settings, x2, y2, angle + Math.PI / 5, nextLength, depth - 1, maxDepth);
    }

    private Color blend(Color start, Color end, float ratio) {
        float clamped = Math.max(0.0f, Math.min(1.0f, ratio));
        int red = Math.round(start.getRed() + (end.getRed() - start.getRed()) * clamped);
        int green = Math.round(start.getGreen() + (end.getGreen() - start.getGreen()) * clamped);
        int blue = Math.round(start.getBlue() + (end.getBlue() - start.getBlue()) * clamped);
        return new Color(red, green, blue);
    }
}
