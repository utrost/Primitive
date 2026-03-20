package org.trostheide.primitive.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.trostheide.primitive.raster.ScanlineBuffer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RgbaImageTest {

    @Test
    void testConstructionDefaultsToWhite() {
        RgbaImage img = new RgbaImage(10, 10);
        assertEquals(10, img.width);
        assertEquals(10, img.height);
        assertEquals(100, img.pixels.length);

        // All pixels should be opaque white (0xFFFFFFFF)
        for (int pixel : img.pixels) {
            assertEquals(0xFFFFFFFF, pixel);
        }
    }

    @Test
    void testSinglePixelImage() {
        RgbaImage img = new RgbaImage(1, 1);
        assertEquals(1, img.pixels.length);
        assertEquals(0xFFFFFFFF, img.pixels[0]);
    }

    @Test
    void testToBufferedImage() {
        RgbaImage img = new RgbaImage(4, 4);
        // Set a known pixel
        img.pixels[0] = 0xFFFF0000; // Red
        img.pixels[5] = 0xFF00FF00; // Green (row 1, col 1)

        BufferedImage bi = img.toBufferedImage();
        assertEquals(4, bi.getWidth());
        assertEquals(4, bi.getHeight());
        assertEquals(BufferedImage.TYPE_INT_ARGB, bi.getType());

        assertEquals(0xFFFF0000, bi.getRGB(0, 0));
        assertEquals(0xFF00FF00, bi.getRGB(1, 1));
        assertEquals(0xFFFFFFFF, bi.getRGB(2, 2)); // Untouched = white
    }

    @Test
    void testCopy() {
        RgbaImage original = new RgbaImage(3, 3);
        original.pixels[0] = 0xFF112233;
        original.pixels[4] = 0xFF445566;

        RgbaImage copy = original.copy();

        assertEquals(original.width, copy.width);
        assertEquals(original.height, copy.height);
        assertArrayEquals(original.pixels, copy.pixels);

        // Verify it's a deep copy
        copy.pixels[0] = 0xFF000000;
        assertNotEquals(original.pixels[0], copy.pixels[0]);
    }

    @Test
    void testResize() {
        RgbaImage img = new RgbaImage(10, 10);
        // Fill with red
        for (int i = 0; i < img.pixels.length; i++) {
            img.pixels[i] = 0xFFFF0000;
        }

        RgbaImage resized = img.resize(5, 5);
        assertEquals(5, resized.width);
        assertEquals(5, resized.height);
        assertEquals(25, resized.pixels.length);

        // Downscaled red image should still be mostly red
        int pixel = resized.pixels[12]; // center pixel
        int r = (pixel >> 16) & 0xFF;
        assertTrue(r > 200, "Resized red channel should remain high, got " + r);
    }

    @Test
    void testResizeUpscale() {
        RgbaImage img = new RgbaImage(2, 2);
        for (int i = 0; i < img.pixels.length; i++) {
            img.pixels[i] = 0xFF0000FF; // Blue
        }

        RgbaImage resized = img.resize(10, 10);
        assertEquals(10, resized.width);
        assertEquals(10, resized.height);
        assertEquals(100, resized.pixels.length);
    }

    @Test
    void testSaveAndLoad(@TempDir Path tempDir) throws IOException {
        RgbaImage img = new RgbaImage(8, 8);
        // Create a pattern
        for (int i = 0; i < img.pixels.length; i++) {
            img.pixels[i] = (i % 2 == 0) ? 0xFFFF0000 : 0xFF0000FF;
        }

        File file = tempDir.resolve("test_output.png").toFile();
        img.save(file);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        // Load it back
        RgbaImage loaded = RgbaImage.load(file);
        assertEquals(8, loaded.width);
        assertEquals(8, loaded.height);

        // Verify key pixels survived round-trip
        // Red pixel at (0,0)
        int p0 = loaded.pixels[0];
        assertEquals(0xFF, (p0 >> 16) & 0xFF, "Red channel should be 0xFF");
        assertEquals(0x00, (p0 >> 8) & 0xFF, "Green channel should be 0x00");
        assertEquals(0x00, p0 & 0xFF, "Blue channel should be 0x00");
    }

    @Test
    void testLoadInvalidFile(@TempDir Path tempDir) {
        File nonExistent = tempDir.resolve("does_not_exist.png").toFile();
        assertThrows(Exception.class, () -> RgbaImage.load(nonExistent));
    }

    @Test
    void testFillScanlinesOpaqueColor() {
        RgbaImage img = new RgbaImage(10, 10);
        ScanlineBuffer buffer = new ScanlineBuffer(10);
        // Fill row 5 from x=2 to x=8 with fully opaque red
        buffer.add(5, 2, 8);

        int opaqueRed = 0xFFFF0000;
        img.fillScanlines(buffer, opaqueRed);

        // Check filled region
        for (int x = 2; x < 8; x++) {
            int pixel = img.pixels[5 * 10 + x];
            assertEquals(0xFFFF0000, pixel,
                    "Pixel at (5," + x + ") should be red after opaque fill");
        }

        // Check unfilled region
        assertEquals(0xFFFFFFFF, img.pixels[5 * 10 + 0], "Pixel outside fill should be white");
        assertEquals(0xFFFFFFFF, img.pixels[5 * 10 + 9], "Pixel outside fill should be white");
    }

    @Test
    void testFillScanlinesSemiTransparent() {
        RgbaImage img = new RgbaImage(10, 1);
        // Start with white pixels
        ScanlineBuffer buffer = new ScanlineBuffer(1);
        buffer.add(0, 0, 10);

        // Fill with 50% transparent red (alpha=128)
        int semiRed = (128 << 24) | (255 << 16) | 0 | 0;
        img.fillScanlines(buffer, semiRed);

        // Result should be blended: roughly (255*128 + 255*127) / 255 for R
        // and (0*128 + 255*127) / 255 for G and B
        int pixel = img.pixels[0];
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;

        // Red should stay high (blend of 255 and 255)
        assertTrue(r > 200, "Red should remain high after blend, got " + r);
        // Green should be roughly half (blend of 0 and 255)
        assertTrue(g > 100 && g < 200, "Green should be mid-range after blend, got " + g);
        // Blue should be roughly half
        assertTrue(b > 100 && b < 200, "Blue should be mid-range after blend, got " + b);
    }

    @Test
    void testFillScanlinesMultipleRows() {
        RgbaImage img = new RgbaImage(5, 5);
        ScanlineBuffer buffer = new ScanlineBuffer(10);
        buffer.add(0, 0, 5);
        buffer.add(1, 1, 4);
        buffer.add(2, 2, 3);

        int opaqueBlue = 0xFF0000FF;
        img.fillScanlines(buffer, opaqueBlue);

        // Row 0: all 5 pixels blue
        for (int x = 0; x < 5; x++) {
            assertEquals(0xFF0000FF, img.pixels[x]);
        }
        // Row 1: pixels 1-3 blue, 0 and 4 white
        assertEquals(0xFFFFFFFF, img.pixels[5]);
        assertEquals(0xFF0000FF, img.pixels[6]);
        assertEquals(0xFF0000FF, img.pixels[7]);
        assertEquals(0xFF0000FF, img.pixels[8]);
        assertEquals(0xFFFFFFFF, img.pixels[9]);
        // Row 2: pixel 2 blue
        assertEquals(0xFFFFFFFF, img.pixels[10]);
        assertEquals(0xFFFFFFFF, img.pixels[11]);
        assertEquals(0xFF0000FF, img.pixels[12]);
        assertEquals(0xFFFFFFFF, img.pixels[13]);
    }

    @Test
    void testFillScanlinesEmptyBuffer() {
        RgbaImage img = new RgbaImage(5, 5);
        ScanlineBuffer buffer = new ScanlineBuffer(5);
        // No scanlines added
        img.fillScanlines(buffer, 0xFFFF0000);

        // All pixels should still be white
        for (int pixel : img.pixels) {
            assertEquals(0xFFFFFFFF, pixel);
        }
    }
}
