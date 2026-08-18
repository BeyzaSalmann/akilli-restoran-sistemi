package com.restoran;

import com.restoran.view.AnaPanel;
import com.restoran.service.VeritabaniServisi;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.put("OptionPane.yesButtonText", "Evet");
            UIManager.put("OptionPane.noButtonText", "Hayır");
            UIManager.put("OptionPane.okButtonText", "Tamam");
            UIManager.put("OptionPane.cancelButtonText", "İptal");
            UIManager.put("OptionPane.titleText", "Bilgi");

            VeritabaniServisi veritabaniServisi = new VeritabaniServisi();
            veritabaniServisi.initDB();

            SwingUtilities.invokeLater(() -> {
                AnaPanel anaPencere = new AnaPanel();
                anaPencere.setVisible(true);
                System.out.println("Akıllı Restoran uygulaması başlatıldı.");
            });
        } catch (Exception e) {
            System.err.println("Uygulama başlatılamadı.");
            e.printStackTrace();
        }
    }
}
