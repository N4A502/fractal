package com.example.fractal.render;

import java.awt.Color;
import java.awt.Graphics2D;

public class SierpinskiCarpetRenderer implements FractalRenderer {

    private static final double MIN_CELL_PIXELS = 1.5;

    @Override
    public void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        EscapeTimeColorSettings settings = EscapeTimeColorManager.getSettings();
        graphics.setColor(settings.backgroundColor());
        graphics.fillRect(0, 0, width, height);

        double size = Math.min(width, height) * 0.7 * zoom;
        double x = (width - size) / 2.0 + offsetX;
        double y = (height - size) / 2.0 + offsetY;

        graphics.setColor(settings.gradientColor(0.78f));
        graphics.fillRect((int) Math.round(x), (int) Math.round(y), Math.max(1, (int) Math.round(size)), Math.max(1, (int) Math.round(size)));
        graphics.setColor(settings.insideColor());
        carve(graphics, x, y, size, depth);
    }

    private void carve(Graphics2D graphics, double x, double y, double size, int depth) {
        if (RenderCancellation.isCancelledCurrentThread() || depth <= 0 || size < MIN_CELL_PIXELS) {
            return;
        }

        double sub = size / 3.0;
        graphics.fillRect(
                (int) Math.round(x + sub),
                (int) Math.round(y + sub),
                Math.max(1, (int) Math.round(sub)),
                Math.max(1, (int) Math.round(sub))
        );

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (row == 1 && col == 1) {
                    continue;
                }
                carve(graphics, x + col * sub, y + row * sub, sub, depth - 1);
            }
        }
    }

}
