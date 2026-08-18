package com.restoran.model;

public class Urun {
    private int urunID;
    private String urunAdi;
    private double fiyat;

    public Urun(int urunID, String urunAdi, double fiyat) {
        this.urunID = urunID;
        this.urunAdi = urunAdi;
        this.fiyat = fiyat;
    }

    public int getUrunID() {
        return urunID;
    }

    public void setUrunID(int urunID) {
        this.urunID = urunID;
    }

    public String getUrunAdi() {
        return urunAdi;
    }

    public void setUrunAdi(String urunAdi) {
        this.urunAdi = urunAdi;
    }

    public double getFiyat() {
        return fiyat;
    }

    public void setFiyat(double fiyat) {
        this.fiyat = fiyat;
    }

    @Override
    public String toString() {
        return urunAdi;
    }
} 