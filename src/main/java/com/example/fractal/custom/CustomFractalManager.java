package com.example.fractal.custom;

public final class CustomFractalManager {

    private static volatile CustomFormulaSettings formulaSettings = CustomFormulaSettings.defaults();
    private static volatile CustomCurveSettings curveSettings = CustomCurveSettings.defaults();

    private CustomFractalManager() {
    }

    public static CustomFormulaSettings getFormulaSettings() {
        return formulaSettings;
    }

    public static void setFormulaSettings(CustomFormulaSettings settings) {
        formulaSettings = settings;
    }

    public static CustomCurveSettings getCurveSettings() {
        return curveSettings;
    }

    public static void setCurveSettings(CustomCurveSettings settings) {
        curveSettings = settings;
    }
}
