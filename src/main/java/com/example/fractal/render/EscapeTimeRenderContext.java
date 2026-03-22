package com.example.fractal.render;

public class EscapeTimeRenderContext {

    private final int width;
    private final int height;
    private final int depth;
    private final double zoom;
    private final double offsetX;
    private final double offsetY;
    private final int maxIterations;
    private final long requestSequence;

    public EscapeTimeRenderContext(int width, int height, int depth, double zoom, double offsetX, double offsetY, int maxIterations, long requestSequence) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.zoom = zoom;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.maxIterations = maxIterations;
        this.requestSequence = requestSequence;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    public double zoom() {
        return zoom;
    }

    public double offsetX() {
        return offsetX;
    }

    public double offsetY() {
        return offsetY;
    }

    public int maxIterations() {
        return maxIterations;
    }

    public long requestSequence() {
        return requestSequence;
    }
}