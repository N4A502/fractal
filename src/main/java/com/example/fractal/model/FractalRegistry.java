package com.example.fractal.model;

import com.example.fractal.render.BurningShipRenderer;
import com.example.fractal.render.FractalTreeRenderer;
import com.example.fractal.render.JuliaRenderer;
import com.example.fractal.render.KochSnowflakeRenderer;
import com.example.fractal.render.MandelbrotRenderer;
import com.example.fractal.render.SierpinskiCarpetRenderer;

import java.util.Arrays;
import java.util.List;

public final class FractalRegistry {

    private FractalRegistry() {
    }

    public static List<FractalDefinition> createDefinitions() {
        return Arrays.asList(
                new FractalDefinition(
                        "?????",
                        "??????",
                        "??????????????????????",
                        1,
                        9,
                        6,
                        100,
                        new MandelbrotRenderer()
                ),
                new FractalDefinition(
                        "Julia ?",
                        "??????",
                        "????????? Julia ?????????????",
                        1,
                        9,
                        6,
                        100,
                        new JuliaRenderer()
                ),
                new FractalDefinition(
                        "????",
                        "??????",
                        "???????????????????????",
                        1,
                        9,
                        6,
                        100,
                        new BurningShipRenderer()
                ),
                new FractalDefinition(
                        "????",
                        "??????",
                        "???????????????????????",
                        1,
                        7,
                        4,
                        100,
                        new KochSnowflakeRenderer()
                ),
                new FractalDefinition(
                        "???",
                        "????",
                        "????????????????????",
                        1,
                        10,
                        7,
                        100,
                        new FractalTreeRenderer()
                ),
                new FractalDefinition(
                        "???????",
                        "????",
                        "???????????????????",
                        1,
                        6,
                        4,
                        100,
                        new SierpinskiCarpetRenderer()
                )
        );
    }
}
