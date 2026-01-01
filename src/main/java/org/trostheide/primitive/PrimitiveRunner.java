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
        TRIANGLE, LINE, BEZIER, RECT, POLYLINE, ELLIPSE, COMBO
    }

    private final RgbaImage target;
    private final RgbaImage current;
    private final PrimitiveConfig config;
    private final ExecutorService executor;
    private OptimizationListener listener;
    private volatile boolean running = true;

    // SVG Building
    private final StringBuilder svgContent = new StringBuilder();

    public PrimitiveRunner(RgbaImage target, PrimitiveConfig config) {
        this.target = target;
        this.config = config;
        this.listener = null; // Optional listener

        // Initialize current canvas as blank (opaque white)
        this.current = new RgbaImage(target.width, target.height);
        java.util.Arrays.fill(this.current.pixels, 0xFFFFFFFF);

        // Match threads to CPU cores for embarrassingly parallel workload
        this.executor = Executors.newFixedThreadPool(config.numWorkers());

        // Init SVG Header
        svgContent.append(String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"%d\" height=\"%d\">\n",
                target.width, target.height));
        // Add white background rect to SVG so it looks correct in browsers
        svgContent.append(String.format(
                "<rect width=\"%d\" height=\"%d\" fill=\"#FFFFFF\" />\n", target.width, target.height));
    }

    public void setListener(OptimizationListener listener) {
        this.listener = listener;
    }

    public void stop() {
        this.running = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    public void run(File outputFile) throws IOException, InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();

        if (listener != null) {
            listener.onStart(config.numShapes(), config.mode(), config.numWorkers());
        }

        // Reusable buffer for the final commit to the canvas
        ScanlineBuffer commitBuffer = new ScanlineBuffer(target.height);

        for (int i = 0; i < config.numShapes(); i++) {
            if (!running)
                break;

            // 1. Define the tasks for this step
            List<Callable<ShapeResult>> tasks = new ArrayList<>();
            for (int w = 0; w < config.numWorkers(); w++) {
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

                if (listener != null) {
                    listener.onShapeCommitted(i + 1, config.numShapes(), bestResult,
                            System.currentTimeMillis() - startTime, current);
                }
            }
        }

        // 5. Cleanup and Save
        executor.shutdown();
        current.save(outputFile);

        // Save SVG File
        File svgFile = new File(outputFile.getParent(), outputFile.getName().replace(".png", ".svg"));
        saveSvg(svgFile);

        if (listener != null) {
            listener.onComplete(outputFile, svgFile);
        }
    }

    private void appendSvg(ShapeResult result) {
        int c = result.color();
        // Extract hex color from ARGB int
        String hex = String.format("#%02x%02x%02x", (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
        double opacity = 0.5; // Fixed alpha 128 is approx 0.5 opacity

        synchronized (svgContent) {
            svgContent.append(result.shape().toSvg(hex, opacity)).append("\n");
        }
    }

    private void saveSvg(File file) throws IOException {
        String content;
        synchronized (svgContent) {
            // We append the closing tag temporarily? No, better to just append it to the
            // string written to file.
            // But here we are at the end of run, so we can append it permanently?
            // Wait, if we use getSvgContent(), we need the closing tag too.
            // Let's NOT append it permanently in saveSvg if we want getSvgContent to be
            // usable mid-run.
            // Actually, run() finishes after saveSvg.
            // Let's just return the content + closing tag in getSvgContent.
            // And here in saveSvg we can do the same.
            content = svgContent.toString() + "</svg>";
        }

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        }
    }

    public String getSvgContent() {
        synchronized (svgContent) {
            return svgContent.toString() + "</svg>";
        }
    }

    private Callable<ShapeResult> createWorkerTask() {
        return () -> {
            // Thread-Local allocations to prevent race conditions
            ScanlineBuffer localBuffer = new ScanlineBuffer(target.height);
            Optimizer localOptimizer = new Optimizer(target, current, localBuffer);
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            Shape randomShape;
            Mode currentMode = config.mode();

            // If COMBO, pick a random specific mode for this shape
            // If COMBO, pick a random specific mode for this shape
            if (currentMode == Mode.COMBO) {
                int pick = rnd.nextInt(6);
                if (pick == 0)
                    currentMode = Mode.TRIANGLE;
                else if (pick == 1)
                    currentMode = Mode.LINE;
                else if (pick == 2)
                    currentMode = Mode.BEZIER;
                else if (pick == 3)
                    currentMode = Mode.RECT;
                else if (pick == 4)
                    currentMode = Mode.POLYLINE;
                else
                    currentMode = Mode.ELLIPSE;
            }

            if (currentMode == Mode.TRIANGLE) {
                Triangle t = new Triangle();
                t.x1 = rnd.nextDouble(target.width);
                t.y1 = rnd.nextDouble(target.height);
                t.x2 = rnd.nextDouble(target.width);
                t.y2 = rnd.nextDouble(target.height);
                t.x3 = rnd.nextDouble(target.width);
                t.y3 = rnd.nextDouble(target.height);
                randomShape = t;
            } else if (currentMode == Mode.LINE) {
                double x1 = rnd.nextDouble(target.width);
                double y1 = rnd.nextDouble(target.height);
                double x2 = rnd.nextDouble(target.width);
                double y2 = rnd.nextDouble(target.height);
                randomShape = new Line(x1, y1, x2, y2, 1 + rnd.nextDouble(8));
            } else if (currentMode == Mode.BEZIER) {
                double x1 = rnd.nextDouble(target.width);
                double y1 = rnd.nextDouble(target.height);
                double cx = rnd.nextDouble(target.width);
                double cy = rnd.nextDouble(target.height);
                double x2 = rnd.nextDouble(target.width);
                double y2 = rnd.nextDouble(target.height);
                randomShape = new QuadraticBezier(x1, y1, cx, cy, x2, y2, 1 + rnd.nextDouble(8));
            } else if (currentMode == Mode.RECT) {
                // Start with a random small-ish rectangle
                double w = 16 + rnd.nextDouble(32);
                double h = 16 + rnd.nextDouble(32);
                double x = rnd.nextDouble(target.width);
                double y = rnd.nextDouble(target.height);
                double angle = rnd.nextDouble(360);
                randomShape = new RotatedRectangle(x, y, w, h, angle);
            } else if (currentMode == Mode.POLYLINE) {
                // Start with a random 4-point polyline
                Polyline p = new Polyline(4);
                for (int i = 0; i < 4; i++) {
                    p.xPoints[i] = rnd.nextDouble(target.width);
                    p.yPoints[i] = rnd.nextDouble(target.height);
                }
                p.width = 1 + rnd.nextDouble(4);
                randomShape = p;
            } else { // ELLIPSE
                double x = rnd.nextDouble(target.width);
                double y = rnd.nextDouble(target.height);
                double rx = 16 + rnd.nextDouble(32);
                double ry = 16 + rnd.nextDouble(32);
                double angle = rnd.nextDouble(360);
                randomShape = new Ellipse(x, y, rx, ry, angle);
            }

            // Run Hill Climbing optimization (1000 mutations per shape)
            return localOptimizer.optimize(randomShape, 1000);
        };
    }
}