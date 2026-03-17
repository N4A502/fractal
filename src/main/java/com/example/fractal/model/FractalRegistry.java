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
                        "Mandelbrot 集",
                        "逃逸时间分形",
                        "复平面中的经典分形，展示参数 c 变化后的有界与发散边界。",
                        1,
                        9,
                        6,
                        100,
                        new MandelbrotRenderer()
                ),
                new FractalDefinition(
                        "Julia 集",
                        "逃逸时间分形",
                        "与 Mandelbrot 集对应的 Julia 集，固定参数后观察复迭代轨迹。",
                        1,
                        9,
                        6,
                        100,
                        new JuliaRenderer()
                ),
                new FractalDefinition(
                        "Burning Ship",
                        "逃逸时间分形",
                        "基于绝对值变换的逃逸时间分形，边缘呈现火焰与船帆状结构。",
                        1,
                        9,
                        6,
                        100,
                        new BurningShipRenderer()
                ),
                new FractalDefinition(
                        "Koch 雪花",
                        "L-System / 规则替换分形",
                        "由线段递归替换生成的经典曲线分形，体现自相似边界。",
                        1,
                        7,
                        4,
                        100,
                        new KochSnowflakeRenderer()
                ),
                new FractalDefinition(
                        "分形树",
                        "递归几何分形",
                        "以二叉递归分枝构造的树状分形，适合观察层级结构和角度缩放。",
                        1,
                        10,
                        7,
                        100,
                        new FractalTreeRenderer()
                ),
                new FractalDefinition(
                        "Sierpinski 地毯",
                        "递归几何分形",
                        "将正方形递归切分为九宫格并移除中心区域形成的平面分形。",
                        1,
                        6,
                        4,
                        100,
                        new SierpinskiCarpetRenderer()
                )
        );
    }
}