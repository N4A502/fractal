package com.example.fractal.custom;

public final class CompiledComplexExpression {

    @FunctionalInterface
    public interface Evaluator {
        Complex evaluate(Complex z, Complex c);
    }

    private final Evaluator evaluator;
    private final String normalizedFormula;

    public CompiledComplexExpression(Evaluator evaluator, String normalizedFormula) {
        this.evaluator = evaluator;
        this.normalizedFormula = normalizedFormula;
    }

    public Complex evaluate(Complex z, Complex c) {
        return evaluator.evaluate(z, c);
    }

    public String normalizedFormula() {
        return normalizedFormula;
    }
}
