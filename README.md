# Fractal Explorer

一个基于 Java Swing 的分形演示程序，使用 Maven 构建，可直接导入 IntelliJ IDEA。

本程序完全是AI所写

## 功能

- 图形界面展示分形
- 支持切换分形类型
- 支持调整层级/迭代次数
- 支持缩放显示
- 覆盖主要分形分类：
  - 逃逸时间分形：Mandelbrot、Julia、Burning Ship
  - 递归几何分形：分形树、Sierpinski 地毯
  - 规则替换分形：Koch 雪花

## 运行

### IDEA

1. 使用 IntelliJ IDEA 打开项目根目录
2. 等待 Maven 导入完成
3. 运行 `com.example.fractal.FractalApplication`

### 命令行

如果本机已安装 Maven：

```bash
mvn clean package
java -jar target/fractal-explorer-1.0-SNAPSHOT.jar
```

## JDK

项目当前兼容 Java 8 及以上版本。
