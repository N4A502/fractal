package com.example.fractal.model;

public class FractalViewState {

    private final FractalDefinition definition;
    private final int depth;
    private final double zoom;
    private final double offsetX;
    private final double offsetY;

    public FractalViewState(FractalDefinition definition, int depth, double zoom, double offsetX, double offsetY) {
        this.definition = definition;
        this.depth = depth;
        this.zoom = zoom;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public FractalDefinition definition() {
        return definition;
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

    public FractalViewState withDefinition(FractalDefinition nextDefinition, int nextDepth, double nextZoom) {
        return new FractalViewState(nextDefinition, nextDepth, nextZoom, offsetX, offsetY);
    }

    public FractalViewState withZoomAndOffset(double nextZoom, double nextOffsetX, double nextOffsetY) {
        return new FractalViewState(definition, depth, nextZoom, nextOffsetX, nextOffsetY);
    }

    public FractalViewState withOffset(double nextOffsetX, double nextOffsetY) {
        return new FractalViewState(definition, depth, zoom, nextOffsetX, nextOffsetY);
    }

    public FractalViewState resetOffset() {
        return new FractalViewState(definition, depth, zoom, 0.0, 0.0);
    }

    public boolean hasDefinition() {
        return definition != null;
    }
}