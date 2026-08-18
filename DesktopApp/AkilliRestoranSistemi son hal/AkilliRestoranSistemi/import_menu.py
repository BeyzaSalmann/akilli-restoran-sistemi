#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Menü verilerini CSV dosyasından SQLite veritabanına aktaran script
"""

import sqlite3
import csv
import os

# Dosya yolları
DB_PATH = "restoran.db"
CSV_PATH = "menu_items.csv"

def import_menu_items():
    """CSV dosyasındaki menü öğelerini veritabanına aktarır"""
    
    # Veritabanı bağlantısı
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # Urun tablosunu oluştur (yoksa)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS Urun (
            urun_id INTEGER PRIMARY KEY AUTOINCREMENT,
            urun_adi TEXT NOT NULL,
            kategori TEXT NOT NULL,
            fiyat REAL NOT NULL
        )
    """)
    
    # Mevcut ürünleri kontrol et
    cursor.execute("SELECT COUNT(*) FROM Urun")
    mevcut_sayisi = cursor.fetchone()[0]
    print(f"📊 Mevcut ürün sayısı: {mevcut_sayisi}")
    
    # CSV dosyasını oku
    if not os.path.exists(CSV_PATH):
        print(f"❌ Hata: {CSV_PATH} dosyası bulunamadı!")
        return
    
    eklenen = 0
    guncellenen = 0
    atlanan = 0
    
    with open(CSV_PATH, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        
        for row in reader:
            item_name = row['item_name'].strip()
            category = row['category'].strip()
            
            # Fiyatı temizle ve float'a çevir
            try:
                price = float(row['price'].strip())
            except ValueError:
                print(f"⚠️  Geçersiz fiyat: {item_name} - {row['price']}")
                atlanan += 1
                continue
            
            # Ürün zaten var mı kontrol et (isim ve kategoriye göre)
            cursor.execute("""
                SELECT urun_id FROM Urun 
                WHERE urun_adi = ? AND kategori = ?
            """, (item_name, category))
            
            existing = cursor.fetchone()
            
            if existing:
                # Varsa fiyatı güncelle
                cursor.execute("""
                    UPDATE Urun SET fiyat = ? 
                    WHERE urun_adi = ? AND kategori = ?
                """, (price, item_name, category))
                guncellenen += 1
                print(f"🔄 Güncellendi: {item_name} ({category}) - {price} ₺")
            else:
                # Yoksa ekle
                cursor.execute("""
                    INSERT INTO Urun (urun_adi, kategori, fiyat) 
                    VALUES (?, ?, ?)
                """, (item_name, category, price))
                eklenen += 1
                print(f"✅ Eklendi: {item_name} ({category}) - {price} ₺")
    
    conn.commit()
    conn.close()
    
    print("\n" + "="*50)
    print("📈 İŞLEM ÖZETİ")
    print("="*50)
    print(f"✅ Yeni eklenen: {eklenen}")
    print(f"🔄 Güncellenen: {guncellenen}")
    print(f"⏭️  Atlanan: {atlanan}")
    print(f"📊 Toplam işlem: {eklenen + guncellenen}")
    print("="*50)

if __name__ == "__main__":
    print("🚀 Menü verileri aktarılıyor...\n")
    import_menu_items()
    print("\n✨ İşlem tamamlandı!")

