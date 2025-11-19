package org.trostheide.primitive.shape;

import org.trostheide.primitive.raster.ScanlineBuffer;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Polyline implements Shape {
    public double[] xPoints;
    public double[] yPoints;
    public double width; // stroke width
    private final int count;

    // Reuse a single Line object for rasterizing segments
    private final Line segment = new Line();

    public Polyline(int count) {
        this.count = count;
        this.xPoints = new double[count];
        this.yPoints = new double[count];
        this.width = 1.0;
    }

    private Polyline(double[] x, double[] y, double w) {
        this.count = x.length;
        this.xPoints = Arrays.copyOf(x, count);
        this.yPoints = Arrays.copyOf(y, count);
        this.width = w;
    }

    @Override
    public void rasterize(ScanlineBuffer buffer, int w, int h) {
        segment.width = this.width;
        for (int i = 0; i < count - 1; i++) {
            segment.x1 = xPoints[i];
            segment.y1 = yPoints[i];
            segment.x2 = xPoints[i+1];
            segment.y2 = yPoints[i+1];

            segment.rasterize(buffer, w, h);
        }
    }

    @Override
    public void mutate(int w, int h) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        // 0: Move a specific point
        // 1: Change width
        if (rnd.nextDouble() < 0.2) {
            width = Math.max(1, width + rnd.nextGaussian());
        } else {
            int idx = rnd.nextInt(count);
            double offset = 16;
            xPoints[idx] = clamp(xPoints[idx] + rnd.nextGaussian() * offset, w);
            yPoints[idx] = clamp(yPoints[idx] + rnd.nextGaussian() * offset, h);
        }
    }

    private double clamp(double val, int max) { return Math.max(0, Math.min(max, val)); }

    @Override
    public Shape copy() { return new Polyline(xPoints, yPoints, width); }

    @Override
    public void draw(Graphics2D g, int w, int h) {
        g.setStroke(new BasicStroke((float)width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double path = new Path2D.Double();
        path.moveTo(xPoints[0], yPoints[0]);
        for(int i=1; i<count; i++) path.lineTo(xPoints[i], yPoints[i]);
        g.draw(path);
    }

    @Override
    public String toSvg(String color, double opacity) {
        StringBuilder sb = new StringBuilder();
        sb.append("<polyline points=\"");
        for(int i=0; i<count; i++) {
            sb.append(String.format("%.2f,%.2f ", xPoints[i], yPoints[i]));
        }
        sb.append(String.format("\" stroke=\"%s\" stroke-width=\"%.2f\" stroke-opacity=\"%.2f\" fill=\"none\" stroke-linecap=\"round\" stroke-linejoin=\"round\" />", color, width, opacity));
        return sb.toString();
    }
}