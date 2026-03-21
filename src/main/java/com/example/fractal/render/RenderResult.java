package com.example.fractal.render;

import java.awt.image.BufferedImage;

public class RenderResult {

    private final RenderRequest request;
    private final BufferedImage image;
    private final long durationMillis;

    public RenderResult(RenderRequest request, BufferedImage image, long durationMillis) {
        this.request = request;
        this.image = image;
        this.durationMillis = durationMillis;
    }

    public RenderRequest request() {
        return request;
    }

    public BufferedImage image() {
        return image;
    }

    public long durationMillis() {
        return durationMillis;
    }
}