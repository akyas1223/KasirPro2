package com.example.kasirpro;

public class Transaksi {
    private String namaItems;
    private String metodeBayar;
    private int totalBayar;
    private String jam;

    public Transaksi(String namaItems, String metodeBayar, int totalBayar, String jam) {
        this.namaItems = namaItems;
        this.metodeBayar = metodeBayar;
        this.totalBayar = totalBayar;
        this.jam = jam;
    }

    public String getNamaItems() { return namaItems; }
    public String getMetodeBayar() { return metodeBayar; }
    public int getTotalBayar() { return totalBayar; }
    public String getJam() { return jam != null ? jam : "--:--"; }
}