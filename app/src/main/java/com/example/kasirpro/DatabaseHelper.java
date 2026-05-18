package com.example.kasirpro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "KasirPro.db";
    // FIX: Naikkan versi agar onUpgrade dipanggil ulang dengan logika yang benar
    public static final int DATABASE_VERSION = 9;

    // Tabel User
    public static final String TABLE_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";
    public static final String COL_NAMA_BISNIS = "nama_bisnis";
    public static final String COL_ALAMAT_BISNIS = "alamat_bisnis";
    public static final String COL_QRIS_PATH = "qris_path";
    public static final String COL_FOTO_PROFIL = "foto_profil";

    // Tabel Produk
    public static final String TABLE_PRODUK = "produk";
    public static final String COL_ID_PRODUK = "id_produk";
    public static final String COL_EMAIL_USER = "email_user";
    public static final String COL_KATEGORI = "kategori";
    public static final String COL_NAMA_PRODUK = "nama_produk";
    public static final String COL_HARGA_PRODUK = "harga_produk";
    public static final String COL_GAMBAR_PRODUK = "gambar_produk";

    // Tabel Transaksi
    public static final String TABLE_TRANSAKSI = "transaksi";
    public static final String COL_ID_TRANS = "id_transaksi";
    public static final String COL_EMAIL_TRANS = "email_user_trans";
    public static final String COL_NAMA_ITEM_TRANS = "nama_items";
    public static final String COL_METODE_BAYAR = "metode_bayar";
    public static final String COL_TOTAL_BAYAR = "total_bayar";
    public static final String COL_DD = "dd";
    public static final String COL_MM = "mm";
    public static final String COL_YYYY = "yyyy";
    public static final String COL_JAM = "jam";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EMAIL + " TEXT UNIQUE, "
                + COL_PASSWORD + " TEXT, "
                + COL_NAMA_BISNIS + " TEXT, "
                + COL_ALAMAT_BISNIS + " TEXT, "
                + COL_QRIS_PATH + " TEXT, "
                + COL_FOTO_PROFIL + " TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PRODUK + " ("
                + COL_ID_PRODUK + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EMAIL_USER + " TEXT, "
                + COL_KATEGORI + " TEXT, "
                + COL_NAMA_PRODUK + " TEXT, "
                + COL_HARGA_PRODUK + " INTEGER, "
                + COL_GAMBAR_PRODUK + " TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_TRANSAKSI + " ("
                + COL_ID_TRANS + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EMAIL_TRANS + " TEXT, "
                + COL_NAMA_ITEM_TRANS + " TEXT, "
                + COL_METODE_BAYAR + " TEXT, "
                + COL_TOTAL_BAYAR + " INTEGER, "
                + COL_DD + " TEXT, "
                + COL_MM + " TEXT, "
                + COL_YYYY + " TEXT, " + COL_JAM + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Tambah kolom foto_profil jika belum ada (upgrade dari versi lama)
        try {
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_FOTO_PROFIL + " TEXT");
        } catch (Exception ignored) { /* kolom sudah ada */ }

        // Tambah kolom jam jika belum ada
        try {
            db.execSQL("ALTER TABLE " + TABLE_TRANSAKSI + " ADD COLUMN " + COL_JAM + " TEXT");
        } catch (Exception ignored) { /* kolom sudah ada */ }

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSAKSI);
        // Buat ulang hanya tabel produk dan transaksi
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PRODUK + " ("
                + COL_ID_PRODUK + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EMAIL_USER + " TEXT, "
                + COL_KATEGORI + " TEXT, "
                + COL_NAMA_PRODUK + " TEXT, "
                + COL_HARGA_PRODUK + " INTEGER, "
                + COL_GAMBAR_PRODUK + " TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_TRANSAKSI + " ("
                + COL_ID_TRANS + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EMAIL_TRANS + " TEXT, "
                + COL_NAMA_ITEM_TRANS + " TEXT, "
                + COL_METODE_BAYAR + " TEXT, "
                + COL_TOTAL_BAYAR + " INTEGER, "
                + COL_DD + " TEXT, "
                + COL_MM + " TEXT, "
                + COL_YYYY + " TEXT, " + COL_JAM + " TEXT)");
    }

    // --- FUNGSI USER ---
    public boolean updateFotoProfil(String email, String fotoPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FOTO_PROFIL, fotoPath);
        return db.update(TABLE_USERS, values, COL_EMAIL + "=?", new String[]{email}) > 0;
    }

    public boolean simpanUser(String email, String password, String namaBisnis, String alamatBisnis, String qrisPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        values.put(COL_NAMA_BISNIS, namaBisnis);
        values.put(COL_ALAMAT_BISNIS, alamatBisnis);
        values.put(COL_QRIS_PATH, qrisPath);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean cekLogin(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COL_ID},
                    COL_EMAIL + " = ? AND " + COL_PASSWORD + " = ?",
                    new String[]{email, password}, null, null, null);
            return cursor.getCount() > 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    // Ambil semua data profil user sekaligus
    public String[] getUserData(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS,
                    new String[]{COL_NAMA_BISNIS, COL_ALAMAT_BISNIS, COL_QRIS_PATH, COL_PASSWORD, COL_FOTO_PROFIL},
                    COL_EMAIL + " = ?", new String[]{email}, null, null, null);
            if (cursor.moveToFirst()) {
                return new String[]{
                        cursor.getString(0), // nama_bisnis
                        cursor.getString(1), // alamat_bisnis
                        cursor.getString(2), // qris_path
                        cursor.getString(3), // password
                        cursor.getString(4)  // foto_profil
                };
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public String getFotoProfil(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COL_FOTO_PROFIL},
                    COL_EMAIL + " = ?", new String[]{email}, null, null, null);
            if (cursor.moveToFirst()) return cursor.getString(0);
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public boolean updateUser(String email, String namaBaru, String alamatBaru,
                              String passwordBaru, String qrisBaru) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAMA_BISNIS, namaBaru);
        values.put(COL_ALAMAT_BISNIS, alamatBaru);
        if (passwordBaru != null && !passwordBaru.isEmpty()) {
            values.put(COL_PASSWORD, passwordBaru);
        }
        if (qrisBaru != null && !qrisBaru.isEmpty()) {
            values.put(COL_QRIS_PATH, qrisBaru);
        }
        int result = db.update(TABLE_USERS, values,
                COL_EMAIL + " = ?", new String[]{email});
        return result > 0;
    }

    public String getNamaBisnis(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COL_NAMA_BISNIS},
                    COL_EMAIL + " = ?", new String[]{email}, null, null, null);
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(COL_NAMA_BISNIS));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return "";
    }

    public String getQrisPath(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COL_QRIS_PATH},
                    COL_EMAIL + " = ?", new String[]{email}, null, null, null);
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(COL_QRIS_PATH));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return "";
    }

    // --- FUNGSI PRODUK ---
    public boolean simpanProduk(String emailUser, String kategori, String nama, int harga, String gambarPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EMAIL_USER, emailUser);
        values.put(COL_KATEGORI, kategori);
        values.put(COL_NAMA_PRODUK, nama);
        values.put(COL_HARGA_PRODUK, harga);
        values.put(COL_GAMBAR_PRODUK, gambarPath);
        long result = db.insert(TABLE_PRODUK, null, values);
        return result != -1;
    }

    public Cursor getProdukByUser(String emailUser) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_PRODUK
                        + " WHERE " + COL_EMAIL_USER + " = ?"
                        + " ORDER BY " + COL_KATEGORI + " ASC",
                new String[]{emailUser});
    }

    public boolean updateProduk(String emailUser, String namaLama, String kategoriBaru,
                                String namaBaru, int hargaBaru, String gambarBaru) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_KATEGORI, kategoriBaru);
        values.put(COL_NAMA_PRODUK, namaBaru);
        values.put(COL_HARGA_PRODUK, hargaBaru);
        if (gambarBaru != null && !gambarBaru.isEmpty()) {
            values.put(COL_GAMBAR_PRODUK, gambarBaru);
        }
        int result = db.update(TABLE_PRODUK, values,
                COL_NAMA_PRODUK + "=? AND " + COL_EMAIL_USER + "=?",
                new String[]{namaLama, emailUser});
        return result > 0;
    }

    public boolean hapusProduk(String namaProduk, String emailUser) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_PRODUK,
                COL_NAMA_PRODUK + "=? AND " + COL_EMAIL_USER + "=?",
                new String[]{namaProduk, emailUser});
        return result > 0;
    }

    // --- FUNGSI TRANSAKSI ---
    public boolean simpanTransaksi(String email, String items, String metode, int total,
                                   String dd, String mm, String yyyy, String jam) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EMAIL_TRANS, email);
        values.put(COL_NAMA_ITEM_TRANS, items);
        values.put(COL_METODE_BAYAR, metode);
        values.put(COL_TOTAL_BAYAR, total);
        values.put(COL_DD, dd);
        values.put(COL_MM, mm);
        values.put(COL_YYYY, yyyy);
        values.put(COL_JAM, jam);
        long result = db.insert(TABLE_TRANSAKSI, null, values);
        return result != -1;
    }

    public Cursor getRiwayat(String email, String dd, String mm, String yyyy) {
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder query = new StringBuilder(
                "SELECT * FROM " + TABLE_TRANSAKSI + " WHERE " + COL_EMAIL_TRANS + " = ?");
        List<String> args = new ArrayList<>();
        args.add(email);

        if (dd != null && !dd.isEmpty()) { query.append(" AND ").append(COL_DD).append(" = ?"); args.add(dd); }
        if (mm != null && !mm.isEmpty()) { query.append(" AND ").append(COL_MM).append(" = ?"); args.add(mm); }
        if (yyyy != null && !yyyy.isEmpty()) { query.append(" AND ").append(COL_YYYY).append(" = ?"); args.add(yyyy); }

        query.append(" ORDER BY ").append(COL_ID_TRANS).append(" DESC");
        return db.rawQuery(query.toString(), args.toArray(new String[0]));
    }
}