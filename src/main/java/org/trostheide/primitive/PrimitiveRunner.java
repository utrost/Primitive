package org.trostheide.primitive;

import org.trostheide.primitive.core.Optimizer;
import org.trostheide.primitive.core.ShapeResult;
import org.trostheide.primitive.image.RgbaImage;
import org.trostheide.primitive.raster.ScanlineBuffer;
import org.trostheide.primitive.shape.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PrimitiveRunner {

    public enum Mode {
        TRIANGLE, LINE, BEZIER, COMBO
    }

    private final RgbaImage target;
    private final RgbaImage current;
    private final int numShapes;
    private final int numWorkers;
    private final ExecutorService executor;
    private final Mode mode;

    // SVG Building
    private final StringBuilder svgContent = new StringBuilder();

    public PrimitiveRunner(RgbaImage target, int numShapes, Mode mode) {
        this.target = target;
        this.numShapes = numShapes;
        this.mode = mode;
        // Initialize current canvas as blank (opaque white)
        this.current = new RgbaImage(target.width, target.height);
        java.util.Arrays.fill(this.current.pixels, 0xFFFFFFFF);

        // Match threads to CPU cores for embarrassingly parallel workload
        this.numWorkers = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(numWorkers);

        // Init SVG Header
        svgContent.append(String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"%d\" height=\"%d\">\n",
                target.width, target.height
        ));
        // Add white background rect to SVG so it looks correct in browsers
        svgContent.append(String.format(
                "<rect width=\"%d\" height=\"%d\" fill=\"#FFFFFF\" />\n", target.width, target.height
        ));
    }

    public void run(File outputFile) throws IOException, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();
        System.out.println("Starting processing: " + numShapes + " shapes (" + mode + ") with " + numWorkers + " workers.");

        // Reusable buffer for the final commit to the canvas
        ScanlineBuffer commitBuffer = new ScanlineBuffer(target.height);

        for (int i = 0; i < numShapes; i++) {
            // 1. Define the tasks for this step
            List<Callable<ShapeResult>> tasks = new ArrayList<>();
            for (int w = 0; w < numWorkers; w++) {
                tasks.add(createWorkerTask());
            }

            // 2. Run workers in parallel and wait for results
            List<Future<ShapeResult>> results = executor.invokeAll(tasks);

            // 3. Find the global best result among all workers
            ShapeResult bestResult = null;

            for (Future<ShapeResult> future : results) {
                ShapeResult result = future.get();
                // "Energy Delta" logic: We want the lowest score (most negative delta).
                if (bestResult == null || result.energyDelta() < bestResult.energyDelta()) {
                    bestResult = result;
                }
            }

            // 4. Commit the winner to the main canvas
            if (bestResult != null) {
                commitBuffer.reset();

                // Rasterize the winning shape one last time to get scanlines for drawing
                bestResult.shape().rasterize(commitBuffer, target.width, target.height);

                // Use the optimal color calculated by the worker
                current.fillScanlines(commitBuffer, bestResult.color());

                // Append to SVG string builder
                appendSvg(bestResult);

                System.out.printf("Shape %d/%d added. Score Delta: %d. Time: %dms%n",
                        (i + 1), numShapes, bestResult.energyDelta(), (System.currentTimeMillis() - startTime));
            }
        }

        // 5. Cleanup and Save
        executor.shutdown();
        current.save(outputFile);

        // Save SVG File
        File svgFile = new File(outputFile.getParent(), outputFile.getName().replace(".png", ".svg"));
        saveSvg(svgFile);

        System.out.println("Done! Saved PNG to " + outputFile.getAbsolutePath());
        System.out.println("Done! Saved SVG to " + svgFile.getAbsolutePath());
    }

    private void appendSvg(ShapeResult result) {
        int c = result.color();
        // Extract hex color from ARGB int
        String hex = String.format("#%02x%02x%02x", (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
        double opacity = 0.5; // Fixed alpha 128 is approx 0.5 opacity
        svgContent.append(result.shape().toSvg(hex, opacity)).append("\n");
    }

    private void saveSvg(File file) throws IOException {
        svgContent.append("</svg>");
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(svgContent.toString());
        }
    }

    private Callable<ShapeResult> createWorkerTask() {
        return () -> {
            // Thread-Local allocations to prevent race conditions
            ScanlineBuffer localBuffer = new ScanlineBuffer(target.height);
            Optimizer localOptimizer = new Optimizer(target, current, localBuffer);
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            Shape randomShape;
            int type;

            // Determine shape type based on selected Mode
            if (mode == Mode.TRIANGLE) type = 0;
            else if (mode == Mode.LINE) type = 1;
            else if (mode == Mode.BEZIER) type = 2;
            else { // Mode.COMBO
                type = rnd.nextInt(3); // Randomly pick 0=Triangle, 1=Line, 2=Bezier
            }

            if (type == 0) {
                Triangle t = new Triangle();
                t.x1 = rnd.nextDouble(target.width); t.y1 = rnd.nextDouble(target.height);
                t.x2 = rnd.nextDouble(target.width); t.y2 = rnd.nextDouble(target.height);
                t.x3 = rnd.nextDouble(target.width); t.y3 = rnd.nextDouble(target.height);
                randomShape = t;
            } else if (type == 1) {
                double x1 = rnd.nextDouble(target.width); double y1 = rnd.nextDouble(target.height);
                double x2 = rnd.nextDouble(target.width); double y2 = rnd.nextDouble(target.height);
                // Random width between 1 and 9
                randomShape = new Line(x1, y1, x2, y2, 1 + rnd.nextDouble(8));
            } else {
                double x1 = rnd.nextDouble(target.width); double y1 = rnd.nextDouble(target.height);
                double cx = rnd.nextDouble(target.width); double cy = rnd.nextDouble(target.height);
                double x2 = rnd.nextDouble(target.width); double y2 = rnd.nextDouble(target.height);
                // Random width between 1 and 9
                randomShape = new QuadraticBezier(x1, y1, cx, cy, x2, y2, 1 + rnd.nextDouble(8));
            }

            // Run Hill Climbing optimization (1000 mutations per shape)
            return localOptimizer.optimize(randomShape, 1000);
        };
    }
}