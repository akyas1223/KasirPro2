package com.example.kasirpro;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class TambahProduk extends AppCompatActivity {

    private TextInputEditText etNamaProduk, etKategori, etHargaProduk;
    private MaterialCardView btnPilihGambar;
    private ImageView ivPreviewGambar;
    private LinearLayout layoutPlaceholder;
    private MaterialButton btnSimpanProduk;
    private ImageButton btnBack;

    private DatabaseHelper dbHelper;
    private String imagePath = "";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private boolean isEditMode = false;
    private String namaLamaProduk = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tambah_produk);

        dbHelper = new DatabaseHelper(this);
        initViews();

        isEditMode = getIntent().getBooleanExtra("IS_EDIT", false);
        String extra = getIntent().getStringExtra("NAMA_PRODUK");
        namaLamaProduk = (extra != null) ? extra : "";

        if (isEditMode && !namaLamaProduk.isEmpty()) {
            muatDataProdukUntukEdit(namaLamaProduk);
        }

        btnBack.setOnClickListener(v -> finish());

        // FIX: Minta izin storage terlebih dahulu sebelum buka picker gambar
        btnPilihGambar.setOnClickListener(v -> cekIzinLaluBukaGallery());

        btnSimpanProduk.setOnClickListener(v -> simpanDataKeDatabase());
    }

    private void initViews() {
        etNamaProduk = findViewById(R.id.etNamaProduk);
        etKategori = findViewById(R.id.etKategori);
        etHargaProduk = findViewById(R.id.etHargaProduk);
        btnPilihGambar = findViewById(R.id.btnPilihGambar);
        ivPreviewGambar = findViewById(R.id.ivPreviewGambar);
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder);
        btnSimpanProduk = findViewById(R.id.btnSimpanProduk);
        btnBack = findViewById(R.id.btnBackKeBeranda);
    }

    // FIX: Cek & minta izin runtime sesuai versi Android
    private void cekIzinLaluBukaGallery() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ gunakan READ_MEDIA_IMAGES
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            // Android 12 ke bawah gunakan READ_EXTERNAL_STORAGE
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            bukaGallery();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bukaGallery();
            } else {
                Toast.makeText(this,
                        "Izin akses galeri diperlukan untuk memilih foto produk",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void bukaGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            // Fallback ke ACTION_GET_CONTENT jika ACTION_OPEN_DOCUMENT tidak tersedia
            try {
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.setType("image/*");
                startActivityForResult(fallback, PICK_IMAGE_REQUEST);
            } catch (Exception ex) {
                Toast.makeText(this, "Tidak dapat membuka galeri", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void muatDataProdukUntukEdit(String namaProduk) {
        SharedPreferences session = getSharedPreferences("SESSION_KASIR", MODE_PRIVATE);
        String emailUser = session.getString("logged_in_user", "");
        if (emailUser.isEmpty()) return;

        android.database.Cursor cursor = dbHelper.getProdukByUser(emailUser);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAMA_PRODUK));
                    if (nama.equals(namaProduk)) {
                        String kategori = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_KATEGORI));
                        long harga = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HARGA_PRODUK));
                        String gambar = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GAMBAR_PRODUK));

                        etNamaProduk.setText(nama);
                        etKategori.setText(kategori);
                        etHargaProduk.setText(String.valueOf(harga));

                        if (gambar != null && !gambar.isEmpty()) {
                            imagePath = gambar;
                            tampilkanGambarDariPath(gambar);
                        }
                        break;
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            try {
                // FIX UTAMA: Salin gambar ke internal storage app agar permanen
                // URI dari galeri bisa kedaluwarsa setelah app restart
                String savedPath = salinGambarKeInternal(selectedImageUri);
                if (savedPath != null) {
                    imagePath = savedPath; // simpan path file lokal, bukan URI
                    tampilkanGambar(Uri.fromFile(new java.io.File(savedPath)));
                } else {
                    // Fallback: coba pakai URI langsung (tidak permanen)
                    try {
                        int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(selectedImageUri, takeFlags);
                    } catch (SecurityException se) { /* tidak semua URI support ini */ }
                    imagePath = selectedImageUri.toString();
                    tampilkanGambar(selectedImageUri);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal memuat gambar, coba pilih gambar lain", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Menyalin gambar dari URI ke folder internal app agar tidak hilang saat app restart.
     * Mengembalikan path file lokal, atau null jika gagal.
     */
    private String salinGambarKeInternal(Uri sourceUri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) return null;

            // Folder khusus gambar produk di internal storage
            java.io.File folder = new java.io.File(getFilesDir(), "produk_images");
            if (!folder.exists()) folder.mkdirs();

            // Nama file unik berdasarkan waktu
            String namaFile = "produk_" + System.currentTimeMillis() + ".jpg";
            java.io.File fileOutput = new java.io.File(folder, namaFile);

            java.io.OutputStream outputStream = new java.io.FileOutputStream(fileOutput);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            return fileOutput.getAbsolutePath(); // kembalikan path absolut
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Handle path file lokal (hasil copy) maupun URI string lama
    private void tampilkanGambar(Uri uri) {
        try {
            ivPreviewGambar.setVisibility(View.VISIBLE);
            if (layoutPlaceholder != null) layoutPlaceholder.setVisibility(View.GONE);
            Glide.with(this)
                    .load(uri)
                    .override(800, 600)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivPreviewGambar);
        } catch (Exception e) {
            e.printStackTrace();
            ivPreviewGambar.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    // Overload: load dari string path (bisa file path atau URI string)
    private void tampilkanGambarDariPath(String path) {
        if (path == null || path.isEmpty()) return;
        if (path.startsWith("/")) {
            // Path file lokal
            tampilkanGambar(Uri.fromFile(new java.io.File(path)));
        } else {
            // URI string
            tampilkanGambar(Uri.parse(path));
        }
    }

    private void simpanDataKeDatabase() {
        String nama = getTextSafe(etNamaProduk);
        String kategori = getTextSafe(etKategori);
        String hargaStr = getTextSafe(etHargaProduk);

        SharedPreferences session = getSharedPreferences("SESSION_KASIR", MODE_PRIVATE);
        String emailUser = session.getString("logged_in_user", "");

        if (nama.isEmpty() || kategori.isEmpty() || hargaStr.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi semua data", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEditMode && imagePath.isEmpty()) {
            Toast.makeText(this, "Harap pilih foto produk", Toast.LENGTH_SHORT).show();
            return;
        }

        if (emailUser.isEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show();
            return;
        }

        // FIX: Validasi harga lebih ketat — cegah overflow dan nilai negatif
        long harga;
        try {
            harga = Long.parseLong(hargaStr);
            if (harga < 0) {
                Toast.makeText(this, "Harga tidak boleh negatif", Toast.LENGTH_SHORT).show();
                return;
            }
            if (harga > 2_000_000_000L) {
                Toast.makeText(this, "Harga terlalu besar (maks Rp 2.000.000.000)", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Format harga tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            boolean sukses;
            if (isEditMode) {
                sukses = dbHelper.updateProduk(emailUser, namaLamaProduk, kategori, nama, (int) harga, imagePath);
                if (sukses) {
                    Toast.makeText(this, "Produk berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Gagal memperbarui produk", Toast.LENGTH_SHORT).show();
                }
            } else {
                sukses = dbHelper.simpanProduk(emailUser, kategori, nama, (int) harga, imagePath);
                if (sukses) {
                    Toast.makeText(this, "Produk berhasil ditambahkan!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Gagal menyimpan ke database", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Terjadi kesalahan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getTextSafe(TextInputEditText field) {
        if (field == null || field.getText() == null) return "";
        return field.getText().toString().trim();
    }
}