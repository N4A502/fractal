package com.example.fractal;

import com.example.fractal.model.FractalDefinition;
import com.example.fractal.model.FractalRegistry;
import com.example.fractal.render.AbstractEscapeTimeRenderer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

public class FractalFrame extends JFrame {

    private static final int DEFAULT_ZOOM_SLIDER_MAX = 400;
    private static final Color APP_BACKGROUND = new Color(238, 242, 248);
    private static final Color PANEL_BACKGROUND = new Color(248, 250, 253);
    private static final Color CARD_BACKGROUND = new Color(255, 255, 255);
    private static final Color CARD_BORDER = new Color(220, 226, 235);
    private static final Color TITLE_COLOR = new Color(24, 30, 42);
    private static final Color MUTED_TEXT = new Color(94, 104, 122);
    private static final Color ACCENT = new Color(40, 112, 255);
    private static final Color ACCENT_SOFT = new Color(227, 237, 255);
    private static final Color SUCCESS = new Color(20, 140, 92);
    private static final Color SUCCESS_SOFT = new Color(227, 247, 239);

    private final FractalCanvas canvas;
    private final JComboBox<FractalDefinition> fractalSelector;
    private final JSpinner depthSpinner;
    private final JSlider zoomSlider;
    private final JLabel categoryValueLabel;
    private final JLabel descriptionValueLabel;
    private final JLabel depthValueLabel;
    private final JLabel zoomValueLabel;
    private final JLabel statusLabel;
    private final JLabel backendPillLabel;
    private final JLabel renderPillLabel;
    private final JLabel summaryTitleLabel;
    private final JLabel summarySubtitleLabel;
    private final Timer runtimeInfoTimer;
    private boolean updatingZoomSlider;

    public FractalFrame() {
        super("Fractal Explorer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1380, 880));
        setLayout(new BorderLayout());
        getContentPane().setBackground(APP_BACKGROUND);

        List<FractalDefinition> fractals = FractalRegistry.createDefinitions();
        this.canvas = new FractalCanvas();
        this.fractalSelector = new JComboBox<FractalDefinition>(fractals.toArray(new FractalDefinition[0]));
        this.depthSpinner = new JSpinner(new SpinnerNumberModel(5, 1, null, 1));
        this.zoomSlider = new JSlider(10, DEFAULT_ZOOM_SLIDER_MAX, 100);
        this.categoryValueLabel = new JLabel();
        this.descriptionValueLabel = new JLabel();
        this.depthValueLabel = new JLabel("", SwingConstants.RIGHT);
        this.zoomValueLabel = new JLabel("", SwingConstants.RIGHT);
        this.statusLabel = new JLabel("鼠标: -, - | 缩放: 1.00x | 偏移: (0, 0)");
        this.backendPillLabel = createPillLabel(ACCENT_SOFT, ACCENT.darker());
        this.renderPillLabel = createPillLabel(SUCCESS_SOFT, SUCCESS.darker());
        this.summaryTitleLabel = new JLabel("Fractal Explorer");
        this.summarySubtitleLabel = new JLabel("交互式分形浏览、导出与后端切换状态一体展示");
        this.runtimeInfoTimer = new Timer(250, e -> refreshRuntimeInfo());

        normalizeControlHeights();
        configureHeaderLabels();
        configureCanvas();
        setContentPane(buildRootLayout());

        fractalSelector.addActionListener(e -> syncSelection());
        depthSpinner.addChangeListener(e -> syncControls());
        zoomSlider.addChangeListener(e -> {
            if (!updatingZoomSlider) {
                syncControls();
            }
        });
        canvas.setInteractionListener(new FractalCanvas.InteractionListener() {
            @Override
            public void onZoomChanged(double zoom, int anchorX, int anchorY) {
                setZoomSliderValue(zoom);
                if (anchorX >= 0 && anchorY >= 0) {
                    canvas.applyZoom(zoom, anchorX, anchorY);
                }
                refreshRuntimeInfo();
            }

            @Override
            public void onViewChanged(double zoom, double offsetX, double offsetY, int mouseX, int mouseY) {
                statusLabel.setText(buildStatusText(zoom, offsetX, offsetY, mouseX, mouseY));
                refreshRuntimeInfo();
            }

            @Override
            public void onResetRequested() {
                resetControls();
            }
        });

        runtimeInfoTimer.start();
        syncSelection();
        pack();
        setLocationRelativeTo(null);
    }

    private void configureHeaderLabels() {
        summaryTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        summaryTitleLabel.setForeground(TITLE_COLOR);
        summarySubtitleLabel.setForeground(MUTED_TEXT);
        summarySubtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        statusLabel.setForeground(new Color(52, 61, 78));
    }

    private void configureCanvas() {
        canvas.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(24, 30, 42), 1),
                BorderFactory.createEmptyBorder()
        ));
    }

    private JPanel buildRootLayout() {
        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBackground(APP_BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildMainArea(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildTopBar() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(true);
        header.setBackground(PANEL_BACKGROUND);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(summaryTitleLabel);
        titlePanel.add(createVerticalGap(6));
        titlePanel.add(summarySubtitleLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(backendPillLabel);
        rightPanel.add(renderPillLabel);
        rightPanel.add(createPrimaryButton("高分辨率导出", e -> canvas.exportHighResolutionView()));
        rightPanel.add(createSecondaryButton("重置视图", e -> resetControls()));

        header.add(titlePanel, BorderLayout.CENTER);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMainArea() {
        JPanel content = new JPanel(new BorderLayout(18, 0));
        content.setOpaque(false);
        content.add(buildSidebar(), BorderLayout.WEST);
        content.add(buildCanvasPanel(), BorderLayout.CENTER);
        return content;
    }

    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(360, 0));

        sidebar.add(createHeroCard());
        sidebar.add(createVerticalGap(14));
        sidebar.add(createControlCard());
        sidebar.add(createVerticalGap(14));
        sidebar.add(createExportCard());
        sidebar.add(createVerticalGap(14));
        sidebar.add(createTipsCard());
        sidebar.add(createVerticalGap(14));
        sidebar.add(createCoverageCard());

        JScrollPane scrollPane = new JScrollPane(sidebar);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(APP_BACKGROUND);
        scrollPane.setOpaque(false);
        scrollPane.setPreferredSize(new Dimension(372, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel createHeroCard() {
        JPanel card = createCard(new BorderLayout(0, 12));
        JLabel eyebrow = createMutedLabel("实时浏览与可选 GPU 路线");
        eyebrow.setForeground(ACCENT.darker());
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(eyebrow);
        text.add(createVerticalGap(8));

        JLabel title = new JLabel("分形探索控制台");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TITLE_COLOR);
        text.add(title);
        text.add(createVerticalGap(8));

        JLabel body = createMutedLabel("当前界面已经接到新的渲染管线：异步任务、分块 CPU 后端、自动后端判断与高分辨率导出走同一路径。后续真正接 GPU 时不需要再改 UI。", 300);
        text.add(body);

        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chipRow.setOpaque(false);
        chipRow.add(createInlineBadge("Auto Backend", ACCENT_SOFT, ACCENT.darker()));
        chipRow.add(createInlineBadge("Tile Render", SUCCESS_SOFT, SUCCESS.darker()));
        chipRow.add(createInlineBadge("Hi-Res Export", new Color(255, 242, 224), new Color(161, 100, 26)));

        card.add(text, BorderLayout.CENTER);
        card.add(chipRow, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createControlCard() {
        JPanel card = createCard();
        card.add(createCardTitle("参数控制"));
        card.add(createVerticalGap(14));
        card.add(createSectionLabel("分形类型"));
        card.add(fractalSelector);
        card.add(createVerticalGap(12));
        card.add(createSectionLabel("分类"));
        categoryValueLabel.setForeground(TITLE_COLOR);
        categoryValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(categoryValueLabel);
        card.add(createVerticalGap(12));
        card.add(createSectionLabel("层级 / 迭代"));
        depthValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        depthValueLabel.setForeground(MUTED_TEXT);
        card.add(depthValueLabel);
        card.add(depthSpinner);
        card.add(createVerticalGap(12));
        card.add(createSectionLabel("缩放"));
        zoomValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        zoomValueLabel.setForeground(MUTED_TEXT);
        card.add(zoomValueLabel);
        card.add(zoomSlider);
        return card;
    }

    private JPanel createExportCard() {
        JPanel card = createCard();
        card.add(createCardTitle("导出与运行状态"));
        card.add(createVerticalGap(14));
        card.add(createSectionLabel("导出"));
        card.add(createMutedLabel("支持当前尺寸、2x、4x 与自定义宽高 PNG。预览和导出统一走当前选择的渲染后端。", 300));
        card.add(createVerticalGap(12));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionRow.add(createPrimaryButton("导出高分辨率", e -> canvas.exportHighResolutionView()));
        actionRow.add(createSecondaryButton("导出当前视图", e -> canvas.exportCurrentView()));
        card.add(actionRow);
        card.add(createVerticalGap(14));
        card.add(createSectionLabel("实时状态"));
        card.add(createMutedLabel("顶部会持续显示当前实际后端和最近渲染耗时。没有可用 GPU 时会自动回退 CPU。", 300));
        return card;
    }

    private JPanel createTipsCard() {
        JPanel card = createCard();
        card.add(createCardTitle("交互提示"));
        card.add(createVerticalGap(12));
        card.add(createMutedLabel("左键拖拽平移，滚轮缩放，Shift + 拖拽框选放大，双击快速重置。右键菜单保留两种 PNG 导出入口。", 300));
        return card;
    }

    private JPanel createCoverageCard() {
        JPanel card = createCard();
        card.add(createCardTitle("当前覆盖"));
        card.add(createVerticalGap(12));
        card.add(createMutedLabel("逃逸时间分形已接性能路线。递归几何和规则替换分形仍保留 Java2D 路径，但它们也复用统一的导出与状态展示壳。", 300));
        return card;
    }

    private JPanel buildCanvasPanel() {
        JPanel shell = new JPanel(new BorderLayout(0, 12));
        shell.setOpaque(true);
        shell.setBackground(PANEL_BACKGROUND);
        shell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("渲染视口");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TITLE_COLOR);
        JLabel subtitle = createMutedLabel("预览会在后台渲染期间冻结上一帧并保持拖拽反馈。状态栏会同步显示后端和复平面坐标。", 700);

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.add(title);
        titleStack.add(createVerticalGap(4));
        titleStack.add(subtitle);

        header.add(titleStack, BorderLayout.CENTER);
        shell.add(header, BorderLayout.NORTH);
        shell.add(canvas, BorderLayout.CENTER);
        return shell;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private void syncSelection() {
        FractalDefinition definition = (FractalDefinition) fractalSelector.getSelectedItem();
        if (definition == null) {
            return;
        }

        categoryValueLabel.setText(definition.category());
        descriptionValueLabel.setText("<html><body style='width:300px'>" + definition.description() + "</body></html>");
        summaryTitleLabel.setText(definition.name());
        summarySubtitleLabel.setText(definition.description());
        ((SpinnerNumberModel) depthSpinner.getModel()).setMinimum(definition.minDepth());
        depthSpinner.setValue(Math.max(definition.defaultDepth(), definition.minDepth()));
        zoomSlider.setMaximum(Math.max(DEFAULT_ZOOM_SLIDER_MAX, definition.defaultZoom()));
        zoomSlider.setValue(definition.defaultZoom());
        canvas.resetView();
        refreshRuntimeInfo();
        syncControls();
    }

    private void syncControls() {
        FractalDefinition definition = (FractalDefinition) fractalSelector.getSelectedItem();
        if (definition == null) {
            return;
        }

        int depth = ((Number) depthSpinner.getValue()).intValue();
        double zoom = zoomSlider.getValue() / 100.0;
        depthValueLabel.setText(depth + " 级");
        zoomValueLabel.setText(String.format("%.2f x", zoom));
        canvas.render(definition, depth, zoom);
        refreshRuntimeInfo();
    }

    private void resetControls() {
        FractalDefinition definition = (FractalDefinition) fractalSelector.getSelectedItem();
        if (definition == null) {
            return;
        }

        canvas.resetView();
        zoomSlider.setMaximum(Math.max(DEFAULT_ZOOM_SLIDER_MAX, definition.defaultZoom()));
        depthSpinner.setValue(Math.max(definition.defaultDepth(), definition.minDepth()));
        zoomSlider.setValue(definition.defaultZoom());
        syncControls();
    }

    private void setZoomSliderValue(double zoom) {
        int sliderValue = Math.max(10, (int) Math.round(zoom * 100));
        updatingZoomSlider = true;
        try {
            if (sliderValue > zoomSlider.getMaximum()) {
                int newMax = zoomSlider.getMaximum();
                while (sliderValue > newMax) {
                    newMax *= 2;
                }
                zoomSlider.setMaximum(newMax);
            }
            zoomSlider.setValue(sliderValue);
        } finally {
            updatingZoomSlider = false;
        }
    }

    private void refreshRuntimeInfo() {
        String backendText = canvas.getBackendDescription();
        boolean rendering = canvas.isRenderInProgress();
        backendPillLabel.setText("后端: " + backendText);
        renderPillLabel.setText(rendering ? "状态: 渲染中" : "最近渲染: " + canvas.getLastRenderDurationMillis() + " ms");
        renderPillLabel.setBackground(rendering ? ACCENT_SOFT : SUCCESS_SOFT);
        renderPillLabel.setForeground(rendering ? ACCENT.darker() : SUCCESS.darker());
    }

    private String buildStatusText(double zoom, double offsetX, double offsetY, int mouseX, int mouseY) {
        String mouse = mouseX >= 0 && mouseY >= 0 ? mouseX + ", " + mouseY : "-, -";
        String complex = buildComplexCoordinateText(mouseX, mouseY, zoom, offsetX, offsetY);
        String backend = canvas.getBackendDescription();
        return String.format("鼠标: %s%s | 缩放: %.2fx | 偏移: (%.0f, %.0f) | 后端: %s", mouse, complex, zoom, offsetX, offsetY, backend);
    }

    private String buildComplexCoordinateText(int mouseX, int mouseY, double zoom, double offsetX, double offsetY) {
        if (mouseX < 0 || mouseY < 0) {
            return "";
        }

        FractalDefinition definition = (FractalDefinition) fractalSelector.getSelectedItem();
        if (definition == null || !(definition.renderer() instanceof AbstractEscapeTimeRenderer)) {
            return "";
        }

        AbstractEscapeTimeRenderer renderer = (AbstractEscapeTimeRenderer) definition.renderer();
        double real = renderer.mapPlaneX(mouseX, canvas.getWidth(), zoom, offsetX);
        double imaginary = renderer.mapPlaneY(mouseY, canvas.getWidth(), canvas.getHeight(), zoom, offsetY);
        return String.format(" | 复平面: %.6f %+.6fi", real, imaginary);
    }

    private void normalizeControlHeights() {
        normalizeControlHeight(fractalSelector);
        normalizeControlHeight(depthSpinner);
        zoomSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void normalizeControlHeight(JComponent component) {
        Dimension size = component.getPreferredSize();
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
        component.setPreferredSize(new Dimension(Integer.MAX_VALUE, size.height));
        component.setMinimumSize(new Dimension(120, size.height));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(CARD_BACKGROUND);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private JPanel createCard(BorderLayout layout) {
        JPanel card = new JPanel(layout);
        card.setOpaque(true);
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private JLabel createCardTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 17));
        label.setForeground(TITLE_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(TITLE_COLOR);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED_TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createMutedLabel(String text, int width) {
        JLabel label = new JLabel("<html><body style='width:" + width + "px'>" + text + "</body></html>");
        label.setForeground(MUTED_TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createPillLabel(Color background, Color foreground) {
        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        return label;
    }

    private JLabel createInlineBadge(String text, Color background, Color foreground) {
        JLabel label = createPillLabel(background, foreground);
        label.setText(text);
        return label;
    }

    private JButton createPrimaryButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        button.setFocusPainted(false);
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        return button;
    }

    private JButton createSecondaryButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        button.setFocusPainted(false);
        button.setBackground(CARD_BACKGROUND);
        button.setForeground(TITLE_COLOR);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        return button;
    }

    private Component createVerticalGap(int height) {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setPreferredSize(new Dimension(0, height));
        gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        gap.setMinimumSize(new Dimension(0, height));
        return gap;
    }
}