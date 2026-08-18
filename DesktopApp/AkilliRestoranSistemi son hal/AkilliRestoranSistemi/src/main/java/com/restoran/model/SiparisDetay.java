package com.restoran.model;

public class SiparisDetay {
    private Urun urun;
    private int adet;

    public SiparisDetay(Urun urun, int adet) {
        this.urun = urun;
        this.adet = adet;
    }

    public Urun getUrun() {
        return urun;
    }

    public int getAdet() {
        return adet;
    }

    public double getToplamTutar() {
        return urun.getFiyat() * adet;
    }

    public void setAdet(int adet) {
        this.adet = adet;
    }
} 