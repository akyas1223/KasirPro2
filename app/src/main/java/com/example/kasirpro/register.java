package com.example.kasirpro;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import com.google.android.material.card.MaterialCardView;

public class register extends AppCompatActivity {

    private EditText etEmail, etPassword, etNamaBisnis, etAlamatBisnis;
    private CheckBox cbSyarat;
    private Button btnDaftarSubmit;
    private ImageView btnBack, imgQrisPreview;
    private MaterialCardView btnUploadQris;
    private TextView tvLoginLink, tvLabelUpload;
    private LinearLayout layoutPlaceholder;
    private DatabaseHelper dbHelper;

    private String qrisPath = "";
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        // Inisialisasi Views - Sesuaikan ID dengan XML
        etEmail = findViewById(R.id.etEmailReg);
        etPassword = findViewById(R.id.etPasswordReg);
        etNamaBisnis = findViewById(R.id.etNamaBisnis);
        etAlamatBisnis = findViewById(R.id.etAlamatBisnis);
        cbSyarat = findViewById(R.id.cbSyarat);
        btnDaftarSubmit = findViewById(R.id.btnDaftarSubmit);
        btnBack = findViewById(R.id.btnBack);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnUploadQris = findViewById(R.id.btnUploadQris);
        imgQrisPreview = findViewById(R.id.imgQrisPreview);
        tvLabelUpload = findViewById(R.id.tvLabelUpload); // Sekarang ID ini ada di XML
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder);

        btnBack.setOnClickListener(v -> finish());

        btnUploadQris.setOnClickListener(v -> cekIzinLaluBukaGallery());

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(register.this, login.class));
            finish();
        });

        btnDaftarSubmit.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String nama = etNamaBisnis.getText().toString().trim();
            String alamat = etAlamatBisnis.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || nama.isEmpty() || alamat.isEmpty()) {
                Toast.makeText(this, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show();
            } else if (qrisPath.isEmpty()) {
                Toast.makeText(this, "Mohon upload gambar QRIS Anda", Toast.LENGTH_SHORT).show();
            } else if (!cbSyarat.isChecked()) {
                Toast.makeText(this, "Anda harus menyetujui syarat & ketentuan", Toast.LENGTH_SHORT).show();
            } else {
                boolean berhasil = dbHelper.simpanUser(email, password, nama, alamat, qrisPath);

                if (berhasil) {
                    SharedPreferences session = getSharedPreferences("SESSION_KASIR", MODE_PRIVATE);
                    SharedPreferences.Editor editor = session.edit();
                    editor.putString("logged_in_user", email);
                    editor.putBoolean("is_logged_in", true);
                    editor.apply();

                    Toast.makeText(this, "Pendaftaran Berhasil!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(register.this, beranda.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Pendaftaran Gagal (Email sudah terdaftar)", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private static final int PERMISSION_REQUEST_CODE = 100;

    private void cekIzinLaluBukaGallery() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
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
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bukaGallery();
            } else {
                Toast.makeText(this, "Izin akses galeri diperlukan", Toast.LENGTH_LONG).show();
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
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            try {
                try {
                    // FIX: wrap dalam try-catch tersendiri — tidak semua URI support persistable
                    getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException se) {
                    // URI tidak support persistable, lanjutkan saja
                }
                qrisPath = imageUri.toString();

                // Munculkan Preview
                imgQrisPreview.setVisibility(View.VISIBLE);
                imgQrisPreview.setImageURI(imageUri);

                // Sembunyikan Placeholder (Ikon dan Teks)
                if (layoutPlaceholder != null) {
                    layoutPlaceholder.setVisibility(View.GONE);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
            }
        }
    }
}