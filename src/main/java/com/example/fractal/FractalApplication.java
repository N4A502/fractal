package com.example.fractal;

import com.example.fractal.render.EscapeTimeBackendSelector;

import javax.swing.SwingUtilities;

public final class FractalApplication {

    private FractalApplication() {
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(EscapeTimeBackendSelector::shutdown, "fractal-render-backend-shutdown"));
        SwingUtilities.invokeLater(() -> {
            FractalFrame frame = new FractalFrame();
            frame.setVisible(true);
        });
    }
}