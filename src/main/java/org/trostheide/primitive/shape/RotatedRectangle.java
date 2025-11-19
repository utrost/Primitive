package org.trostheide.primitive.shape;

import org.trostheide.primitive.raster.ScanlineBuffer;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.concurrent.ThreadLocalRandom;

public class RotatedRectangle implements Shape {
    public double cx, cy; // Center
    public double w, h;   // Dimensions
    public double angle;  // Rotation in degrees

    // Reusable triangles for rasterization
    private final Triangle t1 = new Triangle();
    private final Triangle t2 = new Triangle();

    public RotatedRectangle() {}

    public RotatedRectangle(double cx, double cy, double w, double h, double angle) {
        this.cx = cx; this.cy = cy;
        this.w = w; this.h = h;
        this.angle = angle;
    }

    @Override
    public void rasterize(ScanlineBuffer buffer, int width, int height) {
        // Calculate 4 corners
        double dx = w / 2.0;
        double dy = h / 2.0;
        double rad = Math.toRadians(angle);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        // Corner offsets relative to center
        double x1 = (-dx * cos - -dy * sin) + cx;
        double y1 = (-dx * sin + -dy * cos) + cy;

        double x2 = (dx * cos - -dy * sin) + cx;
        double y2 = (dx * sin + -dy * cos) + cy;

        double x3 = (dx * cos - dy * sin) + cx;
        double y3 = (dx * sin + dy * cos) + cy;

        double x4 = (-dx * cos - dy * sin) + cx;
        double y4 = (-dx * sin + dy * cos) + cy;

        t1.x1 = x1; t1.y1 = y1; t1.x2 = x2; t1.y2 = y2; t1.x3 = x3; t1.y3 = y3;
        t2.x1 = x1; t2.y1 = y1; t2.x2 = x3; t2.y2 = y3; t2.x3 = x4; t2.y3 = y4;

        t1.rasterize(buffer, width, height);
        t2.rasterize(buffer, width, height);
    }

    @Override
    public void mutate(int width, int height) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int type = rnd.nextInt(3);
        double offset = 16;

        if (type == 0) { // Move Center
            cx = clamp(cx + rnd.nextGaussian() * offset, width);
            cy = clamp(cy + rnd.nextGaussian() * offset, height);
        } else if (type == 1) { // Resize
            w = clamp(w + rnd.nextGaussian() * offset, width);
            h = clamp(h + rnd.nextGaussian() * offset, height);
            w = Math.max(1, w);
            h = Math.max(1, h);
        } else { // Rotate
            angle += rnd.nextGaussian() * 10;
        }
    }

    private double clamp(double val, int max) { return Math.max(0, Math.min(max, val)); }

    @Override
    public Shape copy() { return new RotatedRectangle(cx, cy, w, h, angle); }

    @Override
    public void draw(Graphics2D g, int width, int height) {
        double dx = w / 2.0; double dy = h / 2.0;
        double rad = Math.toRadians(angle);
        double cos = Math.cos(rad); double sin = Math.sin(rad);

        Path2D.Double p = new Path2D.Double();
        p.moveTo((-dx * cos - -dy * sin) + cx, (-dx * sin + -dy * cos) + cy);
        p.lineTo((dx * cos - -dy * sin) + cx, (dx * sin + -dy * cos) + cy);
        p.lineTo((dx * cos - dy * sin) + cx, (dx * sin + dy * cos) + cy);
        p.lineTo((-dx * cos - dy * sin) + cx, (-dx * sin + dy * cos) + cy);
        p.closePath();
        g.fill(p);
    }

    @Override
    public String toSvg(String color, double opacity) {
        // FIX: Removed the extra "translate(-cx, -cy)" which was incorrectly shifting the shape back.
        // Since the rect is defined at -w/2, -h/2 (centered at 0), we only need to rotate
        // and then move it to the destination (cx, cy).
        return String.format(
                "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" transform=\"translate(%.2f,%.2f) rotate(%.2f)\" fill=\"%s\" fill-opacity=\"%.2f\" />",
                -w/2, -h/2, w, h, cx, cy, angle, color, opacity
        );
    }
}