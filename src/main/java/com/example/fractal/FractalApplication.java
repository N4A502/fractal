package com.example.fractal;

import javax.swing.SwingUtilities;

public final class FractalApplication {

    private FractalApplication() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FractalFrame frame = new FractalFrame();
            frame.setVisible(true);
        });
    }
}
