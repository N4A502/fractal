package com.example.fractal.render;

import com.example.fractal.model.FractalViewState;

public class RenderRequest {

    private final FractalViewState viewState;
    private final int width;
    private final int height;

    public RenderRequest(FractalViewState viewState, int width, int height) {
        this.viewState = viewState;
        this.width = width;
        this.height = height;
    }

    public static RenderRequest of(FractalViewState viewState, int width, int height) {
        return new RenderRequest(viewState, width, height);
    }

    public FractalViewState viewState() {
        return viewState;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}