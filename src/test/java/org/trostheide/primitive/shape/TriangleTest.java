package org.trostheide.primitive.shape;

import org.junit.jupiter.api.Test;
import org.trostheide.primitive.raster.ScanlineBuffer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TriangleTest {

    @Test
    void testRasterizationVisualVerification() throws IOException {
        int width = 256;
        int height = 256;

        // 1. Create a fixed triangle
        // Using coordinates that cover various edge cases (steep slopes, flat tops)
        Triangle triangle = new Triangle(10, 10, 200, 50, 50, 200);

        // 2. Rasterize using our custom logic
        ScanlineBuffer buffer = new ScanlineBuffer(height);
        triangle.rasterize(buffer, width, height);

        // 3. Create an image to visualize the output
        BufferedImage debugImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = debugImage.createGraphics();

        // Fill white background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // DRAW RAW SCANLINES (Red)
        // We manually plot every pixel the rasterizer claims it covers
        int red = 0xFFFF0000; // ARGB Red
        for (int i = 0; i < buffer.count; i++) {
            int y = buffer.y[i];
            int x1 = buffer.x1[i];
            int x2 = buffer.x2[i];

            for (int x = x1; x < x2; x++) {
                debugImage.setRGB(x, y, red);
            }
        }

        // DRAW REFERENCE WIREFRAME (Blue)
        // Uses Java's standard high-precision floating point rendering
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(1.0f));
        triangle.draw(g, width, height); // Calls the draw method we implemented using Path2D

        g.dispose();

        // 4. Save to file for inspection
        File outputFile = new File("target/triangle_debug.png");
        // Ensure target directory exists
        outputFile.getParentFile().mkdirs();

        ImageIO.write(debugImage, "png", outputFile);

        System.out.println("Visual debug image saved to: " + outputFile.getAbsolutePath());
        System.out.println("Check this image: Red pixels (rasterizer) should be exactly inside Blue lines (reference).");

        // Simple assertion to ensure we actually generated data
        assertTrue(buffer.count > 0, "Buffer should have scanlines");
    }

    @Test
    void testSmallTriangleLogic() {
        // Test a small triangle to verify connectivity
        // Triangle: (2,0) -> (0,2) -> (4,2)
        // This is a flat-bottom triangle.
        // Y=0: Tip at x=2. In standard integer rasterization, a single point often yields 0 width (2..2), which is valid.
        // Y=1: Mid-section. Should definitely span from approx x=1 to x=3.

        Triangle t = new Triangle(2, 0, 0, 2, 4, 2);
        ScanlineBuffer buf = new ScanlineBuffer(10);
        t.rasterize(buf, 10, 10);

        // We verify that the triangle is processed and produces scanlines for its body (Y=1)
        boolean foundY1 = false;
        for(int i=0; i<buf.count; i++) {
            if (buf.y[i] == 1) {
                foundY1 = true;
                int start = buf.x1[i];
                int end = buf.x2[i];

                // At Y=1, the left edge (2->0) should be around 1.0
                // The right edge (2->4) should be around 3.0
                // So we expect a span containing pixels 1 and 2.
                assertTrue(start <= 1, "Start X at Y=1 should be <= 1");
                assertTrue(end >= 3, "End X at Y=1 should be >= 3");
                assertTrue(end > start, "Should have non-zero width at Y=1");
            }
        }
        assertTrue(foundY1, "Should have a scanline at Y=1 (the triangle body)");
    }
}