package com.example.fractal.model;

import com.example.fractal.render.FractalRenderer;

public class FractalDefinition {

    private final String name;
    private final String category;
    private final String description;
    private final int minDepth;
    private final int maxDepth;
    private final int defaultDepth;
    private final int defaultZoom;
    private final FractalRenderer renderer;

    public FractalDefinition(String name,
                             String category,
                             String description,
                             int minDepth,
                             int maxDepth,
                             int defaultDepth,
                             int defaultZoom,
                             FractalRenderer renderer) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
        this.defaultDepth = defaultDepth;
        this.defaultZoom = defaultZoom;
        this.renderer = renderer;
    }

    public String name() {
        return name;
    }

    public String category() {
        return category;
    }

    public String description() {
        return description;
    }

    public int minDepth() {
        return minDepth;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public int defaultDepth() {
        return defaultDepth;
    }

    public int defaultZoom() {
        return defaultZoom;
    }

    public FractalRenderer renderer() {
        return renderer;
    }

    @Override
    public String toString() {
        return name;
    }
}