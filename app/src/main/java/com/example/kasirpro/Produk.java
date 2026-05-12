package com.example.kasirpro;

import java.io.Serializable; // 1. TAMBAHKAN IMPORT INI

// 2. TAMBAHKAN 'implements Serializable'
public class Produk implements Serializable {

    // 3. TAMBAHKAN serialVersionUID (opsional tapi disarankan agar data stabil)
    private static final long serialVersionUID = 1L;

    private String kategori;
    private String nama;
    private String gambar;
    private int harga;

    // Constructor
    public Produk(String kategori, String nama, int harga, String gambar) {
        this.kategori = kategori;
        this.nama = nama;
        this.harga = harga;
        this.gambar = gambar;
    }

    // Getter dan Setter
    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getGambar() { return gambar; }
    public void setGambar(String gambar) { this.gambar = gambar; }

    public int getHarga() { return harga; }
    public void setHarga(int harga) { this.harga = harga; }
}