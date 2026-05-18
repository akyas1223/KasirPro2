package com.example.kasirpro;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

public class register extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword, etNamaBisnis, etAlamatBisnis;
    private CheckBox cbSyarat;
    private MaterialButton btnDaftarSubmit;
    private ImageButton btnBack;
    private TextView tvLoginLink, tvLabelUpload;
    private MaterialCardView btnUploadQris, btnFotoProfil;
    private ImageView imgQrisPreview, ivFotoProfilPreview, ivFotoProfilDefault;
    private LinearLayout layoutPlaceholder;
    private DatabaseHelper dbHelper;

    private String qrisPath = "";
    private String fotoProfilPath = "";

    private static final int PICK_QRIS_REQUEST  = 1;
    private static final int PICK_FOTO_REQUEST  = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private int pendingPickType = 0; // 1=QRIS, 2=foto profil

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        etEmail        = findViewById(R.id.etEmailReg);
        etPassword     = findViewById(R.id.etPasswordReg);
        etNamaBisnis   = findViewById(R.id.etNamaBisnis);
        etAlamatBisnis = findViewById(R.id.etAlamatBisnis);
        cbSyarat       = findViewById(R.id.cbSyarat);
        btnDaftarSubmit = findViewById(R.id.btnDaftarSubmit);
        btnBack        = findViewById(R.id.btnBack);
        tvLoginLink    = findViewById(R.id.tvLoginLink);

        // QRIS
        btnUploadQris    = findViewById(R.id.btnUploadQris);
        imgQrisPreview   = findViewById(R.id.imgQrisPreview);
        tvLabelUpload    = findViewById(R.id.tvLabelUpload);
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder);

        // Foto Profil
        btnFotoProfil       = findViewById(R.id.btnFotoProfil);
        ivFotoProfilPreview = findViewById(R.id.ivFotoProfilPreview);
        ivFotoProfilDefault = findViewById(R.id.ivFotoProfilDefault);

        btnBack.setOnClickListener(v -> finish());

        // Klik foto profil
        if (btnFotoProfil != null) {
            btnFotoProfil.setOnClickListener(v -> {
                pendingPickType = 2;
                cekIzinLaluBukaGallery();
            });
        }

        // Klik upload QRIS
        btnUploadQris.setOnClickListener(v -> {
            pendingPickType = 1;
            cekIzinLaluBukaGallery();
        });

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(register.this, login.class));
            finish();
        });

        btnDaftarSubmit.setOnClickListener(v -> {
            String email   = getText(etEmail);
            String password = getText(etPassword);
            String nama    = getText(etNamaBisnis);
            String alamat  = getText(etAlamatBisnis);

            if (email.isEmpty() || password.isEmpty() || nama.isEmpty() || alamat.isEmpty()) {
                Toast.makeText(this, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show();
            } else if (qrisPath.isEmpty()) {
                Toast.makeText(this, "Mohon upload gambar QRIS", Toast.LENGTH_SHORT).show();
            } else if (!cbSyarat.isChecked()) {
                Toast.makeText(this, "Anda harus menyetujui syarat & ketentuan", Toast.LENGTH_SHORT).show();
            } else {
                boolean berhasil = dbHelper.simpanUser(email, password, nama, alamat, qrisPath);
                if (berhasil) {
                    // Simpan foto profil jika ada
                    if (!fotoProfilPath.isEmpty()) {
                        dbHelper.updateFotoProfil(email, fotoProfilPath);
                    }

                    SharedPreferences session = getSharedPreferences("SESSION_KASIR", MODE_PRIVATE);
                    session.edit()
                            .putString("logged_in_user", email)
                            .putBoolean("is_logged_in", true)
                            .apply();

                    Toast.makeText(this, "Pendaftaran Berhasil!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(register.this, beranda.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Email sudah terdaftar", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

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
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();
        try {
            if (requestCode == PICK_FOTO_REQUEST) {
                // Foto profil
                String saved = salinGambarKeInternal(uri, "foto_profil");
                fotoProfilPath = saved != null ? saved : uri.toString();
                if (ivFotoProfilPreview != null) {
                    ivFotoProfilPreview.setVisibility(View.VISIBLE);
                    Glide.with(this).load(new File(fotoProfilPath.startsWith("/")
                            ? fotoProfilPath : uri.getPath())).centerCrop().into(ivFotoProfilPreview);
                    if (ivFotoProfilDefault != null) ivFotoProfilDefault.setVisibility(View.GONE);
                }
            } else if (requestCode == PICK_QRIS_REQUEST) {
                // QRIS
                String saved = salinGambarKeInternal(uri, "qris_images");
                qrisPath = saved != null ? saved : uri.toString();
                if (imgQrisPreview != null) {
                    imgQrisPreview.setVisibility(View.VISIBLE);
                    Glide.with(this).load(fotoProfilPath.startsWith("/")
                            ? new File(qrisPath) : uri).centerInside().into(imgQrisPreview);
                }
                if (layoutPlaceholder != null) layoutPlaceholder.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
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
        } catch (Exception e) {
            return null;
        }
    }

    private String getText(TextInputEditText field) {
        if (field == null || field.getText() == null) return "";
        return field.getText().toString().trim();
    }
}