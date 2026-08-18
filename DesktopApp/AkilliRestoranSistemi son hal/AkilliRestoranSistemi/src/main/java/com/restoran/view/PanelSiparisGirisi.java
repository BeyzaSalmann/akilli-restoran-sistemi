package com.restoran.view;

import com.restoran.model.SiparisDetay;
import com.restoran.model.Urun;
import com.restoran.service.VeritabaniServisi;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PanelSiparisGirisi extends JPanel {
    
    private VeritabaniServisi veritabaniServisi;
    private JTable urunTablo;
    private DefaultTableModel urunTabloModel;
    private JTable sepetTablo;
    private DefaultTableModel sepetTabloModel;
    private JSpinner miktarSpinner;
    private JComboBox<String> masaComboBox;
    private JLabel toplamTutarLabel;
    private List<Urun> urunler;
    private List<SiparisDetay> sepet;
    
    public PanelSiparisGirisi() {
        veritabaniServisi = new VeritabaniServisi();
        sepet = new ArrayList<>();
        initComponents();
        SwingUtilities.invokeLater(this::urunleriYukle);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        setBackground(new Color(248, 249, 250));
        
        // Modern başlık paneli
        JPanel baslikPanel = new JPanel(new BorderLayout());
        baslikPanel.setOpaque(false);
        baslikPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel baslikLabel = new JLabel("📋 Sipariş Girişi");
        baslikLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        baslikLabel.setForeground(new Color(33, 37, 41));
        baslikPanel.add(baslikLabel, BorderLayout.WEST);
        
        // Sağ taraf - Ürün Ekle butonu
        JButton btnUrunEkle = createModernButton("➕ Ürün Ekle", 
            new Color(40, 167, 69), Color.WHITE);
        btnUrunEkle.setPreferredSize(new Dimension(160, 40));
        btnUrunEkle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnUrunEkle.addActionListener(e -> urunEkleDialog());
        baslikPanel.add(btnUrunEkle, BorderLayout.EAST);
        
        add(baslikPanel, BorderLayout.NORTH);
        
        // Ana içerik paneli - Ürün listesi ve sepet yan yana
        JPanel anaPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        anaPanel.setOpaque(false);
        
        // Sol panel - Ürün Listesi
        JPanel urunPanel = createUrunPanel();
        anaPanel.add(urunPanel);
        
        // Sağ panel - Sepet
        JPanel sepetPanel = createSepetPanel();
        anaPanel.add(sepetPanel);
        
        add(anaPanel, BorderLayout.CENTER);
        
        // Alt panel - Butonlar
        JPanel altPanel = createAltPanel();
        add(altPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createUrunPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 1, 1, 1, new Color(222, 226, 230)),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        // Başlık
        JLabel baslikLabel = new JLabel("🛒 Ürün Listesi");
        baslikLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        baslikLabel.setForeground(new Color(52, 58, 64));
        baslikLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(baslikLabel, BorderLayout.NORTH);
        
        // Tablo başlıkları
        String[] kolonlar = {"Ürün ID", "Ürün Adı", "Fiyat ($)"};
        urunTabloModel = new DefaultTableModel(kolonlar, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        urunTablo = new JTable(urunTabloModel);
        urunTablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        urunTablo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        urunTablo.setRowHeight(32);
        urunTablo.setGridColor(new Color(233, 236, 239));
        urunTablo.setSelectionBackground(new Color(0, 123, 255));
        urunTablo.setSelectionForeground(Color.WHITE);
        
        // Tablo header stil
        JTableHeader header = urunTablo.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(52, 58, 64));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        
        JScrollPane scrollPane = new JScrollPane(urunTablo);
        scrollPane.setBorder(new LineBorder(new Color(222, 226, 230), 1));
        scrollPane.setPreferredSize(new Dimension(400, 420));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Miktar seçimi ve sepete ekle butonu
        JPanel urunAltPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        urunAltPanel.setOpaque(false);
        urunAltPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        JLabel miktarLabel = new JLabel("Miktar:");
        miktarLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        miktarLabel.setForeground(new Color(73, 80, 87));
        urunAltPanel.add(miktarLabel);
        
        miktarSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        miktarSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        miktarSpinner.setPreferredSize(new Dimension(90, 38));
        miktarSpinner.setBorder(new CompoundBorder(
            new LineBorder(new Color(206, 212, 218), 1),
            new EmptyBorder(5, 10, 5, 10)
        ));
        urunAltPanel.add(miktarSpinner);
        
        JButton sepeteEkleBtn = createModernButton("➕ Sepete Ekle", new Color(40, 167, 69), new Color(255, 255, 255));
        sepeteEkleBtn.setPreferredSize(new Dimension(180, 42));
        sepeteEkleBtn.addActionListener(e -> sepeteEkle());
        urunAltPanel.add(sepeteEkleBtn);
        
        panel.add(urunAltPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createSepetPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 1, 1, 1, new Color(222, 226, 230)),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        // Başlık
        JLabel baslikLabel = new JLabel("🛍️ Sepet");
        baslikLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        baslikLabel.setForeground(new Color(52, 58, 64));
        baslikLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(baslikLabel, BorderLayout.NORTH);
        
        // Sepet tablosu
        String[] sepetKolonlar = {"Ürün Adı", "Adet", "Birim Fiyat", "Toplam"};
        sepetTabloModel = new DefaultTableModel(sepetKolonlar, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        sepetTablo = new JTable(sepetTabloModel);
        sepetTablo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sepetTablo.setRowHeight(32);
        sepetTablo.setGridColor(new Color(233, 236, 239));
        sepetTablo.setSelectionBackground(new Color(255, 193, 7));
        sepetTablo.setSelectionForeground(new Color(33, 37, 41));
        
        // Tablo header stil
        JTableHeader header = sepetTablo.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(52, 58, 64));
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        
        JScrollPane scrollPane = new JScrollPane(sepetTablo);
        scrollPane.setBorder(new LineBorder(new Color(222, 226, 230), 1));
        scrollPane.setPreferredSize(new Dimension(400, 350));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Toplam tutar ve butonlar
        JPanel sepetAltPanel = new JPanel(new BorderLayout(15, 10));
        sepetAltPanel.setOpaque(false);
        sepetAltPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        // Toplam tutar ve masa seçimi paneli
        JPanel toplamPanel = new JPanel(new BorderLayout(15, 10));
        toplamPanel.setOpaque(false);
        toplamPanel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, new Color(222, 226, 230)),
            new EmptyBorder(15, 0, 15, 0)
        ));
        
        // Sol taraf - Masa seçimi (genişlik sınırlı)
        JPanel masaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        masaPanel.setOpaque(false);
        masaPanel.setPreferredSize(new Dimension(400, 35)); // Maksimum genişlik
        masaPanel.setMaximumSize(new Dimension(400, 35));
        JLabel masaLabel = new JLabel("🪑 Masa No:");
        masaLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        masaLabel.setForeground(new Color(73, 80, 87));
        masaPanel.add(masaLabel);
        
        // Masa listesini veritabanından al
        List<Integer> mevcutMasalar = veritabaniServisi.getMasaListesi();
        String[] masaListesi = new String[mevcutMasalar.size() + 1];
        masaListesi[0] = "Masa Seçiniz...";
        for (int i = 0; i < mevcutMasalar.size(); i++) {
            masaListesi[i + 1] = "Masa " + mevcutMasalar.get(i);
        }
        
        masaComboBox = new JComboBox<>(masaListesi);
        masaComboBox.setFont(new Font("Segoe UI", Font.BOLD, 14));
        masaComboBox.setPreferredSize(new Dimension(180, 35));
        masaComboBox.setBorder(new CompoundBorder(
            new LineBorder(new Color(0, 123, 255), 1),
            new EmptyBorder(5, 10, 5, 10)
        ));
        masaComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (index == 0) {
                    c.setForeground(new Color(128, 128, 128)); // Gri renk
                } else {
                    c.setForeground(new Color(0, 0, 0)); // Siyah renk
                }
                return c;
            }
        });
        masaPanel.add(masaComboBox);
        
        // Yeni Masa Ekle butonu
        JButton yeniMasaBtn = createModernButton("➕ Yeni Masa", new Color(40, 167, 69), Color.WHITE);
        yeniMasaBtn.setPreferredSize(new Dimension(140, 35));
        yeniMasaBtn.addActionListener(e -> yeniMasaEkle());
        masaPanel.add(yeniMasaBtn);
        
        toplamPanel.add(masaPanel, BorderLayout.WEST);
        
        // Sağ taraf - Toplam tutar (minimum genişlik garantili)
        JPanel toplamPanelSag = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        toplamPanelSag.setOpaque(false);
        toplamPanelSag.setPreferredSize(new Dimension(250, 35)); // Minimum genişlik
        toplamPanelSag.setMinimumSize(new Dimension(250, 35));
        JLabel toplamLabel = new JLabel("Toplam Tutar:");
        toplamLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        toplamLabel.setForeground(new Color(73, 80, 87));
        toplamPanelSag.add(toplamLabel);
        
        toplamTutarLabel = new JLabel("0.00 $");
        toplamTutarLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        toplamTutarLabel.setForeground(new Color(40, 167, 69));
        toplamPanelSag.add(toplamTutarLabel);
        
        toplamPanel.add(toplamPanelSag, BorderLayout.EAST);
        
        sepetAltPanel.add(toplamPanel, BorderLayout.NORTH);
        
        // Butonlar
        JPanel butonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        butonPanel.setOpaque(false);
        
        JButton sepetiTemizleBtn = createModernButton("🗑️ Sepeti Temizle", new Color(108, 117, 125), new Color(255, 255, 255));
        sepetiTemizleBtn.setPreferredSize(new Dimension(180, 38));
        sepetiTemizleBtn.addActionListener(e -> sepetiTemizle());
        butonPanel.add(sepetiTemizleBtn);
        
        JButton secilenUrunSilBtn = createModernButton("❌ Seçili Ürünü Sil", new Color(220, 53, 69), new Color(255, 255, 255));
        secilenUrunSilBtn.setPreferredSize(new Dimension(180, 38));
        secilenUrunSilBtn.addActionListener(e -> secilenUrunuSil());
        butonPanel.add(secilenUrunSilBtn);
        
        sepetAltPanel.add(butonPanel, BorderLayout.SOUTH);
        
        panel.add(sepetAltPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createAltPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        JButton siparisKaydetBtn = createModernButton("💾 SİPARİŞİ KAYDET", new Color(0, 123, 255), new Color(255, 255, 255));
        siparisKaydetBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        siparisKaydetBtn.setPreferredSize(new Dimension(280, 50));
        siparisKaydetBtn.addActionListener(e -> siparisiKaydet());
        panel.add(siparisKaydetBtn);
        
        return panel;
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
        int minWidth = Math.max(preferredSize.width + 30, 120); // Minimum 120px + 30px padding
        button.setPreferredSize(new Dimension(minWidth, 38));
        
        return button;
    }
    
    private void urunleriYukle() {
        try {
            if (urunTabloModel == null) {
                System.err.println("⚠ urunTabloModel henüz oluşturulmamış! Kısa süre sonra tekrar denenecek...");
                // UI hazır olduktan sonra tekrar dene
                SwingUtilities.invokeLater(() -> {
                    SwingUtilities.invokeLater(() -> urunleriYukle());
                });
                return;
            }
            
            urunler = veritabaniServisi.getTumUrunler();
            
            if (urunler == null) {
                System.err.println("Ürün listesi alınamadı.");
                return;
            }
            
            if (urunler.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "Ürün bulunamadı! Lütfen '➕ Ürün Ekle' butonunu kullanarak ürün ekleyin.",
                        "Bilgi",
                        JOptionPane.INFORMATION_MESSAGE);
                });
                return;
            }
            
            urunTabloModel.setRowCount(0);
            
            for (Urun urun : urunler) {
                if (urun != null && urun.getUrunAdi() != null) {
                    Object[] row = {
                        urun.getUrunID(),
                        urun.getUrunAdi(),
                        String.format("%.2f", urun.getFiyat())
                    };
                    urunTabloModel.addRow(row);
                }
            }
            
            // Tabloyu güncelle
            if (urunTablo != null) {
                urunTablo.revalidate();
                urunTablo.repaint();
            }
            
        } catch (Exception e) {
            System.err.println("Ürünler yüklenirken hata: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                    "Ürünler yüklenirken bir hata oluştu:\n" + e.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    
    private void sepeteEkle() {
        int selectedRow = urunTablo.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Lütfen sepete eklemek için bir ürün seçin!",
                "Uyarı",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int urunID = (Integer) urunTabloModel.getValueAt(selectedRow, 0);
        Urun secilenUrun = urunler.stream()
            .filter(u -> u.getUrunID() == urunID)
            .findFirst()
            .orElse(null);
        
        if (secilenUrun != null) {
            int miktar = (Integer) miktarSpinner.getValue();
            
            // Sepette aynı ürün var mı kontrol et
            boolean urunVar = false;
            for (SiparisDetay detay : sepet) {
                if (detay.getUrun().getUrunID() == secilenUrun.getUrunID()) {
                    detay.setAdet(detay.getAdet() + miktar);
                    urunVar = true;
                    break;
                }
            }
            
            if (!urunVar) {
                sepet.add(new SiparisDetay(secilenUrun, miktar));
            }
            
            sepetiGuncelle();
        }
    }
    
    private void sepetiGuncelle() {
        sepetTabloModel.setRowCount(0);
        double toplam = 0;
        
        for (SiparisDetay detay : sepet) {
            Object[] row = {
                detay.getUrun().getUrunAdi(),
                detay.getAdet(),
                String.format("%.2f $", detay.getUrun().getFiyat()),
                String.format("%.2f $", detay.getToplamTutar())
            };
            sepetTabloModel.addRow(row);
            toplam += detay.getToplamTutar();
        }
        
        toplamTutarLabel.setText(String.format("%.2f $", toplam));
    }
    
    private void sepetiTemizle() {
        int onay = JOptionPane.showConfirmDialog(this,
            "Sepeti temizlemek istediğinize emin misiniz?",
            "Onay",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (onay == JOptionPane.YES_OPTION) {
            sepet.clear();
            sepetiGuncelle();
        }
    }
    
    private void secilenUrunuSil() {
        int selectedRow = sepetTablo.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Lütfen silmek için bir ürün seçin!",
                "Uyarı",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        sepet.remove(selectedRow);
        sepetiGuncelle();
    }
    
    private void siparisiKaydet() {
        if (sepet.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Sepetiniz boş! Lütfen siparişe ürün ekleyin.",
                "Uyarı",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Seçilen masayı al
        String secilenMasa = (String) masaComboBox.getSelectedItem();
        if (secilenMasa == null || secilenMasa.equals("Masa Seçiniz...")) {
            JOptionPane.showMessageDialog(this,
                "Lütfen bir masa seçiniz!",
                "Uyarı",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Masa numarasını string'den çıkar: "Masa 7" -> 7
        Integer masaNo = Integer.parseInt(secilenMasa.replace("Masa ", ""));
        double toplamTutar = sepet.stream()
            .mapToDouble(SiparisDetay::getToplamTutar)
            .sum();
        
        int onay = JOptionPane.showConfirmDialog(this,
            String.format("Masa %d için siparişi kaydetmek istediğinize emin misiniz?\n\nToplam Tutar: %.2f $", masaNo, toplamTutar),
            "Sipariş Onayı",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (onay == JOptionPane.YES_OPTION) {
            try {
                // Önce masanın var olup olmadığını kontrol et, yoksa ekle
                List<Integer> mevcutMasalar = veritabaniServisi.getMasaListesi();
                if (!mevcutMasalar.contains(masaNo)) {
                    veritabaniServisi.masaEkle(masaNo);
                }
                
                boolean mevcutSipariseEklendi = veritabaniServisi.siparisKaydet(sepet, masaNo);
                
                String mesaj;
                if (mevcutSipariseEklendi) {
                    mesaj = String.format(
                        "Ürünler mevcut siparişe eklendi!\n\nMasa: %d\nEklenen Tutar: %.2f $\n\n" +
                        "Mevcut siparişinize yeni ürünler eklendi. Aktif Siparişler sekmesinden toplam tutarı görebilirsiniz.",
                        masaNo, toplamTutar
                    );
                } else {
                    mesaj = String.format("Sipariş başarıyla kaydedildi!\nMasa: %d\nToplam: %.2f $", masaNo, toplamTutar);
                }
                
                JOptionPane.showMessageDialog(this,
                    mesaj,
                    "Başarılı",
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Sepeti temizle
                sepet.clear();
                sepetiGuncelle();
                
                // Masa listesini güncelle
                masaListesiniGuncelle();
                
                // Panelleri güncellemek için ana panele sinyal gönder
                SwingUtilities.invokeLater(() -> {
                    Container parent = getParent();
                    while (parent != null && !(parent instanceof JTabbedPane)) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof JTabbedPane) {
                        JTabbedPane tabbedPane = (JTabbedPane) parent;
                        
                        // Aktif Siparişler sekmesini güncelle
                        int aktifSiparislerIndex = -1;
                        int duyguAnaliziIndex = -1;
                        
                        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                            String tabTitle = tabbedPane.getTitleAt(i);
                            if (tabTitle.equals("Aktif Siparişler")) {
                                aktifSiparislerIndex = i;
                            } else if (tabTitle.equals("Duygu Analizi")) {
                                duyguAnaliziIndex = i;
                            }
                        }
                        
                        // Aktif Siparişler panelini güncelle
                        if (aktifSiparislerIndex >= 0) {
                            JPanel aktifPanel = (JPanel) tabbedPane.getComponentAt(aktifSiparislerIndex);
                            if (aktifPanel instanceof PanelAktifSiparisler) {
                                ((PanelAktifSiparisler) aktifPanel).siparisleriYenile();
                            }
                        }
                        
                        // Duygu Analizi panelini güncelle (masalar için)
                        if (duyguAnaliziIndex >= 0) {
                            JPanel duyguPanel = (JPanel) tabbedPane.getComponentAt(duyguAnaliziIndex);
                            if (duyguPanel instanceof PanelDuyguAnalizi) {
                                // PanelDuyguAnalizi'ni yeniden oluştur
                                tabbedPane.removeTabAt(duyguAnaliziIndex);
                                PanelDuyguAnalizi yeniPanel = new PanelDuyguAnalizi();
                                tabbedPane.insertTab("Duygu Analizi", null, yeniPanel, null, duyguAnaliziIndex);
                            }
                        }
                        
                        // Genel Bakış panelini güncelle (dolu masa sayısı ve diğer istatistikler için)
                        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                            String tabTitle = tabbedPane.getTitleAt(i);
                            if (tabTitle.equals("Genel Bakış")) {
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
                    "Sipariş kaydedilirken bir hata oluştu:\n" + e.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void urunEkleDialog() {
        // Ürün ekleme diyalogu
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JTextField urunAdiField = new JTextField(20);
        JComboBox<String> kategoriCombo = new JComboBox<>(new String[]{"Ana Yemek", "Salata", "İçecek", "Tatlı", "Atıştırmalık"});
        JTextField fiyatField = new JTextField(20);
        
        panel.add(new JLabel("Ürün Adı:"));
        panel.add(urunAdiField);
        panel.add(new JLabel("Kategori:"));
        panel.add(kategoriCombo);
        panel.add(new JLabel("Fiyat ($):"));
        panel.add(fiyatField);
        
        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Yeni Ürün Ekle",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            String urunAdi = urunAdiField.getText().trim();
            String kategori = (String) kategoriCombo.getSelectedItem();
            String fiyatStr = fiyatField.getText().trim();
            
            // Validasyon
            if (urunAdi.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Ürün adı boş olamaz!",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (fiyatStr.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Fiyat boş olamaz!",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (kategori == null || kategori.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Kategori seçilmelidir!",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                // Fiyat string'ini temizle (virgül nokta dönüşümü)
                fiyatStr = fiyatStr.replace(",", ".");
                double fiyat = Double.parseDouble(fiyatStr);
                
                if (fiyat <= 0) {
                    throw new NumberFormatException("Fiyat pozitif olmalıdır");
                }
                
                if (fiyat > 10000) {
                    throw new NumberFormatException("Fiyat çok yüksek! Maksimum 10000 $ olabilir.");
                }
                
                // Ürünü ekle
                veritabaniServisi.urunEkle(urunAdi, kategori, fiyat);
                
                JOptionPane.showMessageDialog(this,
                    String.format("Ürün başarıyla eklendi!\n%s - %.2f $", urunAdi, fiyat),
                    "Başarılı",
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Ürün listesini yenile
                SwingUtilities.invokeLater(() -> urunleriYukle());
                
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Geçerli bir fiyat giriniz!\nÖrnek: 45.50 veya 45,50",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            } catch (Exception e) {
                String hataMesaji = e.getMessage();
                if (hataMesaji == null || hataMesaji.isEmpty()) {
                    hataMesaji = e.getClass().getSimpleName();
                }
                JOptionPane.showMessageDialog(this,
                    "Ürün eklenirken bir hata oluştu:\n" + hataMesaji + "\n\nDetaylar konsola yazdırıldı.",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
                System.err.println("Ürün eklenirken hata:");
                e.printStackTrace();
            }
        }
    }
    
    private void yeniMasaEkle() {
        String masaNoStr = JOptionPane.showInputDialog(
            this,
            "Yeni masa numarasını girin (1-50):",
            "Yeni Masa Ekle",
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (masaNoStr != null && !masaNoStr.trim().isEmpty()) {
            try {
                int masaNo = Integer.parseInt(masaNoStr.trim());
                if (masaNo < 1 || masaNo > 50) {
                    JOptionPane.showMessageDialog(this,
                        "Masa numarası 1 ile 50 arasında olmalıdır!",
                        "Hata",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Masa zaten var mı kontrol et
                List<Integer> mevcutMasalar = veritabaniServisi.getMasaListesi();
                if (mevcutMasalar.contains(masaNo)) {
                    JOptionPane.showMessageDialog(this,
                        "Bu masa zaten sistemde mevcut!",
                        "Bilgi",
                        JOptionPane.INFORMATION_MESSAGE);
                    // Mevcut masayı seç
                    masaComboBox.setSelectedItem("Masa " + masaNo);
                    return;
                }
                
                // Yeni masayı ekle
                veritabaniServisi.masaEkle(masaNo);
                JOptionPane.showMessageDialog(this,
                    "Masa " + masaNo + " başarıyla eklendi!",
                    "Başarılı",
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Masa listesini güncelle
                masaListesiniGuncelle();
                
                // Yeni eklenen masayı seç
                masaComboBox.setSelectedItem("Masa " + masaNo);
                
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Geçerli bir masa numarası giriniz!",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Masa eklenirken bir hata oluştu:\n" + e.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    private void masaListesiniGuncelle() {
        List<Integer> mevcutMasalar = veritabaniServisi.getMasaListesi();
        String secilenMasa = (String) masaComboBox.getSelectedItem();
        
        String[] masaListesi = new String[mevcutMasalar.size() + 1];
        masaListesi[0] = "Masa Seçiniz...";
        for (int i = 0; i < mevcutMasalar.size(); i++) {
            masaListesi[i + 1] = "Masa " + mevcutMasalar.get(i);
        }
        
        masaComboBox.setModel(new DefaultComboBoxModel<>(masaListesi));
        
        // Önceki seçimi koru (eğer hala mevcutsa)
        if (secilenMasa != null && !secilenMasa.equals("Masa Seçiniz...")) {
            masaComboBox.setSelectedItem(secilenMasa);
        } else {
            masaComboBox.setSelectedIndex(0);
        }
    }
}
