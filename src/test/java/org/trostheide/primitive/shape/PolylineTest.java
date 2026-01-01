package org.trostheide.primitive.shape;

import org.junit.jupiter.api.Test;
import org.trostheide.primitive.raster.ScanlineBuffer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PolylineTest {

    @Test
    void testPolylineVisual() throws IOException {
        int width = 256;
        int height = 256;

        // 4 points zig-zag
        Polyline poly = new Polyline(4);
        poly.xPoints[0] = 20;
        poly.yPoints[0] = 20;
        poly.xPoints[1] = 100;
        poly.yPoints[1] = 200;
        poly.xPoints[2] = 180;
        poly.yPoints[2] = 50;
        poly.xPoints[3] = 240;
        poly.yPoints[3] = 240;
        poly.width = 15;

        // Rasterize
        ScanlineBuffer buffer = new ScanlineBuffer(height);
        poly.rasterize(buffer, width, height);

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
        g.setColor(new Color(0, 0, 255, 128)); // Blue semi-transparent
        g.setStroke(new BasicStroke((float) poly.width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Path2D.Double path = new Path2D.Double();
        path.moveTo(poly.xPoints[0], poly.yPoints[0]);
        for (int i = 1; i < 4; i++)
            path.lineTo(poly.xPoints[i], poly.yPoints[i]);
        g.draw(path);

        g.dispose();

        File outputFile = new File("target/polyline_debug.png");
        outputFile.getParentFile().mkdirs();
        ImageIO.write(debugImage, "png", outputFile);

        System.out.println("Visual debug image saved to: " + outputFile.getAbsolutePath());
        assertTrue(buffer.count > 0);
    }
}
