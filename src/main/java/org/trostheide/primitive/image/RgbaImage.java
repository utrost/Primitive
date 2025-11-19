package org.trostheide.primitive.image;

import org.trostheide.primitive.raster.ScanlineBuffer;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class RgbaImage {
    public final int width;
    public final int height;
    public final int[] pixels; // The raw data: 0xAARRGGBB

    public RgbaImage(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
        // Initialize with opaque white or transparent depending on need.
        // Default int is 0 (transparent), often we want opaque background.
        Arrays.fill(pixels, 0xFFFFFFFF);
    }

    private RgbaImage(int width, int height, int[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    /**
     * Factory: Loads an image from disk and converts it to our efficient int[] format.
     */
    public static RgbaImage load(File file) throws IOException {
        BufferedImage temp = ImageIO.read(file);
        if (temp == null) throw new IOException("Could not read image: " + file);

        // Convert to ensured TYPE_INT_ARGB to simplify extraction
        BufferedImage argbImage = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argbImage.createGraphics();
        g.drawImage(temp, 0, 0, null);
        g.dispose();

        int w = argbImage.getWidth();
        int h = argbImage.getHeight();

        // Grab the data buffer directly if possible, or use getRGB
        // getRGB is safer across platforms and fast enough for "load-once"
        int[] rawPixels = argbImage.getRGB(0, 0, w, h, null, 0, w);

        return new RgbaImage(w, h, rawPixels);
    }

    /**
     * Resizes the image. Important for performance: running the algorithm on 4000px images is slow.
     * Scaling down to ~256px is standard practice for the algorithm.
     */
    public RgbaImage resize(int targetWidth, int targetHeight) {
        BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        temp.setRGB(0, 0, width, height, pixels, 0, width);

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(temp, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        int[] newPixels = scaled.getRGB(0, 0, targetWidth, targetHeight, null, 0, targetWidth);
        return new RgbaImage(targetWidth, targetHeight, newPixels);
    }

    /**
     * Saves the current state to a file.
     */
    public void save(File file) throws IOException {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, width, height, pixels, 0, width);
        ImageIO.write(out, "png", file);
    }

    /**
     * Creates a deep copy of the image data.
     * Essential for the Worker threads to have their own "Current Canvas" to mutate.
     */
    public RgbaImage copy() {
        return new RgbaImage(width, height, Arrays.copyOf(pixels, pixels.length));
    }

    // ==================================================================================
    //  ALGORITHMIC CORE: Color Solving
    // ==================================================================================

    /**
     * Computes the average color of the pixels covered by the provided scanlines.
     * This implements the "Optimal Color Computation".
     * * @param scanlines The geometry mask
     * @param scanlines     The alpha we intend to use (0-255).
     * (Note: If solving for alpha=1 (opaque), this is a simple average).
     * @return          The packed int color (0xAARRGGBB).
     */
    public int computeAverageColor(ScanlineBuffer scanlines) {
        long rSum = 0;
        long gSum = 0;
        long bSum = 0;
        int count = 0;

        // Access arrays directly to avoid method overhead
        int[] yArr = scanlines.y;
        int[] x1Arr = scanlines.x1;
        int[] x2Arr = scanlines.x2;
        int numScanlines = scanlines.count;

        for (int i = 0; i < numScanlines; i++) {
            int y = yArr[i];
            int xStart = x1Arr[i];
            int xEnd = x2Arr[i];
            int rowOffset = y * width; // Optimization: calculate row offset once

            for (int x = xStart; x < xEnd; x++) {
                int p = pixels[rowOffset + x];

                // Bitwise extraction of channels
                rSum += (p >> 16) & 0xFF;
                gSum += (p >> 8) & 0xFF;
                bSum += p & 0xFF;
                count++;
            }
        }

        if (count == 0) return 0xFF000000; // Fallback

        int r = (int) (rSum / count);
        int g = (int) (gSum / count);
        int b = (int) (bSum / count);

        // Return opaque color (alpha 255).
        // The algorithm typically computes the RGB, and sets a fixed or searched Alpha.
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Draws the scanlines onto THIS image with a specific color.
     * Used to commit a successful shape to the canvas.
     */
    public void fillScanlines(ScanlineBuffer scanlines, int color) {
        int cr = (color >> 16) & 0xFF;
        int cg = (color >> 8) & 0xFF;
        int cb = (color & 0xFF);
        int ca = (color >> 24) & 0xFF;

        // Pre-calculate alpha blending factors (0-255 scale)
        // Standard blending: Out = (Src * Alpha + Dst * (255 - Alpha)) / 255
        int invAlpha = 255 - ca;

        int[] yArr = scanlines.y;
        int[] x1Arr = scanlines.x1;
        int[] x2Arr = scanlines.x2;
        int numScanlines = scanlines.count;

        for (int i = 0; i < numScanlines; i++) {
            int y = yArr[i];
            int xStart = x1Arr[i];
            int xEnd = x2Arr[i];
            int rowOffset = y * width;

            for (int x = xStart; x < xEnd; x++) {
                int idx = rowOffset + x;
                int bg = pixels[idx];

                // Extract background components
                int br = (bg >> 16) & 0xFF;
                int bg_ = (bg >> 8) & 0xFF;
                int bb = (bg & 0xFF);

                // Blend
                int r = (cr * ca + br * invAlpha) / 255;
                int g = (cg * ca + bg_ * invAlpha) / 255;
                int b = (cb * ca + bb * invAlpha) / 255;

                pixels[idx] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }
    }
}