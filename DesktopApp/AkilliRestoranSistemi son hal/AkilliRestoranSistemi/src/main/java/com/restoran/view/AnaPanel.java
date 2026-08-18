package com.restoran.view;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class AnaPanel extends JFrame {

    public AnaPanel() {
        setTitle("Akıllı Restoran Uygulaması");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setIconImage(createRestaurantIcon());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Genel Bakış", new PanelGenelBakis(tabbedPane));
        tabbedPane.addTab("Duygu Analizi", new PanelDuyguAnalizi());
        tabbedPane.addTab("Aktif Siparişler", new PanelAktifSiparisler());
        tabbedPane.addTab("Sipariş Girişi", new PanelSiparisGirisi());
        add(tabbedPane);
    }

    private java.awt.Image createRestaurantIcon() {
        int width = 64;
        int height = 64;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint backgroundGradient = new GradientPaint(
            0, 0, new Color(255, 87, 34),
            width, height, new Color(255, 152, 0)
        );
        g2.setPaint(backgroundGradient);
        g2.fillRoundRect(0, 0, width, height, 12, 12);

        g2.setColor(new Color(139, 69, 19));
        int[] xPoints = {width / 2, width / 8, width * 7 / 8};
        int[] yPoints = {height / 4, height / 2, height / 2};
        g2.fillPolygon(xPoints, yPoints, 3);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(width / 4, height / 2, width / 2, height / 3, 5, 5);

        g2.setColor(new Color(101, 67, 33));
        g2.fillRoundRect(width * 9 / 20, height * 11 / 16, width / 10, height / 4, 3, 3);

        g2.setColor(new Color(33, 150, 243));
        g2.fillOval(width * 7 / 16, height * 9 / 16, width / 8, width / 8);

        g2.setColor(new Color(233, 30, 99));
        g2.setStroke(new java.awt.BasicStroke(3.0f));
        g2.drawLine(width * 3 / 8, height * 3 / 4, width * 3 / 8, height * 7 / 8);
        g2.drawLine(width * 11 / 32, height * 13 / 16, width * 3 / 8, height * 7 / 8);
        g2.drawLine(width * 13 / 32, height * 13 / 16, width * 3 / 8, height * 7 / 8);
        g2.drawLine(width * 5 / 8, height * 3 / 4, width * 5 / 8, height * 7 / 8);
        g2.drawLine(width * 5 / 8, height * 3 / 4, width * 11 / 16, height * 25 / 32);

        g2.setColor(new Color(255, 235, 59));
        drawStar(g2, width / 4, height / 3, 4);
        drawStar(g2, width * 3 / 4, height / 3, 4);

        g2.dispose();
        return image;
    }

    private void drawStar(Graphics2D g2, int centerX, int centerY, int radius) {
        int nPoints = 5;
        int[] xPoints = new int[nPoints * 2];
        int[] yPoints = new int[nPoints * 2];

        for (int i = 0; i < nPoints * 2; i++) {
            double angle = Math.PI * i / nPoints - Math.PI / 2;
            int r = (i % 2 == 0) ? radius : radius / 2;
            xPoints[i] = centerX + (int) (r * Math.cos(angle));
            yPoints[i] = centerY + (int) (r * Math.sin(angle));
        }
        g2.fillPolygon(xPoints, yPoints, nPoints * 2);
    }
}
