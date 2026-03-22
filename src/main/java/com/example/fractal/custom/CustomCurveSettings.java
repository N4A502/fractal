package com.example.fractal.custom;

import java.util.List;

public final class CustomCurveSettings {

    private final String imagePath;
    private final int threshold;
    private final double simplifyTolerance;
    private final int branchCount;
    private final double childScale;
    private final List<CurvePoint> contour;
    private final String validationMessage;

    public CustomCurveSettings(String imagePath,
                               int threshold,
                               double simplifyTolerance,
                               int branchCount,
                               double childScale,
                               List<CurvePoint> contour,
                               String validationMessage) {
        this.imagePath = imagePath;
        this.threshold = threshold;
        this.simplifyTolerance = simplifyTolerance;
        this.branchCount = branchCount;
        this.childScale = childScale;
        this.contour = contour == null ? List.of() : List.copyOf(contour);
        this.validationMessage = validationMessage;
    }

    public static CustomCurveSettings defaults() {
        return new CustomCurveSettings(null, 160, 6.0, 4, 0.52, List.of(), "Import an image to extract a curve.");
    }

    public String imagePath() {
        return imagePath;
    }

    public int threshold() {
        return threshold;
    }

    public double simplifyTolerance() {
        return simplifyTolerance;
    }

    public int branchCount() {
        return branchCount;
    }

    public double childScale() {
        return childScale;
    }

    public List<CurvePoint> contour() {
        return contour;
    }

    public String validationMessage() {
        return validationMessage;
    }

    public boolean hasContour() {
        return !contour.isEmpty();
    }
}
