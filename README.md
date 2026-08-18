# Akıllı Restoran Yönetim Sistemi

Restoran işletmeleri için masaüstü yönetim uygulaması, garson mobil uygulaması ve yapay zeka destekli duygu analizi.

## Özellikler

- Sipariş oluşturma, masa yönetimi ve hesap kapatma
- SQLite üzerinde menü, masa ve sipariş verileri
- Sepet bazlı ürün önerisi (Apriori)
- Kameradan canlı duygu analizi (Python + TensorFlow)
- Garson mobil uygulaması (Expo / React Native)

## Gereksinimler

- Java 24
- Maven 3.8+
- Python 3.9+
- Node.js 18+
- Kamera (duygu analizi için)
- Android emülatör veya Expo Go (mobil için)

## Proje Yapısı

```
ARS_2/
├── DesktopApp/.../AkilliRestoranSistemi/   # Java masaüstü uygulaması
├── Server/                                 # Duygu analizi soket sunucusu
│   └── ars_api/                            # Mobil için FastAPI
├── mobile/                                 # Garson uygulaması (Expo)
├── SiparisTahmin/                          # Öneri algoritması ve örnek veri
└── .env.example
```

## Kurulum

```bash
git clone <repo-url>
cd ARS_2
cp .env.example .env
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
pip install -r Server/ars_api/requirements.txt
cd mobile && npm install && cd ..
```

Eğitilmiş Keras model dosyasını (`final_stable_model.h5`) `Server/` dizinine koyun.

## Çalıştırma

### 1. Duygu analiz sunucusu

```bash
cd Server
python emotion_server2.py
```

### 2. Masaüstü uygulama

```bash
cd "DesktopApp/AkilliRestoranSistemi son hal/AkilliRestoranSistemi"
mvn compile exec:java
```

### 3. Mobil API (FastAPI)

```bash
cd Server
uvicorn ars_api.main:app --host 0.0.0.0 --port 8000
```

Swagger: http://127.0.0.1:8000/docs  
Garson PIN: `1234`

### 4. Mobil uygulama

```bash
cd mobile
npx expo start --android
```

| Ortam | API adresi |
|-------|------------|
| Android emülatör | `http://10.0.2.2:8000` |
| Gerçek telefon | `EXPO_PUBLIC_API_URL=http://<bilgisayar-ip>:8000 npx expo start --android` |

## Ortam Değişkenleri

| Değişken | Açıklama | Varsayılan |
|---|---|---|
| `EMOTION_HOST` | Python soket sunucusunun dinlediği adres | `0.0.0.0` |
| `EMOTION_SERVER_PORT` | Duygu analizi portu | `9999` |
| `EMOTION_SERVER_HOST` | Java istemcisinin bağlandığı adres | `localhost` |
| `EMOTION_MODEL_PATH` | Keras model dosyası | `final_stable_model.h5` |
| `DB_URL` | SQLite JDBC URL | `jdbc:sqlite:restoran.db` |
| `ARS_WAITER_PIN` | Mobil garson giriş PIN'i | `1234` |


Eksiksiz liste için `.env.example` dosyasına bakın.
