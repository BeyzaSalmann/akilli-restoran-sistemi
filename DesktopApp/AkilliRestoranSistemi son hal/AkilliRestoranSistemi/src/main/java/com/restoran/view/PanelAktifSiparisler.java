package com.restoran.view;

import com.restoran.service.RestoranOneriSistemi;
import com.restoran.service.VeritabaniServisi;
import com.restoran.service.VeritabaniServisi.SiparisBilgisi;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PanelAktifSiparisler extends JPanel {
    
    private VeritabaniServisi veritabaniServisi;
    private RestoranOneriSistemi oneriSistemi;
    private JPanel siparislerPaneli;
    private JScrollPane scrollPane;
    
    public PanelAktifSiparisler() {
        veritabaniServisi = new VeritabaniServisi();
        baslatOneriSistemi();
        initComponents();
        siparisleriYenile();
    }
    
    private void baslatOneriSistemi() {
        String menuPath = "menu_items.csv";
        String ordersPath = "order_details.csv";
        
        try {
            oneriSistemi = new RestoranOneriSistemi(menuPath, ordersPath);
            
            // Verileri yükle ve modeli eğit (arka planda)
            new Thread(() -> {
                try {
                    oneriSistemi.veriYukleVeHazirla();
                    oneriSistemi.modeliEgit(0.002, 0.1);
                } catch (Exception e) {
                    System.err.println("Öneri sistemi başlatılamadı: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            System.err.println("Öneri sistemi oluşturulamadı: " + e.getMessage());
        }
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        setBackground(new Color(248, 249, 250));
        
        // Ana başlık
        JPanel baslikPanel = new JPanel(new BorderLayout());
        baslikPanel.setOpaque(false);
        baslikPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel lblAnaBaslik = new JLabel("📋 Aktif Verilen Siparişler");
        lblAnaBaslik.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblAnaBaslik.setForeground(new Color(33, 37, 41));
        baslikPanel.add(lblAnaBaslik, BorderLayout.WEST);
        
        // Yenile butonu
        JButton btnYenile = createModernButton("🔄 Yenile", new Color(0, 123, 255), Color.WHITE);
        btnYenile.setPreferredSize(new Dimension(130, 35));
        btnYenile.addActionListener(e -> siparisleriYenile());
        baslikPanel.add(btnYenile, BorderLayout.EAST);
        
        add(baslikPanel, BorderLayout.NORTH);
        
        // Sipariş kartları paneli (dikey liste)
        siparislerPaneli = new JPanel();
        siparislerPaneli.setLayout(new BoxLayout(siparislerPaneli, BoxLayout.Y_AXIS));
        siparislerPaneli.setOpaque(false);
        
        // ScrollPane içine al
        scrollPane = new JScrollPane(siparislerPaneli);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(108, 117, 125);
                this.trackColor = new Color(248, 249, 250);
            }
        });
        
        add(scrollPane, BorderLayout.CENTER);
        
        // En alt: "Tüm Siparişleri Gör" label
        JPanel altPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        altPanel.setOpaque(false);
        altPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        JLabel lblTumSiparisler = new JLabel("👁️ Tüm Siparişleri Gör");
        lblTumSiparisler.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTumSiparisler.setForeground(new Color(0, 123, 255));
        lblTumSiparisler.setCursor(new Cursor(Cursor.HAND_CURSOR));
        altPanel.add(lblTumSiparisler);
        
        add(altPanel, BorderLayout.SOUTH);
    }
    
    public void siparisleriYenile() {
        // Mevcut kartları temizle
        siparislerPaneli.removeAll();
        
        // Veritabanından aktif siparişleri al
        List<SiparisBilgisi> aktifSiparisler = veritabaniServisi.getAktifSiparisler();
        
        if (aktifSiparisler.isEmpty()) {
            JLabel lblBos = new JLabel("<html><div style='text-align: center; padding: 40px;'>" +
                "<h2 style='color: #6c757d;'>📭 Henüz aktif sipariş yok</h2>" +
                "<p style='color: #adb5bd;'>Yeni siparişler burada görünecektir.</p>" +
                "</div></html>", SwingConstants.CENTER);
            lblBos.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            lblBos.setAlignmentX(Component.CENTER_ALIGNMENT);
            siparislerPaneli.add(lblBos);
        } else {
            for (int i = 0; i < aktifSiparisler.size(); i++) {
                SiparisBilgisi siparis = aktifSiparisler.get(i);
                
                // Masa bilgisi ile başlık oluştur
                String baslik;
                if (siparis.getMasaNo() != null) {
                    baslik = "🪑 Masa " + siparis.getMasaNo();
                } else {
                    baslik = "📱 Online Sipariş #" + siparis.getSiparisId();
                }
                
                // Durum rengi belirle
                Color durumRengi;
                Color arkaPlanRenk;
                switch (siparis.getDurum()) {
                    case "Hazırlanıyor":
                        durumRengi = new Color(255, 193, 7);
                        arkaPlanRenk = new Color(255, 243, 204);
                        break;
                    case "Servis Edildi":
                        durumRengi = new Color(40, 167, 69);
                        arkaPlanRenk = new Color(220, 248, 198);
                        break;
                    case "Ödeme Bekleniyor":
                        durumRengi = new Color(220, 53, 69);
                        arkaPlanRenk = new Color(255, 220, 220);
                        break;
                    default:
                        durumRengi = new Color(108, 117, 125);
                        arkaPlanRenk = new Color(248, 249, 250);
                }
                
                JPanel kart = createModernSiparisKarti(
                    baslik,
                    siparis.getUrunler() != null && !siparis.getUrunler().isEmpty() ? 
                        siparis.getUrunler() : "Sipariş kaydediliyor...",
                    siparis.getDurum(),
                    String.format("%.2f $", siparis.getToplamTutar()),
                    durumRengi,
                    arkaPlanRenk,
                    siparis.getSiparisId()
                );
                
                siparislerPaneli.add(kart);
                if (i < aktifSiparisler.size() - 1) {
                    siparislerPaneli.add(Box.createVerticalStrut(15));
                }
            }
        }
        
        siparislerPaneli.add(Box.createVerticalGlue());
        
        // Paneli yeniden çiz
        siparislerPaneli.revalidate();
        siparislerPaneli.repaint();
    }
    
    /**
     * Modern sipariş kartı bileşenini oluşturan yardımcı metod
     */
    private JPanel createModernSiparisKarti(String baslik, String urunler, String durum, 
                                            String tutar, Color durumRengi, Color arkaPlanRenk, int siparisId) {
        JPanel kart = new JPanel(new BorderLayout(20, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient arka plan
                GradientPaint gradient = new GradientPaint(
                    0, 0, Color.WHITE,
                    getWidth(), getHeight(), arkaPlanRenk
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Sol kenar vurgusu
                g2.setColor(durumRengi);
                g2.fillRoundRect(0, 0, 5, getHeight(), 12, 12);
                
                g2.dispose();
            }
        };
        
        kart.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 1, 1, 1, new Color(222, 226, 230)),
            new EmptyBorder(20, 25, 20, 25)
        ));
        kart.setBackground(Color.WHITE);
        kart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        kart.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Sol taraf: Başlık ve ürünler
        JPanel solPanel = new JPanel();
        solPanel.setLayout(new BoxLayout(solPanel, BoxLayout.Y_AXIS));
        solPanel.setOpaque(false);
        
        // Başlık (Masa/Online Sipariş)
        JLabel lblBaslik = new JLabel(baslik);
        lblBaslik.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblBaslik.setForeground(new Color(33, 37, 41));
        lblBaslik.setAlignmentX(Component.LEFT_ALIGNMENT);
        solPanel.add(lblBaslik);
        
        solPanel.add(Box.createVerticalStrut(8));
        
        // Ürünler (maksimum 80 karakter)
        String urunlerKisa = urunler.length() > 80 ? urunler.substring(0, 77) + "..." : urunler;
        JLabel lblUrunler = new JLabel(urunlerKisa);
        lblUrunler.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblUrunler.setForeground(new Color(108, 117, 125));
        lblUrunler.setAlignmentX(Component.LEFT_ALIGNMENT);
        solPanel.add(lblUrunler);
        
        kart.add(solPanel, BorderLayout.WEST);
        
        // Sağ taraf: Durum butonu ve tutar
        JPanel sagPanel = new JPanel();
        sagPanel.setLayout(new BoxLayout(sagPanel, BoxLayout.Y_AXIS));
        sagPanel.setOpaque(false);
        
        // Durum butonu - tıklanınca durum değişir
        JButton btnDurum = createModernButton(durum, durumRengi, Color.WHITE);
        btnDurum.setPreferredSize(new Dimension(160, 35));
        btnDurum.setMaximumSize(new Dimension(160, 35));
        btnDurum.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDurum.setAlignmentX(Component.RIGHT_ALIGNMENT);
        btnDurum.addActionListener(e -> siparisDurumunuDegistir(siparisId, durum));
        sagPanel.add(btnDurum);
        
        sagPanel.add(Box.createVerticalStrut(8));
        
        // Tutar
        JLabel lblTutar = new JLabel(tutar);
        lblTutar.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTutar.setForeground(new Color(33, 37, 41));
        lblTutar.setAlignmentX(Component.RIGHT_ALIGNMENT);
        sagPanel.add(lblTutar);
        
        // Alt butonlar: Tamamla, İptal ve Tavsiye
        JPanel butonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        butonPanel.setOpaque(false);
        butonPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        JButton btnTamamla = createModernButton("✅ Tamamla", new Color(40, 167, 69), Color.WHITE);
        btnTamamla.setPreferredSize(new Dimension(140, 35));
        btnTamamla.addActionListener(e -> siparisiTamamla(siparisId));
        butonPanel.add(btnTamamla);
        
        JButton btnIptal = createModernButton("❌ İptal", new Color(220, 53, 69), Color.WHITE);
        btnIptal.setPreferredSize(new Dimension(120, 35));
        btnIptal.addActionListener(e -> siparisiIptalEt(siparisId));
        butonPanel.add(btnIptal);
        
        // Tavsiye Al butonu
        JButton btnTavsiye = createModernButton("💡 Tavsiye", new Color(0, 123, 255), Color.WHITE);
        btnTavsiye.setPreferredSize(new Dimension(130, 35));
        btnTavsiye.addActionListener(e -> siparisIcinTavsiyeAl(siparisId, urunler));
        butonPanel.add(btnTavsiye);
        
        sagPanel.add(butonPanel);
        
        kart.add(sagPanel, BorderLayout.EAST);
        
        return kart;
    }
    
    private JButton createModernButton(String text, Color backgroundColor, Color textColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                Color drawColor;
                if (getModel().isPressed()) {
                    drawColor = backgroundColor.darker();
                } else if (getModel().isRollover()) {
                    drawColor = backgroundColor.brighter();
                } else {
                    drawColor = backgroundColor;
                }
                
                g2.setColor(drawColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(textColor);
        button.setBackground(backgroundColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Minimum genişlik ve padding için otomatik boyutlandırma
        Dimension preferredSize = button.getPreferredSize();
        int minWidth = Math.max(preferredSize.width + 40, 130); // Minimum 130px + 40px padding
        button.setPreferredSize(new Dimension(minWidth, 35));
        
        return button;
    }
    
    private void siparisDurumunuDegistir(int siparisId, String mevcutDurum) {
        String sonrakiDurum = null;
        
        // Mevcut duruma göre sonraki durumu belirle
        // Akış: Hazırlanıyor → Servis Edildi (sonrası hesap kapatma)
        switch (mevcutDurum) {
            case "Hazırlanıyor":
                sonrakiDurum = "Servis Edildi";
                break;
            case "Servis Edildi":
                // Servis Edildi durumundan sonra durum değiştirme yok, hesap kapatılacak
                JOptionPane.showMessageDialog(this,
                    "Bu sipariş servis edildi. Hesap kapatmak için Duygu Analizi sekmesinden masaya ait 'Hesap Kapat' butonunu kullanın.",
                    "Bilgi",
                    JOptionPane.INFORMATION_MESSAGE);
                return; // Durum değiştirme, kullanıcıyı bilgilendir
            default:
                sonrakiDurum = "Servis Edildi";
                break;
        }
        
        try {
            veritabaniServisi.siparisDurumGuncelle(siparisId, sonrakiDurum);
            siparisleriYenile();
            
            // Duygu Analizi ve Genel Bakış panellerini güncelle
            SwingUtilities.invokeLater(() -> {
                Container parent = getParent();
                while (parent != null && !(parent instanceof JTabbedPane)) {
                    parent = parent.getParent();
                }
                if (parent instanceof JTabbedPane) {
                    JTabbedPane tabbedPane = (JTabbedPane) parent;
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        String tabTitle = tabbedPane.getTitleAt(i);
                        if (tabTitle.equals("Duygu Analizi")) {
                            JPanel duyguPanel = (JPanel) tabbedPane.getComponentAt(i);
                            if (duyguPanel instanceof PanelDuyguAnalizi) {
                                tabbedPane.removeTabAt(i);
                                PanelDuyguAnalizi yeniPanel = new PanelDuyguAnalizi();
                                tabbedPane.insertTab("Duygu Analizi", null, yeniPanel, null, i);
                            }
                        } else if (tabTitle.equals("Genel Bakış")) {
                            JPanel genelPanel = (JPanel) tabbedPane.getComponentAt(i);
                            if (genelPanel instanceof PanelGenelBakis) {
                                ((PanelGenelBakis) genelPanel).yenile();
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Sipariş durumu güncellenirken hata oluştu:\n" + e.getMessage(),
                "Hata",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void siparisiTamamla(int siparisId) {
        int onay = JOptionPane.showConfirmDialog(this,
            "Bu siparişi tamamlamak istediğinize emin misiniz?\n\nSipariş tamamlandıktan sonra aktif listeden çıkacak ve masa boşaltılacaktır.",
            "Siparişi Tamamla",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (onay == JOptionPane.YES_OPTION) {
            try {
                veritabaniServisi.siparisiTamamla(siparisId);
                JOptionPane.showMessageDialog(this,
                    "Sipariş başarıyla tamamlandı!",
                    "Başarılı",
                    JOptionPane.INFORMATION_MESSAGE);
                siparisleriYenile();
                
                // Duygu Analizi panelini güncelle
                SwingUtilities.invokeLater(() -> {
                    Container parent = getParent();
                    while (parent != null && !(parent instanceof JTabbedPane)) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof JTabbedPane) {
                        JTabbedPane tabbedPane = (JTabbedPane) parent;
                        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                            if (tabbedPane.getTitleAt(i).equals("Duygu Analizi")) {
                                JPanel duyguPanel = (JPanel) tabbedPane.getComponentAt(i);
                                if (duyguPanel instanceof PanelDuyguAnalizi) {
                                    tabbedPane.removeTabAt(i);
                                    PanelDuyguAnalizi yeniPanel = new PanelDuyguAnalizi();
                                    tabbedPane.insertTab("Duygu Analizi", null, yeniPanel, null, i);
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Sipariş tamamlanırken hata oluştu:\n" + e.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void siparisiIptalEt(int siparisId) {
        int onay = JOptionPane.showConfirmDialog(this,
            "Bu siparişi iptal etmek istediğinize emin misiniz?",
            "Siparişi İptal Et",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (onay == JOptionPane.YES_OPTION) {
            try {
                veritabaniServisi.siparisiIptalEt(siparisId);
                JOptionPane.showMessageDialog(this,
                    "Sipariş başarıyla iptal edildi!",
                    "Başarılı",
                    JOptionPane.INFORMATION_MESSAGE);
                siparisleriYenile();
                
                // Duygu Analizi ve Genel Bakış panellerini güncelle
                SwingUtilities.invokeLater(() -> {
                    Container parent = getParent();
                    while (parent != null && !(parent instanceof JTabbedPane)) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof JTabbedPane) {
                        JTabbedPane tabbedPane = (JTabbedPane) parent;
                        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                            String tabTitle = tabbedPane.getTitleAt(i);
                            if (tabTitle.equals("Duygu Analizi")) {
                                JPanel duyguPanel = (JPanel) tabbedPane.getComponentAt(i);
                                if (duyguPanel instanceof PanelDuyguAnalizi) {
                                    tabbedPane.removeTabAt(i);
                                    PanelDuyguAnalizi yeniPanel = new PanelDuyguAnalizi();
                                    tabbedPane.insertTab("Duygu Analizi", null, yeniPanel, null, i);
                                }
                            } else if (tabTitle.equals("Genel Bakış")) {
                                JPanel genelPanel = (JPanel) tabbedPane.getComponentAt(i);
                                if (genelPanel instanceof PanelGenelBakis) {
                                    ((PanelGenelBakis) genelPanel).yenile();
                                }
                            }
                        }
                    }
                });
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Sipariş iptal edilirken hata oluştu:\n" + e.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void siparisIcinTavsiyeAl(int siparisId, String urunlerString) {
        // Öneri sistemi hazır mı kontrol et
        if (oneriSistemi == null) {
            JOptionPane.showMessageDialog(this,
                "Öneri sistemi henüz hazır değil. Lütfen birkaç saniye bekleyip tekrar deneyin.",
                "Bilgi",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        List<String> sepetUrunleri = parseUrunler(urunlerString);
        
        if (sepetUrunleri.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Bu siparişte ürün bulunamadı.\n\nÜrün bilgisi: " + (urunlerString != null ? urunlerString : "null"),
                "Uyarı",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            RestoranOneriSistemi.TavsiyeSonucu sonuc = oneriSistemi.tavsiyeAl(sepetUrunleri);
            
            // Sonucu göster
            String mesaj;
            String baslik;
            int mesajTipi;
            
            if (sonuc != null && sonuc.onerilenUrun != null) {
                mesaj = String.format(
                    "<html><div style='text-align: center; padding: 10px;'>" +
                    "<h2 style='color: #28a745; margin-bottom: 15px;'>💡 Tavsiye Edilen Ürün</h2>" +
                    "<p style='font-size: 18px; font-weight: bold; color: #212529; margin: 10px 0;'>%s</p>" +
                    "<p style='font-size: 14px; color: #6c757d; margin-top: 15px;'>Güven Oranı: <span style='color: #007bff; font-weight: bold;'>%.1f%%</span></p>" +
                    "<p style='font-size: 12px; color: #adb5bd; margin-top: 10px;'>Sipariş #%d'deki ürünlere göre önerilmiştir.</p>" +
                    "</div></html>",
                    sonuc.onerilenUrun, sonuc.guven * 100, siparisId
                );
                baslik = "Ürün Tavsiyesi";
                mesajTipi = JOptionPane.INFORMATION_MESSAGE;
            } else {
                mesaj = "<html><div style='text-align: center; padding: 10px;'>" +
                        "<h3 style='color: #6c757d;'>Tavsiye Bulunamadı</h3>" +
                        "<p style='margin-top: 10px;'>Bu ürün kombinasyonu için yeterli veri bulunmamaktadır.</p>" +
                        "<p style='font-size: 12px; color: #adb5bd; margin-top: 10px;'>Farklı ürünler deneyebilirsiniz.</p>" +
                        "</div></html>";
                baslik = "Bilgi";
                mesajTipi = JOptionPane.INFORMATION_MESSAGE;
            }
            
            JOptionPane.showMessageDialog(this, mesaj, baslik, mesajTipi);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Tavsiye alınırken bir hata oluştu:\n" + e.getMessage(),
                "Hata",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Sipariş ürün metnini sepet listesine çevirir.
     * Örnek: "Hamburger (2), French Fries (1)" → ["Hamburger", "French Fries"]
     */
    private List<String> parseUrunler(String urunlerString) {
        List<String> urunler = new ArrayList<>();
        
        if (urunlerString == null || urunlerString.trim().isEmpty()) {
            return urunler;
        }
        
        // Virgülle ayır (regex ile daha güvenli)
        String[] parcalar = urunlerString.split(",\\s*");
        
        for (String parca : parcalar) {
            parca = parca.trim();
            if (parca.isEmpty()) continue;
            
            // "(2)" veya " (2)" gibi parantez içindeki sayıyı kaldır
            int parantezIndex = parca.indexOf("(");
            if (parantezIndex > 0) {
                parca = parca.substring(0, parantezIndex).trim();
            }
            
            // Başta/sonda boşlukları temizle
            parca = parca.trim();
            
            if (!parca.isEmpty()) {
                urunler.add(parca);
            }
        }
        
        return urunler;
    }
}

