package com.example.fractal.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

public class KochSnowflakeRenderer implements FractalRenderer {

    @Override
    public void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        EscapeTimeColorSettings settings = EscapeTimeColorManager.getSettings();
        graphics.setColor(settings.backgroundColor());
        graphics.fillRect(0, 0, width, height);

        graphics.setStroke(new BasicStroke(1.4f));
        graphics.setColor(settings.gradientColor(0.55f));

        double size = Math.min(width, height) * 0.38 * zoom;
        double centerX = width / 2.0 + offsetX;
        double centerY = height / 2.0 + size * 0.1 + offsetY;

        Point a = new Point(centerX, centerY - size);
        Point b = new Point(centerX - size * Math.sin(Math.PI / 3), centerY + size / 2.0);
        Point c = new Point(centerX + size * Math.sin(Math.PI / 3), centerY + size / 2.0);

        Path2D path = new Path2D.Double();
        path.moveTo(a.getX(), a.getY());
        drawSegment(path, a, b, depth);
        drawSegment(path, b, c, depth);
        drawSegment(path, c, a, depth);
        path.closePath();

        graphics.draw(path);
    }

    private void drawSegment(Path2D path, Point start, Point end, int depth) {
        if (depth <= 1) {
            path.lineTo(end.getX(), end.getY());
            return;
        }

        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();

        Point p1 = new Point(start.getX() + dx / 3.0, start.getY() + dy / 3.0);
        Point p2 = new Point(start.getX() + dx * 2.0 / 3.0, start.getY() + dy * 2.0 / 3.0);

        double angle = Math.PI / 3.0;
        double peakX = p1.getX() + (dx / 3.0) * Math.cos(-angle) - (dy / 3.0) * Math.sin(-angle);
        double peakY = p1.getY() + (dx / 3.0) * Math.sin(-angle) + (dy / 3.0) * Math.cos(-angle);
        Point peak = new Point(peakX, peakY);

        drawSegment(path, start, p1, depth - 1);
        drawSegment(path, p1, peak, depth - 1);
        drawSegment(path, peak, p2, depth - 1);
        drawSegment(path, p2, end, depth - 1);
    }


    private static final class Point {
        private final double x;
        private final double y;

        private Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        private double getX() {
            return x;
        }

        private double getY() {
            return y;
        }
    }
}
