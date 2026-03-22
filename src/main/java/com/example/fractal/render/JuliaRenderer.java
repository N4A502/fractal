package com.example.fractal.render;

public class JuliaRenderer extends AbstractEscapeTimeRenderer {

    @Override
    protected int iteratePoint(double x0, double y0, int maxIterations) {
        double x = x0;
        double y = y0;
        double cx = getJuliaCx();
        double cy = getJuliaCy();
        int iteration = 0;

        while (x * x + y * y <= 4.0 && iteration < maxIterations) {
            double nextX = x * x - y * y + cx;
            y = 2 * x * y + cy;
            x = nextX;
            iteration++;
        }
        return iteration;
    }

    @Override
    protected double getBaseScale() {
        return 3.2;
    }

    @Override
    protected double getCenterX() {
        return 0.0;
    }

    @Override
    protected double getCenterY() {
        return 0.0;
    }

    @Override
    protected EscapeTimeShaderProfile getShaderProfile() {
        return EscapeTimeShaderProfile.JULIA;
    }
}