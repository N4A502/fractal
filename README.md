# Fractal Explorer

一个基于 Java 17 的桌面分形浏览器，当前使用纯 JavaFX UI，并保留 Java 渲染内核。

项目现在包含两条渲染路径：
- 默认 CPU 路径：分块并行渲染，覆盖所有分形类型
- 可选 GPU 路径：逃逸时间分形可在满足条件时尝试走 LWJGL/OpenGL，失败时自动回退 CPU

## 当前能力

- 浏览多种分形类型
- 调整层级 / 迭代深度
- 鼠标滚轮缩放、拖拽平移、框选放大、双击重置
- 导出当前视图 PNG
- 导出高分辨率 PNG，支持当前尺寸、2x、4x 和自定义尺寸
- 自动显示当前实际渲染后端与最近渲染耗时
- Windows 下可生成带运行时的 app image

## 分形类型

- Escape-time fractal
  - Mandelbrot Set
  - Julia Set
  - Burning Ship
- Recursive geometry
  - Fractal Tree
  - Sierpinski Carpet
- Rule substitution fractal
  - Koch Snowflake

## 技术栈

- Java 17
- JavaFX 21
- Maven
- LWJGL 3

## 项目结构

- `src/main/java/com/example/fractal/FractalApplication.java`
  - 应用入口
- `src/main/java/com/example/fractal/FractalFxWindow.java`
  - JavaFX 主窗口
- `src/main/java/com/example/fractal/FractalFxViewport.java`
  - 视口交互、预览和导出
- `src/main/java/com/example/fractal/render/`
  - 渲染后端、后端选择、CPU/GPU 路径
- `scripts/package-windows.ps1`
  - Windows app image 打包脚本

## 运行要求

- JDK 17 或更高版本
- Maven 3.9 或更高版本
- 默认 Maven 本地仓库：`%USERPROFILE%\\.m2\\repository`

## 开发运行

推荐直接运行应用入口：

```bash
mvn compile
```

然后在 IDE 中运行：

```text
com.example.fractal.FractalApplication
```

说明：
- 不建议直接使用普通的 `java -jar target/*.jar` 作为最终运行方式，因为 JavaFX 运行时不属于标准 JDK classpath 体验的一部分，裸跑 jar 很容易出现运行时组件缺失问题。
- 对日常开发，优先使用 IDE 运行或带运行时的打包结果。

## Windows 打包

生成带运行时的 Windows app image：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\package-windows.ps1
```

输出目录：

```text
target\dist\FractalExplorer
```

如果你已经完成编译，也可以跳过脚本里的编译步骤：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\package-windows.ps1 -SkipCompile
```

如需显式指定 Maven 本地仓库：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\package-windows.ps1 -MavenRepo "C:\path\to\repository"
```

## GPU 路径说明

GPU 路径只对 escape-time fractal 生效，并且依赖本机图形环境与 LWJGL 相关依赖可用。

如果以下任一条件不满足，程序会自动回退到 CPU：
- 当前环境没有可用 GPU 加速迹象
- GPU 后端初始化失败
- 本地 Maven 缓存中缺少 LWJGL 相关 jar
- 驱动或 OpenGL 能力不满足要求

这意味着：
- 没有 GPU 的机器仍然可以正常运行
- 打包结果在缺少 GPU 依赖时仍然可以作为 CPU 版使用

## 导出说明

导出 PNG 时会使用当前分形类型、深度、缩放和偏移重新渲染，而不是简单截屏。因此：
- 高分辨率导出不受当前窗口分辨率限制
- 导出结果与当前视图参数保持一致

## 当前限制

- GPU 路径目前只覆盖 escape-time fractal
- Windows 打包脚本当前只生成 Windows app image，不生成安装器
- 如果本机 Maven 缓存里没有 LWJGL 依赖，打包出的程序会退回 CPU 路径

## 仓库状态

当前主分支已经合并了以下方向：
- 高分辨率导出
- 纯 JavaFX UI 替换旧 Swing 壳
- CPU 分块渲染
- 可选 GPU 渲染与自动回退
- Windows 打包脚本