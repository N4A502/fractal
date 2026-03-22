package com.example.fractal.custom;

public enum CustomFormulaMode {
    MANDELBROT_LIKE("Mandelbrot-like"),
    JULIA_LIKE("Julia-like");

    private final String label;

    CustomFormulaMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
