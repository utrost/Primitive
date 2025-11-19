package org.trostheide.primitive.core;

import org.trostheide.primitive.image.RgbaImage;
import org.trostheide.primitive.raster.ScanlineBuffer;
import org.trostheide.primitive.shape.Shape;

public class Optimizer {
    private final RgbaImage target;
    private final RgbaImage current;
    private final ScanlineBuffer buffer;
    private final int width;
    private final int height;

    // Fixed alpha allows for efficient color solving.
    // 128 (approx 0.5) is standard for "stacking" shapes.
    private static final int FIXED_ALPHA = 128;
    private static final double ALPHA_RATIO = FIXED_ALPHA / 255.0;
    private static final double INV_ALPHA_RATIO = 1.0 - ALPHA_RATIO;

    public Optimizer(RgbaImage target, RgbaImage current, ScanlineBuffer buffer) {
        this.target = target;
        this.current = current;
        this.buffer = buffer;
        this.width = target.width;
        this.height = target.height;
    }

    /**
     * The Hill Climbing Loop.
     * @param initShape The starting random shape
     * @param iterations How many mutations to attempt (e.g., 1000)
     * @return The best shape found after the iterations
     */
    public ShapeResult optimize(Shape initShape, int iterations) {
        Shape bestShape = initShape.copy();

        // CRITICAL FIX: Reset buffer before initial rasterization
        buffer.reset();
        bestShape.rasterize(buffer, width, height);
        // FIX: Removed unused 'bestShape' parameter from computeEnergyDelta
        long bestEnergyDelta = computeEnergyDelta(buffer);

        Shape workingShape = bestShape.copy();

        for (int i = 0; i < iterations; i++) {
            workingShape.mutate(width, height);

            // CRITICAL FIX: Reset buffer before every mutation rasterization.
            // If missing, scanlines accumulate, causing massive slowdown.
            buffer.reset();
            workingShape.rasterize(buffer, width, height);

            // FIX: Removed unused 'workingShape' parameter from computeEnergyDelta
            long energyDelta = computeEnergyDelta(buffer);

            if (energyDelta < bestEnergyDelta) {
                bestEnergyDelta = energyDelta;
                bestShape = workingShape.copy();
            } else {
                workingShape = bestShape.copy();
            }
        }

        // Finalize
        // CRITICAL FIX: Reset buffer before final rasterization
        buffer.reset();
        bestShape.rasterize(buffer, width, height);
        int bestColor = computeOptimalColor(buffer);

        return new ShapeResult(bestShape, bestEnergyDelta, bestColor);
    }

    // FIX: Removed 'Shape shape' parameter from signature
    private long computeEnergyDelta(ScanlineBuffer scanlines) {
        if (scanlines.count == 0) return Long.MAX_VALUE;
        int color = computeOptimalColor(scanlines);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;

        long currentError = 0;
        long newError = 0;
        int[] yArr = scanlines.y;
        int[] x1Arr = scanlines.x1;
        int[] x2Arr = scanlines.x2;
        int count = scanlines.count;

        for (int i = 0; i < count; i++) {
            int y = yArr[i];
            int xStart = x1Arr[i];
            int xEnd = x2Arr[i];
            int rowOffset = y * width;
            for (int x = xStart; x < xEnd; x++) {
                int idx = rowOffset + x;
                int t = target.pixels[idx];
                int c = current.pixels[idx];
                int tr = (t >> 16) & 0xFF; int tg = (t >> 8) & 0xFF; int tb = (t) & 0xFF;
                int cr = (c >> 16) & 0xFF; int cg = (c >> 8) & 0xFF; int cb = (c) & 0xFF;
                currentError += sq(tr - cr) + sq(tg - cg) + sq(tb - cb);
                int nr = (int) (r * ALPHA_RATIO + cr * INV_ALPHA_RATIO);
                int ng = (int) (g * ALPHA_RATIO + cg * INV_ALPHA_RATIO);
                int nb = (int) (b * ALPHA_RATIO + cb * INV_ALPHA_RATIO);
                newError += sq(tr - nr) + sq(tg - ng) + sq(tb - nb);
            }
        }
        return newError - currentError;
    }

    public int computeOptimalColor(ScanlineBuffer scanlines) {
        long t_r = 0, t_g = 0, t_b = 0;
        long c_r = 0, c_g = 0, c_b = 0;
        int count = 0;
        int[] yArr = scanlines.y;
        int[] x1Arr = scanlines.x1;
        int[] x2Arr = scanlines.x2;
        int lines = scanlines.count;
        for (int i = 0; i < lines; i++) {
            int y = yArr[i];
            int xStart = x1Arr[i];
            int xEnd = x2Arr[i];
            int rowOffset = y * width;
            for (int x = xStart; x < xEnd; x++) {
                int idx = rowOffset + x;
                int t = target.pixels[idx];
                int c = current.pixels[idx];
                t_r += (t >> 16) & 0xFF; t_g += (t >> 8) & 0xFF; t_b += (t) & 0xFF;
                c_r += (c >> 16) & 0xFF; c_g += (c >> 8) & 0xFF; c_b += (c) & 0xFF;
                count++;
            }
        }
        if (count == 0) return 0;
        double avgTr = (double) t_r / count;
        double avgTg = (double) t_g / count;
        double avgTb = (double) t_b / count;
        double avgCr = (double) c_r / count;
        double avgCg = (double) c_g / count;
        double avgCb = (double) c_b / count;
        int r = (int) ((avgTr - avgCr * INV_ALPHA_RATIO) / ALPHA_RATIO);
        int g = (int) ((avgTg - avgCg * INV_ALPHA_RATIO) / ALPHA_RATIO);
        int b = (int) ((avgTb - avgCb * INV_ALPHA_RATIO) / ALPHA_RATIO);
        r = clamp(r); g = clamp(g); b = clamp(b);
        return (FIXED_ALPHA << 24) | (r << 16) | (g << 8) | b;
    }

    // FIX: Explicit long cast to silence warning and prevent potential overflow
    private long sq(int x) { return (long) x * x; }

    private int clamp(int val) { return Math.max(0, Math.min(255, val)); }
}