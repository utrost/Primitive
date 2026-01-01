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

class LineTest {

    @Test
    void testLineRasterizationVisual() throws IOException {
        int width = 256;
        int height = 256;

        // Thick line from (50,50) to (200,200) with width 20
        Line line = new Line(50, 50, 200, 200, 20);

        // Rasterize
        ScanlineBuffer buffer = new ScanlineBuffer(height);
        line.rasterize(buffer, width, height);

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

        // 2. Draw Reference Outline (Blue)
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(1.0f));

        // reimplement corner calc for verification
        double dx = line.x2 - line.x1;
        double dy = line.y2 - line.y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        double nx = -dy / len * (line.width / 2);
        double ny = dx / len * (line.width / 2);

        Path2D.Double path = new Path2D.Double();
        path.moveTo(line.x1 + nx, line.y1 + ny);
        path.lineTo(line.x2 + nx, line.y2 + ny);
        path.lineTo(line.x2 - nx, line.y2 - ny);
        path.lineTo(line.x1 - nx, line.y1 - ny);
        path.closePath();

        g.draw(path);

        g.dispose();

        File outputFile = new File("target/line_debug.png");
        outputFile.getParentFile().mkdirs();
        ImageIO.write(debugImage, "png", outputFile);

        System.out.println("Visual debug image saved to: " + outputFile.getAbsolutePath());
        assertTrue(buffer.count > 0);
    }

    @Test
    void testHorizontalLine() {
        // Horizontal line, width 10
        Line line = new Line(10, 50, 100, 50, 10);
        ScanlineBuffer buffer = new ScanlineBuffer(100);
        line.rasterize(buffer, 200, 200);

        // Should produce scanlines around y=45 to y=55
        boolean hasCenter = false;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;

        for (int i = 0; i < buffer.count; i++) {
            if (buffer.y[i] == 50) {
                hasCenter = true;
                minX = Math.min(minX, buffer.x1[i]);
                maxX = Math.max(maxX, buffer.x2[i]);
            }
        }

        if (hasCenter) {
            assertTrue(maxX - minX >= 80, "Combined width at Y=50 should be >= 80. Got: " + (maxX - minX));
        }
        assertTrue(hasCenter, "Should cover center line");
    }
}
