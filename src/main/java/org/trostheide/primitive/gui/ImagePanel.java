package org.trostheide.primitive.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel {
    private BufferedImage image;

    public ImagePanel() {
        this.setBackground(Color.DARK_GRAY);
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
        if (image != null) {
            // Draw image centered and scaled to fit (preserving aspect ratio)
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();

            double scaleX = (double) panelWidth / imgWidth;
            double scaleY = (double) panelHeight / imgHeight;
            double scale = Math.min(scaleX, scaleY);

            // Limit scale to 1.0 if image is smaller than panel? No, let's scale up pixel
            // art style maybe?
            // Or just fit. Let's simple fit.

            int newWidth = (int) (imgWidth * scale);
            int newHeight = (int) (imgHeight * scale);

            int x = (panelWidth - newWidth) / 2;
            int y = (panelHeight - newHeight) / 2;

            g.drawImage(image, x, y, newWidth, newHeight, this);
        }
    }
}
