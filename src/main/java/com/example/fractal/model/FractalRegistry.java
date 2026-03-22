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
                        "曼德勃罗集",
                        "逃逸时间分形",
                        "经典的复平面分形，适合做深度缩放和边界观察。",
                        1,
                        9,
                        6,
                        100,
                        new MandelbrotRenderer()
                ),
                new FractalDefinition(
                        "Julia ?",
                        "Julia 集",
                        "逃逸时间分形",
                        "与固定复常数配对的 Julia 集，适合观察轨道形态变化。",
                        9,
                        6,
                        100,
                        new JuliaRenderer()
                ),
                new FractalDefinition(
                        "燃烧之船",
                        "逃逸时间分形",
                        "带有折叠坐标的尖锐分形，边缘有类似火焰的纹理。",
                        1,
                        9,
                        6,
                        100,
                        new BurningShipRenderer()
                ),
                new FractalDefinition(
                        "科赫雪花",
                        "替换规则分形",
                        "通过重复替换线段生成的递归曲线，结构规整清晰。",
                        1,
                        7,
                        4,
                        100,
                        new KochSnowflakeRenderer()
                ),
                new FractalDefinition(
                        "分形树",
                        "递归几何",
                        "多分支递归结构，适合观察层级和角度变化。",
                        1,
                        10,
                        7,
                        100,
                        new FractalTreeRenderer()
                ),
                new FractalDefinition(
                        "谢尔宾斯基地毯",
                        "递归几何",
                        "反复移除九宫格中心区域得到的平面分形。",
                        1,
                        6,
                        4,
                        100,
                        new SierpinskiCarpetRenderer()
                )
        );
    }
}
