package org.trostheide.primitive.shape;

import org.junit.jupiter.api.Test;
import org.trostheide.primitive.raster.ScanlineBuffer;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that all Shape implementations honor the Shape contract:
 * copy(), mutate(), rasterize(), toSvg(), draw().
 */
class ShapeContractTest {

    private static final int W = 100, H = 100;

    private Shape[] allShapes() {
        return new Shape[]{
                new Triangle(10, 10, 80, 20, 50, 80),
                new Line(10, 10, 80, 80, 3),
                new QuadraticBezier(10, 50, 50, 10, 90, 50, 2),
                new RotatedRectangle(50, 50, 40, 30, 15),
                new Ellipse(50, 50, 30, 20, 0),
                createPolyline()
        };
    }

    private Polyline createPolyline() {
        Polyline p = new Polyline(4);
        p.xPoints[0] = 10; p.yPoints[0] = 10;
        p.xPoints[1] = 40; p.yPoints[1] = 80;
        p.xPoints[2] = 70; p.yPoints[2] = 20;
        p.xPoints[3] = 90; p.yPoints[3] = 90;
        p.width = 2;
        return p;
    }

    // --- Copy Contract ---

    @Test
    void testCopyReturnsNewInstance() {
        for (Shape shape : allShapes()) {
            Shape copy = shape.copy();
            assertNotSame(shape, copy, shape.getClass().getSimpleName() + ".copy() returned same instance");
        }
    }

    @Test
    void testCopyRasterizesSame() {
        for (Shape shape : allShapes()) {
            ScanlineBuffer buf1 = new ScanlineBuffer(H);
            shape.rasterize(buf1, W, H);

            Shape copy = shape.copy();
            ScanlineBuffer buf2 = new ScanlineBuffer(H);
            copy.rasterize(buf2, W, H);

            assertEquals(buf1.count, buf2.count,
                    shape.getClass().getSimpleName() + " copy should produce same scanline count");
            for (int i = 0; i < buf1.count; i++) {
                assertEquals(buf1.y[i], buf2.y[i]);
                assertEquals(buf1.x1[i], buf2.x1[i]);
                assertEquals(buf1.x2[i], buf2.x2[i]);
            }
        }
    }

    // --- Rasterize Contract ---

    @Test
    void testRasterizeProducesScanlines() {
        for (Shape shape : allShapes()) {
            ScanlineBuffer buf = new ScanlineBuffer(H);
            shape.rasterize(buf, W, H);
            assertTrue(buf.count > 0,
                    shape.getClass().getSimpleName() + " should produce scanlines");
        }
    }

    @Test
    void testRasterizeStaysInBounds() {
        for (Shape shape : allShapes()) {
            ScanlineBuffer buf = new ScanlineBuffer(H);
            shape.rasterize(buf, W, H);

            for (int i = 0; i < buf.count; i++) {
                assertTrue(buf.y[i] >= 0 && buf.y[i] < H,
                        shape.getClass().getSimpleName() + " y out of bounds: " + buf.y[i]);
                assertTrue(buf.x1[i] >= 0,
                        shape.getClass().getSimpleName() + " x1 out of bounds: " + buf.x1[i]);
                assertTrue(buf.x2[i] <= W,
                        shape.getClass().getSimpleName() + " x2 out of bounds: " + buf.x2[i]);
                assertTrue(buf.x1[i] < buf.x2[i],
                        shape.getClass().getSimpleName() + " invalid span: x1=" + buf.x1[i] + " x2=" + buf.x2[i]);
            }
        }
    }

    @Test
    void testRasterizeDoesNotResetBuffer() {
        for (Shape shape : allShapes()) {
            ScanlineBuffer buf = new ScanlineBuffer(H);
            // Pre-add an entry
            buf.add(0, 0, 1);
            int before = buf.count;

            shape.rasterize(buf, W, H);
            // Shape should append, not reset
            assertTrue(buf.count >= before,
                    shape.getClass().getSimpleName() + " should not reset the buffer");
        }
    }

    // --- Mutate Contract ---

    @Test
    void testMutateDoesNotThrow() {
        for (Shape shape : allShapes()) {
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 100; i++) {
                    shape.mutate(W, H);
                }
            }, shape.getClass().getSimpleName() + ".mutate() threw an exception");
        }
    }

    @Test
    void testMutateStaysInBoundsAfterRepeatedMutations() {
        for (Shape shape : allShapes()) {
            for (int i = 0; i < 200; i++) {
                shape.mutate(W, H);
            }
            // After many mutations, rasterize should still produce valid scanlines
            ScanlineBuffer buf = new ScanlineBuffer(H * 2);
            shape.rasterize(buf, W, H);

            for (int i = 0; i < buf.count; i++) {
                assertTrue(buf.y[i] >= 0 && buf.y[i] < H,
                        shape.getClass().getSimpleName() + " y out of bounds after mutations: " + buf.y[i]);
                assertTrue(buf.x1[i] >= 0,
                        shape.getClass().getSimpleName() + " x1 out of bounds after mutations");
                assertTrue(buf.x2[i] <= W,
                        shape.getClass().getSimpleName() + " x2 out of bounds after mutations");
            }
        }
    }

    // --- SVG Contract ---

    @Test
    void testToSvgReturnsNonEmpty() {
        for (Shape shape : allShapes()) {
            String svg = shape.toSvg("#FF0000", 0.5);
            assertNotNull(svg, shape.getClass().getSimpleName() + ".toSvg() returned null");
            assertFalse(svg.isEmpty(), shape.getClass().getSimpleName() + ".toSvg() returned empty");
        }
    }

    @Test
    void testToSvgContainsColor() {
        for (Shape shape : allShapes()) {
            String svg = shape.toSvg("#ABCDEF", 0.75);
            assertTrue(svg.contains("#ABCDEF"),
                    shape.getClass().getSimpleName() + " SVG should contain the color");
            assertTrue(svg.contains("0.75"),
                    shape.getClass().getSimpleName() + " SVG should contain the opacity");
        }
    }

    @Test
    void testToSvgIsValidXmlFragment() {
        for (Shape shape : allShapes()) {
            String svg = shape.toSvg("#FF0000", 0.5);
            assertTrue(svg.startsWith("<"),
                    shape.getClass().getSimpleName() + " SVG should start with '<'");
            assertTrue(svg.contains(">"),
                    shape.getClass().getSimpleName() + " SVG should contain '>'");
        }
    }

    // --- Draw Contract ---

    @Test
    void testDrawDoesNotThrow() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);

        for (Shape shape : allShapes()) {
            assertDoesNotThrow(() -> shape.draw(g, W, H),
                    shape.getClass().getSimpleName() + ".draw() threw an exception");
        }
        g.dispose();
    }
}
