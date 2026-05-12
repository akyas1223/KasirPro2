package com.example.kasirpro;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_splash);

        // Get Views
        ImageView logo = findViewById(R.id.logoView);
        TextView title = findViewById(R.id.titleView);
        TextView subtitle = findViewById(R.id.subtitleView);

        // Load Animation
        Animation logoAnim = AnimationUtils.loadAnimation(this, R.anim.wipe_in_up);

        Animation titleAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        titleAnim.setStartOffset(300);

        Animation subtitleAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        subtitleAnim.setStartOffset(400);

        // Start Animations
        logo.startAnimation(logoAnim);
        title.startAnimation(titleAnim);
        subtitle.startAnimation(subtitleAnim);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(Splash.this, welcome.class));
            // Fade out splash, welcome slide masuk — sinkron
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1600);
    }
}