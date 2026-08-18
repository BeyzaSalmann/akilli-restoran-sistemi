package com.restoran.service;

import com.restoran.model.Urun;
import com.restoran.util.VeritabaniBaglanti;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class VeritabaniServisi {

    public void initDB() {
        String createMusteriSQL = "CREATE TABLE IF NOT EXISTS Musteri ("
            + " musteriID INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " duyguDurumu TEXT NOT NULL,"
            + " siparisZamani DATETIME DEFAULT CURRENT_TIMESTAMP);";

        String createMenuSQL = "CREATE TABLE IF NOT EXISTS Menu ("
            + " menuID INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " duyguKategorisi TEXT NOT NULL,"
            + " urunAdi TEXT NOT NULL,"
            + " fiyat REAL NOT NULL);";

        String createUrunSQL = "CREATE TABLE IF NOT EXISTS Urun ("
            + " urun_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " urun_adi TEXT NOT NULL,"
            + " kategori TEXT NOT NULL,"
            + " fiyat REAL NOT NULL);";

        String createMasaSQL = "CREATE TABLE IF NOT EXISTS Masa ("
            + " masa_id INTEGER PRIMARY KEY,"
            + " masa_no INTEGER UNIQUE NOT NULL,"
            + " durum TEXT DEFAULT 'Boş',"
            + " olusturma_tarihi DATETIME DEFAULT CURRENT_TIMESTAMP);";

        try (Connection conn = VeritabaniBaglanti.getConnection();
             Statement stmt = conn.createStatement()) {
            
            if (conn != null) {
                stmt.execute(createMusteriSQL);
                stmt.execute(createMenuSQL);
                stmt.execute(createUrunSQL);
                stmt.execute(createMasaSQL);
                varsayilanVerileriEkle(conn);
            } else {
                System.err.println("Veritabanı bağlantısı kurulamadı.");
            }

        } catch (SQLException e) {
            System.err.println("Tablo oluşturma hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void varsayilanVerileriEkle(Connection conn) {
        try (Statement countStmt = conn.createStatement();
             ResultSet rs = countStmt.executeQuery("SELECT COUNT(*) FROM Urun")) {

            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }
            
            if (count == 0) {
                String insertSQL = "INSERT INTO Urun(urun_adi, kategori, fiyat) VALUES(?,?,?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                    String[] urunler = {
                        "Çorba", "Ana Yemek", "45.0",
                        "Etli Yemek", "Ana Yemek", "120.0",
                        "Tavuk Sote", "Ana Yemek", "95.0",
                        "Salata", "Salata", "40.0",
                        "Kola", "İçecek", "25.0",
                        "Su", "İçecek", "10.0"
                    };

                    for (int i = 0; i < urunler.length; i += 3) {
                        pstmt.setString(1, urunler[i]);
                        pstmt.setString(2, urunler[i + 1]);
                        pstmt.setDouble(3, Double.parseDouble(urunler[i + 2]));
                        pstmt.executeUpdate();
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Varsayılan veriler eklenirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Urun> getTumUrunler() {
        List<Urun> urunler = new ArrayList<>();
        String sql = "SELECT urun_id, urun_adi, fiyat FROM Urun";
        
        try (Connection conn = VeritabaniBaglanti.getConnection()) {
            if (conn == null) {
                System.err.println("Veritabanı bağlantısı kurulamadı.");
                return urunler;
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    int urunID = rs.getInt("urun_id");
                    String urunAdi = rs.getString("urun_adi");
                    double fiyat = rs.getDouble("fiyat");
                    urunler.add(new Urun(urunID, urunAdi, fiyat));
                }

            } catch (SQLException e) {
                System.err.println("SQL sorgusu hatası: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (SQLException e) {
            System.err.println("Veritabanı bağlantı hatası: " + e.getMessage());
            e.printStackTrace();
        }

        return urunler;
    }

    public List<Urun> getMenuOnerileri(String duyguKategorisi) {
        List<Urun> oneriler = new ArrayList<>();
        String sql = "SELECT menuID, urunAdi, fiyat FROM Menu WHERE duyguKategorisi = ?";

        try (Connection conn = VeritabaniBaglanti.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, duyguKategorisi);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int menuID = rs.getInt("menuID");
                String urunAdi = rs.getString("urunAdi");
                double fiyat = rs.getDouble("fiyat");
                oneriler.add(new Urun(menuID, urunAdi, fiyat));
            }

        } catch (SQLException e) {
            System.err.println("Menü önerileri alınırken hata: " + e.getMessage());
        }
        
        return oneriler;
    }

    public void musteriKaydet(String duyguDurumu) {
        String sql = "INSERT INTO Musteri(duyguDurumu) VALUES(?)";

        try (Connection conn = VeritabaniBaglanti.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, duyguDurumu);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Müşteri kaydederken hata: " + e.getMessage());
        }
    }

    public void urunEkle(String urunAdi, String kategori, double fiyat) {
        // Önce Urun tablosunun var olduğundan emin ol
        String createUrunSQL = "CREATE TABLE IF NOT EXISTS Urun ("
            + " urun_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " urun_adi TEXT NOT NULL,"
            + " kategori TEXT NOT NULL,"
            + " fiyat REAL NOT NULL);";
        
        String sql = "INSERT INTO Urun(urun_adi, kategori, fiyat) VALUES(?, ?, ?)";

        Connection conn = null;
        try {
            conn = VeritabaniBaglanti.getConnection();
            if (conn == null) {
                throw new RuntimeException("Veritabanı bağlantısı alınamadı!");
            }
            
            // Tabloyu oluştur (yoksa)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createUrunSQL);
            }
            
            // Ürünü ekle
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, urunAdi);
                pstmt.setString(2, kategori);
                pstmt.setDouble(3, fiyat);
                int affectedRows = pstmt.executeUpdate();
                
                if (affectedRows == 0) {
                    throw new RuntimeException("Ürün eklenemedi (etkilenen satır sayısı: 0)");
                }
            }
            
            // Bağlantıyı kapatma - singleton olduğu için diğer işlemler kullanabilir

        } catch (SQLException e) {
            System.err.println("✗ Ürün eklenirken SQL hatası: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            throw new RuntimeException("Ürün eklenirken veritabanı hatası: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("✗ Ürün eklenirken genel hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Ürün eklenirken hata: " + e.getMessage(), e);
        }
    }

    // Sipariş kaydet - mevcut siparişe eklendi mi bilgisini döndürür
    public boolean siparisKaydet(List<com.restoran.model.SiparisDetay> siparisDetaylari, Integer masaNo) {
        String createSiparisSQL = "CREATE TABLE IF NOT EXISTS Siparis ("
            + " siparis_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " siparis_tarihi DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + " toplam_tutar REAL NOT NULL,"
            + " masa_no INTEGER,"
            + " durum TEXT DEFAULT 'Hazırlanıyor');";

        String createSiparisDetaySQL = "CREATE TABLE IF NOT EXISTS SiparisDetay ("
            + " detay_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " siparis_id INTEGER NOT NULL,"
            + " urun_id INTEGER NOT NULL,"
            + " adet INTEGER NOT NULL,"
            + " birim_fiyat REAL NOT NULL,"
            + " toplam_fiyat REAL NOT NULL,"
            + " FOREIGN KEY (siparis_id) REFERENCES Siparis(siparis_id),"
            + " FOREIGN KEY (urun_id) REFERENCES Urun(urun_id));";

        try (Connection conn = VeritabaniBaglanti.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Tabloları oluştur
            stmt.execute(createSiparisSQL);
            stmt.execute(createSiparisDetaySQL);
            
            // Toplam tutarı hesapla
            double yeniToplamTutar = 0;
            for (com.restoran.model.SiparisDetay detay : siparisDetaylari) {
                yeniToplamTutar += detay.getToplamTutar();
            }
            
            int siparisId = -1;
            boolean mevcutSipariseEklendi = false;
            
            // Eğer masa numarası varsa, o masada aktif sipariş var mı kontrol et
            if (masaNo != null) {
                String aktifSiparisSQL = "SELECT siparis_id, toplam_tutar FROM Siparis " +
                                         "WHERE masa_no = ? " +
                                         "AND (durum IS NULL OR (durum != 'Tamamlandı' AND durum != 'İptal Edildi')) " +
                                         "ORDER BY siparis_tarihi DESC LIMIT 1";
                
                try (PreparedStatement aktifPstmt = conn.prepareStatement(aktifSiparisSQL)) {
                    aktifPstmt.setInt(1, masaNo);
                    ResultSet aktifRs = aktifPstmt.executeQuery();
                    
                    if (aktifRs.next()) {
                        // Aktif sipariş var, mevcut siparişe ekle
                        siparisId = aktifRs.getInt("siparis_id");
                        double mevcutToplam = aktifRs.getDouble("toplam_tutar");
                        double guncelToplam = mevcutToplam + yeniToplamTutar;
                        mevcutSipariseEklendi = true;
                        
                        // Toplam tutarı güncelle
                        String guncelleSQL = "UPDATE Siparis SET toplam_tutar = ? WHERE siparis_id = ?";
                        try (PreparedStatement guncellePstmt = conn.prepareStatement(guncelleSQL)) {
                            guncellePstmt.setDouble(1, guncelToplam);
                            guncellePstmt.setInt(2, siparisId);
                            guncellePstmt.executeUpdate();
                        }
                    }
                }
            }
            
            // Eğer aktif sipariş bulunamadıysa yeni sipariş oluştur
            if (siparisId == -1) {
                String siparisSQL = "INSERT INTO Siparis(toplam_tutar, masa_no, durum) VALUES(?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(siparisSQL, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setDouble(1, yeniToplamTutar);
                    if (masaNo != null) {
                        pstmt.setInt(2, masaNo);
                    } else {
                        pstmt.setNull(2, java.sql.Types.INTEGER);
                    }
                    pstmt.setString(3, "Hazırlanıyor");
                    pstmt.executeUpdate();
                    
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        siparisId = rs.getInt(1);
                    }
                }
            }
            
            // Sipariş detaylarını kaydet (hem yeni hem mevcut sipariş için)
            String detaySQL = "INSERT INTO SiparisDetay(siparis_id, urun_id, adet, birim_fiyat, toplam_fiyat) VALUES(?,?,?,?,?)";
            try (PreparedStatement detayPstmt = conn.prepareStatement(detaySQL)) {
                for (com.restoran.model.SiparisDetay detay : siparisDetaylari) {
                    detayPstmt.setInt(1, siparisId);
                    detayPstmt.setInt(2, detay.getUrun().getUrunID());
                    detayPstmt.setInt(3, detay.getAdet());
                    detayPstmt.setDouble(4, detay.getUrun().getFiyat());
                    detayPstmt.setDouble(5, detay.getToplamTutar());
                    detayPstmt.executeUpdate();
                }
            }
            
            // Masaya sipariş verildiğinde masa durumunu "Dolu" olarak güncelle
            if (masaNo != null) {
                masaDurumGuncelle(masaNo, "Dolu");
            }
            
            return mevcutSipariseEklendi;

        } catch (SQLException e) {
            System.err.println("✗ Sipariş kaydederken hata: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<SiparisBilgisi> getAktifSiparisler() {
        List<SiparisBilgisi> siparisler = new ArrayList<>();
        
        // Önce siparişleri al (Tamamlandı, İptal Edildi durumlarını hariç tut)
        // Servis Edildi durumundaki siparişler de gösterilecek (hesap kapatılmayı bekliyor)
        String sqlSiparis = "SELECT siparis_id, toplam_tutar, masa_no, durum " +
                            "FROM Siparis " +
                            "WHERE (durum IS NULL OR (durum != 'Tamamlandı' AND durum != 'İptal Edildi')) " +
                            "ORDER BY siparis_tarihi DESC";

        try (Connection conn = VeritabaniBaglanti.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlSiparis)) {

            while (rs.next()) {
                int siparisId = rs.getInt("siparis_id");
                double toplamTutar = rs.getDouble("toplam_tutar");
                Integer masaNo = rs.getObject("masa_no") != null ? rs.getInt("masa_no") : null;
                String durum = rs.getString("durum");
                if (durum == null) {
                    durum = "Hazırlanıyor";
                }
                
                // Her sipariş için ürünleri al
                String sqlUrunler = "SELECT u.urun_adi, sd.adet " +
                                    "FROM SiparisDetay sd " +
                                    "JOIN Urun u ON sd.urun_id = u.urun_id " +
                                    "WHERE sd.siparis_id = ?";
                
                List<String> urunListesi = new ArrayList<>();
                int urunSayisi = 0;
                
                try (PreparedStatement pstmtUrunler = conn.prepareStatement(sqlUrunler)) {
                    pstmtUrunler.setInt(1, siparisId);
                    try (ResultSet rsUrunler = pstmtUrunler.executeQuery()) {
                        while (rsUrunler.next()) {
                            String urunAdi = rsUrunler.getString("urun_adi");
                            int adet = rsUrunler.getInt("adet");
                            if (urunAdi != null) {
                                urunListesi.add(urunAdi + " (" + adet + ")");
                                urunSayisi++;
                            }
                        }
                    }
                } catch (SQLException eUrun) {
                    System.err.println("⚠ Sipariş " + siparisId + " için ürün bilgisi alınamadı: " + eUrun.getMessage());
                    // Ürün bilgisi alınamasa bile siparişi göster
                }
                
                // Ürün listesini virgülle birleştir
                String urunler = urunListesi.isEmpty() ? 
                    String.format("Sipariş #%d - Toplam: %.2f $", siparisId, toplamTutar) : 
                    String.join(", ", urunListesi);

                siparisler.add(new SiparisBilgisi(siparisId, masaNo, durum, urunler, toplamTutar, urunSayisi));
            }

        } catch (SQLException e) {
            System.err.println("✗ Aktif siparişler alınırken hata: " + e.getMessage());
            e.printStackTrace();
        }

        return siparisler;
    }

    public void masaEkle(int masaNo) {
        // Önce masa tablosunun var olduğundan emin ol
        String createMasaSQL = "CREATE TABLE IF NOT EXISTS Masa ("
            + " masa_id INTEGER PRIMARY KEY,"
            + " masa_no INTEGER UNIQUE NOT NULL,"
            + " durum TEXT DEFAULT 'Boş',"
            + " olusturma_tarihi DATETIME DEFAULT CURRENT_TIMESTAMP);";
        
        try (Connection conn = VeritabaniBaglanti.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Masa tablosunu oluştur (yoksa)
            stmt.execute(createMasaSQL);
            
            // Masayı ekle
            String sql = "INSERT OR IGNORE INTO Masa(masa_no, durum) VALUES(?, 'Boş')";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, masaNo);
                int result = pstmt.executeUpdate();
                
                if (result == 0) {
                    String checkSQL = "SELECT COUNT(*) FROM Masa WHERE masa_no = ?";
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
                        checkStmt.setInt(1, masaNo);
                        ResultSet rs = checkStmt.executeQuery();
                        if (!(rs.next() && rs.getInt(1) > 0)) {
                            throw new RuntimeException("Masa eklenemedi (bilinmeyen hata)");
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("✗ Masa eklenirken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Masa eklenirken veritabanı hatası: " + e.getMessage(), e);
        }
    }

    public List<Integer> getMasaListesi() {
        List<Integer> masalar = new ArrayList<>();
        String sql = "SELECT masa_no FROM Masa ORDER BY masa_no";

        try (Connection conn = VeritabaniBaglanti.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                masalar.add(rs.getInt("masa_no"));
            }

        } catch (SQLException e) {
            System.err.println("✗ Masa listesi alınırken hata: " + e.getMessage());
            e.printStackTrace();
        }

        return masalar;
    }
    
    public boolean masaAktifSiparisVarMi(int masaNo) {
        String sql = "SELECT COUNT(*) as sayi FROM Siparis " +
                     "WHERE masa_no = ? " +
                     "AND (durum IS NULL OR (durum != 'Tamamlandı' AND durum != 'İptal Edildi'))";
        
        try (Connection conn = VeritabaniBaglanti.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, masaNo);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("sayi") > 0;
            }
        } catch (SQLException e) {
            System.err.println("✗ Masa aktif sipariş kontrolü yapılırken hata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public void masaSil(int masaNo) {
        // Önce masada aktif sipariş var mı kontrol et
        if (masaAktifSiparisVarMi(masaNo)) {
            throw new RuntimeException("Bu masada aktif siparişler bulunmaktadır. Lütfen önce siparişleri tamamlayın veya iptal edin.");
        }
        
        String sql = "DELETE FROM Masa WHERE masa_no = ?";
        
        try (Connection conn = VeritabaniBaglanti.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, masaNo);
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new RuntimeException("Masa bulunamadı veya silinemedi");
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Masa silinirken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Masa silinirken veritabanı hatası: " + e.getMessage(), e);
        }
    }
    
    public String getMasaDurumu(int masaNo) {
        String sql = "SELECT durum FROM Masa WHERE masa_no = ?";
        
        try (Connection conn = VeritabaniBaglanti.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, masaNo);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("durum");
            }
        } catch (SQLException e) {
            System.err.println("✗ Masa durumu alınırken hata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return "Boş"; // Varsayılan durum
    }
    
    public void masaDurumGuncelle(int masaNo, String durum) {
        String sql = "UPDATE Masa SET durum = ? WHERE masa_no = ?";
        
        try (Connection conn = VeritabaniBaglanti.getConnection()) {
            if (conn == null) {
                throw new RuntimeException("Veritabanı bağlantısı alınamadı!");
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, durum);
                pstmt.setInt(2, masaNo);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Masa durumu güncellenirken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Masa durumu güncellenirken veritabanı hatası: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("✗ Masa durumu güncellenirken genel hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Masa durumu güncellenirken hata: " + e.getMessage(), e);
        }
    }
    
    public void siparisDurumGuncelle(int siparisId, String yeniDurum) {
        String sql = "UPDATE Siparis SET durum = ? WHERE siparis_id = ?";
        
        try (Connection conn = VeritabaniBaglanti.getConnection()) {
            if (conn == null) {
                throw new RuntimeException("Veritabanı bağlantısı alınamadı!");
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, yeniDurum);
                pstmt.setInt(2, siparisId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Sipariş durumu güncellenirken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Sipariş durumu güncellenirken veritabanı hatası: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("✗ Sipariş durumu güncellenirken genel hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Sipariş durumu güncellenirken hata: " + e.getMessage(), e);
        }
    }
    
    public Integer siparisMasaNoAl(int siparisId) {
        String sql = "SELECT masa_no FROM Siparis WHERE siparis_id = ?";
        
        try (Connection conn = VeritabaniBaglanti.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, siparisId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Object masaNoObj = rs.getObject("masa_no");
                return masaNoObj != null ? rs.getInt("masa_no") : null;
            }
        } catch (SQLException e) {
            System.err.println("✗ Sipariş masa numarası alınırken hata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public void siparisiTamamla(int siparisId) {
        siparisDurumGuncelle(siparisId, "Tamamlandı");
        
        // Eğer siparişin masası varsa, masayı boşalt
        Integer masaNo = siparisMasaNoAl(siparisId);
        if (masaNo != null) {
            masaDurumGuncelle(masaNo, "Boş");
        }
    }
    
    public void siparisiIptalEt(int siparisId) {
        siparisDurumGuncelle(siparisId, "İptal Edildi");
        
        // İptal edilen siparişin masası varsa, masayı boşalt
        Integer masaNo = siparisMasaNoAl(siparisId);
        if (masaNo != null) {
            masaDurumGuncelle(masaNo, "Boş");
        }
    }
    
    public double masaHesabiKapat(int masaNo) {
        // Masaya ait tüm aktif siparişleri bul
        String sql = "SELECT siparis_id, toplam_tutar FROM Siparis " +
                     "WHERE masa_no = ? " +
                     "AND (durum IS NULL OR (durum != 'Tamamlandı' AND durum != 'İptal Edildi'))";
        
        double toplamTutar = 0.0;
        List<Integer> siparisIdleri = new ArrayList<>();
        
        try (Connection conn = VeritabaniBaglanti.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, masaNo);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int siparisId = rs.getInt("siparis_id");
                double tutar = rs.getDouble("toplam_tutar");
                siparisIdleri.add(siparisId);
                toplamTutar += tutar;
            }
            
            // Tüm siparişleri tamamlandı olarak işaretle
            for (int siparisId : siparisIdleri) {
                siparisDurumGuncelle(siparisId, "Tamamlandı");
            }
            
            // Masayı boşalt
            masaDurumGuncelle(masaNo, "Boş");

        } catch (SQLException e) {
            System.err.println("✗ Masa hesabı kapatılırken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Masa hesabı kapatılırken veritabanı hatası: " + e.getMessage(), e);
        }
        
        return toplamTutar;
    }
    
    // İstatistik metodları
    public double getBugununCirosu() {
        String sql = "SELECT COALESCE(SUM(toplam_tutar), 0) as toplam " +
                     "FROM Siparis " +
                     "WHERE DATE(siparis_tarihi) = DATE('now') " +
                     "AND (durum IS NULL OR durum != 'İptal Edildi')";
        
        try (Connection conn = VeritabaniBaglanti.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getDouble("toplam");
            }
        } catch (SQLException e) {
            System.err.println("✗ Bugünkü ciro hesaplanırken hata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0.0;
    }
    
    public int getBugununSiparisSayisi() {
        String sql = "SELECT COUNT(*) as sayi " +
                     "FROM Siparis " +
                     "WHERE DATE(siparis_tarihi) = DATE('now') " +
                     "AND (durum IS NULL OR durum != 'İptal Edildi')";
        
        try (Connection conn = VeritabaniBaglanti.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("sayi");
            }
        } catch (SQLException e) {
            System.err.println("✗ Bugünkü sipariş sayısı hesaplanırken hata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    public int getAktifSiparisSayisi() {
        String sql = "SELECT COUNT(*) as sayi " +
                     "FROM Siparis " +
                     "WHERE (durum IS NULL OR (durum != 'Servis Edildi' AND durum != 'Tamamlandı' AND durum != 'İptal Edildi'))";
        
        try (Connection conn = VeritabaniBaglanti.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("sayi");
            }
        } catch (SQLException e) {
            System.err.println("✗ Aktif sipariş sayısı hesaplanırken hata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    public int getDoluMasaSayisi() {
        String sql = "SELECT COUNT(*) as sayi FROM Masa WHERE durum = 'Dolu'";
        
        try (Connection conn = VeritabaniBaglanti.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("sayi");
            }
        } catch (SQLException e) {
            System.err.println("✗ Dolu masa sayısı hesaplanırken hata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    public double getOrtalamaSiparisTutari() {
        String sql = "SELECT COALESCE(AVG(toplam_tutar), 0) as ortalama " +
                     "FROM Siparis " +
                     "WHERE DATE(siparis_tarihi) = DATE('now') " +
                     "AND (durum IS NULL OR durum != 'İptal Edildi')";
        
        try (Connection conn = VeritabaniBaglanti.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getDouble("ortalama");
            }
        } catch (SQLException e) {
            System.err.println("✗ Ortalama sipariş tutarı hesaplanırken hata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0.0;
    }

    // Sipariş bilgisi için iç sınıf
    public static class SiparisBilgisi {
        private int siparisId;
        private Integer masaNo;
        private String durum;
        private String urunler;
        private double toplamTutar;
        private int urunSayisi;

        public SiparisBilgisi(int siparisId, Integer masaNo, String durum, String urunler, double toplamTutar, int urunSayisi) {
            this.siparisId = siparisId;
            this.masaNo = masaNo;
            this.durum = durum;
            this.urunler = urunler;
            this.toplamTutar = toplamTutar;
            this.urunSayisi = urunSayisi;
        }

        public int getSiparisId() { return siparisId; }
        public Integer getMasaNo() { return masaNo; }
        public String getDurum() { return durum; }
        public String getUrunler() { return urunler; }
        public double getToplamTutar() { return toplamTutar; }
        public int getUrunSayisi() { return urunSayisi; }
    }
} 