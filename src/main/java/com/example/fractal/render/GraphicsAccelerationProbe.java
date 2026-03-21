package com.example.fractal.render;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public final class GraphicsAccelerationProbe {

    private GraphicsAccelerationProbe() {
    }

    public static boolean isGpuAccelerationLikelyAvailable() {
        return describeLikelyGpuAcceleration() != null;
    }

    public static String describeLikelyGpuAcceleration() {
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }

        String opengl = System.getProperty("sun.java2d.opengl");
        if ("true".equalsIgnoreCase(opengl)) {
            return "Java2D OpenGL";
        }

        String d3d = System.getProperty("sun.java2d.d3d");
        if ("true".equalsIgnoreCase(d3d)) {
            return "Java2D Direct3D";
        }

        try {
            GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = environment.getDefaultScreenDevice();
            if (device != null && device.getDefaultConfiguration().getImageCapabilities().isAccelerated()) {
                return "Java2D accelerated surface";
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }
}