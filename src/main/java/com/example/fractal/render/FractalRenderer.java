package com.example.fractal.render;

import java.awt.Graphics2D;

public interface FractalRenderer {
    void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY);
}