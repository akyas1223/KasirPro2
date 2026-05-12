package com.example.kasirpro;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class welcome extends AppCompatActivity {

    private Button btnDaftar, btnMasuk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        if (findViewById(R.id.main) != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Animasi masuk elemen — sinkron dengan akhir splash (1600ms)
        Animation animAtas = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);
        Animation animTengah = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);
        animTengah.setStartOffset(80);
        Animation animBawah = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);
        animBawah.setStartOffset(160);

        TextView tvWelcome  = findViewById(R.id.tvWelcome);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        ImageView imgLogo   = findViewById(R.id.imgLogo);

        if (tvWelcome  != null) tvWelcome.startAnimation(animAtas);
        if (tvSubtitle != null) tvSubtitle.startAnimation(animAtas);
        if (imgLogo    != null) imgLogo.startAnimation(animTengah);

        btnDaftar = findViewById(R.id.btnDaftar);
        btnMasuk  = findViewById(R.id.btnMasuk);

        if (btnDaftar != null) btnDaftar.startAnimation(animBawah);
        if (btnMasuk  != null) btnMasuk.startAnimation(animBawah);

        if (btnDaftar != null) {
            btnDaftar.setOnClickListener(v ->
                    startActivity(new Intent(welcome.this, register.class)));
        }
        if (btnMasuk != null) {
            btnMasuk.setOnClickListener(v ->
                    startActivity(new Intent(welcome.this, login.class)));
        }
    }
}