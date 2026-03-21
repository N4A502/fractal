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
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

public class FractalFrame extends JFrame {

    private static final int DEFAULT_ZOOM_SLIDER_MAX = 400;
    private static final Color CARD_BORDER = new Color(220, 226, 235);
    private static final Color CARD_BACKGROUND = new Color(248, 250, 253);

    private final FractalCanvas canvas;
    private final JComboBox<FractalDefinition> fractalSelector;
    private final JSpinner depthSpinner;
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
        this.depthSpinner = new JSpinner(new SpinnerNumberModel(5, 1, null, 1));
        this.zoomSlider = new JSlider(10, DEFAULT_ZOOM_SLIDER_MAX, 100);
        this.categoryLabel = new JLabel();
        this.descriptionLabel = new JLabel();
        this.depthValueLabel = new JLabel("", SwingConstants.RIGHT);
        this.zoomValueLabel = new JLabel("", SwingConstants.RIGHT);
        this.statusLabel = new JLabel("鼠标: -, - | 缩放: 1.00x | 偏移: (0, 0)");

        normalizeControlHeights();

        add(buildControlPanel(), BorderLayout.WEST);
        add(canvas, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        fractalSelector.addActionListener(e -> syncSelection());
        depthSpinner.addChangeListener(e -> syncControls());
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

    private void normalizeControlHeights() {
        normalizeControlHeight(fractalSelector);
        normalizeControlHeight(depthSpinner);
    }

    private void normalizeControlHeight(JComponent component) {
        Dimension size = component.getPreferredSize();
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
        component.setPreferredSize(new Dimension(Integer.MAX_VALUE, size.height));
        component.setMinimumSize(new Dimension(120, size.height));
        component.setAlignmentX(LEFT_ALIGNMENT);
    }

    private JPanel buildControlPanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(320, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(createSectionLabel("分形类型"));
        panel.add(fractalSelector);
        panel.add(createSpacer());

        panel.add(createSectionLabel("分类"));
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 2));
        categoryLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(categoryLabel);
        panel.add(createSpacer());

        panel.add(createSectionLabel("说明"));
        descriptionLabel.setVerticalAlignment(SwingConstants.TOP);
        descriptionLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        descriptionLabel.setOpaque(true);
        descriptionLabel.setBackground(Color.WHITE);
        descriptionLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(descriptionLabel);
        panel.add(createSpacer());

        panel.add(createSectionLabel("层级 / 迭代"));
        depthValueLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(depthValueLabel);
        panel.add(depthSpinner);
        panel.add(createSpacer());

        panel.add(createSectionLabel("缩放"));
        zoomValueLabel.setAlignmentX(LEFT_ALIGNMENT);
        zoomSlider.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(zoomValueLabel);
        panel.add(zoomSlider);
        panel.add(createSpacer());

        JButton resetButton = new JButton("重置视图");
        resetButton.addActionListener(e -> resetControls());
        JButton exportButton = new JButton("高分辨率导出");
        exportButton.addActionListener(e -> canvas.exportHighResolutionView());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
        buttonPanel.add(resetButton);
        buttonPanel.add(new JLabel("  "));
        buttonPanel.add(exportButton);
        panel.add(buttonPanel);
        panel.add(createSpacer());

        panel.add(createInfoCard(
                "交互说明",
                "<html>1. 左键拖拽平移<br/>2. 滚轮以鼠标为中心无极缩放<br/>3. Shift + 拖拽框选缩放<br/>4. 双击重置视图<br/>5. 右键导出 PNG / 高分辨率 PNG</html>"
        ));
        panel.add(createSpacer());

        panel.add(createInfoCard(
                "导出说明",
                "<html>支持当前尺寸、2x、4x 与自定义宽高 PNG 导出</html>"
        ));
        panel.add(createSpacer());

        panel.add(createInfoCard(
                "分类覆盖",
                "<html>1. 逃逸时间分形<br/>2. 递归几何分形<br/>3. L-System / 规则替换分形</html>"
        ));

        return panel;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createInfoCard(String title, String content) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setOpaque(true);
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        JLabel contentLabel = new JLabel(content);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(contentLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSpacer() {
        JPanel spacer = new JPanel(new BorderLayout());
        spacer.setPreferredSize(new Dimension(0, 12));
        spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        spacer.setAlignmentX(LEFT_ALIGNMENT);
        return spacer;
    }

    private void syncSelection() {
        FractalDefinition definition = (FractalDefinition) fractalSelector.getSelectedItem();
        if (definition == null) {
            return;
        }

        categoryLabel.setText(definition.category());
        descriptionLabel.setText("<html><body style='width:240px'>" + definition.description() + "</body></html>");
        ((SpinnerNumberModel) depthSpinner.getModel()).setMinimum(definition.minDepth());
        depthSpinner.setValue(Math.max(definition.defaultDepth(), definition.minDepth()));
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

        int depth = ((Number) depthSpinner.getValue()).intValue();
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
        depthSpinner.setValue(Math.max(definition.defaultDepth(), definition.minDepth()));
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
