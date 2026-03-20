package org.trostheide.primitive.core;

import org.junit.jupiter.api.Test;
import org.trostheide.primitive.image.RgbaImage;
import org.trostheide.primitive.raster.ScanlineBuffer;
import org.trostheide.primitive.shape.Triangle;

import static org.junit.jupiter.api.Assertions.*;

class OptimizerTest {

    @Test
    void testOptimalColorOnUniformTarget() {
        int w = 10, h = 10;
        RgbaImage target = new RgbaImage(w, h);
        RgbaImage current = new RgbaImage(w, h);

        // Target is pure red, current is white
        for (int i = 0; i < target.pixels.length; i++) {
            target.pixels[i] = 0xFFFF0000;
        }

        ScanlineBuffer buffer = new ScanlineBuffer(h);
        Optimizer opt = new Optimizer(target, current, buffer);

        // Fill entire image scanlines
        for (int y = 0; y < h; y++) {
            buffer.add(y, 0, w);
        }

        int color = opt.computeOptimalColor(buffer);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        // With white current and red target, optimal color should have high red
        assertTrue(r > 200, "Red channel should be high, got " + r);
        assertTrue(g < 50, "Green channel should be low, got " + g);
        assertTrue(b < 50, "Blue channel should be low, got " + b);
        assertEquals(128, a, "Alpha should be fixed at 128");
    }

    @Test
    void testOptimalColorWhenTargetMatchesCurrent() {
        int w = 10, h = 10;
        RgbaImage target = new RgbaImage(w, h);
        RgbaImage current = new RgbaImage(w, h);

        // Both are white - optimal color to blend should also produce white
        ScanlineBuffer buffer = new ScanlineBuffer(h);
        Optimizer opt = new Optimizer(target, current, buffer);

        for (int y = 0; y < h; y++) {
            buffer.add(y, 0, w);
        }

        int color = opt.computeOptimalColor(buffer);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // When target == current (both white), optimal is white
        assertTrue(r > 200, "Red should be high when target matches current");
        assertTrue(g > 200, "Green should be high when target matches current");
        assertTrue(b > 200, "Blue should be high when target matches current");
    }

    @Test
    void testOptimalColorEmptyBuffer() {
        RgbaImage target = new RgbaImage(10, 10);
        RgbaImage current = new RgbaImage(10, 10);
        ScanlineBuffer buffer = new ScanlineBuffer(10);
        Optimizer opt = new Optimizer(target, current, buffer);

        int color = opt.computeOptimalColor(buffer);
        assertEquals(0, color, "Empty buffer should return 0");
    }

    @Test
    void testOptimizeProducesResult() {
        int w = 50, h = 50;
        RgbaImage target = new RgbaImage(w, h);
        RgbaImage current = new RgbaImage(w, h);

        // Make target different from current
        for (int i = 0; i < target.pixels.length; i++) {
            target.pixels[i] = 0xFFFF0000; // Red target
        }

        ScanlineBuffer buffer = new ScanlineBuffer(h);
        Optimizer opt = new Optimizer(target, current, buffer);

        // Large triangle covering most of the image
        Triangle t = new Triangle(0, 0, w, 0, w / 2.0, h);

        ShapeResult result = opt.optimize(t, 100);
        assertNotNull(result);
        assertNotNull(result.shape());
        // Energy delta should be negative (improvement) for a shape on a different-colored target
        assertTrue(result.energyDelta() < 0,
                "Energy delta should be negative (improvement), got " + result.energyDelta());
    }

    @Test
    void testOptimizeReturnsShapeCopy() {
        int w = 20, h = 20;
        RgbaImage target = new RgbaImage(w, h);
        RgbaImage current = new RgbaImage(w, h);
        for (int i = 0; i < target.pixels.length; i++) {
            target.pixels[i] = 0xFF00FF00;
        }

        ScanlineBuffer buffer = new ScanlineBuffer(h);
        Optimizer opt = new Optimizer(target, current, buffer);

        Triangle t = new Triangle(5, 5, 15, 5, 10, 15);
        ShapeResult result = opt.optimize(t, 50);

        // Result shape should be a valid triangle
        assertTrue(result.shape() instanceof Triangle);
    }

    @Test
    void testOptimizeWithFewIterations() {
        int w = 20, h = 20;
        RgbaImage target = new RgbaImage(w, h);
        RgbaImage current = new RgbaImage(w, h);
        for (int i = 0; i < target.pixels.length; i++) {
            target.pixels[i] = 0xFF0000FF;
        }

        ScanlineBuffer buffer = new ScanlineBuffer(h);
        Optimizer opt = new Optimizer(target, current, buffer);

        Triangle t = new Triangle(0, 0, 20, 0, 10, 20);
        // Even with just 1 iteration, should produce a valid result
        ShapeResult result = opt.optimize(t, 1);
        assertNotNull(result);
        assertNotNull(result.shape());
    }

    @Test
    void testShapeResultRecord() {
        Triangle t = new Triangle(0, 0, 10, 0, 5, 10);
        ShapeResult result = new ShapeResult(t, -500, 0xFF804020);

        assertSame(t, result.shape());
        assertEquals(-500, result.energyDelta());
        assertEquals(0xFF804020, result.color());
    }
}
