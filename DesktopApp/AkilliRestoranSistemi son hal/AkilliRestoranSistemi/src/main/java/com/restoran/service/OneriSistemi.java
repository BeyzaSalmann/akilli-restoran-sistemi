package com.restoran.service;

import com.restoran.model.Urun;
import java.util.List;
import java.util.Random;

public class OneriSistemi {
    
    private VeritabaniServisi veritabaniServisi;
    private Random random = new Random();

    public OneriSistemi(VeritabaniServisi veritabaniServisi) {
        this.veritabaniServisi = veritabaniServisi;
    }

    public String oneriyiOlustur(int mutlulukOrani) {
        String duyguKategorisi;
        
        if (mutlulukOrani > 70) {
            duyguKategorisi = "mutlu";
        } else if (mutlulukOrani > 30) {
            duyguKategorisi = "notr";
        } else {
            duyguKategorisi = "uzgun";
        }
        
        List<Urun> oneriler = veritabaniServisi.getMenuOnerileri(duyguKategorisi);
        
        if (oneriler == null || oneriler.isEmpty()) {
            List<Urun> tumUrunler = veritabaniServisi.getTumUrunler();
            if (tumUrunler == null || tumUrunler.isEmpty()) {
                return "💡 Lütfen yukarıdaki menüden bir ürün seçin.";
            }
            Urun randomUrun = tumUrunler.get(random.nextInt(tumUrunler.size()));
            return String.format("🍽️ Şef Önerisi: %s (%.2f $)", 
                randomUrun.getUrunAdi(), randomUrun.getFiyat());
        }
        
        Urun secilenUrun = oneriler.get(random.nextInt(oneriler.size()));
        
        return String.format("🎯 Şef Önerisi: %s (%.2f $)!", 
            secilenUrun.getUrunAdi(), 
            secilenUrun.getFiyat());
    }
} 