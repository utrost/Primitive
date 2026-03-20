package org.trostheide.primitive.gui;

import org.trostheide.primitive.PrimitiveRunner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Hashtable;

public class SettingsPanel extends JPanel {

    private final JSlider shapeCountSlider;
    private final JLabel shapeCountValue;
    private final JComboBox<PrimitiveRunner.Mode> modeComboBox;
    private final JSpinner workerSpinner;
    private final JButton startButton;
    private final JButton loadButton;
    private final ActionListener onStop;
    private final ActionListener onStart;
    private boolean isRunning = false;

    public SettingsPanel(ActionListener onStart, ActionListener onStop) {
        this.onStart = onStart;
        this.onStop = onStop;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setPreferredSize(new Dimension(280, 0));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // -- Title --
        JLabel title = new JLabel("Settings");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(LEFT_ALIGNMENT);
        title.setBorder(new EmptyBorder(0, 0, 16, 0));
        content.add(title);

        // -- Load Image button (prominent) --
        loadButton = new JButton("Open Image...");
        loadButton.setAlignmentX(LEFT_ALIGNMENT);
        loadButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loadButton.setFont(loadButton.getFont().deriveFont(Font.BOLD, 14f));
        loadButton.setFocusPainted(false);
        loadButton.putClientProperty("JButton.buttonType", "roundRect");
        content.add(loadButton);
        content.add(Box.createVerticalStrut(16));

        content.add(createSeparator());
        content.add(Box.createVerticalStrut(12));

        // -- Mode --
        content.add(createSectionLabel("Shape Mode"));
        content.add(Box.createVerticalStrut(4));
        modeComboBox = new JComboBox<>(PrimitiveRunner.Mode.values());
        modeComboBox.setSelectedItem(PrimitiveRunner.Mode.TRIANGLE);
        modeComboBox.setAlignmentX(LEFT_ALIGNMENT);
        modeComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        content.add(modeComboBox);
        content.add(Box.createVerticalStrut(16));

        // -- Shape Count --
        JPanel shapeHeader = new JPanel(new BorderLayout());
        shapeHeader.setAlignmentX(LEFT_ALIGNMENT);
        shapeHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        shapeHeader.setOpaque(false);
        JLabel shapesLabel = createSectionLabel("Number of Shapes");
        shapeCountValue = new JLabel("100");
        shapeCountValue.setFont(shapeCountValue.getFont().deriveFont(Font.BOLD, 13f));
        shapeHeader.add(shapesLabel, BorderLayout.WEST);
        shapeHeader.add(shapeCountValue, BorderLayout.EAST);
        content.add(shapeHeader);
        content.add(Box.createVerticalStrut(4));

        shapeCountSlider = new JSlider(50, 2000, 100);
        shapeCountSlider.setAlignmentX(LEFT_ALIGNMENT);
        shapeCountSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        shapeCountSlider.setMajorTickSpacing(500);
        shapeCountSlider.setMinorTickSpacing(50);
        shapeCountSlider.setPaintTicks(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(50, new JLabel("50"));
        labelTable.put(500, new JLabel("500"));
        labelTable.put(1000, new JLabel("1k"));
        labelTable.put(1500, new JLabel("1.5k"));
        labelTable.put(2000, new JLabel("2k"));
        shapeCountSlider.setLabelTable(labelTable);
        shapeCountSlider.setPaintLabels(true);

        shapeCountSlider.addChangeListener(e -> shapeCountValue.setText(String.valueOf(shapeCountSlider.getValue())));
        content.add(shapeCountSlider);
        content.add(Box.createVerticalStrut(16));

        // -- Workers --
        content.add(createSectionLabel("Worker Threads"));
        content.add(Box.createVerticalStrut(4));
        int cores = Runtime.getRuntime().availableProcessors();
        workerSpinner = new JSpinner(new SpinnerNumberModel(cores, 1, 128, 1));
        workerSpinner.setAlignmentX(LEFT_ALIGNMENT);
        workerSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        content.add(workerSpinner);

        content.add(Box.createVerticalStrut(16));
        content.add(createSeparator());
        content.add(Box.createVerticalStrut(12));

        // -- Keyboard shortcuts hint --
        JLabel hintLabel = new JLabel("<html><small>Ctrl+Enter: Start &nbsp; Esc: Stop<br>Ctrl+O: Open &nbsp; Ctrl+T: Theme</small></html>");
        hintLabel.setForeground(ThemeManager.getSubtleText());
        hintLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(hintLabel);

        content.add(Box.createVerticalGlue());

        add(content, BorderLayout.CENTER);

        // -- Start/Stop Button --
        startButton = new JButton("Start Processing");
        startButton.addActionListener(onStart);
        startButton.setFont(startButton.getFont().deriveFont(Font.BOLD, 14f));
        startButton.setFocusPainted(false);
        startButton.setPreferredSize(new Dimension(0, 44));
        startButton.putClientProperty("JButton.buttonType", "roundRect");
        startButton.setBackground(ThemeManager.getAccentColor());
        startButton.setForeground(Color.WHITE);

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBorder(new EmptyBorder(12, 0, 0, 0));
        btnPanel.add(startButton, BorderLayout.CENTER);

        add(btnPanel, BorderLayout.SOUTH);

        // Update hint on theme change
        ThemeManager.addChangeListener(() -> hintLabel.setForeground(ThemeManager.getSubtleText()));
    }

    public void setLoadAction(ActionListener action) {
        for (ActionListener al : loadButton.getActionListeners())
            loadButton.removeActionListener(al);
        loadButton.addActionListener(action);
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setForeground(ThemeManager.getSubtleText());
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator();
        sep.setAlignmentX(LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    public int getShapeCount() {
        return shapeCountSlider.getValue();
    }

    public PrimitiveRunner.Mode getSelectedMode() {
        return (PrimitiveRunner.Mode) modeComboBox.getSelectedItem();
    }

    public int getWorkerCount() {
        return (Integer) workerSpinner.getValue();
    }

    public void setControlsEnabled(boolean enabled) {
        shapeCountSlider.setEnabled(enabled);
        modeComboBox.setEnabled(enabled);
        workerSpinner.setEnabled(enabled);
        loadButton.setEnabled(enabled);
    }

    public void setRunning(boolean running) {
        isRunning = running;
        setControlsEnabled(!running);

        // Swap button action and text
        for (ActionListener al : startButton.getActionListeners())
            startButton.removeActionListener(al);

        if (running) {
            startButton.setText("Stop Processing");
            startButton.setBackground(ThemeManager.getErrorColor());
            startButton.addActionListener(onStop);
        } else {
            startButton.setText("Start Processing");
            startButton.setBackground(ThemeManager.getAccentColor());
            startButton.addActionListener(onStart);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
