package com.example.kasirpro;

public class Transaksi {
    private String namaItems;
    private String metodeBayar;
    private int totalBayar;

    public Transaksi(String namaItems, String metodeBayar, int totalBayar) {
        this.namaItems = namaItems;
        this.metodeBayar = metodeBayar;
        this.totalBayar = totalBayar;
    }

    public String getNamaItems() {
        return namaItems;
    }

    public String getMetodeBayar() {
        return metodeBayar;
    }

    public int getTotalBayar() {
        return totalBayar;
    }
}