package com.example.kasirpro;

import java.io.Serializable;

public class item_keranjang implements Serializable {
    // Tambahkan ini untuk stabilitas data saat Intent
    private static final long serialVersionUID = 1L;

    private Produk produk;
    private int jumlah;

    public item_keranjang(Produk produk, int jumlah) {
        this.produk = produk;
        this.jumlah = jumlah;
    }

    public Produk getProduk() {
        return produk;
    }

    public int getJumlah() {
        return jumlah;
    }

    public void setJumlah(int jumlah) {
        this.jumlah = jumlah;
    }

    public int getSubtotal() {
        // Pastikan produk tidak null untuk menghindari crash
        if (produk != null) {
            return produk.getHarga() * jumlah;
        }
        return 0;
    }
}