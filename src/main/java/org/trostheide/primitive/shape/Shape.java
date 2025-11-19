package org.trostheide.primitive.shape;

import org.trostheide.primitive.raster.ScanlineBuffer;
import java.awt.Graphics2D;

public interface Shape {
    /**
     * Populates the provided buffer with the scanlines covered by this shape.
     * CRITICAL CHANGE: This method should NOT call buffer.reset().
     * The caller is responsible for clearing the buffer.
     */
    void rasterize(ScanlineBuffer buffer, int width, int height);

    void mutate(int width, int height);

    Shape copy();

    void draw(Graphics2D g, int width, int height);

    /**
     * Returns the SVG string representation of this shape.
     * @param colorHex The hex color string (e.g., "#FF0000")
     * @param opacity The opacity (0.0 to 1.0)
     */
    String toSvg(String colorHex, double opacity);
}