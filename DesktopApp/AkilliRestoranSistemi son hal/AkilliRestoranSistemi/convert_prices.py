#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Dolar fiyatlarını Türk Lirası'na çeviren script
"""

import sqlite3

# Dolar kuru (örnek: 1 USD = 35 TL - güncel kuru kullanabilirsiniz)
USD_TO_TRY = 35.0

conn = sqlite3.connect('restoran.db')
cursor = conn.cursor()

# Tüm ürünleri al
cursor.execute('SELECT urun_id, urun_adi, fiyat FROM Urun')
urunler = cursor.fetchall()

print('🔄 Fiyatlar TL\'ye çevriliyor...\n')
print(f'Dönüşüm kuru: 1 USD = {USD_TO_TRY} TL\n')

guncellenen = 0
for urun_id, urun_adi, eski_fiyat in urunler:
    yeni_fiyat = round(eski_fiyat * USD_TO_TRY, 2)
    cursor.execute('UPDATE Urun SET fiyat = ? WHERE urun_id = ?', (yeni_fiyat, urun_id))
    print(f'✅ {urun_adi}: ${eski_fiyat:.2f} → {yeni_fiyat:.2f} ₺')
    guncellenen += 1

conn.commit()
conn.close()

print(f'\n✨ Toplam {guncellenen} ürünün fiyatı güncellendi!')

