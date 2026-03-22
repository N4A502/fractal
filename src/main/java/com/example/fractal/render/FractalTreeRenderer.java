package com.example.fractal.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class FractalTreeRenderer implements FractalRenderer {

    @Override
    public void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        EscapeTimeColorSettings settings = EscapeTimeColorManager.getSettings();
        graphics.setColor(backgroundFrom(settings));
        graphics.fillRect(0, 0, width, height);
        drawBranch(graphics, settings, width / 2.0 + offsetX, height - 60 + offsetY, -Math.PI / 2, Math.min(width, height) * 0.20 * zoom, depth, depth);
    }

    private void drawBranch(Graphics2D graphics, EscapeTimeColorSettings settings, double x1, double y1, double angle, double length, int depth, int maxDepth) {
        if (depth <= 0 || length < 2) {
            return;
        }

        double x2 = x1 + Math.cos(angle) * length;
        double y2 = y1 + Math.sin(angle) * length;

        float progress = maxDepth <= 1 ? 1.0f : (maxDepth - depth) / (float) (maxDepth - 1);
        float hue = (settings.hueStartDegrees() - progress * settings.hueRangeDegrees()) / 360.0f;
        float brightness = Math.min(1.0f, settings.brightnessFloor() + settings.brightnessRange() * (0.35f + progress * 0.65f));
        graphics.setColor(Color.getHSBColor(hue, Math.max(0.18f, settings.saturation()), brightness));
        graphics.setStroke(new BasicStroke(Math.max(1.0f, depth * 0.75f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));

        double nextLength = length * 0.72;
        drawBranch(graphics, settings, x2, y2, angle - Math.PI / 6, nextLength, depth - 1, maxDepth);
        drawBranch(graphics, settings, x2, y2, angle + Math.PI / 5, nextLength, depth - 1, maxDepth);
    }

    private Color backgroundFrom(EscapeTimeColorSettings settings) {
        int rgb = settings.insideColorRgb();
        return new Color(((rgb >> 16) & 0xFF) / 10, ((rgb >> 8) & 0xFF) / 10, (rgb & 0xFF) / 10);
    }
}
