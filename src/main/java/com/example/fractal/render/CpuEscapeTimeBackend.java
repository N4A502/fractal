package com.example.fractal.render;

import java.util.Arrays;
import java.util.stream.IntStream;

public class CpuEscapeTimeBackend implements EscapeTimeBackend {

    private static final int DEFAULT_TILE_SIZE = 96;

    @Override
    public int[] renderPixels(AbstractEscapeTimeRenderer renderer, EscapeTimeRenderContext context) {
        int width = context.width();
        int height = context.height();
        int[] pixels = new int[width * height];
        Arrays.fill(pixels, renderer.insideColorRgb());

        int tileSize = resolveTileSize(context);
        int tilesX = (width + tileSize - 1) / tileSize;
        int tilesY = (height + tileSize - 1) / tileSize;
        int tileCount = tilesX * tilesY;

        IntStream.range(0, tileCount).parallel().forEach(tileIndex -> {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            int tileX = tileIndex % tilesX;
            int tileY = tileIndex / tilesX;
            int startX = tileX * tileSize;
            int startY = tileY * tileSize;
            int endX = Math.min(startX + tileSize, width);
            int endY = Math.min(startY + tileSize, height);

            for (int py = startY; py < endY; py++) {
                if (((py - startY) & 15) == 0 && Thread.currentThread().isInterrupted()) {
                    return;
                }

                int rowOffset = py * width;
                for (int px = startX; px < endX; px++) {
                    if (((px - startX) & 63) == 0 && Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    double x0 = renderer.mapPlaneX(px, width, context.zoom(), context.offsetX());
                    double y0 = renderer.mapPlaneY(py, width, height, context.zoom(), context.offsetY());
                    int iterations = renderer.iteratePoint(x0, y0, context.maxIterations());
                    pixels[rowOffset + px] = renderer.computeColorRgb(iterations, context.maxIterations());
                }
            }
        });

        return pixels;
    }

    private int resolveTileSize(EscapeTimeRenderContext context) {
        int largestDimension = Math.max(context.width(), context.height());
        if (largestDimension >= 3000) {
            return 160;
        }
        if (largestDimension >= 1800) {
            return 128;
        }
        return DEFAULT_TILE_SIZE;
    }
}