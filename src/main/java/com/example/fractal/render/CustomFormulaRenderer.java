package com.example.fractal.render;

import com.example.fractal.custom.Complex;
import com.example.fractal.custom.CompiledComplexExpression;
import com.example.fractal.custom.CustomFormulaMode;
import com.example.fractal.custom.CustomFormulaSettings;
import com.example.fractal.custom.CustomFractalManager;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class CustomFormulaRenderer implements FractalRenderer {

    @Override
    public void render(Graphics2D graphics, int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        graphics.drawImage(renderImage(width, height, depth, zoom, offsetX, offsetY), 0, 0, null);
    }

    @Override
    public BufferedImage renderImage(int width, int height, int depth, double zoom, double offsetX, double offsetY) {
        EscapeTimeColorSettings colors = EscapeTimeColorManager.getSettings();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];
        CustomFormulaSettings settings = CustomFractalManager.getFormulaSettings();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0xFF000000 | colors.backgroundColorRgb();
        }
        if (!settings.isValid()) {
            image.setRGB(0, 0, width, height, pixels, 0, width);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(colors.gradientColor(0.65f));
            graphics.drawString(settings.validationMessage() == null ? "Invalid custom formula." : settings.validationMessage(), 24, 32);
            graphics.dispose();
            return image;
        }

        CompiledComplexExpression expression = settings.expression();
        int maxIterations = 60 + depth * 40;
        double centerX = settings.mode() == CustomFormulaMode.MANDELBROT_LIKE ? -0.5 : 0.0;
        double centerY = 0.0;
        double baseScale = 3.2 / Math.max(0.0001, zoom);
        Complex juliaConstant = new Complex(settings.juliaReal(), settings.juliaImaginary());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (RenderCancellation.isCancelledCurrentThread()) {
                    image.setRGB(0, 0, width, height, pixels, 0, width);
                    return image;
                }
                double planeX = (x - width / 2.0 - offsetX) * baseScale / width + centerX;
                double planeY = (y - height / 2.0 - offsetY) * baseScale / width + centerY;
                Complex point = new Complex(planeX, planeY);
                Complex z = settings.mode() == CustomFormulaMode.MANDELBROT_LIKE ? Complex.ZERO : point;
                Complex c = settings.mode() == CustomFormulaMode.MANDELBROT_LIKE ? point : juliaConstant;
                int iteration = 0;
                while (iteration < maxIterations) {
                    z = expression.evaluate(z, c);
                    if (!z.isFinite() || z.abs() > 2.0) {
                        break;
                    }
                    iteration++;
                }
                pixels[y * width + x] = iteration >= maxIterations
                        ? 0xFF000000 | colors.insideColorRgb()
                        : 0xFF000000 | colors.computeColorRgb(iteration, maxIterations);
            }
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    @Override
    public String backendDescription() {
        return "Custom CPU";
    }
}
