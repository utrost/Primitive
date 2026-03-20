package org.trostheide.primitive.gui;

import org.trostheide.primitive.PrimitiveConfig;
import org.trostheide.primitive.PrimitiveRunner;
import org.trostheide.primitive.OptimizationListener;
import org.trostheide.primitive.core.ShapeResult;
import org.trostheide.primitive.image.RgbaImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.util.List;

public class PrimitiveFrame extends JFrame {

    private final ImagePanel imagePanel;
    private final SettingsPanel settingsPanel;
    private final JTextArea logArea;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JToggleButton themeToggle;
    private File currentFile;
    private RgbaImage lastGeneratedImage;
    private PrimitiveRunner currentRunner;

    public PrimitiveFrame() {
        super("Primitive");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        // -- Components --
        imagePanel = new ImagePanel();
        settingsPanel = new SettingsPanel(e -> startProcessing(), e -> stopProcessing());

        // -- Log area --
        logArea = new JTextArea(4, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        logArea.putClientProperty("JTextArea.roundedCorners", true);

        // -- Progress bar --
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setValue(0);
        progressBar.setPreferredSize(new Dimension(0, 24));

        // -- Status label --
        statusLabel = new JLabel("Load an image to begin");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
        statusLabel.setForeground(ThemeManager.getSubtleText());

        // -- Toolbar --
        JToolBar toolBar = createToolBar();

        // -- Theme toggle --
        themeToggle = new JToggleButton(ThemeManager.isDark() ? "\u263E" : "\u2600");
        themeToggle.setSelected(ThemeManager.isDark());
        themeToggle.setToolTipText("Toggle Light/Dark Theme");
        themeToggle.setFont(themeToggle.getFont().deriveFont(16f));
        themeToggle.setFocusPainted(false);
        themeToggle.addActionListener(e -> {
            ThemeManager.toggle();
            themeToggle.setText(ThemeManager.isDark() ? "\u263E" : "\u2600");
            statusLabel.setForeground(ThemeManager.getSubtleText());
            imagePanel.repaint();
        });
        toolBar.addSeparator();
        toolBar.add(themeToggle);

        // -- Bottom panel (log + progress + status) --
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                "Processing Log"));
        bottomPanel.add(logScroll, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout(8, 0));
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        statusBar.add(progressBar, BorderLayout.CENTER);
        statusBar.add(statusLabel, BorderLayout.EAST);
        bottomPanel.add(statusBar, BorderLayout.SOUTH);

        // -- Main content (image + settings side by side) --
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, settingsPanel, imagePanel);
        splitPane.setDividerLocation(280);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        // -- Layout --
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // -- Menu --
        setJMenuBar(createMenuBar());

        // -- Wire load button --
        settingsPanel.setLoadAction(e -> openImage());

        // -- Drag and Drop --
        setupDragAndDrop();

        // -- Keyboard shortcuts --
        setupKeyboardShortcuts();

        // -- Theme listener --
        ThemeManager.addChangeListener(() -> {
            logScroll.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                    "Processing Log"));
        });
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JButton openBtn = new JButton("Open Image");
        openBtn.setToolTipText("Open an image file (Ctrl+O)");
        openBtn.setFocusPainted(false);
        openBtn.addActionListener(e -> openImage());

        JButton saveBtn = new JButton("Save Output");
        saveBtn.setToolTipText("Save generated image (Ctrl+S)");
        saveBtn.setFocusPainted(false);
        saveBtn.addActionListener(e -> saveImage());

        toolBar.add(openBtn);
        toolBar.addSeparator(new Dimension(8, 0));
        toolBar.add(saveBtn);

        return toolBar;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem openItem = new JMenuItem("Open Image...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> openImage());
        fileMenu.add(openItem);

        JMenuItem saveItem = new JMenuItem("Save Image...");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveImage());
        fileMenu.add(saveItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // View menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        JMenuItem toggleThemeItem = new JMenuItem("Toggle Theme");
        toggleThemeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        toggleThemeItem.addActionListener(e -> {
            ThemeManager.toggle();
            themeToggle.setSelected(ThemeManager.isDark());
            themeToggle.setText(ThemeManager.isDark() ? "\u263E" : "\u2600");
            statusLabel.setForeground(ThemeManager.getSubtleText());
            imagePanel.repaint();
        });
        viewMenu.add(toggleThemeItem);

        JMenuItem clearLogItem = new JMenuItem("Clear Log");
        clearLogItem.addActionListener(e -> logArea.setText(""));
        viewMenu.add(clearLogItem);

        menuBar.add(viewMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Primitive Java\n\n" +
                "Image-to-vector art generator using geometric primitives.\n" +
                "Based on Michael Fogleman's Primitive.\n\n" +
                "Drag & drop or use File > Open to load an image.",
                "About Primitive", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        return menuBar;
    }

    private void setupDragAndDrop() {
        new DropTarget(this, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent dtde) {
                try {
                    Transferable tr = dtde.getTransferable();
                    DataFlavor[] flavors = tr.getTransferDataFlavors();
                    for (DataFlavor flavor : flavors) {
                        if (flavor.isFlavorJavaFileListType()) {
                            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                            @SuppressWarnings("unchecked")
                            List<File> list = (List<File>) tr.getTransferData(flavor);
                            if (list != null && !list.isEmpty()) {
                                loadFile(list.get(0));
                            }
                            dtde.dropComplete(true);
                            return;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                dtde.rejectDrop();
            }
        });
    }

    private void setupKeyboardShortcuts() {
        // Ctrl+Enter to start processing
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "startProcessing");
        getRootPane().getActionMap().put("startProcessing", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startProcessing();
            }
        });

        // Escape to stop processing
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "stopProcessing");
        getRootPane().getActionMap().put("stopProcessing", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopProcessing();
            }
        });
    }

    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        chooser.setDialogTitle("Open Image");
        chooser.setFileFilter(new FileNameExtensionFilter("Image files (PNG, JPG, BMP, GIF)", "png", "jpg", "jpeg", "bmp", "gif"));
        chooser.setAcceptAllFileFilterUsed(true);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            loadFile(chooser.getSelectedFile());
        }
    }

    private void loadFile(File file) {
        try {
            var img = ImageIO.read(file);
            if (img != null) {
                currentFile = file;
                imagePanel.setImage(img);
                setTitle("Primitive \u2014 " + file.getName());
                statusLabel.setText(file.getName() + " (" + img.getWidth() + "\u00D7" + img.getHeight() + ")");
                statusLabel.setForeground(ThemeManager.getSubtleText());
                logArea.append("Loaded: " + file.getName() + " (" + img.getWidth() + "\u00D7" + img.getHeight() + ")\n");
                progressBar.setValue(0);
                progressBar.setString("Ready");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Could not load image: " + file.getName() + "\nSupported formats: PNG, JPG, BMP, GIF",
                        "Unsupported Format", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveImage() {
        if (lastGeneratedImage == null) {
            JOptionPane.showMessageDialog(this, "No generated image to save.\nRun processing first.",
                    "Nothing to Save", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        chooser.setSelectedFile(new File("output.png"));
        chooser.setDialogTitle("Save Generated Image");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Image", "png"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                lastGeneratedImage.save(file);

                if (currentRunner != null) {
                    String svg = currentRunner.getSvgContent();
                    if (svg != null && !svg.isEmpty()) {
                        String absPath = file.getAbsolutePath();
                        String svgPath;
                        if (absPath.toLowerCase().endsWith(".png")) {
                            svgPath = absPath.substring(0, absPath.length() - 4) + ".svg";
                        } else {
                            svgPath = absPath + ".svg";
                        }
                        try (java.io.FileWriter fw = new java.io.FileWriter(svgPath)) {
                            fw.write(svg);
                        }
                        JOptionPane.showMessageDialog(this,
                                "Saved PNG and SVG:\n" + file.getName() + "\n" + new File(svgPath).getName(),
                                "Saved", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                }

                JOptionPane.showMessageDialog(this, "Saved to " + file.getName(),
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void stopProcessing() {
        if (currentRunner != null) {
            currentRunner.stop();
            setTitle("Primitive \u2014 Stopping...");
            progressBar.setString("Stopping...");
        }
    }

    private void startProcessing() {
        if (currentFile == null) {
            JOptionPane.showMessageDialog(this,
                    "Please open an image first.\n\nUse the Open Image button, File > Open, or drag & drop.",
                    "No Image Loaded", JOptionPane.WARNING_MESSAGE);
            return;
        }

        settingsPanel.setRunning(true);

        int shapes = settingsPanel.getShapeCount();
        PrimitiveRunner.Mode mode = settingsPanel.getSelectedMode();
        int threads = settingsPanel.getWorkerCount();

        PrimitiveConfig config = new PrimitiveConfig(shapes, mode, threads);

        progressBar.setMaximum(shapes);
        progressBar.setValue(0);
        progressBar.setString("Starting...");

        new Thread(() -> {
            try {
                RgbaImage target = RgbaImage.load(currentFile);
                currentRunner = new PrimitiveRunner(target, config);

                currentRunner.setListener(new OptimizationListener() {
                    @Override
                    public void onStart(int totalShapes, PrimitiveRunner.Mode mode, int numWorkers) {
                        SwingUtilities.invokeLater(() -> {
                            logArea.setText("");
                            logArea.append(String.format("Processing: %d shapes (%s) with %d workers%n",
                                    totalShapes, mode, numWorkers));
                            progressBar.setString("Processing...");
                        });
                    }

                    @Override
                    public void onShapeCommitted(int step, int totalSteps, ShapeResult result, long timeMs,
                            RgbaImage currentImage) {
                        lastGeneratedImage = currentImage;
                        BufferedImage img = currentImage.toBufferedImage();
                        SwingUtilities.invokeLater(() -> {
                            imagePanel.setImage(img);
                            setTitle(String.format("Primitive \u2014 %d/%d shapes", step, totalSteps));
                            progressBar.setValue(step);
                            progressBar.setString(String.format("%d/%d (%d%%)", step, totalSteps,
                                    (int) (100.0 * step / totalSteps)));
                            logArea.append(String.format("Shape %d/%d  \u0394score: %d  %dms%n",
                                    step, totalSteps, result.energyDelta(), timeMs));
                            logArea.setCaretPosition(logArea.getDocument().getLength());
                        });
                    }

                    @Override
                    public void onComplete(File pngFile, File svgFile) {
                        SwingUtilities.invokeLater(() -> {
                            settingsPanel.setRunning(false);
                            setTitle("Primitive \u2014 Complete!");
                            progressBar.setValue(progressBar.getMaximum());
                            progressBar.setString("Complete!");
                            statusLabel.setText("Generation complete");
                            statusLabel.setForeground(ThemeManager.getSuccessColor());
                            logArea.append("Generation complete!\n");
                            if (pngFile != null && pngFile.exists()) {
                                logArea.append("Output: " + pngFile.getName() + "\n");
                            }
                        });
                    }
                });

                File tempOut = File.createTempFile("primitive_out", ".png");
                currentRunner.run(tempOut);
                tempOut.delete();

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(PrimitiveFrame.this, "Error: " + e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    settingsPanel.setRunning(false);
                    progressBar.setString("Error");
                    progressBar.setValue(0);
                });
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ThemeManager.initialize();
            new PrimitiveFrame().setVisible(true);
        });
    }
}
