package com.example.fractal.render;

public enum EscapeTimeColorPreset {
    CLASSIC("Classic", new EscapeTimeColorSettings(342.0f, 306.0f, 0.85f, 0.35f, 0.65f, 0x050812)),
    MINIMAL("Minimal", new EscapeTimeColorSettings(210.0f, 36.0f, 0.15f, 0.28f, 0.42f, 0x11151c)),
    WHITE("White", new EscapeTimeColorSettings(0.0f, 0.0f, 0.0f, 0.78f, 0.18f, 0xFAFAFA)),
    BLACK("Black", new EscapeTimeColorSettings(0.0f, 0.0f, 0.0f, 0.12f, 0.78f, 0x040404)),
    OCEAN("Ocean", new EscapeTimeColorSettings(220.0f, 92.0f, 0.72f, 0.25f, 0.60f, 0x07111d)),
    FIRE("Fire", new EscapeTimeColorSettings(28.0f, 82.0f, 0.95f, 0.22f, 0.76f, 0x080202));

    private final String label;
    private final EscapeTimeColorSettings settings;

    EscapeTimeColorPreset(String label, EscapeTimeColorSettings settings) {
        this.label = label;
        this.settings = settings;
    }

    public EscapeTimeColorSettings createSettings() {
        return new EscapeTimeColorSettings(
                settings.hueStartDegrees(),
                settings.hueRangeDegrees(),
                settings.saturation(),
                settings.brightnessFloor(),
                settings.brightnessRange(),
                settings.insideColorRgb()
        );
    }

    @Override
    public String toString() {
        return label;
    }
}