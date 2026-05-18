package com.example.kasirpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnMasukSubmit;
    private ImageView btnBack;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- 1. CEK SESI (AUTO LOGIN) ---
        SharedPreferences session = getSharedPreferences("SESSION_KASIR", MODE_PRIVATE);
        if (session.getBoolean("is_logged_in", false)) {
            String email = session.getString("logged_in_user", "");
            if (!email.isEmpty()) {
                // Inisialisasi dbHelper dulu sebelum pindah agar tidak null pointer
                dbHelper = new DatabaseHelper(this);
                moveToBeranda(email);
                return;
            }
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Inisialisasi Database dan View
        dbHelper = new DatabaseHelper(this);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnMasukSubmit = findViewById(R.id.btnMasukSubmit);
        btnBack = findViewById(R.id.btnBack);

        // Pengaturan Padding System Bar yang Aman
        // Menggunakan android.R.id.content agar tidak perlu manual ID 'main' di XML
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Tombol Kembali
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(login.this, welcome.class));
                finish();
            });
        }

        // Link ke Daftar
        android.widget.TextView tvDaftarLink = findViewById(R.id.tvDaftarLink);
        if (tvDaftarLink != null) {
            tvDaftarLink.setOnClickListener(v -> {
                startActivity(new Intent(login.this, register.class));
            });
        }

        // Logika Klik Tombol Masuk
        btnMasukSubmit.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(login.this, "Email dan Password wajib diisi", Toast.LENGTH_SHORT).show();
            } else {
                if (dbHelper.cekLogin(email, password)) {
                    // SIMPAN SESI
                    SharedPreferences.Editor editor = session.edit();
                    editor.putString("logged_in_user", email);
                    editor.putBoolean("is_logged_in", true);
                    editor.apply();

                    Toast.makeText(login.this, "Login Berhasil", Toast.LENGTH_SHORT).show();
                    moveToBeranda(email);
                } else {
                    Toast.makeText(login.this, "Email atau Password salah!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void moveToBeranda(String email) {
        String namaBisnis = dbHelper != null ? dbHelper.getNamaBisnis(email) : "Kasir";

        Intent intent = new Intent(login.this, beranda.class);
        intent.putExtra("NAMA_USER", namaBisnis);
        intent.putExtra("EMAIL_USER", email);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}