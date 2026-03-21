package com.example.fractal.render;

public interface EscapeTimeBackend {
    int[] renderPixels(AbstractEscapeTimeRenderer renderer, EscapeTimeRenderContext context);
}