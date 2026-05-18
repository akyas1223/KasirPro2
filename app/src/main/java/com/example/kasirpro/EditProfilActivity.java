package com.example.kasirpro;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class EditProfilActivity extends AppCompatActivity {

    private TextInputEditText etEmailProfil, etNamaBisnisProfil, etAlamatProfil;
    private TextInputEditText etPasswordBaruProfil, etKonfirmasiPasswordProfil;
    private ImageView imgQrisProfilPreview, ivFotoProfilPreview, ivFotoProfilDefault;
    private LinearLayout layoutQrisPlaceholder;
    private MaterialCardView btnUploadQrisProfil, btnFotoProfil;
    private MaterialButton btnSimpanProfil;
    private ImageButton btnBackProfil;

    private DatabaseHelper dbHelper;
    private String emailUser, qrisPathBaru = "", fotoProfilBaru = "";

    private static final int PICK_QRIS_REQUEST   = 1;
    private static final int PICK_FOTO_REQUEST   = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private int pendingPickType = 0; // 1=QRIS, 2=foto profil

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profil);

        dbHelper = new DatabaseHelper(this);

        SharedPreferences session = getSharedPreferences("SESSION_KASIR", MODE_PRIVATE);
        emailUser = session.getString("logged_in_user", "");

        initViews();
        muatDataProfil();

        btnBackProfil.setOnClickListener(v -> finish());
        btnFotoProfil.setOnClickListener(v -> { pendingPickType = 2; cekIzinLaluBukaGallery(); });
        btnUploadQrisProfil.setOnClickListener(v -> { pendingPickType = 1; cekIzinLaluBukaGallery(); });
        btnSimpanProfil.setOnClickListener(v -> simpanPerubahan());
    }

    private void initViews() {
        etEmailProfil              = findViewById(R.id.etEmailProfil);
        etNamaBisnisProfil         = findViewById(R.id.etNamaBisnisProfil);
        etAlamatProfil             = findViewById(R.id.etAlamatProfil);
        etPasswordBaruProfil       = findViewById(R.id.etPasswordBaruProfil);
        etKonfirmasiPasswordProfil = findViewById(R.id.etKonfirmasiPasswordProfil);
        imgQrisProfilPreview       = findViewById(R.id.imgQrisProfilPreview);
        ivFotoProfilPreview        = findViewById(R.id.ivFotoProfilPreview);
        ivFotoProfilDefault        = findViewById(R.id.ivFotoProfilDefault);
        layoutQrisPlaceholder      = findViewById(R.id.layoutQrisPlaceholder);
        btnUploadQrisProfil        = findViewById(R.id.btnUploadQrisProfil);
        btnFotoProfil              = findViewById(R.id.btnFotoProfil);
        btnSimpanProfil            = findViewById(R.id.btnSimpanProfil);
        btnBackProfil              = findViewById(R.id.btnBackProfil);
    }

    private void muatDataProfil() {
        if (emailUser.isEmpty()) return;
        etEmailProfil.setText(emailUser);
        String[] data = dbHelper.getUserData(emailUser);
        if (data != null) {
            etNamaBisnisProfil.setText(data[0]);
            etAlamatProfil.setText(data[1]);
            // QRIS
            if (data[2] != null && !data[2].isEmpty()) {
                qrisPathBaru = data[2];
                tampilkanGambarQris(qrisPathBaru);
            }
            // Foto profil (index 4)
            if (data.length > 4 && data[4] != null && !data[4].isEmpty()) {
                fotoProfilBaru = data[4];
                tampilFotoProfil(fotoProfilBaru);
            }
        }
    }

    private void tampilFotoProfil(String path) {
        try {
            if (ivFotoProfilPreview == null) return;
            ivFotoProfilPreview.setVisibility(View.VISIBLE);
            if (ivFotoProfilDefault != null) ivFotoProfilDefault.setVisibility(View.GONE);
            Object src = path.startsWith("/") ? new File(path) : Uri.parse(path);
            Glide.with(this).load(src).centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivFotoProfilPreview);
        } catch (Exception e) {
            if (ivFotoProfilPreview != null)
                ivFotoProfilPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private void tampilkanGambarQris(String path) {
        try {
            imgQrisProfilPreview.setVisibility(View.VISIBLE);
            layoutQrisPlaceholder.setVisibility(View.GONE);
            Object src = path.startsWith("/") ? new File(path) : Uri.parse(path);
            Glide.with(this).load(src).centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imgQrisProfilPreview);
        } catch (Exception e) {
            imgQrisProfilPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private void simpanPerubahan() {
        String nama    = getText(etNamaBisnisProfil);
        String alamat  = getText(etAlamatProfil);
        String pwBaru  = getText(etPasswordBaruProfil);
        String pwKonfirm = getText(etKonfirmasiPasswordProfil);

        if (nama.isEmpty() || alamat.isEmpty()) {
            Toast.makeText(this, "Nama dan alamat bisnis tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pwBaru.isEmpty()) {
            if (pwBaru.length() < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!pwBaru.equals(pwKonfirm)) {
                Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        boolean sukses = dbHelper.updateUser(emailUser, nama, alamat,
                pwBaru.isEmpty() ? null : pwBaru,
                qrisPathBaru.isEmpty() ? null : qrisPathBaru);

        // Simpan foto profil jika ada
        if (!fotoProfilBaru.isEmpty()) {
            dbHelper.updateFotoProfil(emailUser, fotoProfilBaru);
        }

        if (sukses) {
            Toast.makeText(this, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Gagal menyimpan perubahan", Toast.LENGTH_SHORT).show();
        }
    }

    // ---- PERMISSION & GALLERY ----
    private void cekIzinLaluBukaGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

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
        if (requestCode == PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            bukaGallery();
        } else {
            Toast.makeText(this, "Izin akses galeri diperlukan", Toast.LENGTH_LONG).show();
        }
    }

    private void bukaGallery() {
        int reqCode = pendingPickType == 2 ? PICK_FOTO_REQUEST : PICK_QRIS_REQUEST;
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, reqCode);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.setType("image/*");
                startActivityForResult(fallback, reqCode);
            } catch (Exception ex) {
                Toast.makeText(this, "Tidak dapat membuka galeri", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == PICK_QRIS_REQUEST || requestCode == PICK_FOTO_REQUEST)
                && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                String savedPath = salinGambarKeInternal(uri,
                        requestCode == PICK_FOTO_REQUEST ? "foto_profil" : "qris_images");
                String finalPath = savedPath != null ? savedPath : uri.toString();

                if (requestCode == PICK_FOTO_REQUEST) {
                    fotoProfilBaru = finalPath;
                    tampilFotoProfil(finalPath);
                } else {
                    qrisPathBaru = finalPath;
                    tampilkanGambarQris(finalPath);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String salinGambarKeInternal(Uri sourceUri, String folderName) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) return null;
            File folder = new File(getFilesDir(), folderName);
            if (!folder.exists()) folder.mkdirs();
            File fileOutput = new File(folder, folderName + "_" + System.currentTimeMillis() + ".jpg");
            OutputStream outputStream = new FileOutputStream(fileOutput);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
            outputStream.close();
            inputStream.close();
            return fileOutput.getAbsolutePath();
        } catch (Exception e) { return null; }
    }

    private String getText(TextInputEditText field) {
        if (field == null || field.getText() == null) return "";
        return field.getText().toString().trim();
    }
}