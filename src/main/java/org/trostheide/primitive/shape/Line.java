package org.trostheide.primitive.shape;

import org.trostheide.primitive.raster.ScanlineBuffer;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.Line2D;
import java.util.concurrent.ThreadLocalRandom;

public class Line implements Shape {
    public double x1, y1, x2, y2;
    public double width; // stroke width

    // Reusable triangles for rasterization (composition)
    private final Triangle t1 = new Triangle();
    private final Triangle t2 = new Triangle();

    public Line() {}

    public Line(double x1, double y1, double x2, double y2, double width) {
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
        this.width = width;
    }

    @Override
    public void rasterize(ScanlineBuffer buffer, int w, int h) {
        // Calculate the 4 corners of the thick line
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx*dx + dy*dy);

        if (len == 0) return;

        // Normal vector scaled by half width
        double nx = -dy / len * (width / 2);
        double ny = dx / len * (width / 2);

        // 4 Corners
        double px1 = x1 + nx, py1 = y1 + ny;
        double px2 = x1 - nx, py2 = y1 - ny;
        double px3 = x2 - nx, py3 = y2 - ny;
        double px4 = x2 + nx, py4 = y2 + ny;

        // Update internal triangles to form the rectangle (quad)
        // Quad: 1-2-3-4. Triangles: 1-2-3 and 3-4-1
        t1.x1 = px1; t1.y1 = py1;
        t1.x2 = px2; t1.y2 = py2;
        t1.x3 = px3; t1.y3 = py3;

        t2.x1 = px3; t2.y1 = py3;
        t2.x2 = px4; t2.y2 = py4;
        t2.x3 = px1; t2.y3 = py1;

        // Rasterize both (appending to buffer, since we removed reset())
        t1.rasterize(buffer, w, h);
        t2.rasterize(buffer, w, h);
    }

    @Override
    public void mutate(int w, int h) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int type = rnd.nextInt(3);
        double offset = 16;

        if (type == 0) { // Move p1
            x1 = clamp(x1 + rnd.nextGaussian() * offset, w);
            y1 = clamp(y1 + rnd.nextGaussian() * offset, h);
        } else if (type == 1) { // Move p2
            x2 = clamp(x2 + rnd.nextGaussian() * offset, w);
            y2 = clamp(y2 + rnd.nextGaussian() * offset, h);
        } else { // Change width
            width = clamp(width + rnd.nextGaussian(), w); // allow width up to canvas size theoretically, but practically smaller
            width = Math.max(1, width); // Min width 1
        }
    }

    private double clamp(double val, int max) {
        return Math.max(0, Math.min(max, val));
    }

    @Override
    public Shape copy() {
        return new Line(x1, y1, x2, y2, width);
    }

    @Override
    public void draw(Graphics2D g, int w, int h) {
        g.setStroke(new BasicStroke((float)width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    @Override
    public String toSvg(String color, double opacity) {
        return String.format(
                "<line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"%s\" stroke-width=\"%.2f\" stroke-opacity=\"%.2f\" fill=\"none\" />",
                x1, y1, x2, y2, color, width, opacity
        );
    }
}