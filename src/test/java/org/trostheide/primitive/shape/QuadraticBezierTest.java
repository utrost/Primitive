package org.trostheide.primitive.shape;

import org.junit.jupiter.api.Test;
import org.trostheide.primitive.raster.ScanlineBuffer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class QuadraticBezierTest {

    @Test
    void testBezierVisual() throws IOException {
        int width = 256;
        int height = 256;

        // Curve from left-mid to right-mid, control point top-right
        QuadraticBezier curve = new QuadraticBezier(20, 200, 200, 20, 240, 240, 10);

        // Rasterize
        ScanlineBuffer buffer = new ScanlineBuffer(height);
        curve.rasterize(buffer, width, height);

        // Visualize
        BufferedImage debugImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = debugImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // 1. Draw Rasterized Pixels (Red)
        g.setColor(Color.RED);
        for (int i = 0; i < buffer.count; i++) {
            int y = buffer.y[i];
            int x1 = buffer.x1[i];
            int x2 = buffer.x2[i];
            g.fillRect(x1, y, x2 - x1, 1);
        }

        // 2. Draw Reference (Blue Outline of the curve)
        g.setColor(Color.BLUE);
        // Use a stroke slightly larger than the filled width?
        // Or just outline the stroke.
        // Actually, just drawing the curve with the same width in Blue allows us to see
        // if the Red segments sit "inside" or "under" the Blue stroke.
        // But since we want to see discrepancies, let's use a composite or
        // transparency?
        // No, simplest is Red filled, Blue stroked outline??
        // Drawing the curve with 'g.draw' fills the stroke.

        g.setStroke(new BasicStroke((float) curve.width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Use semi-transparent Blue to see overlap
        g.setColor(new Color(0, 0, 255, 128));
        g.draw(new QuadCurve2D.Double(curve.x1, curve.y1, curve.cx, curve.cy, curve.x2, curve.y2));

        g.dispose();

        File outputFile = new File("target/bezier_debug.png");
        outputFile.getParentFile().mkdirs();
        ImageIO.write(debugImage, "png", outputFile);

        System.out.println("Visual debug image saved to: " + outputFile.getAbsolutePath());
        assertTrue(buffer.count > 0);
    }
}
