package com.example.fractal.render;

public class MandelbrotRenderer extends AbstractEscapeTimeRenderer {

    @Override
    protected int iteratePoint(double x0, double y0, int maxIterations) {
        double x = 0.0;
        double y = 0.0;
        int iteration = 0;

        while (x * x + y * y <= 4.0 && iteration < maxIterations) {
            double nextX = x * x - y * y + x0;
            y = 2 * x * y + y0;
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
        return -0.5;
    }

    @Override
    protected double getCenterY() {
        return 0.0;
    }
}
