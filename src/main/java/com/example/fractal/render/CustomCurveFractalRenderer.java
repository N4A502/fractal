package com.example.fractal.render;

import com.example.fractal.custom.CurvePoint;
import com.example.fractal.custom.CustomCurveSettings;
import com.example.fractal.custom.CustomFractalManager;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class CustomCurveFractalRenderer implements FractalRenderer {

    private static final double MIN_RENDER_SCALE = 6.0;

    @Override
    public void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        EscapeTimeColorSettings colors = EscapeTimeColorManager.getSettings();
        graphics.setColor(colors.backgroundColor());
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        CustomCurveSettings settings = CustomFractalManager.getCurveSettings();
        if (!settings.hasContour()) {
            graphics.setColor(colors.gradientColor(0.65f));
            String message = settings.validationMessage() == null ? "Import an image to extract a contour." : settings.validationMessage();
            graphics.drawString(message, 24, 32);
            return;
        }

        AffineTransform root = new AffineTransform();
        double scale = Math.min(width, height) * 0.42 * zoom;
        root.translate(width / 2.0 + offsetX, height / 2.0 + offsetY);
        root.scale(scale, scale);
        drawRecursive(graphics, settings, root, depth, depth);
    }

    private void drawRecursive(Graphics2D graphics,
                               CustomCurveSettings settings,
                               AffineTransform transform,
                               int depth,
                               int maxDepth) {
        if (RenderCancellation.isCancelledCurrentThread() || depth <= 0) {
            return;
        }

        List<CurvePoint> contour = settings.contour();
        Path2D path = buildPath(contour, transform);
        float progress = maxDepth <= 1 ? 1.0f : (maxDepth - depth) / (float) (maxDepth - 1);
        EscapeTimeColorSettings colors = EscapeTimeColorManager.getSettings();
        graphics.setColor(colors.gradientColor(progress));
        graphics.setStroke(new BasicStroke(Math.max(1.0f, depth * 0.65f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(path);

        double scale = Math.hypot(transform.getScaleX(), transform.getShearX());
        if (depth <= 1 || scale < MIN_RENDER_SCALE) {
            return;
        }

        List<Anchor> anchors = selectAnchors(contour, settings.branchCount());
        for (Anchor anchor : anchors) {
            AffineTransform child = new AffineTransform(transform);
            child.translate(anchor.x(), anchor.y());
            child.rotate(anchor.angle());
            child.scale(settings.childScale(), settings.childScale());
            drawRecursive(graphics, settings, child, depth - 1, maxDepth);
        }
    }

    private Path2D buildPath(List<CurvePoint> contour, AffineTransform transform) {
        Path2D path = new Path2D.Double();
        boolean first = true;
        for (CurvePoint point : contour) {
            java.awt.geom.Point2D transformed = transform.transform(new java.awt.geom.Point2D.Double(point.x(), point.y()), null);
            if (first) {
                path.moveTo(transformed.getX(), transformed.getY());
                first = false;
            } else {
                path.lineTo(transformed.getX(), transformed.getY());
            }
        }
        if (!contour.isEmpty()) {
            path.closePath();
        }
        return path;
    }

    private List<Anchor> selectAnchors(List<CurvePoint> contour, int count) {
        List<Anchor> anchors = new ArrayList<>();
        if (contour.size() < 3 || count <= 0) {
            return anchors;
        }
        int step = Math.max(1, contour.size() / count);
        for (int index = 0; index < contour.size() && anchors.size() < count; index += step) {
            CurvePoint previous = contour.get((index - 1 + contour.size()) % contour.size());
            CurvePoint current = contour.get(index);
            CurvePoint next = contour.get((index + 1) % contour.size());
            double angle = Math.atan2(next.y() - previous.y(), next.x() - previous.x());
            anchors.add(new Anchor(current.x(), current.y(), angle));
        }
        return anchors;
    }

    @Override
    public String backendDescription() {
        return "Curve CPU";
    }

    private record Anchor(double x, double y, double angle) {
    }
}
