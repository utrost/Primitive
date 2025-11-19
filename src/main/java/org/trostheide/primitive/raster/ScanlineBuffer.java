package org.trostheide.primitive.raster;

import java.util.Arrays;

public final class ScanlineBuffer {
    public int[] y;
    public int[] x1;
    public int[] x2;
    public int count;

    public ScanlineBuffer(int capacity) {
        this.y = new int[capacity];
        this.x1 = new int[capacity];
        this.x2 = new int[capacity];
        this.count = 0;
    }

    /**
     * CRITICAL: This must set count to 0 to prevent accumulation.
     */
    public void reset() {
        this.count = 0;
    }

    public void add(int yCoord, int startX, int endX) {
        if (count >= y.length) {
            resize(count * 2);
        }
        y[count] = yCoord;
        x1[count] = startX;
        x2[count] = endX;
        count++;
    }

    private void resize(int newSize) {
        y = Arrays.copyOf(y, newSize);
        x1 = Arrays.copyOf(x1, newSize);
        x2 = Arrays.copyOf(x2, newSize);
    }
}