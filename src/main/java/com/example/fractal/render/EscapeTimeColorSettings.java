package com.example.fractal.render;

import java.awt.Color;

public final class EscapeTimeColorSettings {

    private final float hueStartDegrees;
    private final float hueRangeDegrees;
    private final float saturation;
    private final float brightnessFloor;
    private final float brightnessRange;
    private final int insideColorRgb;

    public EscapeTimeColorSettings(float hueStartDegrees,
                                   float hueRangeDegrees,
                                   float saturation,
                                   float brightnessFloor,
                                   float brightnessRange,
                                   int insideColorRgb) {
        this.hueStartDegrees = clamp(hueStartDegrees, 0.0f, 360.0f);
        this.hueRangeDegrees = clamp(hueRangeDegrees, 0.0f, 360.0f);
        this.saturation = clamp(saturation, 0.0f, 1.0f);
        this.brightnessFloor = clamp(brightnessFloor, 0.0f, 1.0f);
        this.brightnessRange = clamp(brightnessRange, 0.0f, 1.0f);
        this.insideColorRgb = insideColorRgb & 0xFFFFFF;
    }

    public float hueStartDegrees() {
        return hueStartDegrees;
    }

    public float hueRangeDegrees() {
        return hueRangeDegrees;
    }

    public float saturation() {
        return saturation;
    }

    public float brightnessFloor() {
        return brightnessFloor;
    }

    public float brightnessRange() {
        return brightnessRange;
    }

    public int insideColorRgb() {
        return insideColorRgb;
    }

    public int computeColorRgb(int iterations, int maxIterations) {
        if (iterations >= maxIterations) {
            return 0xFF000000 | insideColorRgb;
        }

        float progress = iterations / (float) maxIterations;
        float hue = wrapHue((hueStartDegrees - progress * hueRangeDegrees) / 360.0f);
        float brightness = clamp(brightnessFloor + brightnessRange * progress, 0.0f, 1.0f);
        return 0xFF000000 | (Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF);
    }

    private float wrapHue(float hue) {
        float wrapped = hue % 1.0f;
        return wrapped < 0.0f ? wrapped + 1.0f : wrapped;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}