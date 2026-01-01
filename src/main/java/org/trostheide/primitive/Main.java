package org.trostheide.primitive;

import org.trostheide.primitive.core.ShapeResult;
import org.trostheide.primitive.image.RgbaImage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        List<String> positionalArgs = new ArrayList<>();
        int requestedWorkers = Runtime.getRuntime().availableProcessors();

        // 0. Parse Arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--cores")) {
                if (i + 1 < args.length) {
                    String val = args[i + 1];
                    if (val.equalsIgnoreCase("all")) {
                        requestedWorkers = Runtime.getRuntime().availableProcessors();
                    } else {
                        try {
                            requestedWorkers = Integer.parseInt(val);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid number for --cores: " + val);
                            System.exit(1);
                        }
                    }
                    i++; // Skip value
                } else {
                    System.err.println("Missing value for --cores");
                    System.exit(1);
                }
            } else {
                positionalArgs.add(args[i]);
            }
        }

        if (positionalArgs.size() < 3) {
            System.out.println("Usage: java -jar primitive.jar [options] <input.png> <output.png> <num_shapes> [mode]");
            System.out.println("Options:");
            System.out.println("  --cores <number|all>  Number of worker threads (default: all)");
            System.out.println("Modes: triangle, line, bezier, rect, polyline, ellipse, combo (default: triangle)");
            System.exit(1);
        }

        try {
            File input = new File(positionalArgs.get(0));
            File output = new File(positionalArgs.get(1));
            int count = Integer.parseInt(positionalArgs.get(2));

            // Parse Mode (default to TRIANGLE if not specified)
            PrimitiveRunner.Mode mode = PrimitiveRunner.Mode.TRIANGLE;

            if (positionalArgs.size() > 3) {
                String modeStr = positionalArgs.get(3).toUpperCase(Locale.ROOT);
                try {
                    mode = PrimitiveRunner.Mode.valueOf(modeStr);
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown mode: " + positionalArgs.get(3) + ". Using TRIANGLE.");
                    System.err.println("Available modes: TRIANGLE, LINE, BEZIER, RECT, POLYLINE, ELLIPSE, COMBO");
                    mode = PrimitiveRunner.Mode.TRIANGLE;
                }
            }

            // 1. Load Input
            RgbaImage target = RgbaImage.load(input);

            // 2. Configure
            PrimitiveConfig config = new PrimitiveConfig(count, mode, requestedWorkers);

            // 3. Run
            PrimitiveRunner runner = new PrimitiveRunner(target, config);

            // 4. Attach Listener for Console Output
            runner.setListener(new OptimizationListener() {
                @Override
                public void onStart(int totalShapes, PrimitiveRunner.Mode mode, int numWorkers) {
                    System.out.println("Starting processing: " + totalShapes + " shapes (" + mode + ") with "
                            + numWorkers + " workers.");
                }

                @Override
                public void onShapeCommitted(int shapeNumber, int totalShapes, ShapeResult result, long timeElapsedMs,
                        RgbaImage currentImage) {
                    System.out.printf("Shape %d/%d added. Score Delta: %d. Time: %dms%n",
                            shapeNumber, totalShapes, result.energyDelta(), timeElapsedMs);
                }

                @Override
                public void onComplete(File pngFile, File svgFile) {
                    System.out.println("Done! Saved PNG to " + pngFile.getAbsolutePath());
                    System.out.println("Done! Saved SVG to " + svgFile.getAbsolutePath());
                }
            });

            runner.run(output);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}