package com.example.fractal.render;

import java.awt.Color;
import java.awt.Graphics2D;

public class SierpinskiCarpetRenderer implements FractalRenderer {

    @Override
    public void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        EscapeTimeColorSettings settings = EscapeTimeColorManager.getSettings();
        graphics.setColor(settings.backgroundColor());
        graphics.fillRect(0, 0, width, height);

        int size = (int) Math.round(Math.min(width, height) * 0.7 * zoom);
        int x = (int) Math.round((width - size) / 2.0 + offsetX);
        int y = (int) Math.round((height - size) / 2.0 + offsetY);

        graphics.setColor(settings.gradientColor(0.78f));
        graphics.fillRect(x, y, size, size);
        graphics.setColor(settings.insideColor());
        carve(graphics, x, y, size, depth);
    }

    private void carve(Graphics2D graphics, int x, int y, int size, int depth) {
        if (depth <= 0 || size < 3) {
            return;
        }

        int sub = size / 3;
        graphics.fillRect(x + sub, y + sub, sub, sub);

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
