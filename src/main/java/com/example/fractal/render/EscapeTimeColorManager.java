package com.example.fractal.render;

public final class EscapeTimeColorManager {

    private static volatile EscapeTimeColorSettings settings = EscapeTimeColorPreset.CLASSIC.createSettings();

    private EscapeTimeColorManager() {
    }

    public static EscapeTimeColorSettings getSettings() {
        return settings;
    }

    public static void setSettings(EscapeTimeColorSettings nextSettings) {
        settings = nextSettings;
    }
}