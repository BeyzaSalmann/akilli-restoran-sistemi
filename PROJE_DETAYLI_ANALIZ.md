# AKILLI RESTORAN YÖNETİM SİSTEMİ - DETAYLI PROJE ANALİZİ

## 1. PROJE GENEL BAKIŞ

### 1.1. Proje Tanımı
**Akıllı Restoran Yönetim Sistemi**, restoran işletmeleri için geliştirilmiş kapsamlı bir masaüstü uygulamasıdır. Sistem, yapay zeka destekli duygu analizi, makine öğrenmesi tabanlı ürün öneri sistemi ve gerçek zamanlı sipariş yönetimi özelliklerini bir araya getirir.

### 1.2. Proje Kapsamı
- **Masaüstü Uygulama**: Java Swing ile geliştirilmiş modern kullanıcı arayüzü
- **Yapay Zeka Entegrasyonu**: Python tabanlı gerçek zamanlı duygu analizi
- **Öneri Sistemi**: Apriori algoritması ile sepet bazlı ürün önerileri
- **Veritabanı Yönetimi**: SQLite ile ilişkisel veri saklama
- **Sipariş Yönetimi**: Tam kapsamlı sipariş oluşturma, takip ve yönetim
- **Masa Yönetimi**: Dinamik masa ekleme, durum takibi ve hesap kapatma

### 1.3. Teknoloji Stack
**Frontend:**
- Java 24
- Java Swing (GUI Framework)
- Maven (Build Tool)
- FlatLaf (Modern UI Library)
- SwingX (Gelişmiş Swing Bileşenleri)

**Backend:**
- Python 3.x
- TensorFlow/Keras (Deep Learning)
- OpenCV (Computer Vision)
- NumPy (Numerical Computing)

**Veritabanı:**
- SQLite (Embedded Database)
- JDBC (Java Database Connectivity)

**İletişim:**
- TCP/IP Socket Programming
- JSON (Veri Formatı)
- Binary Image Streaming

**Algoritmalar:**
- Apriori Algorithm (Association Rule Mining)
- Convolutional Neural Network (CNN) - Duygu Analizi
- Haar Cascade (Yüz Tespiti)

---

## 2. SİSTEM MİMARİSİ

### 2.1. Genel Mimari
Sistem **3 katmanlı mimari** yapısına sahiptir:

```
┌─────────────────────────────────────────────────┐
│         PRESENTATION LAYER (Java Swing)         │
│  - AnaPanel (JFrame)                            │
│  - PanelGenelBakis                              │
│  - PanelDuyguAnalizi                            │
│  - PanelAktifSiparisler                         │
│  - PanelSiparisGirisi                           │
└─────────────────────────────────────────────────┘
                    ↕
┌─────────────────────────────────────────────────┐
│         BUSINESS LOGIC LAYER (Java Services)    │
│  - VeritabaniServisi                            │
│  - RestoranOneriSistemi (Apriori)               │
│  - DuyguAnaliziServisi                          │
│  - OneriSistemi                                 │
└─────────────────────────────────────────────────┘
         ↕                    ↕
┌──────────────────┐  ┌──────────────────────┐
│   DATA LAYER      │  │   AI SERVER LAYER    │
│   (SQLite)        │  │   (Python)           │
│   - 5 Tablo       │  │   - TensorFlow Model  │
│   - CRUD Ops      │  │   - OpenCV           │
└──────────────────┘  └──────────────────────┘
```

### 2.2. Bileşenler Arası İletişim

**Java ↔ SQLite:**
- JDBC bağlantısı (Singleton Pattern)
- PreparedStatement ile güvenli sorgular
- Transaction yönetimi

**Java ↔ Python:**
- TCP/IP Socket (Port 9999)
- Protokol: [JSON_SIZE (4 byte)] + [JSON] + [IMAGE_SIZE (4 byte)] + [JPEG_IMAGE]
- Big-endian byte order
- Gerçek zamanlı frame streaming

**Veri Akışı:**
```
Kamera → Python Server → Socket → Java Client → UI Update
CSV Data → Apriori Algorithm → Rules → Java Service → UI
User Input → Java UI → Database → Business Logic → Response
```

### 2.3. Design Patterns Kullanımı

1. **Singleton Pattern**: `VeritabaniBaglanti` - Tek veritabanı bağlantısı
2. **MVC Pattern**: Model-View-Controller benzeri yapı
3. **Factory Pattern**: `createModernButton()` - Buton oluşturma
4. **Observer Pattern**: Swing event listeners

---

## 3. VERİTABANI YAPISI

### 3.1. Tablo Şeması

**1. Musteri Tablosu:**
```sql
CREATE TABLE Musteri (
    musteriID INTEGER PRIMARY KEY AUTOINCREMENT,
    duyguDurumu TEXT NOT NULL,
    siparisZamani DATETIME DEFAULT CURRENT_TIMESTAMP
);
```
- Müşteri duygu durumu kayıtları
- Sipariş zamanı takibi

**2. Menu Tablosu:**
```sql
CREATE TABLE Menu (
    menuID INTEGER PRIMARY KEY AUTOINCREMENT,
    duyguKategorisi TEXT NOT NULL,
    urunAdi TEXT NOT NULL,
    fiyat REAL NOT NULL
);
```
- Duygu bazlı menü önerileri
- Kategori: mutlu, notr, uzgun

**3. Urun Tablosu:**
```sql
CREATE TABLE Urun (
    urun_id INTEGER PRIMARY KEY AUTOINCREMENT,
    urun_adi TEXT NOT NULL,
    kategori TEXT NOT NULL,
    fiyat REAL NOT NULL
);
```
- Ana ürün kataloğu
- Kategoriler: Ana Yemek, Salata, İçecek, Tatlı, Atıştırmalık
- CSV'den import edilen ürünler

**4. Masa Tablosu:**
```sql
CREATE TABLE Masa (
    masa_id INTEGER PRIMARY KEY,
    masa_no INTEGER UNIQUE NOT NULL,
    durum TEXT DEFAULT 'Boş',
    olusturma_tarihi DATETIME DEFAULT CURRENT_TIMESTAMP
);
```
- Masa yönetimi
- Durumlar: Boş, Dolu
- Dinamik masa ekleme (1-50 arası)

**5. Siparis Tablosu:**
```sql
CREATE TABLE Siparis (
    siparis_id INTEGER PRIMARY KEY AUTOINCREMENT,
    siparis_tarihi DATETIME DEFAULT CURRENT_TIMESTAMP,
    toplam_tutar REAL NOT NULL,
    masa_no INTEGER,
    durum TEXT DEFAULT 'Hazırlanıyor'
);
```
- Ana sipariş kayıtları
- Durumlar: Hazırlanıyor, Servis Edildi, Tamamlandı, İptal Edildi
- Otomatik tarih kaydı

**6. SiparisDetay Tablosu:**
```sql
CREATE TABLE SiparisDetay (
    detay_id INTEGER PRIMARY KEY AUTOINCREMENT,
    siparis_id INTEGER NOT NULL,
    urun_id INTEGER NOT NULL,
    adet INTEGER NOT NULL,
    birim_fiyat REAL NOT NULL,
    toplam_fiyat REAL NOT NULL,
    FOREIGN KEY (siparis_id) REFERENCES Siparis(siparis_id),
    FOREIGN KEY (urun_id) REFERENCES Urun(urun_id)
);
```
- Sipariş detayları (ürün bazlı)
- Foreign key ilişkileri
- Fiyat bilgisi korunur (tarihsel veri)

### 3.2. İlişkisel Yapı
- **Siparis** → **SiparisDetay** (1-N)
- **SiparisDetay** → **Urun** (N-1)
- **Siparis** → **Masa** (N-1, nullable)

### 3.3. Veri İşlemleri
- **CRUD Operations**: Tüm tablolar için tam CRUD desteği
- **Transaction Management**: Sipariş kayıtlarında transaction kullanımı
- **Data Validation**: Foreign key constraints, unique constraints
- **Auto-increment**: Primary key'ler otomatik artırılır

---

## 4. KULLANICI ARAYÜZÜ (UI)

### 4.1. Ana Pencere (AnaPanel)
- **Boyut**: 1200x800 piksel
- **Başlık**: "Akıllı Restoran Uygulaması"
- **Yapı**: JTabbedPane ile 4 sekme
- **İkon**: Programatik olarak çizilmiş restoran ikonu (64x64)

### 4.2. Panel 1: Genel Bakış (PanelGenelBakis)

**Özellikler:**
- **8 İstatistik Kartı** (2x4 Grid Layout):
  1. Bugünkü Ciro (Yeşil tema)
  2. Bugünkü Sipariş Sayısı (Mavi tema)
  3. Aktif Siparişler (Sarı tema)
  4. Ortalama Sipariş Tutarı (Kırmızı tema)
  5. Dolu Masalar (Mor tema)
  6. Toplam Masalar (Turkuaz tema)
  7. Menü Ürünleri (Turuncu tema)
  8. Sistem Durumu (Pembe tema)

- **Hızlı Erişim Butonu**: "⚡ HIZLI SİPARİŞ" - Sipariş Girişi sekmesine yönlendirme
- **Renk Kodlaması**: Her kart için özel renk paleti
- **Gradient Arka Planlar**: Modern görsel tasarım
- **Gerçek Zamanlı Veri**: Veritabanından anlık istatistik çekme

**İstatistik Metodları:**
- `getBugununCirosu()`: Bugünkü toplam ciro
- `getBugununSiparisSayisi()`: Bugünkü sipariş sayısı
- `getAktifSiparisSayisi()`: Hazırlanan siparişler
- `getDoluMasaSayisi()`: Dolu masa sayısı
- `getOrtalamaSiparisTutari()`: Ortalama sipariş tutarı

### 4.3. Panel 2: Duygu Analizi (PanelDuyguAnalizi)

**Özellikler:**
- **Gerçek Zamanlı Kamera Akışı**: Python sunucusundan gelen görüntüler
- **Duygu Göstergesi**: Anlık tespit edilen duygu durumu
- **Socket Bağlantısı**: localhost:9999
- **Otomatik Yeniden Bağlanma**: Bağlantı koparsa 2 saniye sonra tekrar dener
- **Görsel Geri Bildirim**: Duyguya göre renk değişimi
  - Mutlu: Yeşil (#28a745)
  - Kızgın: Kırmızı (#dc3545)
  - Diğer: Mavi (#007bff)

**Teknik Detaylar:**
- **Thread Yönetimi**: Daemon thread ile arka plan işleme
- **Swing Thread Safety**: `SwingUtilities.invokeLater()` kullanımı
- **Image Processing**: JPEG decode, BufferedImage oluşturma
- **JSON Parsing**: Basit string parsing (org.json alternatifi)

**Protokol Detayları:**
```
1. JSON boyutu okunur (4 byte, big-endian)
2. JSON verisi okunur (UTF-8)
3. Resim boyutu okunur (4 byte, big-endian)
4. JPEG resim verisi okunur
5. UI güncellenir
```

### 4.4. Panel 3: Aktif Siparişler (PanelAktifSiparisler)

**Özellikler:**
- **Sipariş Kartları**: Her sipariş için modern kart tasarımı
- **Durum Yönetimi**: Hazırlanıyor → Servis Edildi akışı
- **Ürün Listesi**: Her siparişteki ürünler ve adetleri
- **Toplam Tutar**: Her siparişin toplam fiyatı
- **Aksiyon Butonları**:
  - ✅ Tamamla (Yeşil, 140px)
  - ❌ İptal (Kırmızı, 120px)
  - 💡 Tavsiye (Mavi, 130px)
  - Durum Değiştir (Duruma göre renk)

**Sipariş Kartı Tasarımı:**
- Gradient arka plan
- Sol kenar vurgusu (durum rengi)
- Yuvarlatılmış köşeler (12px radius)
- Responsive layout

**Öneri Sistemi Entegrasyonu:**
- Her sipariş kartında "💡 Tavsiye" butonu
- Apriori algoritması ile ürün önerisi
- Güven skoru gösterimi
- HTML formatlı mesaj kutusu

**Özellikler:**
- **Otomatik Sipariş Birleştirme**: Aynı masaya ek siparişler otomatik birleşir
- **Durum Geçişleri**: Hazırlanıyor → Servis Edildi
- **Masa Bilgisi**: Her siparişte masa numarası gösterimi
- **Yenile Butonu**: Manuel yenileme

### 4.5. Panel 4: Sipariş Girişi (PanelSiparisGirisi)

**Özellikler:**
- **2 Panel Layout**: Ürün Listesi (Sol) + Sepet (Sağ)
- **Ürün Tablosu**: Tüm ürünler listelenir
- **Sepet Sistemi**: Dinamik sepet yönetimi
- **Masa Seçimi**: Dropdown liste + Yeni Masa butonu
- **Toplam Tutar**: Gerçek zamanlı hesaplama

**Ürün Listesi Paneli:**
- JTable ile ürün gösterimi
- Sütunlar: Ürün ID, Ürün Adı, Kategori, Fiyat
- Miktar Spinner (1-20 arası)
- "➕ Sepete Ekle" butonu
- "➕ Ürün Ekle" butonu (Dialog ile yeni ürün ekleme)

**Sepet Paneli:**
- JTable ile sepet gösterimi
- Sütunlar: Ürün Adı, Adet, Birim Fiyat, Toplam
- "🗑️ Sepeti Temizle" butonu
- "❌ Seçili Ürünü Sil" butonu
- Toplam tutar gösterimi

**Masa Yönetimi:**
- ComboBox ile masa seçimi
- "Masa Seçiniz..." placeholder
- "➕ Yeni Masa" butonu (1-50 arası)
- Otomatik masa ekleme (sipariş kaydedilirken)

**Sipariş Kaydetme:**
- Validasyon: Sepet boş olamaz, masa seçilmeli
- Onay dialogu: Kullanıcı onayı
- Otomatik sipariş birleştirme: Aynı masada aktif sipariş varsa birleştir
- Başarı mesajı: Mevcut siparişe eklendi / Yeni sipariş oluşturuldu

**Ürün Ekleme Dialogu:**
- Ürün Adı (TextField)
- Kategori (ComboBox: Ana Yemek, Salata, İçecek, Tatlı, Atıştırmalık)
- Fiyat (TextField, 0-10000 $ arası)
- Validasyon: Boş alan kontrolü, fiyat formatı kontrolü

### 4.6. UI Tasarım Prensipleri

**Renk Paleti:**
- Ana Renkler:
  - Başarı: #28a745 (Yeşil)
  - Bilgi: #007bff (Mavi)
  - Uyarı: #ffc107 (Sarı)
  - Hata: #dc3545 (Kırmızı)
  - İkincil: #6c757d (Gri)

**Tipografi:**
- Font: Segoe UI
- Başlıklar: BOLD, 26-28px
- Butonlar: BOLD, 14px
- Metinler: PLAIN, 12-16px

**Buton Tasarımı:**
- Yuvarlatılmış köşeler: 10px radius
- Hover efekti: Renk parlaklaşır
- Press efekti: Renk koyulaşır
- Minimum genişlik: 120-130px
- Padding: 30-40px
- Cursor: HAND_CURSOR

**Layout Yönetimi:**
- BorderLayout: Ana paneller
- GridLayout: İstatistik kartları
- FlowLayout: Buton grupları
- BoxLayout: Dikey listeler
- GridBagLayout: Özel yerleşimler

---

## 5. İŞ MANTIĞI (BUSINESS LOGIC)

### 5.1. VeritabaniServisi

**Sorumluluklar:**
- Tüm veritabanı işlemleri
- CRUD operasyonları
- İstatistik hesaplamaları
- Transaction yönetimi

**Ana Metodlar:**

**Ürün Yönetimi:**
- `getTumUrunler()`: Tüm ürünleri listeler
- `urunEkle(String ad, String kategori, double fiyat)`: Yeni ürün ekler
- `urunSil(int urunId)`: Ürün siler

**Sipariş Yönetimi:**
- `siparisKaydet(List<SiparisDetay>, Integer masaNo)`: Sipariş kaydeder
  - **Akıllı Birleştirme**: Aynı masada aktif sipariş varsa birleştirir
  - **Return**: boolean (mevcut siparişe eklendi mi?)
- `getAktifSiparisler()`: Aktif siparişleri listeler
- `siparisDurumunuGuncelle(int siparisId, String yeniDurum)`: Durum günceller
- `siparisiTamamla(int siparisId)`: Siparişi tamamlar, masayı boşaltır
- `siparisiIptalEt(int siparisId)`: Siparişi iptal eder

**Masa Yönetimi:**
- `getMasaListesi()`: Tüm masaları listeler
- `masaEkle(int masaNo)`: Yeni masa ekler (1-50 arası)
- `masaSil(int masaNo)`: Masayı siler (aktif sipariş kontrolü ile)
- `masaDurumGuncelle(int masaNo, String durum)`: Masa durumunu günceller
- `masaAktifSiparisVarMi(int masaNo)`: Aktif sipariş kontrolü

**İstatistikler:**
- `getBugununCirosu()`: Bugünkü toplam ciro
- `getBugununSiparisSayisi()`: Bugünkü sipariş sayısı
- `getAktifSiparisSayisi()`: Hazırlanan sipariş sayısı
- `getDoluMasaSayisi()`: Dolu masa sayısı
- `getOrtalamaSiparisTutari()`: Ortalama sipariş tutarı

**Özel Özellikler:**
- **Otomatik Sipariş Birleştirme**: Aynı masaya yeni sipariş eklendiğinde mevcut aktif siparişe eklenir
- **Masa Durum Yönetimi**: Sipariş verildiğinde masa otomatik "Dolu" olur
- **Foreign Key Kontrolü**: Sipariş detaylarında ürün kontrolü

### 5.2. RestoranOneriSistemi (Apriori Algorithm)

**Algoritma Açıklaması:**
Apriori algoritması, market basket analysis (sepet analizi) için kullanılan bir association rule mining algoritmasıdır. Sistemde, geçmiş sipariş verilerinden ürün birlikteliklerini öğrenir ve yeni siparişler için öneri üretir.

**Veri Yapısı:**
- `transactions`: List<Set<String>> - Her sipariş bir Set (benzersiz ürünler)
- `rules`: List<Rule> - Çıkarılan kurallar
- `Rule`: antecedent (Set<String>), consequent (String), confidence (double)

**Algoritma Adımları:**

**1. Veri Yükleme (`veriYukleVeHazirla()`):**
- `menu_items.csv` okunur → menuMap oluşturulur
- `order_details.csv` okunur → ordersMap oluşturulur
- Her sipariş Set'e dönüştürülür (benzersizlik için)
- BOM (Byte Order Mark) karakteri temizlenir
- Dinamik header parsing

**2. Model Eğitimi (`modeliEgit(double minSupport, double minConfidence)`):**
- **minSupport**: Minimum destek oranı (örn: 0.002 = %0.2)
- **minConfidence**: Minimum güven oranı (örn: 0.1 = %10)

**Adım A: Tekli Ürün Frekansları**
- Her ürünün kaç siparişte geçtiği sayılır
- minSupport eşiğini geçenler seçilir

**Adım B: Çoklu Kombinasyonlar (k=2, k=3)**
- Join Step: Frekanslı itemset'ler birleştirilir
- Prune Step: Eşiği geçmeyenler elenir
- Maksimum 3'lü kombinasyonlar

**Adım C: Kural Oluşturma**
- Her itemset için alt kümeler bulunur
- Antecedent → Consequent kuralları oluşturulur
- Confidence hesaplanır: P(consequent | antecedent)
- minConfidence eşiğini geçenler saklanır
- Güvene göre sıralanır (büyükten küçüğe)

**3. Tavsiye Üretme (`tavsiyeAl(List<String> sepetListesi)`):**

**3 Seviyeli Eşleştirme Stratejisi:**

**Seviye 1: Tam Eşleşme**
- Sepet tam olarak bir kuralın antecedent'ine eşit mi?
- En yüksek güvenli kural seçilir

**Seviye 2: Alt Küme Eşleşmesi**
- Sepetin alt kümeleri kurallarla eşleşiyor mu?
- Güven %20 azaltılır (adjustedConfidence = confidence * 0.8)

**Seviye 3: Kısmi Eşleşme**
- En az bir ürün ortak mı?
- Ortak oran hesaplanır
- Güven: confidence * ortakOran * 0.6

**Minimum Güven Eşiği**: %5 (0.05)

**Return**: `TavsiyeSonucu` (onerilenUrun, guven)

**Örnek Kural:**
```
Antecedent: {Hamburger, French Fries}
Consequent: Cola
Confidence: 0.45 (%45)
```

**Performans:**
- Arka plan thread'de çalışır
- İlk yükleme: ~2-3 saniye
- Tavsiye alma: <100ms

### 5.3. Duygu Analizi Servisi

**Mevcut Durum:**
- Simülasyon modu (random mutluluk oranı)
- Python entegrasyonu hazır (PanelDuyguAnalizi'nde)

**Python Sunucu Entegrasyonu:**
- Socket bağlantısı: localhost:9999
- Gerçek zamanlı frame alımı
- JSON metadata parsing
- UI güncelleme

### 5.4. OneriSistemi

**Duygu Bazlı Öneriler:**
- Mutluluk oranı > 70: "mutlu" kategorisi
- Mutluluk oranı 30-70: "notr" kategorisi
- Mutluluk oranı < 30: "uzgun" kategorisi
- Menu tablosundan kategoriye göre öneri

---

## 6. PYTHON SUNUCU (DUYGU ANALİZİ)

### 6.1. emotion_server.py

**Özellikler:**
- **Model**: `final_stable_model.h5` (TensorFlow/Keras)
- **Sınıflar**: 7 duygu sınıfı
  - Kizgin (Angry)
  - Kucumseme (Contempt)
  - Tiksinme (Disgust)
  - Korku (Fear)
  - Mutlu (Happy)
  - Uzgun (Sad)
  - Saskin (Surprise)

**İşlem Akışı:**
1. Model yüklenir
2. Socket server başlatılır (0.0.0.0:9999)
3. Java bağlantısı beklenir
4. Kamera açılır (VideoCapture(0))
5. Her frame için:
   - Yüz tespiti (Haar Cascade)
   - ROI çıkarımı (48x48 resize)
   - Normalizasyon (0-1 arası)
   - Model tahmini
   - En yüksek confidence seçilir
   - JSON metadata oluşturulur
   - JPEG encode
   - Socket'e gönderilir

**Protokol:**
```
[JSON_SIZE (4 byte, big-endian)] + 
[JSON (UTF-8)] + 
[IMAGE_SIZE (4 byte, big-endian)] + 
[JPEG_IMAGE (binary)]
```

**JSON Formatı:**
```json
{
    "count": 1,
    "primary_emotion": "Mutlu",
    "details": [
        {
            "emotion": "Mutlu",
            "confidence": 85.3,
            "box": [100, 150, 200, 200]
        }
    ]
}
```

**Görsel İşleme:**
- Yüz çerçeveleme (mavi dikdörtgen)
- Duygu etiketi (yeşil/kırmızı)
- Confidence gösterimi

---

## 7. VERİ SETLERİ VE VERİ İŞLEME

### 7.1. CSV Dosyaları

**menu_items.csv:**
- Sütunlar: menu_item_id, item_name, category, price
- Ürün kataloğu
- Import script: `import_menu.py`

**order_details.csv:**
- Sütunlar: order_id, item_id, quantity
- Geçmiş sipariş verileri
- Apriori algoritması için kullanılır

### 7.2. Veri İşleme Scriptleri

**import_menu.py:**
- CSV'den ürünleri okur
- Veritabanına ekler
- Duplicate kontrolü yapar

**convert_prices.py:**
- Fiyat dönüşümleri (TL ↔ USD)

**fix_product_ids.py:**
- Ürün ID'lerini yeniden indeksler (1'den başlar)
- Foreign key referanslarını günceller

### 7.3. Veri Temizleme
- BOM karakteri temizleme
- Null değer kontrolü
- Tip dönüşümleri (float → int)
- Duplicate kontrolü

---

## 8. KULLANICI DENEYİMİ (UX)

### 8.1. Navigasyon
- **Sekmeli Arayüz**: 4 ana sekme
- **Hızlı Erişim**: Genel Bakış'tan Sipariş Girişi'ne direkt geçiş
- **Breadcrumb**: Her panelde başlık gösterimi

### 8.2. Geri Bildirimler
- **Başarı Mesajları**: Yeşil, bilgilendirici
- **Hata Mesajları**: Kırmızı, açıklayıcı
- **Uyarı Mesajları**: Sarı, dikkat çekici
- **Onay Dialogları**: Kritik işlemler için

### 8.3. Validasyon
- **Sepet Kontrolü**: Boş sepet ile sipariş kaydedilemez
- **Masa Seçimi**: Masa seçilmeden sipariş kaydedilemez
- **Fiyat Kontrolü**: 0-10000 $ arası
- **Masa Numarası**: 1-50 arası
- **Miktar**: 1-20 arası

### 8.4. Erişilebilirlik
- **Türkçe Arayüz**: Tüm metinler Türkçe
- **Buton Metinleri**: Türkçeleştirilmiş (Evet, Hayır, Tamam, İptal)
- **Renk Kodlaması**: Görsel ipuçları
- **Büyük Butonlar**: Kolay tıklama

---

## 9. PERFORMANS VE OPTİMİZASYON

### 9.1. Veritabanı Optimizasyonu
- **Singleton Connection**: Tek bağlantı kullanımı
- **PreparedStatement**: SQL injection koruması + performans
- **Index Kullanımı**: Primary key'ler otomatik indexlenir
- **Transaction Yönetimi**: Kritik işlemlerde transaction

### 9.2. Thread Yönetimi
- **Swing Thread**: UI güncellemeleri EDT'de
- **Background Threads**: 
  - Apriori eğitimi
  - Socket bağlantısı
  - Veri yükleme
- **Daemon Threads**: Uygulama kapanınca otomatik sonlanır

### 9.3. Bellek Yönetimi
- **Connection Pooling**: Tek bağlantı, yeniden kullanım
- **Image Buffering**: Sadece son frame saklanır
- **List Management**: ArrayList kullanımı (hızlı erişim)

### 9.4. Algoritma Optimizasyonu
- **Apriori**: Maksimum 3'lü kombinasyonlar (performans için)
- **Subset Generation**: Bit manipulation ile hızlı alt küme üretimi
- **Rule Caching**: Kurallar hafızada tutulur

---

## 10. HATA YÖNETİMİ VE GÜVENLİK

### 10.1. Exception Handling
- **Try-Catch Blokları**: Tüm kritik işlemlerde
- **SQLException**: Veritabanı hataları
- **IOException**: Dosya okuma hataları
- **NumberFormatException**: Sayı dönüşüm hataları
- **NullPointerException**: Null kontrolü

### 10.2. Kullanıcı Dostu Hata Mesajları
- Türkçe hata mesajları
- Açıklayıcı bilgilendirmeler
- Çözüm önerileri

### 10.3. Güvenlik
- **PreparedStatement**: SQL injection koruması
- **Input Validation**: Kullanıcı girdileri kontrol edilir
- **Connection Security**: Local SQLite (güvenli)

---

## 11. ÖZEL ÖZELLİKLER VE YENİLİKLER

### 11.1. Akıllı Sipariş Birleştirme
- Aynı masaya yeni sipariş eklendiğinde otomatik birleşir
- Toplam tutar otomatik güncellenir
- Kullanıcıya bilgilendirme mesajı

### 11.2. Esnek Öneri Sistemi
- 3 seviyeli eşleştirme (Tam → Alt Küme → Kısmi)
- Minimum güven eşiği (%5)
- Güven skoru gösterimi

### 11.3. Dinamik Masa Yönetimi
- Runtime'da masa ekleme (1-50)
- Otomatik masa durumu güncelleme
- Masa silme (aktif sipariş kontrolü ile)

### 11.4. Gerçek Zamanlı Güncellemeler
- Socket ile canlı görüntü akışı
- Anlık duygu analizi
- Otomatik UI yenileme

### 11.5. Modern UI Tasarımı
- Gradient arka planlar
- Yuvarlatılmış köşeler
- Renk kodlaması
- Responsive layout

---

## 12. KOD İSTATİSTİKLERİ

- **Toplam Java Dosyası**: 15
- **Toplam Kod Satırı**: ~3,887 satır
- **Model Sınıfları**: 2 (Urun, SiparisDetay)
- **View Sınıfları**: 5 (AnaPanel + 4 Panel)
- **Service Sınıfları**: 5
- **Util Sınıfları**: 1
- **Controller Sınıfları**: 1

**Dosya Dağılımı:**
- View: ~2,000 satır
- Service: ~1,500 satır
- Model: ~100 satır
- Util: ~100 satır
- Controller: ~50 satır

---

## 13. BAĞIMLILIKLAR VE KÜTÜPHANELER

### 13.1. Maven Dependencies (pom.xml)

**SQLite JDBC:**
- GroupId: org.xerial
- ArtifactId: sqlite-jdbc
- Version: 3.46.0.0

**FlatLaf (Modern UI):**
- GroupId: com.formdev
- ArtifactId: flatlaf
- Version: 3.4.1

**SwingX:**
- GroupId: org.swinglabs.swingx
- ArtifactId: swingx-all
- Version: 1.6.5-1

**JSON:**
- GroupId: org.json
- ArtifactId: json
- Version: 20231013

**Webcam Capture:**
- GroupId: com.github.sarxos
- ArtifactId: webcam-capture
- Version: 0.3.12

### 13.2. Python Dependencies

**Gerekli Kütüphaneler:**
- tensorflow (Deep Learning)
- keras (Model API)
- opencv-python (Computer Vision)
- numpy (Numerical Computing)
- json (Built-in)

---

## 14. KULLANIM SENARYOLARI

### 14.1. Senaryo 1: Yeni Sipariş Oluşturma
1. Sipariş Girişi sekmesine geç
2. Ürün listesinden ürün seç
3. Miktar belirle
4. "Sepete Ekle" butonuna tıkla
5. Masa seç (veya yeni masa ekle)
6. "SİPARİŞİ KAYDET" butonuna tıkla
7. Onay ver
8. Başarı mesajı görüntülenir

### 14.2. Senaryo 2: Siparişe Ürün Ekleme
1. Aynı masaya yeni sipariş oluştur
2. Sistem otomatik olarak mevcut siparişe ekler
3. "Mevcut siparişe eklendi" mesajı gösterilir
4. Toplam tutar otomatik güncellenir

### 14.3. Senaryo 3: Ürün Önerisi Alma
1. Aktif Siparişler sekmesine geç
2. Bir sipariş kartında "💡 Tavsiye" butonuna tıkla
3. Sistem Apriori algoritması ile öneri üretir
4. Önerilen ürün ve güven skoru gösterilir

### 14.4. Senaryo 4: Duygu Analizi
1. Python sunucusunu başlat
2. Duygu Analizi sekmesine geç
3. Java otomatik olarak Python'a bağlanır
4. Kamera görüntüsü görüntülenir
5. Anlık duygu durumu gösterilir

### 14.5. Senaryo 5: Sipariş Durumu Güncelleme
1. Aktif Siparişler sekmesinde durum butonuna tıkla
2. Durum: Hazırlanıyor → Servis Edildi
3. "Tamamla" butonu ile sipariş tamamlanır
4. Masa otomatik boşaltılır

---

## 15. TEKNİK ZORLUKLAR VE ÇÖZÜMLER

### 15.1. Socket İletişimi
**Sorun**: Java ve Python arasında binary veri transferi
**Çözüm**: 
- Struct packing (big-endian)
- JSON + JPEG ayrımı
- Byte array yönetimi

### 15.2. Thread Safety
**Sorun**: Swing thread'de socket işlemleri
**Çözüm**: 
- SwingUtilities.invokeLater()
- Daemon threads
- Thread-safe değişkenler

### 15.3. Apriori Performansı
**Sorun**: Büyük veri setlerinde yavaşlık
**Çözüm**: 
- Maksimum 3'lü kombinasyonlar
- Minimum support/confidence eşikleri
- Arka plan thread'de eğitim

### 15.4. Veritabanı Bağlantı Yönetimi
**Sorun**: Çoklu bağlantı, kaynak tüketimi
**Çözüm**: 
- Singleton pattern
- Connection pooling benzeri yapı
- Otomatik yeniden bağlanma

### 15.5. UI Responsiveness
**Sorun**: Uzun süren işlemlerde UI donması
**Çözüm**: 
- Background threads
- Progress indicators
- Asenkron işlemler

---

## 16. GELECEKTEKİ GELİŞTİRMELER

### 16.1. Önerilen Özellikler
- **Web Arayüzü**: Spring Boot ile REST API
- **Mobil Uygulama**: Android/iOS
- **Gelişmiş Raporlama**: Grafikler, trend analizi
- **Çoklu Restoran Desteği**: Multi-tenant yapı
- **Cloud Deployment**: AWS/Azure entegrasyonu
- **Real-time Notifications**: WebSocket ile bildirimler

### 16.2. Teknik İyileştirmeler
- **Caching**: Redis ile önbellekleme
- **Load Balancing**: Çoklu Python sunucu
- **Database Migration**: Flyway/Liquibase
- **Unit Testing**: JUnit test coverage
- **CI/CD Pipeline**: Otomatik build ve deploy

---

## 17. PROJE BAŞARILARI VE METRİKLER

### 17.1. Teknik Başarılar
- ✅ Tam entegre sistem (Java + Python)
- ✅ Gerçek zamanlı işleme
- ✅ AI/ML entegrasyonu
- ✅ Modern ve kullanıcı dostu arayüz
- ✅ Ölçeklenebilir mimari
- ✅ Temiz kod yapısı

### 17.2. Kod Kalitesi
- MVC pattern uygulaması
- Singleton pattern (veritabanı)
- Exception handling
- Input validation
- Türkçe arayüz

### 17.3. Kullanıcı Deneyimi
- Sezgisel navigasyon
- Hızlı işlem akışı
- Görsel geri bildirimler
- Hata yönetimi

---

## 18. SONUÇ

**Akıllı Restoran Yönetim Sistemi**, modern yazılım geliştirme prensipleri, yapay zeka teknolojileri ve kullanıcı odaklı tasarımı bir araya getiren kapsamlı bir projedir. Sistem, restoran işletmelerinin günlük operasyonlarını optimize etmek, müşteri deneyimini iyileştirmek ve veri odaklı karar vermeyi desteklemek için tasarlanmıştır.

**Proje Öne Çıkan Özellikler:**
1. **AI/ML Entegrasyonu**: Gerçek zamanlı duygu analizi
2. **Akıllı Öneri Sistemi**: Apriori algoritması ile ürün önerileri
3. **Otomatik İş Akışları**: Sipariş birleştirme, masa yönetimi
4. **Modern UI/UX**: Kullanıcı dostu, görsel olarak çekici arayüz
5. **Ölçeklenebilir Mimari**: Gelecekteki geliştirmelere açık yapı

Bu proje, yazılım mühendisliği, yapay zeka, veri bilimi ve kullanıcı deneyimi tasarımı alanlarında kapsamlı bir uygulama örneği sunmaktadır.



