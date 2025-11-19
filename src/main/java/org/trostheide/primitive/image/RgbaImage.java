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
        Arrays.fill(pixels, 0xFFFFFFFF);
    }

    private RgbaImage(int width, int height, int[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    public static RgbaImage load(File file) throws IOException {
        BufferedImage temp = ImageIO.read(file);
        if (temp == null) throw new IOException("Could not read image: " + file);

        BufferedImage argbImage = new BufferedImage(temp.getWidth(), temp.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argbImage.createGraphics();
        g.drawImage(temp, 0, 0, null);
        g.dispose();

        int w = argbImage.getWidth();
        int h = argbImage.getHeight();
        int[] rawPixels = argbImage.getRGB(0, 0, w, h, null, 0, w);

        return new RgbaImage(w, h, rawPixels);
    }

    @SuppressWarnings("unused") // Kept for future UI/resizing features
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

    public void save(File file) throws IOException {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, width, height, pixels, 0, width);
        ImageIO.write(out, "png", file);
    }

    @SuppressWarnings("unused") // Kept for future state management
    public RgbaImage copy() {
        return new RgbaImage(width, height, Arrays.copyOf(pixels, pixels.length));
    }

    public void fillScanlines(ScanlineBuffer scanlines, int color) {
        int cr = (color >> 16) & 0xFF;
        int cg = (color >> 8) & 0xFF;
        int cb = (color & 0xFF);
        int ca = (color >> 24) & 0xFF;

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
                int br = (bg >> 16) & 0xFF;
                int bg_ = (bg >> 8) & 0xFF;
                int bb = (bg & 0xFF);

                int r = (cr * ca + br * invAlpha) / 255;
                int g = (cg * ca + bg_ * invAlpha) / 255;
                int b = (cb * ca + bb * invAlpha) / 255;

                pixels[idx] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }
    }
    // REMOVED: unused computeAverageColor (logic is in Optimizer)
}