package com.example.fractal.custom;

public final class CustomFormulaSettings {

    private final String formulaText;
    private final CustomFormulaMode mode;
    private final double juliaReal;
    private final double juliaImaginary;
    private final CompiledComplexExpression expression;
    private final String validationMessage;

    public CustomFormulaSettings(String formulaText,
                                 CustomFormulaMode mode,
                                 double juliaReal,
                                 double juliaImaginary,
                                 CompiledComplexExpression expression,
                                 String validationMessage) {
        this.formulaText = formulaText;
        this.mode = mode;
        this.juliaReal = juliaReal;
        this.juliaImaginary = juliaImaginary;
        this.expression = expression;
        this.validationMessage = validationMessage;
    }

    public static CustomFormulaSettings defaults() {
        return ComplexFormulaParser.compile("z^2 + c", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355);
    }

    public String formulaText() {
        return formulaText;
    }

    public CustomFormulaMode mode() {
        return mode;
    }

    public double juliaReal() {
        return juliaReal;
    }

    public double juliaImaginary() {
        return juliaImaginary;
    }

    public CompiledComplexExpression expression() {
        return expression;
    }

    public String validationMessage() {
        return validationMessage;
    }

    public boolean isValid() {
        return expression != null && validationMessage == null;
    }
}
