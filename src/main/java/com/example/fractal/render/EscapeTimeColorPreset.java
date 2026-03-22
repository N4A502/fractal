package com.example.fractal.render;

public enum EscapeTimeColorPreset {
    CLASSIC("\u7ecf\u5178", new EscapeTimeColorSettings(342.0f, 306.0f, 0.85f, 0.35f, 0.65f, 0x050812, 0x6BE3FF, 0x070C1A)),
    MINIMAL("\u6781\u7b80", new EscapeTimeColorSettings(210.0f, 36.0f, 0.15f, 0.28f, 0.42f, 0x11151C, 0xC8D0D9, 0x0E1116)),
    WHITE("\u767d\u8272", new EscapeTimeColorSettings(0.0f, 0.0f, 0.0f, 0.78f, 0.18f, 0xFAFAFA, 0xE9E9E9, 0xFFFFFF)),
    BLACK("\u9ed1\u8272", new EscapeTimeColorSettings(0.0f, 0.0f, 0.0f, 0.12f, 0.78f, 0x040404, 0xF2F2F2, 0x040404)),
    OCEAN("\u6d77\u6d0b", new EscapeTimeColorSettings(220.0f, 92.0f, 0.72f, 0.25f, 0.60f, 0x07111D, 0x53B8FF, 0x07111D)),
    FIRE("\u706b\u7130", new EscapeTimeColorSettings(28.0f, 82.0f, 0.95f, 0.22f, 0.76f, 0x080202, 0xFF8A3D, 0x120503));

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
                settings.insideColorRgb(),
                settings.curveColorRgb(),
                settings.backgroundColorRgb()
        );
    }

    public boolean matches(EscapeTimeColorSettings other) {
        return Float.compare(settings.hueStartDegrees(), other.hueStartDegrees()) == 0
                && Float.compare(settings.hueRangeDegrees(), other.hueRangeDegrees()) == 0
                && Float.compare(settings.saturation(), other.saturation()) == 0
                && Float.compare(settings.brightnessFloor(), other.brightnessFloor()) == 0
                && Float.compare(settings.brightnessRange(), other.brightnessRange()) == 0
                && settings.insideColorRgb() == other.insideColorRgb()
                && settings.curveColorRgb() == other.curveColorRgb()
                && settings.backgroundColorRgb() == other.backgroundColorRgb();
    }

    @Override
    public String toString() {
        return label;
    }
}
