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

## 当前结构

### UI 层
- `src/main/java/com/example/fractal/FractalApplication.java`
  - JavaFX 启动入口
- `src/main/java/com/example/fractal/FractalFxWindow.java`
  - 主窗口壳
  - 当前采用“顶部菜单 + 左侧控制栏 + 中间大视口 + 底部状态栏”的布局
  - 顶部功能区已经从大块工具栏重构为菜单栏，主要控制集中到左侧
- `src/main/java/com/example/fractal/FractalFxViewport.java`
  - 视口交互、预览、右键菜单、PNG 导出
  - 支持固定逻辑视图尺寸，并在窗口缩放时按比例缩放显示

### 渲染层
- `src/main/java/com/example/fractal/render/FractalRenderService.java`
  - 通用后台渲染服务
- `src/main/java/com/example/fractal/render/AbstractEscapeTimeRenderer.java`
  - Escape-time 分形公共逻辑
  - 当前已接入统一调色设置
- `src/main/java/com/example/fractal/render/CpuEscapeTimeBackend.java`
  - 稳定 CPU 路径
  - 按块并行渲染像素
- `src/main/java/com/example/fractal/render/EscapeTimeBackendSelector.java`
  - 自动选择 GPU 或 CPU
  - GPU 不可用时自动回退 CPU
- `src/main/java/com/example/fractal/render/LwjglGpuEscapeTimeBackend.java`
  - 可选 GPU 路径
  - 使用 LWJGL + OpenGL shader
- `src/main/java/com/example/fractal/render/EscapeTimeColorSettings.java`
  - 调色参数模型
- `src/main/java/com/example/fractal/render/EscapeTimeColorPreset.java`
  - 快速调色预设
- `src/main/java/com/example/fractal/render/EscapeTimeColorManager.java`
  - 全局调色状态管理

### 模型层
- `src/main/java/com/example/fractal/model/FractalRegistry.java`
  - 分形注册表
- `src/main/java/com/example/fractal/model/FractalViewState.java`
  - 当前视图状态：分形类型、深度、缩放、偏移

## 当前已实现功能

### 视图尺寸控制
- 左侧新增 `Viewport size` 区域
- 支持预设尺寸与自定义宽高
- 当前逻辑视图尺寸会驱动：
  - 预览渲染尺寸
  - 应用窗口缩放时的等比显示
  - 当前视图默认导出尺寸
- 高分辨率导出默认基于当前视图尺寸，支持当前尺寸、2x、4x、自定义

### 调色系统
- Escape-time 分形支持统一调色链路
- 支持快速风格预设：
  - `Classic`
  - `Minimal`
  - `White`
  - `Black`
  - `Ocean`
  - `Fire`
- 支持精细参数调节：
  - Hue start
  - Hue range
  - Saturation
  - Brightness floor
  - Brightness range
  - Inside color
- CPU 与 GPU 路径都已经接入同一套调色参数

### 布局与交互
- 顶部大功能区已取消，改为菜单栏
- 左侧控制栏集中承载：分形选择、深度、缩放、视图尺寸、调色、导出
- 支持隐藏/显示左侧控制栏，以尽量把空间留给 viewport
- viewport 仍是主区域，尽量占据屏幕

### 当前交互策略
- 平移：拖拽过程中只做冻结帧位移预览，释放后重新渲染
- 缩放：滚轮过程中使用短防抖，停止后重新渲染
- 框选缩放：操作结束后立即渲染
- 双击：重置视图
- 右键：导出当前视图 / 高分辨率 PNG / 重置视图

### 运行时状态
- 左侧显示当前实际后端与最近渲染耗时
- 底部状态栏显示：
  - 鼠标位置
  - 复平面坐标
  - 缩放
  - 偏移
  - 当前视图尺寸
  - 当前后端

## 构建与验证

### 已验证
- `mvn -DskipTests compile` 已通过
- `powershell -ExecutionPolicy Bypass -File scripts/package-windows.ps1 -SkipCompile` 已通过
- 当前打包链路仍可生成：`target/dist/FractalExplorer`

### 已知限制
- GPU 路径目前只覆盖 escape-time 分形
- 如果当前机器缺少可用 GPU / OpenGL 条件，或 LWJGL 依赖不完整，会自动回退 CPU
- 当前环境里打包脚本提示 LWJGL jar 不完整，因此打包产物大概率以 CPU fallback 运行
- Windows 脚本当前生成的是 app image，不是安装器

## 已移除或不再适用的旧结构
- `FractalFrame.java`
- `FractalCanvas.java`
- Swing UI 壳
- 项目内 `.m2/` 作为默认 Maven 仓库
- `FractalLauncher.java`

## 最近相关改动
- 添加视图尺寸设置，并让 viewport 在应用缩放时保持等比显示
- 高分辨率导出默认使用当前视图尺寸
- 新增统一调色系统与快速风格预设
- 将顶部功能区重构为菜单栏，并把主要控制集中到左侧

## 下次继续时建议先读
1. `docs/DEV_CONTEXT.md`
2. `README.md`
3. `src/main/java/com/example/fractal/FractalApplication.java`
4. `src/main/java/com/example/fractal/FractalFxWindow.java`
5. `src/main/java/com/example/fractal/FractalFxViewport.java`
6. `src/main/java/com/example/fractal/render/AbstractEscapeTimeRenderer.java`
7. `src/main/java/com/example/fractal/render/LwjglGpuEscapeTimeBackend.java`
