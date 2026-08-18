package com.restoran.view;

import com.restoran.service.VeritabaniServisi;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class PanelGenelBakis extends JPanel {
    
    private JTabbedPane tabbedPane;
    private VeritabaniServisi veritabaniServisi;
    
    public PanelGenelBakis() {
        veritabaniServisi = new VeritabaniServisi();
        initComponents();
    }
    
    public PanelGenelBakis(JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
        veritabaniServisi = new VeritabaniServisi();
        initComponents();
    }
    
    public void yenile() {
        // Paneli yeniden oluştur
        removeAll();
        initComponents();
        revalidate();
        repaint();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        setBackground(new Color(240, 242, 245));
        
        // Üst panel - Büyük Başlık ve Aksiyon Butonları
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));
        
        // Sol taraf - Büyük Başlık
        JPanel baslikPanel = new JPanel(new BorderLayout());
        baslikPanel.setOpaque(false);
        
        // İkon ve başlık paneli
        JPanel baslikIkonPanel = new JPanel(new BorderLayout(15, 0));
        baslikIkonPanel.setOpaque(false);
        
        // Restoran ikonu (modern ve profesyonel)
        JLabel iconLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int size = 64;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                
                // Daire şeklinde gradient arka plan (sofistike restoran teması)
                GradientPaint backgroundGradient = new GradientPaint(
                    x, y, new Color(139, 69, 19),  // Koyu kahverengi (ahşap tonu)
                    x + size, y + size, new Color(101, 67, 33)  // Daha koyu kahverengi
                );
                g2.setPaint(backgroundGradient);
                g2.fillOval(x, y, size, size);
                
                // İnce kenarlık (altın tonu)
                g2.setColor(new Color(184, 134, 11));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval(x + 1, y + 1, size - 2, size - 2);
                
                // RESTORAN BİNASI - Basit ve temiz
                int binaGenislik = size * 3 / 4;
                int binaYukseklik = size * 4 / 5;
                int binaX = x + (size - binaGenislik) / 2;
                int binaY = y + (size - binaYukseklik) / 2 + size / 10;
                
                // Bina gövdesi (beyaz/krem)
                g2.setColor(new Color(250, 248, 240));
                g2.fillRect(binaX, binaY, binaGenislik, binaYukseklik);
                
                // Bina kenarlığı
                g2.setColor(new Color(200, 180, 160));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRect(binaX, binaY, binaGenislik, binaYukseklik);
                
                // ÇATI (kırmızı/kahverengi)
                int[] catıX = {binaX - size/16, binaX + binaGenislik/2, binaX + binaGenislik + size/16};
                int[] catıY = {binaY, binaY - size/8, binaY};
                g2.setColor(new Color(139, 69, 19)); // Kahverengi çatı
                g2.fillPolygon(catıX, catıY, 3);
                g2.setColor(new Color(101, 67, 33));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawPolygon(catıX, catıY, 3);
                
                // PENCERELER (sarı ışık)
                int pencereBoyut = size / 8;
                int pencere1X = binaX + binaGenislik / 4 - pencereBoyut / 2;
                int pencere2X = binaX + binaGenislik * 3 / 4 - pencereBoyut / 2;
                int pencereY = binaY + binaYukseklik / 4;
                
                // Pencere 1
                g2.setColor(new Color(255, 248, 200));
                g2.fillRect(pencere1X, pencereY, pencereBoyut, pencereBoyut);
                g2.setColor(new Color(200, 150, 50));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(pencere1X, pencereY, pencereBoyut, pencereBoyut);
                // Pencere çerçevesi (haç)
                g2.drawLine(pencere1X + pencereBoyut/2, pencereY, pencere1X + pencereBoyut/2, pencereY + pencereBoyut);
                g2.drawLine(pencere1X, pencereY + pencereBoyut/2, pencere1X + pencereBoyut, pencereY + pencereBoyut/2);
                
                // Pencere 2
                g2.setColor(new Color(255, 248, 200));
                g2.fillRect(pencere2X, pencereY, pencereBoyut, pencereBoyut);
                g2.setColor(new Color(200, 150, 50));
                g2.drawRect(pencere2X, pencereY, pencereBoyut, pencereBoyut);
                // Pencere çerçevesi (haç)
                g2.drawLine(pencere2X + pencereBoyut/2, pencereY, pencere2X + pencereBoyut/2, pencereY + pencereBoyut);
                g2.drawLine(pencere2X, pencereY + pencereBoyut/2, pencere2X + pencereBoyut, pencereY + pencereBoyut/2);
                
                // KAPI (kahverengi)
                int kapiGenislik = size / 6;
                int kapiYukseklik = size / 4;
                int kapiX = binaX + binaGenislik / 2 - kapiGenislik / 2;
                int kapiY = binaY + binaYukseklik - kapiYukseklik - size/20;
                
                g2.setColor(new Color(101, 67, 33));
                g2.fillRect(kapiX, kapiY, kapiGenislik, kapiYukseklik);
                g2.setColor(new Color(60, 40, 20));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRect(kapiX, kapiY, kapiGenislik, kapiYukseklik);
                
                // Kapı kolu (altın)
                g2.setColor(new Color(184, 134, 11));
                g2.fillOval(kapiX + kapiGenislik - size/20, kapiY + kapiYukseklik/2, size/16, size/16);
                
                // Bina zemini (gri)
                g2.setColor(new Color(180, 180, 180));
                g2.fillRect(binaX - size/32, binaY + binaYukseklik, binaGenislik + size/16, size/16);
                
                g2.dispose();
            }
        };
        iconLabel.setPreferredSize(new Dimension(80, 80));
        baslikIkonPanel.add(iconLabel, BorderLayout.WEST);
        
        // Başlık metni
        JLabel lblBaslik = new JLabel("<html><div style='line-height: 1.2;'>" +
            "<span style='font-size: 36px; font-weight: bold; color: #212529;'>Akıllı Restoran</span><br>" +
            "<span style='font-size: 20px; color: #6c757d; font-weight: normal;'>Genel Bakış Paneli</span>" +
            "</div></html>");
        baslikIkonPanel.add(lblBaslik, BorderLayout.CENTER);
        
        baslikPanel.add(baslikIkonPanel, BorderLayout.WEST);
        
        // Sağ taraf - Aksiyon Butonları
        JPanel aksiyonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        aksiyonPanel.setOpaque(false);
        
        JButton btnHizliSiparis = createModernButton("⚡ HIZLI SİPARİŞ", 
            new Color(40, 167, 69), Color.WHITE, 16);
        btnHizliSiparis.setPreferredSize(new Dimension(220, 50));
        btnHizliSiparis.addActionListener(e -> {
            if (tabbedPane != null) {
                tabbedPane.setSelectedIndex(3); // Sipariş Girişi sekmesi
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Sipariş Girişi sayfasına yönlendiriliyorsunuz...", 
                    "Bilgi", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        aksiyonPanel.add(btnHizliSiparis);
        
        topPanel.add(baslikPanel, BorderLayout.WEST);
        topPanel.add(aksiyonPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);
        
        // Ana içerik - Sadece istatistik kartları
        JPanel mainContent = new JPanel(new BorderLayout(20, 20));
        mainContent.setOpaque(false);
        
        // İstatistik Kartları (2x4 Grid)
        JPanel kartlarPaneli = new JPanel(new GridLayout(2, 4, 20, 20));
        kartlarPaneli.setOpaque(false);
        
        // Gerçek istatistikleri al
        double bugununCirosu = veritabaniServisi.getBugununCirosu();
        int bugununSiparisSayisi = veritabaniServisi.getBugununSiparisSayisi();
        int aktifSiparisSayisi = veritabaniServisi.getAktifSiparisSayisi();
        int doluMasaSayisi = veritabaniServisi.getDoluMasaSayisi();
        double ortalamaSiparisTutari = veritabaniServisi.getOrtalamaSiparisTutari();
        
        // Kart 1: Bugünkü Ciro (büyük)
        JPanel kart1 = createPremiumKart(
            "💰 Bugünkü Ciro",
            String.format("%.2f $", bugununCirosu),
            bugununSiparisSayisi > 0 ? "✓ Aktif" : "—",
            bugununSiparisSayisi + " sipariş bugün",
            new Color(40, 167, 69),
            new Color(220, 248, 198),
            new Color(220, 248, 198)
        );
        
        // Kart 2: Bugünkü Sipariş Sayısı
        JPanel kart2 = createPremiumKart(
            "📦 Bugünkü Sipariş",
            String.valueOf(bugununSiparisSayisi),
            aktifSiparisSayisi > 0 ? aktifSiparisSayisi + " aktif" : "—",
            aktifSiparisSayisi + " sipariş hazırlanıyor",
            new Color(0, 123, 255),
            new Color(199, 224, 255),
            new Color(199, 224, 255)
        );
        
        // Kart 3: Aktif Siparişler
        JPanel kart3 = createPremiumKart(
            "⏳ Aktif Siparişler",
            String.valueOf(aktifSiparisSayisi),
            "Hazırlanıyor",
            "Bekleyen siparişler",
            new Color(255, 193, 7),
            new Color(255, 243, 204),
            new Color(255, 243, 204)
        );
        
        // Kart 4: Ortalama Sipariş Tutarı
        JPanel kart4 = createPremiumKart(
            "🛒 Ort. Sipariş",
            String.format("%.2f $", ortalamaSiparisTutari),
            bugununSiparisSayisi > 0 ? "✓" : "—",
            "Bugünkü ortalama",
            new Color(220, 53, 69),
            new Color(255, 220, 220),
            new Color(255, 220, 220)
        );
        
        // Kart 5: Dolu Masalar
        JPanel kart5 = createPremiumKart(
            "🪑 Dolu Masalar",
            String.valueOf(doluMasaSayisi),
            "Aktif",
            "Şu anda dolu masalar",
            new Color(156, 39, 176),
            new Color(243, 229, 245),
            new Color(243, 229, 245)
        );
        
        // Kart 6: Toplam Masalar
        int toplamMasaSayisi = veritabaniServisi.getMasaListesi().size();
        JPanel kart6 = createPremiumKart(
            "📊 Toplam Masalar",
            String.valueOf(toplamMasaSayisi),
            toplamMasaSayisi - doluMasaSayisi + " boş",
            "Sistemdeki toplam masa",
            new Color(0, 172, 193),
            new Color(224, 242, 241),
            new Color(224, 242, 241)
        );
        
        // Kart 7: Ürün Sayısı
        int urunSayisi = veritabaniServisi.getTumUrunler().size();
        JPanel kart7 = createPremiumKart(
            "🍽️ Menü Ürünleri",
            String.valueOf(urunSayisi),
            "Aktif",
            "Sistemdeki ürün sayısı",
            new Color(255, 152, 0),
            new Color(255, 243, 224),
            new Color(255, 243, 224)
        );
        
        // Kart 8: Durum Özeti
        String durumMetni = aktifSiparisSayisi > 0 ? "Çalışıyor" : "Boş";
        JPanel kart8 = createPremiumKart(
            "📈 Sistem Durumu",
            durumMetni,
            bugununSiparisSayisi > 0 ? "✓" : "—",
            "Restoran durumu",
            new Color(233, 30, 99),
            new Color(248, 187, 208),
            new Color(248, 187, 208)
        );
        
        kartlarPaneli.add(kart1);
        kartlarPaneli.add(kart2);
        kartlarPaneli.add(kart3);
        kartlarPaneli.add(kart4);
        kartlarPaneli.add(kart5);
        kartlarPaneli.add(kart6);
        kartlarPaneli.add(kart7);
        kartlarPaneli.add(kart8);
        
        mainContent.add(kartlarPaneli, BorderLayout.CENTER);
        
        add(mainContent, BorderLayout.CENTER);
    }
    
    /**
     * Premium kart bileşenini oluşturan metod (mini grafik ile)
     */
    private JPanel createPremiumKart(String baslik, String deger, String trend, 
                                     String altBilgi, Color anaRenk, 
                                     Color arkaPlanRenk, Color grafikRenk) {
        JPanel kart = new JPanel(new BorderLayout(15, 15)) {
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // Üst kenar vurgusu (daha kalın)
                g2.setColor(anaRenk);
                g2.fillRoundRect(0, 0, getWidth(), 6, 20, 20);
                
                // Sağ üst köşede mini grafik çizgisi
                int graphWidth = 60;
                int graphHeight = 30;
                int startX = getWidth() - graphWidth - 20;
                int startY = 15;
                
                g2.setColor(grafikRenk);
                g2.setStroke(new BasicStroke(2.5f));
                
                // Mini grafik çizgisi
                int[] xPoints = {startX, startX + 10, startX + 20, startX + 30, startX + 40, startX + 50};
                int[] yPoints = new int[6];
                for (int i = 0; i < 6; i++) {
                    yPoints[i] = startY + graphHeight - (int)(Math.sin(i * 0.8) * 8 + 15);
                }
                
                for (int i = 0; i < 5; i++) {
                    g2.drawLine(xPoints[i], yPoints[i], xPoints[i+1], yPoints[i+1]);
                }
                
                g2.dispose();
            }
        };
        
        kart.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(222, 226, 230), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        kart.setBackground(Color.WHITE);
        kart.setPreferredSize(new Dimension(280, 180));
        
        // Üst kısım - Başlık ve Trend
        JPanel ustPanel = new JPanel(new BorderLayout());
        ustPanel.setOpaque(false);
        
        JLabel lblBaslik = new JLabel(baslik);
        lblBaslik.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblBaslik.setForeground(new Color(108, 117, 125));
        ustPanel.add(lblBaslik, BorderLayout.WEST);
        
        JLabel lblTrend = new JLabel(trend);
        lblTrend.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTrend.setForeground(anaRenk);
        ustPanel.add(lblTrend, BorderLayout.EAST);
        
        kart.add(ustPanel, BorderLayout.NORTH);
        
        // Orta kısım - Büyük Değer
        JPanel ortaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ortaPanel.setOpaque(false);
        ortaPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));
        
        JLabel lblDeger = new JLabel(deger);
        lblDeger.setFont(new Font("Segoe UI", Font.BOLD, 42));
        lblDeger.setForeground(anaRenk);
        ortaPanel.add(lblDeger);
        
        kart.add(ortaPanel, BorderLayout.CENTER);
        
        // Alt kısım - Alt Bilgi
        JLabel lblAltBilgi = new JLabel(altBilgi);
        lblAltBilgi.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblAltBilgi.setForeground(new Color(134, 142, 150));
        kart.add(lblAltBilgi, BorderLayout.SOUTH);
        
        return kart;
    }
    
    private JButton createModernButton(String text, Color backgroundColor, Color textColor, int fontSize) {
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
        
        button.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        button.setForeground(textColor);
        button.setBackground(backgroundColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }
}

