package com.example.fractal.custom;

public final class Complex {

    public static final Complex ZERO = new Complex(0.0, 0.0);
    public static final Complex ONE = new Complex(1.0, 0.0);
    public static final Complex I = new Complex(0.0, 1.0);

    private final double real;
    private final double imaginary;

    public Complex(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    public double real() {
        return real;
    }

    public double imaginary() {
        return imaginary;
    }

    public Complex add(Complex other) {
        return new Complex(real + other.real, imaginary + other.imaginary);
    }

    public Complex subtract(Complex other) {
        return new Complex(real - other.real, imaginary - other.imaginary);
    }

    public Complex multiply(Complex other) {
        return new Complex(
                real * other.real - imaginary * other.imaginary,
                real * other.imaginary + imaginary * other.real
        );
    }

    public Complex divide(Complex other) {
        double denominator = other.real * other.real + other.imaginary * other.imaginary;
        if (denominator == 0.0) {
            return new Complex(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        }
        return new Complex(
                (real * other.real + imaginary * other.imaginary) / denominator,
                (imaginary * other.real - real * other.imaginary) / denominator
        );
    }

    public Complex negate() {
        return new Complex(-real, -imaginary);
    }

    public Complex exp() {
        double expReal = Math.exp(real);
        return new Complex(expReal * Math.cos(imaginary), expReal * Math.sin(imaginary));
    }

    public Complex log() {
        return new Complex(Math.log(abs()), Math.atan2(imaginary, real));
    }

    public Complex sin() {
        return new Complex(
                Math.sin(real) * Math.cosh(imaginary),
                Math.cos(real) * Math.sinh(imaginary)
        );
    }

    public Complex cos() {
        return new Complex(
                Math.cos(real) * Math.cosh(imaginary),
                -Math.sin(real) * Math.sinh(imaginary)
        );
    }

    public Complex tan() {
        return sin().divide(cos());
    }

    public Complex pow(Complex exponent) {
        if (real == 0.0 && imaginary == 0.0) {
            return ZERO;
        }
        return log().multiply(exponent).exp();
    }

    public Complex absValue() {
        return new Complex(abs(), 0.0);
    }

    public double abs() {
        return Math.hypot(real, imaginary);
    }

    public boolean isFinite() {
        return Double.isFinite(real) && Double.isFinite(imaginary);
    }
}
