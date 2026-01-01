package org.trostheide.primitive.gui;

import org.trostheide.primitive.PrimitiveRunner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class SettingsPanel extends JPanel {

    private final JSlider shapeCountSlider;
    private final JLabel shapeCountLabel;
    private final JComboBox<PrimitiveRunner.Mode> modeComboBox;
    private final JSpinner workerSpinner;
    private final JButton startButton;
    private final ActionListener onStop;
    private final ActionListener onStart;

    public SettingsPanel(ActionListener onStart, ActionListener onStop) {
        this.onStart = onStart;
        this.onStop = onStop;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Configuration"));

        JPanel content = new JPanel(new GridLayout(4, 2, 5, 5));

        // 1. Mode
        content.add(new JLabel("Mode:"));
        modeComboBox = new JComboBox<>(PrimitiveRunner.Mode.values());
        modeComboBox.setSelectedItem(PrimitiveRunner.Mode.TRIANGLE);
        content.add(modeComboBox);

        // 2. Shape Count
        content.add(new JLabel("Shapes:"));
        shapeCountSlider = new JSlider(50, 2000, 100);
        shapeCountLabel = new JLabel("100");
        shapeCountSlider.addChangeListener(e -> shapeCountLabel.setText(String.valueOf(shapeCountSlider.getValue())));

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(shapeCountSlider, BorderLayout.CENTER);
        sliderPanel.add(shapeCountLabel, BorderLayout.EAST);
        content.add(sliderPanel);

        // 3. Workers
        content.add(new JLabel("Workers:"));
        int cores = Runtime.getRuntime().availableProcessors();
        workerSpinner = new JSpinner(new SpinnerNumberModel(cores, 1, 128, 1));
        content.add(workerSpinner);

        add(content, BorderLayout.CENTER);

        // 4. Start Button
        startButton = new JButton("Start Processing");
        startButton.addActionListener(onStart);
        startButton.setFont(startButton.getFont().deriveFont(Font.BOLD, 14f));

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        btnPanel.add(startButton, BorderLayout.CENTER);

        add(btnPanel, BorderLayout.SOUTH);
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
        startButton.setEnabled(true); // Always enabled, just toggles action
    }

    public void setRunning(boolean running) {
        if (running) {
            startButton.setText("Stop Processing");
            // Remove start listeners, add stop listeners - or easier: check text in action
            // listener
            // Ideally: separate logic. But let's keep it simple.
            for (ActionListener al : startButton.getActionListeners())
                startButton.removeActionListener(al);
            startButton.addActionListener(onStop);

            shapeCountSlider.setEnabled(false);
            modeComboBox.setEnabled(false);
            workerSpinner.setEnabled(false);
            startButton.setText("Start Processing");
            for (ActionListener al : startButton.getActionListeners())
                startButton.removeActionListener(al);
            startButton.addActionListener(onStart);

            shapeCountSlider.setEnabled(true);
            modeComboBox.setEnabled(true);
            workerSpinner.setEnabled(true);
        }
    }
}
