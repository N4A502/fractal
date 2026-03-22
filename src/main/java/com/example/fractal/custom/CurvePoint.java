package com.example.fractal.custom;

public final class CurvePoint {
    private final double x;
    private final double y;

    public CurvePoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }
}
