# 开发上下文记录

最后更新时间：2026-03-22
当前分支：`main`
当前远端主分支：`origin/main`

## 项目概况

- 项目名称：`fractal-explorer`
- 技术栈：Java 17、JavaFX、Maven、LWJGL
- 当前 UI：纯 JavaFX，不再使用 Swing
- 应用入口：`com.example.fractal.FractalApplication`
- 主窗口：`com.example.fractal.FractalFxWindow`
- 视口交互与导出：`com.example.fractal.FractalFxViewport`

## 当前架构

### UI 层

- `src/main/java/com/example/fractal/FractalApplication.java`
  - 应用启动入口
  - 外层是普通启动器，内部 `JavaFxApp` 继承 `javafx.application.Application`
  - 这样可以避免把 `Application` 子类直接作为主类启动时常见的 JavaFX 运行时问题

- `src/main/java/com/example/fractal/FractalFxWindow.java`
  - JavaFX 主窗口壳
  - 当前布局已经偏向“viewport 优先”
  - 侧栏更窄，并支持 `Hide Controls / Show Controls`
  - 顶栏、底栏高度已压缩，尽量把屏幕空间留给 viewport

- `src/main/java/com/example/fractal/FractalFxViewport.java`
  - 视口交互、预览、右键菜单、PNG 导出
  - 当前已改成“交互结束后再重渲染”：
    - 拖拽平移：鼠标释放后才重新渲染
    - 滚轮缩放：短防抖后再重新渲染
    - 框选缩放：操作结束后立即渲染
  - 交互过程中使用冻结帧做视觉预览，而不是持续后台重渲染

### 渲染层

- `src/main/java/com/example/fractal/render/FractalRenderService.java`
  - 通用后台渲染服务
  - 不依赖 Swing 线程模型

- `src/main/java/com/example/fractal/render/AbstractEscapeTimeRenderer.java`
  - 逃逸时间分形的公共逻辑
  - 统一接入后端选择器

- `src/main/java/com/example/fractal/render/CpuEscapeTimeBackend.java`
  - 当前稳定 CPU 路径
  - 按块并行渲染像素

- `src/main/java/com/example/fractal/render/EscapeTimeBackendSelector.java`
  - 自动选择 GPU 或 CPU
  - 当前实现是“可选 GPU 反射加载 + CPU 回退”
  - 即使缺少 LWJGL 相关 jar，也不会因为类加载失败导致应用直接崩掉

- `src/main/java/com/example/fractal/render/LwjglGpuEscapeTimeBackend.java`
  - 逃逸时间分形 GPU 路径
  - 使用 LWJGL + OpenGL shader
  - 只有在环境满足条件时才会启用

### 模型层

- `src/main/java/com/example/fractal/model/FractalRegistry.java`
  - 分形注册表
- `src/main/java/com/example/fractal/model/FractalViewState.java`
  - 当前视图状态：分形类型、深度、缩放、偏移

## 当前已实现能力

### 分形类型

- Escape-time fractal
  - Mandelbrot Set
  - Julia Set
  - Burning Ship
- Recursive geometry
  - Fractal Tree
  - Sierpinski Carpet
- Rule substitution fractal
  - Koch Snowflake

### 交互能力

- 左键拖拽平移
- 鼠标滚轮缩放
- `Shift + 拖拽` 框选放大
- 双击重置视图
- 右键菜单导出当前视图 / 高分辨率 PNG

### 导出能力

- 导出当前视图 PNG
- 导出高分辨率 PNG
- 支持：当前尺寸、2x、4x、自定义尺寸
- 导出会按当前视图参数重新渲染，不是简单截图

### 运行时状态

- 顶栏显示当前实际渲染后端
- 顶栏显示最近渲染耗时
- 状态栏显示鼠标位置、缩放、偏移、复平面坐标

## 最近关键提交

- `33c9d7c` `feat: defer rerender until interaction ends`
- `2ee25ac` `feat: prioritize viewport in desktop layout`
- `9b8260d` `docs: refresh readme for current architecture`
- `770e581` `Merge branch 'dev'`
- `56a1ed0` `refactor: merge application launcher classes`
- `c645aeb` `build: use default maven repository by default`
- `e2293bd` `build: add windows app packaging script`
- `b329f78` `build: add javafx launcher entrypoint`
- `5fe8f51` `refactor: remove swing shell and finish javafx ui`
- `53378d4` `feat: replace desktop shell with javafx`
- `b34aed3` `feat: add optional lwjgl gpu backend`
- `12c1177` `perf: add tiled escape-time cpu backend`
- `55ee536` `refactor: split render pipeline for performance path`
- `0ba37a2` `feat: add high resolution png export`

## 已移除或不再适用的旧结构

以下内容已经不再存在，后续不要再围绕它们继续开发：

- `FractalFrame.java`
- `FractalCanvas.java`
- Swing UI 壳
- 项目内 `.m2/` 作为默认 Maven 仓库
- `FractalLauncher.java` 独立启动器

## 构建与运行注意点

### Java / Maven

- 当前目标版本：Java 17
- 不再兼容 Java 8
- 默认 Maven 本地仓库应使用：`%USERPROFILE%\.m2\repository`
- 项目内不应再生成 `.m2/`

### JavaFX

- 不建议把普通 `java -jar target/*.jar` 作为主要运行方式
- 更稳妥的方式：
  - 在 IDE 中运行 `com.example.fractal.FractalApplication`
  - 或使用打包脚本生成 app image 再运行

### Windows 打包

- 打包脚本：`scripts/package-windows.ps1`
- 默认使用 Maven 标准本地仓库
- 支持显式传入 `-MavenRepo`
- 当前已验证可生成：`target/dist/FractalExplorer`

### GPU 路径现实情况

- GPU 路径只覆盖 escape-time fractal
- 如果本机没有 GPU、OpenGL 不满足条件、或 Maven 缓存里缺 LWJGL 依赖，会自动回退 CPU
- 当前打包脚本在缺少 LWJGL 依赖时仍可生成 CPU fallback 版 app image

## 当前 UI 状态

- viewport 是主区域，已尽量占屏
- 侧栏默认显示，但可一键折叠
- 顶栏已紧凑化
- 状态栏已压缩
- viewport 上方说明区已移除，直接留给画布

## 当前交互策略

这是最近刚调整过的重点，下次接手时不要误改回去：

- 平移：拖拽过程中只做冻结帧位移预览，结束后渲染
- 缩放：滚轮过程中只做冻结帧缩放预览，停止后渲染
- 框选缩放：结束后立即渲染

如果后续要继续优化，应基于“交互结束后渲染”的策略演进，而不是重新回到“交互中持续后台重渲染”。

## 当前已知限制

- GPU 路径目前只支持 escape-time fractal
- Windows 脚本当前生成 app image，不生成安装器
- 受当前环境限制，Maven 默认仓库有时在沙箱里不可写；脚本会复用已有 `target/classes` 继续打包
- 如果要真正验证 GPU 路径，需要在有 LWJGL 依赖且图形环境正常的机器上运行

## 建议的下一步方向

### 高优先级

- 继续打磨 JavaFX 交互手感
  - 比如缩放结束后的过渡更自然
  - 或增加真正的全屏模式

- 让 viewport 在窄窗口下自动折叠侧栏
  - 现在是手动切换
  - 后续可以加窗口宽度阈值自动收起

- 增强打包链
  - 生成 Windows 安装器
  - 检查并提示 GPU 依赖是否完整

### 中优先级

- GPU 路径继续完善
  - 更稳定的初始化与诊断信息
  - 更好的高分辨率导出支持

- 将递归几何分形的渲染性能继续优化
  - 当前主要性能路线集中在 escape-time fractal

## 下次继续时建议先读

1. `docs/DEV_CONTEXT.md`
2. `README.md`
3. `src/main/java/com/example/fractal/FractalApplication.java`
4. `src/main/java/com/example/fractal/FractalFxWindow.java`
5. `src/main/java/com/example/fractal/FractalFxViewport.java`
6. `src/main/java/com/example/fractal/render/EscapeTimeBackendSelector.java`

如果要快速看最近变化，直接运行：

```bash
git log --oneline -12
```