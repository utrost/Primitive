package org.trostheide.primitive.util;

import org.trostheide.primitive.image.RgbaImage;
import org.trostheide.primitive.raster.ScanlineBuffer;

public class ColorUtils {

    /**
     * Calculates the RMSE between two full images.
     * Used for the initial baseline or final validation.
     */
    public static double rmse(RgbaImage img1, RgbaImage img2) {
        if (img1.width != img2.width || img1.height != img2.height) {
            throw new IllegalArgumentException("Image dimensions must match");
        }

        long totalSqDiff = 0;
        int[] p1 = img1.pixels;
        int[] p2 = img2.pixels;
        int len = p1.length; // loop optimization

        for (int i = 0; i < len; i++) {
            int c1 = p1[i];
            int c2 = p2[i];

            int dr = ((c1 >> 16) & 0xFF) - ((c2 >> 16) & 0xFF);
            int dg = ((c1 >> 8) & 0xFF) - ((c2 >> 8) & 0xFF);
            int db = (c1 & 0xFF) - (c2 & 0xFF);

            totalSqDiff += (dr * dr + dg * dg + db * db);
        }

        // Mean Squared Error
        double mse = (double) totalSqDiff / (img1.width * img1.height);
        return Math.sqrt(mse);
    }
}