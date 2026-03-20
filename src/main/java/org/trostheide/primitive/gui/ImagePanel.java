package org.trostheide.primitive.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel {
    private BufferedImage image;

    public ImagePanel() {
        setOpaque(true);
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        this.repaint();
    }

    public BufferedImage getImage() {
        return image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Background
        g2.setColor(ThemeManager.getCanvasBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (image != null) {
            // Draw image centered and scaled to fit with padding
            int pad = 16;
            int panelWidth = getWidth() - pad * 2;
            int panelHeight = getHeight() - pad * 2;
            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();

            double scaleX = (double) panelWidth / imgWidth;
            double scaleY = (double) panelHeight / imgHeight;
            double scale = Math.min(scaleX, scaleY);

            int newWidth = (int) (imgWidth * scale);
            int newHeight = (int) (imgHeight * scale);

            int x = (getWidth() - newWidth) / 2;
            int y = (getHeight() - newHeight) / 2;

            // Drop shadow
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRoundRect(x + 3, y + 3, newWidth, newHeight, 8, 8);

            // Image with rounded clip
            Shape clip = new RoundRectangle2D.Float(x, y, newWidth, newHeight, 8, 8);
            g2.setClip(clip);
            g2.drawImage(image, x, y, newWidth, newHeight, this);
            g2.setClip(null);

            // Border
            g2.setColor(ThemeManager.getBorderColor());
            g2.setStroke(new BasicStroke(1f));
            g2.draw(clip);
        } else {
            // Empty state: draw drop zone hint
            drawEmptyState(g2);
        }

        g2.dispose();
    }

    private void drawEmptyState(Graphics2D g2) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        // Dashed border rectangle
        float dash[] = {8.0f, 8.0f};
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f));

        int boxW = Math.min(320, getWidth() - 60);
        int boxH = Math.min(200, getHeight() - 60);
        g2.setColor(ThemeManager.getBorderColor());
        g2.drawRoundRect(cx - boxW / 2, cy - boxH / 2, boxW, boxH, 16, 16);

        // Icon (upload arrow)
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(ThemeManager.getSubtleText());
        int iconY = cy - 30;
        // Arrow shaft
        g2.drawLine(cx, iconY - 20, cx, iconY + 15);
        // Arrow head
        g2.drawLine(cx - 10, iconY - 10, cx, iconY - 20);
        g2.drawLine(cx + 10, iconY - 10, cx, iconY - 20);
        // Base line
        g2.drawLine(cx - 15, iconY + 15, cx + 15, iconY + 15);

        // Text
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 15f));
        String primary = "Drop image here";
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(ThemeManager.getSubtleText());
        g2.drawString(primary, cx - fm.stringWidth(primary) / 2, cy + 20);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        fm = g2.getFontMetrics();
        String secondary = "or use Open Image (Ctrl+O)";
        g2.setColor(ThemeManager.getSubtleText().brighter());
        g2.drawString(secondary, cx - fm.stringWidth(secondary) / 2, cy + 40);
    }
}
