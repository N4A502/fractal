# 开发上下文记录

最后更新时间：2026-03-17

## 项目概况

- 项目名称：`fractal-explorer`
- 技术栈：Java 8、Swing、Maven
- 主入口：`com.example.fractal.FractalApplication`
- 主要窗口：`com.example.fractal.FractalFrame`
- 绘图画布：`com.example.fractal.FractalCanvas`

## 当前已实现功能

### 基础功能

- 标准 Maven 工程结构，可直接导入 IDEA
- Swing 图形界面
- 分形类型切换
- 缩放控制
- 层级/迭代控制
- 状态栏显示鼠标、缩放、偏移信息

### 已支持的分形

- 逃逸时间分形
  - Mandelbrot
  - Julia
  - Burning Ship
- 递归几何分形
  - 分形树
  - Sierpinski 地毯
- 规则替换分形
  - Koch 雪花

### 鼠标交互

- 左键拖拽平移
- 滚轮以鼠标为中心无极缩放
- `Shift + 拖拽` 框选缩放
- 双击重置视图
- 右键导出当前视图为 PNG

### 渲染体验优化

- 分形渲染改为后台异步执行，避免阻塞 UI
- 渲染中显示“正在渲染...”提示
- 发起新渲染前，先冻结当前画面为内存快照
- 后台渲染期间，旧帧会先做临时平移/缩放预览
- 拖拽过程中增加轻量节流，避免后台任务过密
- 框选缩放时增加遮罩和选框高亮

## 最近关键提交

- `48f54f6` `perf: throttle drag render updates`
- `783680e` `feat: improve selection zoom preview`
- `909e0e4` `feat: preview frozen frame during render`
- `1eb3c0d` `feat: freeze current frame during async render`
- `92f901b` `refactor: left align panel and uncap depth control`
- `2a1e272` `feat: render fractals asynchronously`
- `aacbcc3` `feat: export current fractal view as png`
- `d5554f1` `chore: initialize fractal explorer project`

## 关键文件说明

- `src/main/java/com/example/fractal/FractalApplication.java`
  - 程序入口
- `src/main/java/com/example/fractal/FractalFrame.java`
  - 左侧控制面板、状态栏、交互绑定
- `src/main/java/com/example/fractal/FractalCanvas.java`
  - 鼠标交互、异步渲染、冻结旧帧预览、导出 PNG
- `src/main/java/com/example/fractal/model/FractalRegistry.java`
  - 分形注册表
- `src/main/java/com/example/fractal/render/`
  - 各类分形渲染器

## 当前 UI 状态

- 左侧面板尽量左对齐
- “层级 / 迭代”已经改成无上限输入
- “缩放”仍是滑块，支持动态扩展最大值
- 信息说明区已整理成卡片风格

## 已知注意点

- 当前环境没有 `mvn` 命令，因此一直是用 `javac` 做编译校验
- 目标兼容 Java 8
- 渲染器目前仍是 CPU 同步像素计算，极高倍率下性能仍可能继续成为瓶颈
- 画布交互和渲染逻辑集中在 `FractalCanvas.java`，后续如果继续增强，建议先考虑适度拆分类

## 建议的下一步开发方向

### 优先级高

- 高分辨率导出
  - 导出不只限于当前窗口分辨率
  - 支持 2x / 4x / 自定义尺寸 PNG

- 状态栏增强
  - 增加渲染耗时
  - 增加“当前是否正在后台渲染”状态

- 渲染性能继续优化
  - 对逃逸时间分形做分块渲染
  - 对高倍率场景做局部更新

### 优先级中

- 缩放增加数值输入框
  - 目前是滑块动态扩容
  - 后续可改成“滑块 + 数值输入”的双控件

- 导出菜单增强
  - 增加“导出高分辨率 PNG”
  - 增加“复制当前图像到剪贴板”

- 交互增强
  - 右键菜单增加“重置视图”
  - 增加键盘快捷键

## 建议的继续方式

下次继续开发时，优先阅读：

1. `docs/DEV_CONTEXT.md`
2. `src/main/java/com/example/fractal/FractalFrame.java`
3. `src/main/java/com/example/fractal/FractalCanvas.java`

如果要快速定位最近行为变化，直接看：

- `git log --oneline -10`