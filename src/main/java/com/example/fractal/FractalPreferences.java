package com.example.fractal;

import com.example.fractal.render.EscapeTimeColorPreset;
import com.example.fractal.render.EscapeTimeColorSettings;

import java.util.prefs.Preferences;

public final class FractalPreferences {

    private static final String NODE = "com/example/fractal";
    private static final String KEY_FRACTAL_NAME = "fractalName";
    private static final String KEY_DEPTH = "depth";
    private static final String KEY_ZOOM = "zoom";
    private static final String KEY_OFFSET_X = "offsetX";
    private static final String KEY_OFFSET_Y = "offsetY";
    private static final String KEY_VIEW_WIDTH = "viewWidth";
    private static final String KEY_VIEW_HEIGHT = "viewHeight";
    private static final String KEY_HUE_START = "hueStart";
    private static final String KEY_HUE_RANGE = "hueRange";
    private static final String KEY_SATURATION = "saturation";
    private static final String KEY_BRIGHTNESS_FLOOR = "brightnessFloor";
    private static final String KEY_BRIGHTNESS_RANGE = "brightnessRange";
    private static final String KEY_INSIDE_COLOR = "insideColor";
    private static final String KEY_CURVE_COLOR = "curveColor";
    private static final String KEY_BACKGROUND_COLOR = "backgroundColor";
    private static final String KEY_SIDEBAR_VISIBLE = "sidebarVisible";
    private static final String KEY_STAGE_WIDTH = "stageWidth";
    private static final String KEY_STAGE_HEIGHT = "stageHeight";
    private static final String KEY_STAGE_MAXIMIZED = "stageMaximized";

    private final Preferences preferences;

    public FractalPreferences() {
        this.preferences = Preferences.userRoot().node(NODE);
    }

    public SavedConfiguration load() {
        EscapeTimeColorSettings defaultPalette = EscapeTimeColorPreset.CLASSIC.createSettings();
        return new SavedConfiguration(
                preferences.get(KEY_FRACTAL_NAME, null),
                preferences.getInt(KEY_DEPTH, 6),
                preferences.getDouble(KEY_ZOOM, 1.0),
                preferences.getDouble(KEY_OFFSET_X, 0.0),
                preferences.getDouble(KEY_OFFSET_Y, 0.0),
                preferences.getInt(KEY_VIEW_WIDTH, 1280),
                preferences.getInt(KEY_VIEW_HEIGHT, 720),
                new EscapeTimeColorSettings(
                        preferences.getFloat(KEY_HUE_START, defaultPalette.hueStartDegrees()),
                        preferences.getFloat(KEY_HUE_RANGE, defaultPalette.hueRangeDegrees()),
                        preferences.getFloat(KEY_SATURATION, defaultPalette.saturation()),
                        preferences.getFloat(KEY_BRIGHTNESS_FLOOR, defaultPalette.brightnessFloor()),
                        preferences.getFloat(KEY_BRIGHTNESS_RANGE, defaultPalette.brightnessRange()),
                        preferences.getInt(KEY_INSIDE_COLOR, defaultPalette.insideColorRgb()),
                        preferences.getInt(KEY_CURVE_COLOR, defaultPalette.curveColorRgb()),
                        preferences.getInt(KEY_BACKGROUND_COLOR, defaultPalette.backgroundColorRgb())
                ),
                preferences.getBoolean(KEY_SIDEBAR_VISIBLE, true),
                preferences.getDouble(KEY_STAGE_WIDTH, 1520.0),
                preferences.getDouble(KEY_STAGE_HEIGHT, 940.0),
                preferences.getBoolean(KEY_STAGE_MAXIMIZED, false)
        );
    }

    public void save(SavedConfiguration configuration) {
        putNullable(KEY_FRACTAL_NAME, configuration.fractalName());
        preferences.putInt(KEY_DEPTH, configuration.depth());
        preferences.putDouble(KEY_ZOOM, configuration.zoom());
        preferences.putDouble(KEY_OFFSET_X, configuration.offsetX());
        preferences.putDouble(KEY_OFFSET_Y, configuration.offsetY());
        preferences.putInt(KEY_VIEW_WIDTH, configuration.viewWidth());
        preferences.putInt(KEY_VIEW_HEIGHT, configuration.viewHeight());
        preferences.putFloat(KEY_HUE_START, configuration.palette().hueStartDegrees());
        preferences.putFloat(KEY_HUE_RANGE, configuration.palette().hueRangeDegrees());
        preferences.putFloat(KEY_SATURATION, configuration.palette().saturation());
        preferences.putFloat(KEY_BRIGHTNESS_FLOOR, configuration.palette().brightnessFloor());
        preferences.putFloat(KEY_BRIGHTNESS_RANGE, configuration.palette().brightnessRange());
        preferences.putInt(KEY_INSIDE_COLOR, configuration.palette().insideColorRgb());
        preferences.putInt(KEY_CURVE_COLOR, configuration.palette().curveColorRgb());
        preferences.putInt(KEY_BACKGROUND_COLOR, configuration.palette().backgroundColorRgb());
        preferences.putBoolean(KEY_SIDEBAR_VISIBLE, configuration.sidebarVisible());
        preferences.putDouble(KEY_STAGE_WIDTH, configuration.stageWidth());
        preferences.putDouble(KEY_STAGE_HEIGHT, configuration.stageHeight());
        preferences.putBoolean(KEY_STAGE_MAXIMIZED, configuration.stageMaximized());
    }

    private void putNullable(String key, String value) {
        if (value == null || value.isBlank()) {
            preferences.remove(key);
        } else {
            preferences.put(key, value);
        }
    }

    public static final class SavedConfiguration {
        private final String fractalName;
        private final int depth;
        private final double zoom;
        private final double offsetX;
        private final double offsetY;
        private final int viewWidth;
        private final int viewHeight;
        private final EscapeTimeColorSettings palette;
        private final boolean sidebarVisible;
        private final double stageWidth;
        private final double stageHeight;
        private final boolean stageMaximized;

        public SavedConfiguration(String fractalName,
                                  int depth,
                                  double zoom,
                                  double offsetX,
                                  double offsetY,
                                  int viewWidth,
                                  int viewHeight,
                                  EscapeTimeColorSettings palette,
                                  boolean sidebarVisible,
                                  double stageWidth,
                                  double stageHeight,
                                  boolean stageMaximized) {
            this.fractalName = fractalName;
            this.depth = depth;
            this.zoom = zoom;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.viewWidth = viewWidth;
            this.viewHeight = viewHeight;
            this.palette = palette;
            this.sidebarVisible = sidebarVisible;
            this.stageWidth = stageWidth;
            this.stageHeight = stageHeight;
            this.stageMaximized = stageMaximized;
        }

        public String fractalName() { return fractalName; }
        public int depth() { return depth; }
        public double zoom() { return zoom; }
        public double offsetX() { return offsetX; }
        public double offsetY() { return offsetY; }
        public int viewWidth() { return viewWidth; }
        public int viewHeight() { return viewHeight; }
        public EscapeTimeColorSettings palette() { return palette; }
        public boolean sidebarVisible() { return sidebarVisible; }
        public double stageWidth() { return stageWidth; }
        public double stageHeight() { return stageHeight; }
        public boolean stageMaximized() { return stageMaximized; }
    }
}
