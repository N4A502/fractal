package com.example.fractal.render;

public class BurningShipRenderer extends AbstractEscapeTimeRenderer {

    @Override
    protected int iteratePoint(double x0, double y0, int maxIterations) {
        double x = 0.0;
        double y = 0.0;
        int iteration = 0;

        while (x * x + y * y <= 4.0 && iteration < maxIterations) {
            double absX = Math.abs(x);
            double absY = Math.abs(y);
            double nextX = absX * absX - absY * absY + x0;
            y = 2 * absX * absY + y0;
            x = nextX;
            iteration++;
        }
        return iteration;
    }

    @Override
    protected double getBaseScale() {
        return 3.4;
    }

    @Override
    protected double getCenterX() {
        return -0.45;
    }

    @Override
    protected double getCenterY() {
        return -0.5;
    }
}
