package org.trostheide.primitive.shape;

import org.junit.jupiter.api.Test;
import org.trostheide.primitive.raster.ScanlineBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Shape-specific tests for edge cases and particular behaviors.
 */
class ShapeSpecificTest {

    // --- Triangle ---

    @Test
    void testTriangleCopyPreservesCoordinates() {
        Triangle t = new Triangle(1, 2, 3, 4, 5, 6);
        Triangle copy = (Triangle) t.copy();
        assertEquals(1, copy.x1); assertEquals(2, copy.y1);
        assertEquals(3, copy.x2); assertEquals(4, copy.y2);
        assertEquals(5, copy.x3); assertEquals(6, copy.y3);
    }

    @Test
    void testTriangleSvgFormat() {
        Triangle t = new Triangle(10, 20, 30, 40, 50, 60);
        String svg = t.toSvg("#FF0000", 0.5);
        assertTrue(svg.contains("<polygon"));
        assertTrue(svg.contains("points="));
        assertTrue(svg.contains("fill=\"#FF0000\""));
        assertTrue(svg.contains("fill-opacity=\"0.50\""));
    }

    @Test
    void testDegenerateTriangleLine() {
        // Degenerate triangle (collinear points)
        Triangle t = new Triangle(0, 0, 50, 50, 100, 100);
        ScanlineBuffer buf = new ScanlineBuffer(200);
        // Should not crash, may produce 0 scanlines
        assertDoesNotThrow(() -> t.rasterize(buf, 200, 200));
    }

    @Test
    void testTriangleOutOfBounds() {
        // Triangle entirely outside canvas
        Triangle t = new Triangle(-100, -100, -50, -100, -75, -50);
        ScanlineBuffer buf = new ScanlineBuffer(100);
        t.rasterize(buf, 100, 100);
        assertEquals(0, buf.count, "Triangle outside canvas should produce no scanlines");
    }

    @Test
    void testTriangleClippedByBounds() {
        // Triangle partially outside canvas
        Triangle t = new Triangle(-10, 10, 50, 10, 20, 90);
        ScanlineBuffer buf = new ScanlineBuffer(100);
        t.rasterize(buf, 100, 100);
        assertTrue(buf.count > 0, "Partially visible triangle should produce scanlines");
        for (int i = 0; i < buf.count; i++) {
            assertTrue(buf.x1[i] >= 0, "Clipped x1 should be >= 0");
            assertTrue(buf.x2[i] <= 100, "Clipped x2 should be <= 100");
        }
    }

    // --- Line ---

    @Test
    void testLineCopyPreservesValues() {
        Line l = new Line(10, 20, 30, 40, 5);
        Line copy = (Line) l.copy();
        assertEquals(10, copy.x1); assertEquals(20, copy.y1);
        assertEquals(30, copy.x2); assertEquals(40, copy.y2);
        assertEquals(5, copy.width);
    }

    @Test
    void testLineSvgFormat() {
        Line l = new Line(10, 20, 30, 40, 3);
        String svg = l.toSvg("#00FF00", 0.8);
        assertTrue(svg.contains("<line"));
        assertTrue(svg.contains("stroke=\"#00FF00\""));
        assertTrue(svg.contains("stroke-opacity=\"0.80\""));
    }

    @Test
    void testZeroLengthLine() {
        // Line with zero length
        Line l = new Line(50, 50, 50, 50, 3);
        ScanlineBuffer buf = new ScanlineBuffer(100);
        // Should not crash
        assertDoesNotThrow(() -> l.rasterize(buf, 100, 100));
        // Zero-length line produces no scanlines
        assertEquals(0, buf.count);
    }

    @Test
    void testHorizontalLineRasterization() {
        Line l = new Line(10, 50, 90, 50, 4);
        ScanlineBuffer buf = new ScanlineBuffer(100);
        l.rasterize(buf, 100, 100);
        assertTrue(buf.count > 0, "Horizontal line should produce scanlines");
    }

    @Test
    void testVerticalLineRasterization() {
        Line l = new Line(50, 10, 50, 90, 4);
        ScanlineBuffer buf = new ScanlineBuffer(100);
        l.rasterize(buf, 100, 100);
        assertTrue(buf.count > 0, "Vertical line should produce scanlines");
    }

    // --- Ellipse ---

    @Test
    void testEllipseCopyPreservesValues() {
        Ellipse e = new Ellipse(50, 50, 30, 20, 45);
        Ellipse copy = (Ellipse) e.copy();
        assertEquals(50, copy.x); assertEquals(50, copy.y);
        assertEquals(30, copy.rx); assertEquals(20, copy.ry);
        assertEquals(45, copy.angle);
    }

    @Test
    void testEllipseSvgFormat() {
        Ellipse e = new Ellipse(50, 50, 30, 20, 45);
        String svg = e.toSvg("#0000FF", 0.5);
        assertTrue(svg.contains("<ellipse"));
        assertTrue(svg.contains("fill=\"#0000FF\""));
        assertTrue(svg.contains("rotate"));
    }

    @Test
    void testCircleIsSymmetric() {
        // Circle (rx == ry, no rotation)
        Ellipse e = new Ellipse(50, 50, 20, 20, 0);
        ScanlineBuffer buf = new ScanlineBuffer(100);
        e.rasterize(buf, 100, 100);
        assertTrue(buf.count > 0);

        // For a circle centered at 50,50 the scanlines at y=50 should be widest
        int maxWidth = 0;
        int maxWidthY = -1;
        for (int i = 0; i < buf.count; i++) {
            int w = buf.x2[i] - buf.x1[i];
            if (w > maxWidth) {
                maxWidth = w;
                maxWidthY = buf.y[i];
            }
        }
        // Widest scanline should be near center (y=50), allowing rounding tolerance
        assertTrue(Math.abs(maxWidthY - 50) <= 6,
                "Widest scanline should be near center, got y=" + maxWidthY);
    }

    @Test
    void testEllipseOutOfBounds() {
        Ellipse e = new Ellipse(-100, -100, 10, 10, 0);
        ScanlineBuffer buf = new ScanlineBuffer(100);
        e.rasterize(buf, 100, 100);
        assertEquals(0, buf.count, "Ellipse fully outside should produce no scanlines");
    }

    // --- RotatedRectangle ---

    @Test
    void testRectCopyPreservesValues() {
        RotatedRectangle r = new RotatedRectangle(50, 50, 40, 30, 15);
        RotatedRectangle copy = (RotatedRectangle) r.copy();
        assertEquals(50, copy.cx); assertEquals(50, copy.cy);
        assertEquals(40, copy.w); assertEquals(30, copy.h);
        assertEquals(15, copy.angle);
    }

    @Test
    void testRectSvgFormat() {
        RotatedRectangle r = new RotatedRectangle(50, 50, 40, 30, 0);
        String svg = r.toSvg("#123456", 0.3);
        assertTrue(svg.contains("<rect"));
        assertTrue(svg.contains("fill=\"#123456\""));
    }

    @Test
    void testAxisAlignedRectangle() {
        RotatedRectangle r = new RotatedRectangle(50, 50, 20, 10, 0);
        ScanlineBuffer buf = new ScanlineBuffer(100);
        r.rasterize(buf, 100, 100);
        assertTrue(buf.count > 0);

        // Non-rotated rect centered at (50,50) 20x10 should span y from ~45 to ~55
        for (int i = 0; i < buf.count; i++) {
            assertTrue(buf.y[i] >= 44 && buf.y[i] <= 56,
                    "Y should be near center for 10-high rect, got " + buf.y[i]);
        }
    }

    // --- QuadraticBezier ---

    @Test
    void testBezierCopyPreservesValues() {
        QuadraticBezier b = new QuadraticBezier(10, 50, 50, 10, 90, 50, 3);
        QuadraticBezier copy = (QuadraticBezier) b.copy();
        assertEquals(10, copy.x1); assertEquals(50, copy.y1);
        assertEquals(50, copy.cx); assertEquals(10, copy.cy);
        assertEquals(90, copy.x2); assertEquals(50, copy.y2);
        assertEquals(3, copy.width);
    }

    @Test
    void testBezierSvgFormat() {
        QuadraticBezier b = new QuadraticBezier(10, 50, 50, 10, 90, 50, 3);
        String svg = b.toSvg("#AABBCC", 0.6);
        assertNotNull(svg);
        assertFalse(svg.isEmpty());
        assertTrue(svg.contains("#AABBCC"));
    }

    @Test
    void testBezierRasterization() {
        QuadraticBezier b = new QuadraticBezier(10, 50, 50, 10, 90, 50, 4);
        ScanlineBuffer buf = new ScanlineBuffer(100);
        b.rasterize(buf, 100, 100);
        assertTrue(buf.count > 0, "Bezier curve should produce scanlines");
    }

    // --- Polyline ---

    @Test
    void testPolylineCopy() {
        Polyline p = new Polyline(3);
        p.xPoints[0] = 10; p.yPoints[0] = 10;
        p.xPoints[1] = 50; p.yPoints[1] = 50;
        p.xPoints[2] = 90; p.yPoints[2] = 10;
        p.width = 3;

        Polyline copy = (Polyline) p.copy();
        assertNotSame(p, copy);
        assertEquals(p.width, copy.width);
        assertArrayEquals(p.xPoints, copy.xPoints);
        assertArrayEquals(p.yPoints, copy.yPoints);
    }

    @Test
    void testPolylineModifyCopyDoesNotAffectOriginal() {
        Polyline p = new Polyline(3);
        p.xPoints[0] = 10; p.yPoints[0] = 10;
        p.xPoints[1] = 50; p.yPoints[1] = 50;
        p.xPoints[2] = 90; p.yPoints[2] = 10;
        p.width = 3;

        Polyline copy = (Polyline) p.copy();
        copy.xPoints[0] = 999;
        assertEquals(10, p.xPoints[0], "Modifying copy should not affect original");
    }
}
