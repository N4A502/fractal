package com.example.fractal.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class FractalTreeRenderer implements FractalRenderer {

    @Override
    public void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        graphics.setColor(new Color(5, 10, 20));
        graphics.fillRect(0, 0, width, height);
        drawBranch(graphics, width / 2.0 + offsetX, height - 60 + offsetY, -Math.PI / 2, Math.min(width, height) * 0.20 * zoom, depth);
    }

    private void drawBranch(Graphics2D graphics, double x1, double y1, double angle, double length, int depth) {
        if (depth <= 0 || length < 2) {
            return;
        }

        double x2 = x1 + Math.cos(angle) * length;
        double y2 = y1 + Math.sin(angle) * length;

        float progress = depth / 10.0f;
        graphics.setColor(Color.getHSBColor(0.22f + progress * 0.12f, 0.7f, 0.45f + progress * 0.35f));
        graphics.setStroke(new BasicStroke(Math.max(1.0f, depth * 0.75f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));

        double nextLength = length * 0.72;
        drawBranch(graphics, x2, y2, angle - Math.PI / 6, nextLength, depth - 1);
        drawBranch(graphics, x2, y2, angle + Math.PI / 5, nextLength, depth - 1);
    }
}