package org.trostheide.primitive.shape;

import org.junit.jupiter.api.Test;
import org.trostheide.primitive.raster.ScanlineBuffer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class EllipseTest {

    @Test
    void testEllipseVisual() throws IOException {
        int width = 256;
        int height = 256;

        // Ellipse centered at 128,128, radii 50,25, rotated 30 deg
        Ellipse ellipse = new Ellipse(128, 128, 50, 25, 30);

        // Rasterize
        ScanlineBuffer buffer = new ScanlineBuffer(height);
        ellipse.rasterize(buffer, width, height);

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

        // 2. Draw Reference (Blue Outline)
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(1.0f));

        Ellipse2D.Double e = new Ellipse2D.Double(-ellipse.rx, -ellipse.ry, ellipse.rx * 2, ellipse.ry * 2);
        Path2D.Double p = new Path2D.Double(e);
        java.awt.geom.AffineTransform t = new java.awt.geom.AffineTransform();
        t.translate(ellipse.x, ellipse.y);
        t.rotate(Math.toRadians(ellipse.angle));
        p.transform(t);

        g.draw(p);

        g.dispose();

        File outputFile = new File("target/ellipse_debug.png");
        outputFile.getParentFile().mkdirs();
        ImageIO.write(debugImage, "png", outputFile);

        System.out.println("Visual debug image saved to: " + outputFile.getAbsolutePath());
        assertTrue(buffer.count > 0);
    }
}
