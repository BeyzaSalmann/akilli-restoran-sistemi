#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Ürün ID'lerini 1'den başlayacak şekilde güncelleyen script
"""

import sqlite3

DB_PATH = "restoran.db"

def fix_product_ids():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    print("🔄 Ürün ID'leri güncelleniyor...\n")
    
    # 1. Mevcut ürünleri ID'ye göre sırala
    cursor.execute("SELECT urun_id, urun_adi FROM Urun ORDER BY urun_id")
    urunler = cursor.fetchall()
    
    print(f"📊 Toplam {len(urunler)} ürün bulundu\n")
    
    # 2. SiparisDetay tablosunda referans var mı kontrol et
    cursor.execute("SELECT COUNT(*) FROM SiparisDetay")
    detay_sayisi = cursor.fetchone()[0]
    
    if detay_sayisi > 0:
        print(f"⚠️  Dikkat: {detay_sayisi} sipariş detayı bulundu.")
        print("   Sipariş detaylarındaki ürün ID'leri de güncellenecek.\n")
        
        # Önce SiparisDetay'daki referansları güncelle
        # Eski ID -> Yeni ID mapping oluştur
        id_mapping = {}
        for i, (eski_id, urun_adi) in enumerate(urunler, start=1):
            id_mapping[eski_id] = i
        
        # SiparisDetay'daki urun_id'leri güncelle
        guncellenen_detay = 0
        for eski_id, yeni_id in id_mapping.items():
            cursor.execute("UPDATE SiparisDetay SET urun_id = ? WHERE urun_id = ?", (yeni_id, eski_id))
            guncellenen_detay += cursor.rowcount
        
        print(f"✅ {guncellenen_detay} sipariş detayı güncellendi\n")
    
    # 3. Geçici tablo oluştur
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS Urun_temp (
            urun_id INTEGER PRIMARY KEY AUTOINCREMENT,
            urun_adi TEXT NOT NULL,
            kategori TEXT NOT NULL,
            fiyat REAL NOT NULL
        )
    """)
    
    # 4. Ürünleri yeni ID'lerle geçici tabloya kopyala
    cursor.execute("SELECT urun_adi, kategori, fiyat FROM Urun ORDER BY urun_id")
    urunler_data = cursor.fetchall()
    
    for urun_adi, kategori, fiyat in urunler_data:
        cursor.execute("""
            INSERT INTO Urun_temp (urun_adi, kategori, fiyat) 
            VALUES (?, ?, ?)
        """, (urun_adi, kategori, fiyat))
    
    # 5. Eski tabloyu sil ve yenisini yeniden adlandır
    cursor.execute("DROP TABLE Urun")
    cursor.execute("ALTER TABLE Urun_temp RENAME TO Urun")
    
    conn.commit()
    
    # 6. Sonucu göster
    cursor.execute("SELECT urun_id, urun_adi FROM Urun ORDER BY urun_id LIMIT 10")
    yeni_urunler = cursor.fetchall()
    
    print("✅ Ürün ID'leri başarıyla güncellendi!\n")
    print("📋 Yeni ID'ler (ilk 10):")
    for urun_id, urun_adi in yeni_urunler:
        print(f"   {urun_id}. {urun_adi}")
    
    cursor.execute("SELECT MIN(urun_id), MAX(urun_id), COUNT(*) FROM Urun")
    min_id, max_id, toplam = cursor.fetchone()
    print(f"\n📊 Özet: ID'ler {min_id}-{max_id} arasında, toplam {toplam} ürün")
    
    conn.close()
    print("\n✨ İşlem tamamlandı!")

if __name__ == "__main__":
    fix_product_ids()

