package org.trostheide.primitive.shape;

import org.trostheide.primitive.raster.ScanlineBuffer;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.concurrent.ThreadLocalRandom;

public class Ellipse implements Shape {
    public double x, y; // Center
    public double rx, ry; // Radii
    public double angle; // in degrees

    public Ellipse() {
    }

    public Ellipse(double x, double y, double rx, double ry, double angle) {
        this.x = x;
        this.y = y;
        this.rx = rx;
        this.ry = ry;
        this.angle = angle;
    }

    @Override
    public void rasterize(ScanlineBuffer buffer, int width, int height) {
        // Precompute constants for the quadratic equation
        double rad = Math.toRadians(angle);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double cos2 = cos * cos;
        double sin2 = sin * sin;
        double rx2 = rx * rx;
        double ry2 = ry * ry;

        // Coefficients for the general ellipse equation relative to center (dX, dY):
        // C1 * dX^2 + C2 * dX * dY + C3 * dY^2 - 1 = 0
        // Derived from substituting rotated coordinates into standard ellipse eq.
        double c1 = (cos2 / rx2) + (sin2 / ry2);
        double c2 = 2 * sin * cos * ((1 / rx2) - (1 / ry2));
        double c3 = (sin2 / rx2) + (cos2 / ry2);

        // Bounding box height to limit Y iteration
        double yRange = Math.sqrt(rx2 * sin2 + ry2 * cos2);
        int yStart = Math.max(0, (int) Math.floor(y - yRange));
        int yEnd = Math.min(height, (int) Math.ceil(y + yRange));

        // Iterate over scanlines
        for (int currY = yStart; currY < yEnd; currY++) {
            double dy = currY - y;

            // Solve quadratic for dX: A*dX^2 + B*dX + C = 0
            // A = c1
            // B = c2 * dy
            // C = c3 * dy^2 - 1
            double a = c1;
            double b = c2 * dy;
            double c = c3 * dy * dy - 1;

            double discriminant = b * b - 4 * a * c;

            if (discriminant >= 0) {
                double sqrtDisc = Math.sqrt(discriminant);
                double dx1 = (-b - sqrtDisc) / (2 * a);
                double dx2 = (-b + sqrtDisc) / (2 * a);

                int xStart = (int) Math.floor(x + dx1);
                int xEnd = (int) Math.ceil(x + dx2);

                // Clamp to image width
                xStart = Math.max(0, xStart);
                xEnd = Math.min(width, xEnd);

                if (xStart < xEnd) {
                    buffer.add(currY, xStart, xEnd);
                }
            }
        }
    }

    @Override
    public void mutate(int width, int height) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int type = rnd.nextInt(3);
        double offset = 16;

        if (type == 0) { // Move Center
            x = clamp(x + rnd.nextGaussian() * offset, width);
            y = clamp(y + rnd.nextGaussian() * offset, height);
        } else if (type == 1) { // Resize radii
            rx = clamp(rx + rnd.nextGaussian() * offset, width);
            ry = clamp(ry + rnd.nextGaussian() * offset, height);
            rx = Math.max(1, rx);
            ry = Math.max(1, ry);
        } else { // Rotate
            angle += rnd.nextGaussian() * 10;
        }
    }

    private double clamp(double val, int max) {
        return Math.max(0, Math.min(max, val));
    }

    @Override
    public Shape copy() {
        return new Ellipse(x, y, rx, ry, angle);
    }

    @Override
    public void draw(Graphics2D g, int width, int height) {
        // Create an Ellipse2D centered at (0,0) then transform it
        Ellipse2D.Double e = new Ellipse2D.Double(-rx, -ry, rx * 2, ry * 2);
        Path2D.Double p = new Path2D.Double(e);

        // Transform: Translate(x,y) * Rotate(angle)
        // Note: Path2D transform applies to the points.
        java.awt.geom.AffineTransform t = new java.awt.geom.AffineTransform();
        t.translate(x, y);
        t.rotate(Math.toRadians(angle));

        p.transform(t);
        g.fill(p);
    }

    @Override
    public String toSvg(String colorHex, double opacity) {
        return String.format(
                "<g transform=\"translate(%.2f,%.2f) rotate(%.2f)\">" +
                        "<ellipse rx=\"%.2f\" ry=\"%.2f\" fill=\"%s\" fill-opacity=\"%.2f\" />" +
                        "</g>",
                x, y, angle, rx, ry, colorHex, opacity);
    }
}
