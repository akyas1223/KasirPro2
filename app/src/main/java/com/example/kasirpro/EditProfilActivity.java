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
    private ImageView imgQrisProfilPreview;
    private LinearLayout layoutQrisPlaceholder;
    private MaterialCardView btnUploadQrisProfil;
    private MaterialButton btnSimpanProfil;
    private ImageButton btnBackProfil;

    private DatabaseHelper dbHelper;
    private String emailUser, qrisPathBaru = "";

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

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
        btnUploadQrisProfil.setOnClickListener(v -> cekIzinLaluBukaGallery());
        btnSimpanProfil.setOnClickListener(v -> simpanPerubahan());
    }

    private void initViews() {
        etEmailProfil              = findViewById(R.id.etEmailProfil);
        etNamaBisnisProfil         = findViewById(R.id.etNamaBisnisProfil);
        etAlamatProfil             = findViewById(R.id.etAlamatProfil);
        etPasswordBaruProfil       = findViewById(R.id.etPasswordBaruProfil);
        etKonfirmasiPasswordProfil = findViewById(R.id.etKonfirmasiPasswordProfil);
        imgQrisProfilPreview       = findViewById(R.id.imgQrisProfilPreview);
        layoutQrisPlaceholder      = findViewById(R.id.layoutQrisPlaceholder);
        btnUploadQrisProfil        = findViewById(R.id.btnUploadQrisProfil);
        btnSimpanProfil            = findViewById(R.id.btnSimpanProfil);
        btnBackProfil              = findViewById(R.id.btnBackProfil);
    }

    private void muatDataProfil() {
        if (emailUser.isEmpty()) return;

        etEmailProfil.setText(emailUser);

        String[] data = dbHelper.getUserData(emailUser);
        if (data != null) {
            etNamaBisnisProfil.setText(data[0]); // nama_bisnis
            etAlamatProfil.setText(data[1]);     // alamat_bisnis

            // Tampilkan QRIS yang sudah ada
            String qrisLama = data[2];
            if (qrisLama != null && !qrisLama.isEmpty()) {
                tampilkanGambarQris(qrisLama);
                qrisPathBaru = qrisLama; // gunakan yang lama jika tidak diganti
            }
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
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.setType("image/*");
                startActivityForResult(fallback, PICK_IMAGE_REQUEST);
            } catch (Exception ex) {
                Toast.makeText(this, "Tidak dapat membuka galeri", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                // Copy ke internal storage agar permanen
                String savedPath = salinGambarKeInternal(uri);
                if (savedPath != null) {
                    qrisPathBaru = savedPath;
                    tampilkanGambarQris(savedPath);
                } else {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {}
                    qrisPathBaru = uri.toString();
                    tampilkanGambarQris(qrisPathBaru);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String salinGambarKeInternal(Uri sourceUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) return null;

            File folder = new File(getFilesDir(), "qris_images");
            if (!folder.exists()) folder.mkdirs();

            File fileOutput = new File(folder, "qris_" + System.currentTimeMillis() + ".jpg");
            OutputStream outputStream = new FileOutputStream(fileOutput);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();
            return fileOutput.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private String getText(TextInputEditText field) {
        if (field == null || field.getText() == null) return "";
        return field.getText().toString().trim();
    }
}