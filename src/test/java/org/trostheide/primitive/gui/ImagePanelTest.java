package org.trostheide.primitive.gui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ImagePanelTest {

    private ImagePanel panel;

    @BeforeEach
    void setUp() {
        ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
        panel = new ImagePanel();
    }

    @Test
    void testInitiallyNoImage() {
        assertNull(panel.getImage());
    }

    @Test
    void testSetAndGetImage() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        panel.setImage(img);
        assertSame(img, panel.getImage());
    }

    @Test
    void testSetNullImage() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        panel.setImage(img);
        panel.setImage(null);
        assertNull(panel.getImage());
    }

    @Test
    void testReplaceImage() {
        BufferedImage img1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        BufferedImage img2 = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        panel.setImage(img1);
        panel.setImage(img2);
        assertSame(img2, panel.getImage());
    }

    @Test
    void testPaintWithoutImage() {
        panel.setSize(400, 300);
        // Should not throw when painting without an image (empty state)
        BufferedImage canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        assertDoesNotThrow(() -> panel.paintComponent(g));
        g.dispose();
    }

    @Test
    void testPaintWithImage() {
        panel.setSize(400, 300);
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        panel.setImage(img);

        BufferedImage canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        assertDoesNotThrow(() -> panel.paintComponent(g));
        g.dispose();
    }

    @Test
    void testPaintWithLargeImage() {
        panel.setSize(200, 200);
        // Image larger than panel - should scale down
        BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
        panel.setImage(img);

        BufferedImage canvas = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        assertDoesNotThrow(() -> panel.paintComponent(g));
        g.dispose();
    }

    @Test
    void testPaintWithTinyImage() {
        panel.setSize(400, 300);
        // Very small image
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        panel.setImage(img);

        BufferedImage canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        assertDoesNotThrow(() -> panel.paintComponent(g));
        g.dispose();
    }
}
