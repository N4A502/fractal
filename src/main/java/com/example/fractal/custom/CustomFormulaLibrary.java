package com.example.fractal.custom;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class CustomFormulaLibrary {

    public enum Group {
        STABLE("Stable"),
        BOLD("Bold"),
        FLORAL("Floral"),
        SPIRAL("Spiral");

        private final String label;

        Group(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public record Template(String name,
                           Group group,
                           String formula,
                           CustomFormulaMode mode,
                           double juliaReal,
                           double juliaImaginary) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static final List<Template> TEMPLATES = List.of(
            new Template("Classic Mandelbrot", Group.STABLE, "z^2 + c", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Cubic Mandelbrot", Group.BOLD, "z^3 + c", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Quartic Mandelbrot", Group.BOLD, "z^4 + c", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Phoenix Twist", Group.BOLD, "z^2 + c^2 + c", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Offset Quadratic", Group.STABLE, "z^2 + c + 0.25", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Imaginary Drift", Group.SPIRAL, "z^2 + c + 0.2*i", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Sine Field", Group.FLORAL, "sin(z) + c", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Cosine Field", Group.FLORAL, "cos(z) + c", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Tangent Fold", Group.SPIRAL, "tan(z) / 2 + c", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Exponential Bloom", Group.FLORAL, "exp(z) / 3 + c", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Log Spiral", Group.SPIRAL, "z^2 + log(z^2 + 1) + c", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Absolute Echo", Group.BOLD, "abs(z^2) + c", CustomFormulaMode.MANDELBROT_LIKE, 0.355, 0.355),
            new Template("Julia Seahorse", Group.STABLE, "z^2 + c", CustomFormulaMode.JULIA_LIKE, -0.74543, 0.11301),
            new Template("Julia Dendrite", Group.STABLE, "z^2 + c", CustomFormulaMode.JULIA_LIKE, -0.70176, -0.3842),
            new Template("Julia Spiral", Group.SPIRAL, "z^2 + c", CustomFormulaMode.JULIA_LIKE, -0.8, 0.156),
            new Template("Julia Airy", Group.FLORAL, "z^2 + c", CustomFormulaMode.JULIA_LIKE, 0.285, 0.01),
            new Template("Julia Dust", Group.BOLD, "z^3 + c", CustomFormulaMode.JULIA_LIKE, -0.12256, 0.74486),
            new Template("Julia Petals", Group.FLORAL, "z^3 + c", CustomFormulaMode.JULIA_LIKE, 0.37, 0.1),
            new Template("Julia Plasma", Group.BOLD, "z^4 + c", CustomFormulaMode.JULIA_LIKE, -0.54, 0.54),
            new Template("Julia Sine", Group.FLORAL, "sin(z) + c", CustomFormulaMode.JULIA_LIKE, 0.12, 0.62),
            new Template("Julia Cosine", Group.FLORAL, "cos(z) + c", CustomFormulaMode.JULIA_LIKE, -0.22, 0.75),
            new Template("Julia Tangent", Group.SPIRAL, "tan(z) / 2 + c", CustomFormulaMode.JULIA_LIKE, -0.18, 0.67),
            new Template("Julia Exponential", Group.FLORAL, "exp(z) / 3 + c", CustomFormulaMode.JULIA_LIKE, -0.1, 0.7),
            new Template("Julia Log Bloom", Group.SPIRAL, "z^2 + log(z^2 + 1) + c", CustomFormulaMode.JULIA_LIKE, 0.18, -0.63)
    );

    private CustomFormulaLibrary() {
    }

    public static List<Template> templates() {
        return TEMPLATES;
    }

    public static List<Template> templates(Group group) {
        return TEMPLATES.stream().filter(template -> template.group() == group).collect(Collectors.toList());
    }

    public static Template randomTemplate() {
        return TEMPLATES.get(ThreadLocalRandom.current().nextInt(TEMPLATES.size()));
    }

    public static Template randomTemplate(Group group) {
        List<Template> matches = templates(group);
        return matches.get(ThreadLocalRandom.current().nextInt(matches.size()));
    }
}
