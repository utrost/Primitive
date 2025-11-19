package org.trostheide.primitive.shape;

import org.trostheide.primitive.raster.ScanlineBuffer;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.QuadCurve2D;
import java.util.concurrent.ThreadLocalRandom;

public class QuadraticBezier implements Shape {
    public double x1, y1; // Start
    public double cx, cy; // Control
    public double x2, y2; // End
    public double width;

    // Quality of approximation (number of line segments)
    private static final int SEGMENTS = 10;
    private final Line[] segments;

    public QuadraticBezier() {
        segments = new Line[SEGMENTS];
        for(int i=0; i<SEGMENTS; i++) segments[i] = new Line();
    }

    public QuadraticBezier(double x1, double y1, double cx, double cy, double x2, double y2, double width) {
        this();
        this.x1 = x1; this.y1 = y1;
        this.cx = cx; this.cy = cy;
        this.x2 = x2; this.y2 = y2;
        this.width = width;
    }

    @Override
    public void rasterize(ScanlineBuffer buffer, int w, int h) {
        // Decompose curve into linear segments
        double prevX = x1;
        double prevY = y1;

        for (int i = 0; i < SEGMENTS; i++) {
            double t = (i + 1) / (double) SEGMENTS;
            double invT = 1 - t;

            // Quadratic Bezier formula: (1-t)^2 * P0 + 2(1-t)t * P1 + t^2 * P2
            double curX = (invT * invT * x1) + (2 * invT * t * cx) + (t * t * x2);
            double curY = (invT * invT * y1) + (2 * invT * t * cy) + (t * t * y2);

            // Update internal line segment
            Line seg = segments[i];
            seg.x1 = prevX; seg.y1 = prevY;
            seg.x2 = curX; seg.y2 = curY;
            seg.width = this.width;

            // Rasterize segment
            seg.rasterize(buffer, w, h);

            prevX = curX;
            prevY = curY;
        }
    }

    @Override
    public void mutate(int w, int h) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int type = rnd.nextInt(4);
        double offset = 16;

        if (type == 0) { x1 = clamp(x1 + rnd.nextGaussian() * offset, w); y1 = clamp(y1 + rnd.nextGaussian() * offset, h); }
        else if (type == 1) { cx = clamp(cx + rnd.nextGaussian() * offset, w); cy = clamp(cy + rnd.nextGaussian() * offset, h); }
        else if (type == 2) { x2 = clamp(x2 + rnd.nextGaussian() * offset, w); y2 = clamp(y2 + rnd.nextGaussian() * offset, h); }
        else { width = Math.max(1, width + rnd.nextGaussian()); }
    }

    private double clamp(double val, int max) { return Math.max(0, Math.min(max, val)); }

    @Override
    public Shape copy() {
        return new QuadraticBezier(x1, y1, cx, cy, x2, y2, width);
    }

    @Override
    public void draw(Graphics2D g, int w, int h) {
        g.setStroke(new BasicStroke((float)width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(x1, y1, cx, cy, x2, y2));
    }

    @Override
    public String toSvg(String color, double opacity) {
        return String.format(
                "<path d=\"M %.2f %.2f Q %.2f %.2f, %.2f %.2f\" stroke=\"%s\" stroke-width=\"%.2f\" stroke-opacity=\"%.2f\" fill=\"none\" stroke-linecap=\"round\" />",
                x1, y1, cx, cy, x2, y2, color, width, opacity
        );
    }
}