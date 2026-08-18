package com.restoran.service;

import com.restoran.controller.AnaPanelController;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

public class DuyguAnaliziServisi implements Runnable {
    private volatile boolean calisiyor = false;
    private JPanel kameraPaneli;
    private AnaPanelController controller;

    public DuyguAnaliziServisi(JPanel panel, AnaPanelController controller) {
        this.kameraPaneli = panel;
        this.controller = controller;
    }

    public void baslat() {
        calisiyor = true;
        new Thread(this).start();
    }

    public void durdur() {
        calisiyor = false;
    }

    @Override
    public void run() {
        while (calisiyor) {
            try {
                gosterKameraMesaji();
                int mutlulukOrani = (int) (Math.random() * 100);

                if (controller != null) {
                    controller.guncelDuyguDurumunuAl(mutlulukOrani);
                }

                Thread.sleep(2000);
            } catch (InterruptedException e) {
                durdur();
            }
        }
    }

    private void gosterKameraMesaji() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (kameraPaneli == null) {
                return;
            }
            Graphics g = kameraPaneli.getGraphics();
            if (g != null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, kameraPaneli.getWidth(), kameraPaneli.getHeight());
                g2d.setColor(Color.GREEN);
                g2d.drawString("Kamera Modülü", 100, 100);
                g2d.drawString("(Simülasyon Modu)", 80, 120);
                g.dispose();
            }
        });
    }
}
