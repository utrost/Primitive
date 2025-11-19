package org.trostheide.primitive.shape;

import org.trostheide.primitive.raster.ScanlineBuffer;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.concurrent.ThreadLocalRandom;

public class Triangle implements Shape {
    public double x1, y1, x2, y2, x3, y3;

    public Triangle() {}

    public Triangle(double x1, double y1, double x2, double y2, double x3, double y3) {
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
        this.x3 = x3; this.y3 = y3;
    }

    @Override
    public void rasterize(ScanlineBuffer buffer, int width, int height) {
        // REMOVED: buffer.reset(); -> Caller must handle this!

        // Sort vertices by Y coordinate (p1 is top, p3 is bottom)
        double tx1 = x1, ty1 = y1;
        double tx2 = x2, ty2 = y2;
        double tx3 = x3, ty3 = y3;

        if (ty1 > ty2) { double t = ty1; ty1 = ty2; ty2 = t; t = tx1; tx1 = tx2; tx2 = t; }
        if (ty2 > ty3) { double t = ty2; ty2 = ty3; ty3 = t; t = tx2; tx2 = tx3; tx3 = t; }
        if (ty1 > ty2) { double t = ty1; ty1 = ty2; ty2 = t; t = tx1; tx1 = tx2; tx2 = t; }

        // Logic for Flat-Bottom and Flat-Top triangle splitting
        double dx13 = 0;
        if (ty3 != ty1) dx13 = (tx3 - tx1) / (ty3 - ty1);

        if (ty1 != ty2) {
            double dx12 = (tx2 - tx1) / (ty2 - ty1);
            processSegment(buffer, width, height, (int)ty1, (int)ty2, tx1, tx1, dx13, dx12);
        }

        if (ty2 != ty3) {
            double dx23 = (tx3 - tx2) / (ty3 - ty2);
            double startXLong = tx1 + (ty2 - ty1) * dx13;
            processSegment(buffer, width, height, (int)ty2, (int)ty3, startXLong, tx2, dx13, dx23);
        }
    }

    private void processSegment(ScanlineBuffer buffer, int w, int h,
                                int yStart, int yEnd,
                                double xA, double xB,
                                double slopeA, double slopeB) {
        for (int y = yStart; y < yEnd; y++) {
            if (y >= 0 && y < h) {
                int startX = (int) xA;
                int endX = (int) xB;

                // Ensure start < end
                if (startX > endX) { int temp = startX; startX = endX; endX = temp; }

                // Clamp X to image bounds
                startX = Math.max(0, startX);
                endX = Math.min(w, endX);

                if (startX < endX) {
                    buffer.add(y, startX, endX);
                }
            }
            xA += slopeA;
            xB += slopeB;
        }
    }

    @Override
    public void mutate(int width, int height) {
        int pointIdx = ThreadLocalRandom.current().nextInt(3);
        double offset = 16;
        double mx = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * offset;
        double my = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * offset;

        if (pointIdx == 0) { x1 = clamp(x1 + mx, width); y1 = clamp(y1 + my, height); }
        else if (pointIdx == 1) { x2 = clamp(x2 + mx, width); y2 = clamp(y2 + my, height); }
        else { x3 = clamp(x3 + mx, width); y3 = clamp(y3 + my, height); }
    }

    private double clamp(double val, int max) {
        return Math.max(0, Math.min(max - 1, val));
    }

    @Override
    public Shape copy() {
        return new Triangle(x1, y1, x2, y2, x3, y3);
    }

    @Override
    public void draw(Graphics2D g, int width, int height) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.closePath();
        g.fill(path);
    }

    @Override
    public String toSvg(String color, double opacity) {
        return String.format(
                "<polygon points=\"%.2f,%.2f %.2f,%.2f %.2f,%.2f\" fill=\"%s\" fill-opacity=\"%.2f\" />",
                x1, y1, x2, y2, x3, y3, color, opacity
        );
    }
}