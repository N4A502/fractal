package com.example.fractal.custom;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public final class CurveContourExtractor {

    private static final int[][] NEIGHBOR_OFFSETS = {
            {-1, -1}, {0, -1}, {1, -1}, {1, 0},
            {1, 1}, {0, 1}, {-1, 1}, {-1, 0}
    };

    private CurveContourExtractor() {
    }

    public static CustomCurveSettings extract(File file,
                                              int threshold,
                                              double simplifyTolerance,
                                              int branchCount,
                                              double childScale) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                return new CustomCurveSettings(file.getAbsolutePath(), threshold, simplifyTolerance, branchCount, childScale, List.of(), "Unsupported image format.");
            }
            boolean[][] mask = buildMask(image, threshold);
            boolean[][] component = largestComponent(mask);
            List<CurvePoint> contour = traceContour(component);
            if (contour.size() < 8) {
                return new CustomCurveSettings(file.getAbsolutePath(), threshold, simplifyTolerance, branchCount, childScale, List.of(), "Could not extract a usable contour.");
            }
            List<CurvePoint> simplified = simplifyClosed(contour, Math.max(1.0, simplifyTolerance));
            List<CurvePoint> normalized = normalize(simplified);
            return new CustomCurveSettings(file.getAbsolutePath(), threshold, simplifyTolerance, branchCount, childScale, normalized, null);
        } catch (IOException ex) {
            return new CustomCurveSettings(file.getAbsolutePath(), threshold, simplifyTolerance, branchCount, childScale, List.of(), ex.getMessage());
        }
    }

    private static boolean[][] buildMask(BufferedImage image, int threshold) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] mask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;
                int luminance = (red * 299 + green * 587 + blue * 114) / 1000;
                mask[y][x] = alpha > 20 && luminance <= threshold;
            }
        }
        return mask;
    }

    private static boolean[][] largestComponent(boolean[][] mask) {
        int height = mask.length;
        int width = height == 0 ? 0 : mask[0].length;
        boolean[][] visited = new boolean[height][width];
        List<int[]> best = List.of();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x] || visited[y][x]) {
                    continue;
                }
                List<int[]> component = new ArrayList<>();
                Deque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{x, y});
                visited[y][x] = true;
                while (!queue.isEmpty()) {
                    int[] point = queue.removeFirst();
                    component.add(point);
                    for (int[] offset : NEIGHBOR_OFFSETS) {
                        int nextX = point[0] + offset[0];
                        int nextY = point[1] + offset[1];
                        if (nextX < 0 || nextY < 0 || nextY >= height || nextX >= width) {
                            continue;
                        }
                        if (!mask[nextY][nextX] || visited[nextY][nextX]) {
                            continue;
                        }
                        visited[nextY][nextX] = true;
                        queue.addLast(new int[]{nextX, nextY});
                    }
                }
                if (component.size() > best.size()) {
                    best = component;
                }
            }
        }
        boolean[][] result = new boolean[height][width];
        for (int[] point : best) {
            result[point[1]][point[0]] = true;
        }
        return result;
    }

    private static List<CurvePoint> traceContour(boolean[][] component) {
        int height = component.length;
        int width = height == 0 ? 0 : component[0].length;
        int startX = -1;
        int startY = -1;
        for (int y = 0; y < height && startX < 0; y++) {
            for (int x = 0; x < width; x++) {
                if (component[y][x] && isBoundary(component, x, y)) {
                    startX = x;
                    startY = y;
                    break;
                }
            }
        }
        if (startX < 0) {
            return List.of();
        }

        List<CurvePoint> contour = new ArrayList<>();
        int currentX = startX;
        int currentY = startY;
        int backtrackX = startX - 1;
        int backtrackY = startY;
        int guard = Math.max(64, width * height * 4);
        do {
            contour.add(new CurvePoint(currentX, currentY));
            int startIndex = neighborIndex(currentX, currentY, backtrackX, backtrackY);
            boolean moved = false;
            for (int step = 1; step <= 8; step++) {
                int candidateIndex = (startIndex + step) % 8;
                int nextX = currentX + NEIGHBOR_OFFSETS[candidateIndex][0];
                int nextY = currentY + NEIGHBOR_OFFSETS[candidateIndex][1];
                if (nextX < 0 || nextY < 0 || nextY >= height || nextX >= width || !component[nextY][nextX]) {
                    continue;
                }
                int previousIndex = (candidateIndex + 7) % 8;
                backtrackX = currentX + NEIGHBOR_OFFSETS[previousIndex][0];
                backtrackY = currentY + NEIGHBOR_OFFSETS[previousIndex][1];
                currentX = nextX;
                currentY = nextY;
                moved = true;
                break;
            }
            if (!moved) {
                break;
            }
            guard--;
        } while (guard > 0 && (currentX != startX || currentY != startY || backtrackX != startX - 1 || backtrackY != startY));

        return deduplicate(contour);
    }

    private static List<CurvePoint> deduplicate(List<CurvePoint> points) {
        List<CurvePoint> result = new ArrayList<>();
        CurvePoint previous = null;
        for (CurvePoint point : points) {
            if (previous == null || previous.x() != point.x() || previous.y() != point.y()) {
                result.add(point);
                previous = point;
            }
        }
        return result;
    }

    private static int neighborIndex(int centerX, int centerY, int pointX, int pointY) {
        for (int i = 0; i < NEIGHBOR_OFFSETS.length; i++) {
            if (centerX + NEIGHBOR_OFFSETS[i][0] == pointX && centerY + NEIGHBOR_OFFSETS[i][1] == pointY) {
                return i;
            }
        }
        return 7;
    }

    private static boolean isBoundary(boolean[][] component, int x, int y) {
        for (int[] offset : NEIGHBOR_OFFSETS) {
            int nextX = x + offset[0];
            int nextY = y + offset[1];
            if (nextX < 0 || nextY < 0 || nextY >= component.length || nextX >= component[0].length || !component[nextY][nextX]) {
                return true;
            }
        }
        return false;
    }

    private static List<CurvePoint> simplifyClosed(List<CurvePoint> points, double epsilon) {
        if (points.size() < 4) {
            return points;
        }
        int splitIndex = 0;
        double maxDistance = -1.0;
        CurvePoint first = points.get(0);
        for (int i = 1; i < points.size(); i++) {
            CurvePoint point = points.get(i);
            double distance = distanceSquared(first, point);
            if (distance > maxDistance) {
                maxDistance = distance;
                splitIndex = i;
            }
        }

        List<CurvePoint> open = new ArrayList<>(points.size() + 1);
        open.addAll(points.subList(splitIndex, points.size()));
        open.addAll(points.subList(0, splitIndex + 1));
        List<CurvePoint> simplified = simplifyOpen(open, epsilon);
        if (!simplified.isEmpty() && !samePoint(simplified.get(0), simplified.get(simplified.size() - 1))) {
            simplified.add(simplified.get(0));
        }
        return simplified;
    }

    private static List<CurvePoint> simplifyOpen(List<CurvePoint> points, double epsilon) {
        if (points.size() <= 2) {
            return new ArrayList<>(points);
        }
        int index = -1;
        double maxDistance = -1.0;
        CurvePoint start = points.get(0);
        CurvePoint end = points.get(points.size() - 1);
        for (int i = 1; i < points.size() - 1; i++) {
            double distance = perpendicularDistance(points.get(i), start, end);
            if (distance > maxDistance) {
                maxDistance = distance;
                index = i;
            }
        }
        if (maxDistance <= epsilon) {
            List<CurvePoint> simplified = new ArrayList<>();
            simplified.add(start);
            simplified.add(end);
            return simplified;
        }
        List<CurvePoint> left = simplifyOpen(points.subList(0, index + 1), epsilon);
        List<CurvePoint> right = simplifyOpen(points.subList(index, points.size()), epsilon);
        left.remove(left.size() - 1);
        left.addAll(right);
        return left;
    }

    private static double perpendicularDistance(CurvePoint point, CurvePoint lineStart, CurvePoint lineEnd) {
        double dx = lineEnd.x() - lineStart.x();
        double dy = lineEnd.y() - lineStart.y();
        if (dx == 0.0 && dy == 0.0) {
            return Math.hypot(point.x() - lineStart.x(), point.y() - lineStart.y());
        }
        double numerator = Math.abs(dy * point.x() - dx * point.y() + lineEnd.x() * lineStart.y() - lineEnd.y() * lineStart.x());
        double denominator = Math.hypot(dx, dy);
        return numerator / denominator;
    }

    private static double distanceSquared(CurvePoint a, CurvePoint b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return dx * dx + dy * dy;
    }

    private static boolean samePoint(CurvePoint a, CurvePoint b) {
        return a.x() == b.x() && a.y() == b.y();
    }

    private static List<CurvePoint> normalize(List<CurvePoint> points) {
        double minX = points.stream().min(Comparator.comparingDouble(CurvePoint::x)).orElseThrow().x();
        double maxX = points.stream().max(Comparator.comparingDouble(CurvePoint::x)).orElseThrow().x();
        double minY = points.stream().min(Comparator.comparingDouble(CurvePoint::y)).orElseThrow().y();
        double maxY = points.stream().max(Comparator.comparingDouble(CurvePoint::y)).orElseThrow().y();
        double width = Math.max(1.0, maxX - minX);
        double height = Math.max(1.0, maxY - minY);
        double scale = Math.max(width, height);
        List<CurvePoint> normalized = new ArrayList<>(points.size());
        for (CurvePoint point : points) {
            double x = ((point.x() - minX) / scale) - width / scale / 2.0;
            double y = ((point.y() - minY) / scale) - height / scale / 2.0;
            normalized.add(new CurvePoint(x, y));
        }
        return normalized;
    }
}
