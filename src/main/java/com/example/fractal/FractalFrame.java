package com.example.fractal;

import com.example.fractal.model.FractalDefinition;
import com.example.fractal.model.FractalRegistry;
import com.example.fractal.render.AbstractEscapeTimeRenderer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

public class FractalFrame extends JFrame {

    private static final int DEFAULT_ZOOM_SLIDER_MAX = 400;

    private final FractalCanvas canvas;
    private final JComboBox<FractalDefinition> fractalSelector;
    private final JSlider depthSlider;
    private final JSlider zoomSlider;
    private final JLabel categoryLabel;
    private final JLabel descriptionLabel;
    private final JLabel depthValueLabel;
    private final JLabel zoomValueLabel;
    private final JLabel statusLabel;

    public FractalFrame() {
        super("Fractal Explorer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1200, 820));
        setLayout(new BorderLayout());

        List<FractalDefinition> fractals = FractalRegistry.createDefinitions();
        this.canvas = new FractalCanvas();
        this.fractalSelector = new JComboBox<FractalDefinition>(fractals.toArray(new FractalDefinition[0]));
        this.depthSlider = new JSlider(1, 9, 5);
        this.zoomSlider = new JSlider(10, DEFAULT_ZOOM_SLIDER_MAX, 100);
        this.categoryLabel = new JLabel();
        this.descriptionLabel = new JLabel();
        this.depthValueLabel = new JLabel("", SwingConstants.RIGHT);
        this.zoomValueLabel = new JLabel("", SwingConstants.RIGHT);
        this.statusLabel = new JLabel("鼠标: -, - | 缩放: 1.00x | 偏移: (0, 0)");

        add(buildControlPanel(), BorderLayout.WEST);
        add(canvas, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        fractalSelector.addActionListener(e -> syncSelection());
        depthSlider.addChangeListener(e -> syncControls());
        zoomSlider.addChangeListener(e -> syncControls());
        canvas.setInteractionListener(new FractalCanvas.InteractionListener() {
            @Override
            public void onZoomChanged(double zoom, int anchorX, int anchorY) {
                canvas.applyZoom(zoom, anchorX, anchorY);
                setZoomSliderValue(zoom);
            }

            @Override
            public void onViewChanged(double zoom, double offsetX, double offsetY, int mouseX, int mouseY) {
                statusLabel.setText(buildStatusText(zoom, offsetX, offsetY, mouseX, mouseY));
            }

            @Override
            public void onResetRequested() {
                resetControls();
            }
        });

        syncSelection();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildControlPanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(320, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(new JLabel("分形类型"));
        panel.add(fractalSelector);
        panel.add(createSpacer());

        panel.add(new JLabel("分类"));
        panel.add(categoryLabel);
        panel.add(createSpacer());

        panel.add(new JLabel("说明"));
        descriptionLabel.setVerticalAlignment(SwingConstants.TOP);
        panel.add(descriptionLabel);
        panel.add(createSpacer());

        panel.add(new JLabel("层级 / 迭代"));
        panel.add(depthValueLabel);
        depthSlider.setMajorTickSpacing(1);
        depthSlider.setPaintTicks(true);
        panel.add(depthSlider);
        panel.add(createSpacer());

        panel.add(new JLabel("缩放"));
        panel.add(zoomValueLabel);
        panel.add(zoomSlider);
        panel.add(createSpacer());

        JButton resetButton = new JButton("重置视图");
        resetButton.addActionListener(e -> resetControls());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.add(resetButton);
        panel.add(buttonPanel);
        panel.add(createSpacer());

        JLabel tipLabel = new JLabel("<html>鼠标操作：<br/>1. 左键拖拽平移<br/>2. 滚轮以鼠标为中心无极缩放<br/>3. Shift + 拖拽框选缩放<br/>4. 双击重置视图</html>");
        tipLabel.setBorder(BorderFactory.createTitledBorder("交互说明"));
        panel.add(tipLabel);
        panel.add(createSpacer());

        JLabel categoryTipLabel = new JLabel("<html>已包含主要分类：<br/>1. 逃逸时间分形<br/>2. 递归几何分形<br/>3. L-System / 规则替换分形</html>");
        categoryTipLabel.setBorder(BorderFactory.createTitledBorder("分类覆盖"));
        panel.add(categoryTipLabel);

        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSpacer() {
        JPanel spacer = new JPanel(new BorderLayout());
        spacer.setPreferredSize(new Dimension(0, 14));
        spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        return spacer;
    }

    private void syncSelection() {
        FractalDefinition definition = (FractalDefinition) fractalSelector.getSelectedItem();
        if (definition == null) {
            return;
        }

        categoryLabel.setText(definition.category());
        descriptionLabel.setText("<html><body style='width:260px'>" + definition.description() + "</body></html>");
        depthSlider.setMinimum(definition.minDepth());
        depthSlider.setMaximum(definition.maxDepth());
        depthSlider.setValue(Math.min(Math.max(definition.defaultDepth(), definition.minDepth()), definition.maxDepth()));
        zoomSlider.setMaximum(Math.max(DEFAULT_ZOOM_SLIDER_MAX, definition.defaultZoom()));
        zoomSlider.setValue(definition.defaultZoom());
        canvas.resetView();
        syncControls();
    }

    private void syncControls() {
        FractalDefinition definition = (FractalDefinition) fractalSelector.getSelectedItem();
        if (definition == null) {
            return;
        }

        int depth = depthSlider.getValue();
        double zoom = zoomSlider.getValue() / 100.0;
        depthValueLabel.setText(depth + " 级");
        zoomValueLabel.setText(String.format("%.2f x", zoom));
        canvas.render(definition, depth, zoom);
    }

    private void resetControls() {
        FractalDefinition definition = (FractalDefinition) fractalSelector.getSelectedItem();
        if (definition == null) {
            return;
        }

        canvas.resetView();
        zoomSlider.setMaximum(Math.max(DEFAULT_ZOOM_SLIDER_MAX, definition.defaultZoom()));
        depthSlider.setValue(definition.defaultDepth());
        zoomSlider.setValue(definition.defaultZoom());
        syncControls();
    }

    private void setZoomSliderValue(double zoom) {
        int sliderValue = Math.max(10, (int) Math.round(zoom * 100));
        if (sliderValue > zoomSlider.getMaximum()) {
            int newMax = zoomSlider.getMaximum();
            while (sliderValue > newMax) {
                newMax *= 2;
            }
            zoomSlider.setMaximum(newMax);
        }
        zoomSlider.setValue(sliderValue);
    }

    private String buildStatusText(double zoom, double offsetX, double offsetY, int mouseX, int mouseY) {
        String mouse = mouseX >= 0 && mouseY >= 0 ? mouseX + ", " + mouseY : "-, -";
        String complex = buildComplexCoordinateText(mouseX, mouseY, zoom, offsetX, offsetY);
        return String.format("鼠标: %s%s | 缩放: %.2fx | 偏移: (%.0f, %.0f)", mouse, complex, zoom, offsetX, offsetY);
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
}