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

class RotatedRectangleTest {

    @Test
    void testRotatedRectVisual() throws IOException {
        int width = 256;
        int height = 256;

        // Center (128,128), Width 100, Height 50, Angle 45 degrees
        RotatedRectangle rect = new RotatedRectangle(128, 128, 100, 50, 45);

        // Rasterize
        ScanlineBuffer buffer = new ScanlineBuffer(height);
        rect.rasterize(buffer, width, height);

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

        // Replicate logic to draw outline
        double dx = rect.w / 2.0;
        double dy = rect.h / 2.0;
        double rad = Math.toRadians(rect.angle);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        Path2D.Double p = new Path2D.Double();
        p.moveTo((-dx * cos - -dy * sin) + rect.cx, (-dx * sin + -dy * cos) + rect.cy);
        p.lineTo((dx * cos - -dy * sin) + rect.cx, (dx * sin + -dy * cos) + rect.cy);
        p.lineTo((dx * cos - dy * sin) + rect.cx, (dx * sin + dy * cos) + rect.cy);
        p.lineTo((-dx * cos - dy * sin) + rect.cx, (-dx * sin + dy * cos) + rect.cy);
        p.closePath();

        g.draw(p);

        g.dispose();

        File outputFile = new File("target/rotated_rect_debug.png");
        outputFile.getParentFile().mkdirs();
        ImageIO.write(debugImage, "png", outputFile);

        System.out.println("Visual debug image saved to: " + outputFile.getAbsolutePath());
        assertTrue(buffer.count > 0);
    }
}
