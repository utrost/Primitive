package org.trostheide.primitive.gui;

import org.trostheide.primitive.PrimitiveConfig;
import org.trostheide.primitive.PrimitiveRunner;
import org.trostheide.primitive.OptimizationListener;
import org.trostheide.primitive.core.ShapeResult;
import org.trostheide.primitive.image.RgbaImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
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
    private File currentFile;
    private RgbaImage lastGeneratedImage;

    public PrimitiveFrame() {
        super("Primitive Java");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);

        // -- Components --
        imagePanel = new ImagePanel();
        settingsPanel = new SettingsPanel(e -> startProcessing(), e -> stopProcessing());

        logArea = new JTextArea(5, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Processing Log"));

        // -- Layout --
        setLayout(new BorderLayout());
        add(imagePanel, BorderLayout.CENTER);
        add(settingsPanel, BorderLayout.WEST);
        add(logScroll, BorderLayout.SOUTH);

        // -- Menu --
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem openItem = new JMenuItem("Open Image...");
        openItem.addActionListener(e -> openImage());
        fileMenu.add(openItem);

        JMenuItem saveItem = new JMenuItem("Save Image...");
        saveItem.addActionListener(e -> saveImage());
        fileMenu.add(saveItem);
        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // -- Drag and Drop --
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

    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));

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
                setTitle("Primitive Java - " + file.getName());
                logArea.append("Loaded image: " + file.getName() + "\n");
            } else {
                JOptionPane.showMessageDialog(this, "Could not load image: " + file.getName(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveImage() {
        if (lastGeneratedImage == null) {
            JOptionPane.showMessageDialog(this, "No generated image to save.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setSelectedFile(new File("output.png"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                lastGeneratedImage.save(file);
                // Also save SVG if possible? Accessing runner's SVG might be tricky here.
                // For now just PNG as per RgbaImage capability.
                JOptionPane.showMessageDialog(this, "Image saved to " + file.getName());
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving image: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private PrimitiveRunner currentRunner;

    private void stopProcessing() {
        if (currentRunner != null) {
            currentRunner.stop();
            // Do NOT enable controls here.
            // Wait for onComplete() to fire, which ensures the thread is truly done.
            setTitle("Primitive Java - Stopping...");
        }
    }

    private void startProcessing() {
        if (currentFile == null) {
            JOptionPane.showMessageDialog(this, "Please open an image first.", "No Image", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Disable controls
        settingsPanel.setRunning(true);

        // Get Config
        int shapes = settingsPanel.getShapeCount();
        PrimitiveRunner.Mode mode = settingsPanel.getSelectedMode();
        int threads = settingsPanel.getWorkerCount();

        PrimitiveConfig config = new PrimitiveConfig(shapes, mode, threads);

        new Thread(() -> {
            try {
                RgbaImage target = RgbaImage.load(currentFile);
                currentRunner = new PrimitiveRunner(target, config);

                currentRunner.setListener(new OptimizationListener() {
                    @Override
                    public void onStart(int totalShapes, PrimitiveRunner.Mode mode, int numWorkers) {
                        SwingUtilities.invokeLater(() -> {
                            logArea.setText(""); // Clear previous log
                            logArea.append(String.format("Starting processing: %d shapes (%s) with %d workers.%n",
                                    totalShapes, mode, numWorkers));
                        });
                    }

                    @Override
                    public void onShapeCommitted(int step, int totalSteps, ShapeResult result, long timeMs,
                            RgbaImage currentImage) {
                        // Zero-copy strategy could be used here, but for now toBufferedImage() is fine.
                        lastGeneratedImage = currentImage;
                        BufferedImage img = currentImage.toBufferedImage();
                        SwingUtilities.invokeLater(() -> {
                            imagePanel.setImage(img);
                            setTitle("Primitive Java - Generating " + step + "/" + totalSteps);
                            logArea.append(String.format("Shape %d/%d added. Score Delta: %d. Time: %dms%n",
                                    step, totalSteps, result.energyDelta(), timeMs));
                            // Auto-scroll
                            logArea.setCaretPosition(logArea.getDocument().getLength());
                        });
                    }

                    @Override
                    public void onComplete(File pngFile, File svgFile) {
                        SwingUtilities.invokeLater(() -> {
                            settingsPanel.setRunning(false);
                            setTitle("Primitive Java - Done!");
                            logArea.append("Generation Complete!\n");
                            if (pngFile != null && pngFile.exists()) {
                                logArea.append("Saved to: " + pngFile.getName() + "\n");
                                JOptionPane.showMessageDialog(PrimitiveFrame.this,
                                        "Generation Complete! Saved to " + pngFile.getName());
                            }
                        });
                    }
                });

                // Temp output file
                File tempOut = File.createTempFile("primitive_out", ".png");
                currentRunner.run(tempOut);
                tempOut.delete();

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(PrimitiveFrame.this, "Error: " + e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    settingsPanel.setControlsEnabled(true);
                });
            }
        }).start();
    }

    // Temporary Main for testing Phase 1
    public static void main(String[] args) {
        // Enable Vector API for GUI run if possible, though not strictly needed for
        // Phase 1
        SwingUtilities.invokeLater(() -> {
            try {
                // Set native look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            new PrimitiveFrame().setVisible(true);
        });
    }
}
