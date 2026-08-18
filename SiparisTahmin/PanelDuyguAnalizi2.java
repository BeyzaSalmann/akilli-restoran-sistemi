package com.restoran.view;

import com.restoran.service.VeritabaniServisi;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;

/**
 * Gerçek Zamanlı Duygu Analizi Paneli
 * Müşteri duygu analizi ve masa durumu izleme ekranı
 */
public class PanelDuyguAnalizi extends JPanel {
    
    public PanelDuyguAnalizi() {
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        setBackground(new Color(248, 249, 250));
        
        // Ana başlık
        JLabel lblAnaBaslik = new JLabel("📹 Gerçek Zamanlı Duygu Analizi (Kamera)");
        lblAnaBaslik.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblAnaBaslik.setForeground(new Color(33, 37, 41));
        lblAnaBaslik.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        add(lblAnaBaslik, BorderLayout.NORTH);
        
        // Orta panel - Canlı akış ve anlık duygu
        JPanel ortaPanel = new JPanel(new BorderLayout(15, 15));
        ortaPanel.setOpaque(false);
        
        // Canlı Akış Alanı
        JPanel videoPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient arka plan
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(240, 242, 245),
                    getWidth(), getHeight(), new Color(230, 232, 235)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                g2.dispose();
            }
        };
        videoPanel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(2, 2, 2, 2, new Color(0, 123, 255)),
            new EmptyBorder(20, 20, 20, 20)
        ));
        videoPanel.setPreferredSize(new Dimension(650, 420));
        
        // Video alanında ortalanmış label
        JPanel videoContent = new JPanel(new GridBagLayout());
        videoContent.setOpaque(false);
        
        JLabel lblVideoPlaceholder = new JLabel("<html><div style='text-align: center;'>" +
            "🎥<br><br>Video Görüntüsü<br>Burada Görünecek</div></html>", JLabel.CENTER);
        lblVideoPlaceholder.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblVideoPlaceholder.setForeground(new Color(108, 117, 125));
        videoContent.add(lblVideoPlaceholder);
        
        videoPanel.add(videoContent, BorderLayout.CENTER);
        
        ortaPanel.add(videoPanel, BorderLayout.CENTER);
        
        // Anlık Duygu Etiketi
        JPanel duyguPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        duyguPanel.setOpaque(false);
        duyguPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
        
        JLabel lblAnlikLabel = new JLabel("ANLIK DUYGU:");
        lblAnlikLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblAnlikLabel.setForeground(new Color(108, 117, 125));
        duyguPanel.add(lblAnlikLabel);
        
        JLabel lblAnlikDuygu = new JLabel("😊 Memnuniyet");
        lblAnlikDuygu.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblAnlikDuygu.setForeground(new Color(40, 167, 69));
        duyguPanel.add(lblAnlikDuygu);
        
        ortaPanel.add(duyguPanel, BorderLayout.SOUTH);
        
        add(ortaPanel, BorderLayout.CENTER);
        
        // Alt panel - Masa durumu
        JPanel altPanel = new JPanel(new BorderLayout(15, 15));
        altPanel.setOpaque(false);
        altPanel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));
        
        // Masa Durumu Başlığı ve Butonlar
        JPanel baslikButonPanel = new JPanel(new BorderLayout());
        baslikButonPanel.setOpaque(false);
        baslikButonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel lblMasaDurumuBaslik = new JLabel("🪑 Masa Durumu Özeti");
        lblMasaDurumuBaslik.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblMasaDurumuBaslik.setForeground(new Color(33, 37, 41));
        baslikButonPanel.add(lblMasaDurumuBaslik, BorderLayout.WEST);
        
        // Masa Sil butonu
        JButton btnMasaSil = createModernButton("🗑️ Masa Sil", new Color(220, 53, 69), Color.WHITE);
        btnMasaSil.setPreferredSize(new Dimension(150, 40));
        btnMasaSil.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnMasaSil.addActionListener(e -> masaSilDialogAc());
        baslikButonPanel.add(btnMasaSil, BorderLayout.EAST);
        
        altPanel.add(baslikButonPanel, BorderLayout.NORTH);
        
        // Masa Kartları Alanı - Dinamik Grid (her satırda 3 masa)
        VeritabaniServisi veritabaniServisi = new VeritabaniServisi();
        List<Integer> masalar = veritabaniServisi.getMasaListesi();
        
        // Satır sayısını hesapla (her satırda 3 masa + 1 "Yeni Masa" butonu)
        int toplamKart = masalar.size() + 1; // Masalar + "Yeni Masa" butonu
        int satirSayisi = (toplamKart + 2) / 3; // Her satırda 3 kart (yuvarlama)
        if (satirSayisi < 1) satirSayisi = 1;
        
        JPanel masalarPaneli = new JPanel(new GridLayout(satirSayisi, 3, 15, 15));
        masalarPaneli.setOpaque(false);
        
        // Tüm masaları göster
        for (int masaNo : masalar) {
            
                       // Masa durumunu veritabanından al
           String masaDurum = veritabaniServisi.getMasaDurumu(masaNo);
           
           // Eğer masada "Servis Edildi" durumunda sipariş varsa, durumu "Servis Edildi" yap
           if (masaServisEdildiDurumundaMi(masaNo)) {
               masaDurum = "Servis Edildi";
           }
           
           // Duruma göre renk ve metin belirle
           Color durumRengi;
           Color arkaPlanRenk;
           String durumMetni;
           
           switch (masaDurum != null ? masaDurum : "Boş") {
               case "Dolu":
                   durumRengi = new Color(220, 53, 69); // Kırmızı
                   arkaPlanRenk = new Color(255, 220, 220);
                   durumMetni = "🟢 Dolu";
                   break;
               case "Hazırlanıyor":
                   durumRengi = new Color(255, 193, 7); // Sarı
                   arkaPlanRenk = new Color(255, 243, 204);
                   durumMetni = "⏳ Hazırlanıyor";
                   break;
               case "Servis Edildi":
                   durumRengi = new Color(0, 123, 255); // Mavi
                   arkaPlanRenk = new Color(209, 232, 255);
                   durumMetni = "✅ Servis Edildi";
                   break;
               default: // Boş
                   durumRengi = new Color(40, 167, 69); // Yeşil
                   arkaPlanRenk = new Color(220, 248, 198);
                   durumMetni = "⚪ Boş";
                   break;
           }
            
            JPanel masaKarti = createModernMasaKarti("Masa " + masaNo, durumMetni, 
                durumRengi, arkaPlanRenk, masaNo);
            masalarPaneli.add(masaKarti);
        }
        
        // Son kart: "+ Yeni Masa" butonu
        JPanel yeniMasaKarti = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 249, 250));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        yeniMasaKarti.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(222, 226, 230), 2, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        JButton btnYeniMasa = createModernButton("➕ Yeni Masa", 
            new Color(0, 123, 255), Color.WHITE);
        btnYeniMasa.setPreferredSize(new Dimension(180, 50));
        btnYeniMasa.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnYeniMasa.addActionListener(e -> {
            String masaNoStr = JOptionPane.showInputDialog(
                this,
                "Yeni masa numarasını girin (1-30):",
                "Yeni Masa Ekle",
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (masaNoStr != null && !masaNoStr.trim().isEmpty()) {
                try {
                    int masaNo = Integer.parseInt(masaNoStr.trim());
                    if (masaNo >= 1 && masaNo <= 30) {
                        try {
                            VeritabaniServisi dbServisi = new VeritabaniServisi();
                            dbServisi.masaEkle(masaNo);
                            JOptionPane.showMessageDialog(
                                this,
                                "Masa " + masaNo + " başarıyla eklendi!",
                                "Başarılı",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                            // Paneli yenile
                            SwingUtilities.invokeLater(() -> {
                                removeAll();
                                initComponents();
                                revalidate();
                                repaint();
                            });
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(
                                this,
                                "Masa eklenirken hata oluştu: " + ex.getMessage(),
                                "Hata",
                                JOptionPane.ERROR_MESSAGE
                            );
                        }
                    } else {
                        JOptionPane.showMessageDialog(
                            this,
                            "Masa numarası 1-30 arasında olmalıdır!",
                            "Hata",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Geçerli bir masa numarası giriniz!",
                        "Hata",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });
        yeniMasaKarti.add(btnYeniMasa, BorderLayout.CENTER);
        
        masalarPaneli.add(yeniMasaKarti);
        
        // ScrollPane ekle - masalar kaydırılabilir olacak
        JScrollPane scrollPane = new JScrollPane(masalarPaneli);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Daha yumuşak kaydırma
        
        // Scroll bar stilini güncelle
        scrollPane.getVerticalScrollBar().setBackground(new Color(240, 242, 245));
        scrollPane.getVerticalScrollBar().setForeground(new Color(108, 117, 125));
        
        altPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(altPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Modern masa kartı bileşenini oluşturan yardımcı metod
     */
    private JPanel createModernMasaKarti(String masaAdi, String durum, 
                                         Color durumRengi, Color arkaPlanRenk, int masaNo) {
        JPanel kart = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient arka plan
                GradientPaint gradient = new GradientPaint(
                    0, 0, arkaPlanRenk,
                    getWidth(), getHeight(), Color.WHITE
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                // Sol kenar vurgusu
                g2.setColor(durumRengi);
                g2.fillRoundRect(0, 0, 5, getHeight(), 12, 12);
                
                g2.dispose();
            }
        };
        
        kart.setLayout(new BoxLayout(kart, BoxLayout.Y_AXIS));
        kart.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 1, 1, 1, new Color(222, 226, 230)),
            new EmptyBorder(20, 20, 20, 20)
        ));
        kart.setBackground(Color.WHITE);
        kart.setPreferredSize(new Dimension(220, 140));
        
        // Masa Adı
        JLabel lblMasaAdi = new JLabel("🪑 " + masaAdi);
        lblMasaAdi.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblMasaAdi.setForeground(new Color(33, 37, 41));
        lblMasaAdi.setAlignmentX(Component.LEFT_ALIGNMENT);
        kart.add(lblMasaAdi);
        
        kart.add(Box.createVerticalStrut(15));
        
        // Durum
        JPanel durumPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        durumPanel.setOpaque(false);
        
        JLabel lblDurum = new JLabel(durum);
        lblDurum.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblDurum.setForeground(durumRengi);
        durumPanel.add(lblDurum);
        
        kart.add(durumPanel);
        
        // Butonlar paneli
        JPanel masaButonlariPanel = new JPanel();
        masaButonlariPanel.setLayout(new BoxLayout(masaButonlariPanel, BoxLayout.Y_AXIS));
        masaButonlariPanel.setOpaque(false);
        masaButonlariPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Eğer masa doluysa veya "Servis Edildi" durumundaki siparişler varsa "Hesap Kapat" butonu ekle
        if (durum.contains("Dolu") || masaServisEdildiDurumundaMi(masaNo)) {
            JButton btnHesapKapat = new JButton("💳 Hesap Kapat");
            btnHesapKapat.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnHesapKapat.setForeground(Color.WHITE);
            btnHesapKapat.setBackground(new Color(0, 123, 255));
            btnHesapKapat.setFocusPainted(false);
            btnHesapKapat.setBorderPainted(false);
            btnHesapKapat.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnHesapKapat.setAlignmentX(Component.LEFT_ALIGNMENT);
            btnHesapKapat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            btnHesapKapat.addActionListener(e -> masaHesabiKapat(masaNo));
            masaButonlariPanel.add(btnHesapKapat);
        }
        
        // Masa silme butonu (her zaman görünür)
        if (masaNo > 0) {
            if (masaButonlariPanel.getComponentCount() > 0) {
                masaButonlariPanel.add(Box.createVerticalStrut(5));
            }
            JButton btnMasaSil = new JButton("🗑️ Sil");
            btnMasaSil.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnMasaSil.setForeground(new Color(220, 53, 69));
            btnMasaSil.setBackground(Color.WHITE);
            btnMasaSil.setBorder(BorderFactory.createLineBorder(new Color(220, 53, 69), 1));
            btnMasaSil.setFocusPainted(false);
            btnMasaSil.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnMasaSil.setAlignmentX(Component.LEFT_ALIGNMENT);
            btnMasaSil.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            btnMasaSil.addActionListener(e -> masaSil(masaNo));
            masaButonlariPanel.add(btnMasaSil);
        }
        
        if (masaButonlariPanel.getComponentCount() > 0) {
            kart.add(Box.createVerticalStrut(10));
            kart.add(masaButonlariPanel);
        }
        
        kart.add(Box.createVerticalGlue());
        
        return kart;
    }
    
    private JButton createModernButton(String text, Color backgroundColor, Color textColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(backgroundColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(backgroundColor.brighter());
                } else {
                    g2.setColor(backgroundColor);
                }
                
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
        
        return button;
    }
    
    private boolean masaServisEdildiDurumundaMi(int masaNo) {
        VeritabaniServisi dbServisi = new VeritabaniServisi();
        String sql = "SELECT COUNT(*) as sayi FROM Siparis " +
                     "WHERE masa_no = ? AND durum = 'Servis Edildi'";
        
        try (java.sql.Connection conn = com.restoran.util.VeritabaniBaglanti.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, masaNo);
            java.sql.ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("sayi") > 0;
            }
        } catch (Exception e) {
            System.err.println("✗ Masa servis durumu kontrol edilirken hata: " + e.getMessage());
        }
        
        return false;
    }
    
    private void masaHesabiKapat(int masaNo) {
        VeritabaniServisi dbServisi = new VeritabaniServisi();
        
        // Masanın toplam hesabını hesapla (aktif siparişleri kontrol et)
        double toplamTutar = 0.0;
        try {
            toplamTutar = dbServisi.masaHesabiKapat(masaNo);
            
            if (toplamTutar > 0) {
                JOptionPane.showMessageDialog(this,
                    String.format("Masa %d hesabı başarıyla kapatıldı!\n\nToplam Tutar: %.2f ₺\n\nMasa boşaltıldı.", 
                        masaNo, toplamTutar),
                    "Hesap Kapatıldı",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    String.format("Masa %d için aktif sipariş bulunamadı.", masaNo),
                    "Bilgi",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
                            // Paneli yenile
                SwingUtilities.invokeLater(() -> {
                    removeAll();
                    initComponents();
                    revalidate();
                    repaint();
                });
                
                // Diğer panelleri güncelle
                SwingUtilities.invokeLater(() -> {
                    Container parent = getParent();
                    while (parent != null && !(parent instanceof JTabbedPane)) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof JTabbedPane) {
                        JTabbedPane tabbedPane = (JTabbedPane) parent;
                        
                        // Aktif Siparişler panelini güncelle
                        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                            String tabTitle = tabbedPane.getTitleAt(i);
                            if (tabTitle.equals("Aktif Siparişler")) {
                                JPanel aktifPanel = (JPanel) tabbedPane.getComponentAt(i);
                                if (aktifPanel instanceof PanelAktifSiparisler) {
                                    ((PanelAktifSiparisler) aktifPanel).siparisleriYenile();
                                }
                            }
                            // Genel Bakış panelini güncelle (masa sayısı için)
                            else if (tabTitle.equals("Genel Bakış")) {
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
                "Masa hesabı kapatılırken hata oluştu:\n" + e.getMessage(),
                "Hata",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void masaSilDialogAc() {
        VeritabaniServisi dbServisi = new VeritabaniServisi();
        List<Integer> masalar = dbServisi.getMasaListesi();
        
        if (masalar.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Silinecek masa bulunamadı.",
                "Bilgi",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Masa listesi string oluştur
        String[] masaListesi = masalar.stream()
            .map(m -> "Masa " + m)
            .toArray(String[]::new);
        
        String secilenMasaStr = (String) JOptionPane.showInputDialog(
            this,
            "Silmek istediğiniz masayı seçin:",
            "Masa Sil",
            JOptionPane.QUESTION_MESSAGE,
            null,
            masaListesi,
            masaListesi[0]
        );
        
        if (secilenMasaStr != null) {
            try {
                int masaNo = Integer.parseInt(secilenMasaStr.replace("Masa ", ""));
                masaSil(masaNo);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Geçersiz masa numarası.",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void masaSil(int masaNo) {
        VeritabaniServisi dbServisi = new VeritabaniServisi();
        
        // Onay al
        int onay = JOptionPane.showConfirmDialog(this,
            String.format("Masa %d'yı silmek istediğinize emin misiniz?\n\nNot: Eğer masada aktif sipariş varsa silme işlemi yapılamayacaktır.", masaNo),
            "Masa Sil",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (onay == JOptionPane.YES_OPTION) {
            try {
                dbServisi.masaSil(masaNo);
                JOptionPane.showMessageDialog(this,
                    String.format("Masa %d başarıyla silindi!", masaNo),
                    "Başarılı",
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Paneli yenile
                SwingUtilities.invokeLater(() -> {
                    removeAll();
                    initComponents();
                    revalidate();
                    repaint();
                });
                
                // Genel Bakış panelini güncelle (masa sayısı için)
                SwingUtilities.invokeLater(() -> {
                    Container parent = getParent();
                    while (parent != null && !(parent instanceof JTabbedPane)) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof JTabbedPane) {
                        JTabbedPane tabbedPane = (JTabbedPane) parent;
                        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                            if (tabbedPane.getTitleAt(i).equals("Genel Bakış")) {
                                JPanel genelPanel = (JPanel) tabbedPane.getComponentAt(i);
                                if (genelPanel instanceof PanelGenelBakis) {
                                    ((PanelGenelBakis) genelPanel).yenile();
                                }
                            }
                        }
                    }
                });
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this,
                    "Masa silinirken hata oluştu:\n" + e.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Masa silinirken beklenmeyen bir hata oluştu:\n" + e.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
}

