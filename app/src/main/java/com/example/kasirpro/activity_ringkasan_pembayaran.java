package com.example.kasirpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;

public class activity_ringkasan_pembayaran extends AppCompatActivity {

    private RecyclerView rvKeranjang;
    private KeranjangAdapter keranjangAdapter;
    private ArrayList<item_keranjang> listBelanja;

    private TextView tvTotalBayar, tvBadgeItem, tvTunai, tvQRIS;
    private ImageView btnBack, imgTunai, imgQRIS;
    private LinearLayout btnTunaiClick, btnQRISClick;
    private MaterialCardView cardTunai, cardQRIS;
    private Button btnBayarFinal;

    private String metodeTerpilih = "Tunai";
    private String emailUserLogin;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ringkasan_pembayaran);

        dbHelper = new DatabaseHelper(this);
        initViews();

        SharedPreferences session = getSharedPreferences("SESSION_KASIR", MODE_PRIVATE);
        emailUserLogin = getIntent().getStringExtra("EMAIL_USER");
        if (emailUserLogin == null || emailUserLogin.isEmpty()) {
            emailUserLogin = session.getString("logged_in_user", "");
        }

        listBelanja = (ArrayList<item_keranjang>) getIntent().getSerializableExtra("DATA_BELANJA");
        if (listBelanja == null) listBelanja = new ArrayList<>();

        setupRecyclerView();
        setupPaymentSelection();

        btnBack.setOnClickListener(v -> finish());
        btnBayarFinal.setOnClickListener(v -> {
            if (metodeTerpilih.equals("QRIS")) tampilkanDialogQRIS();
            else eksekusiPembayaran();
        });

        updateTotalUI();
    }

    private void initViews() {
        rvKeranjang = findViewById(R.id.rvItemRingkasan);
        tvTotalBayar = findViewById(R.id.tvTotalRingkasan);
        tvBadgeItem = findViewById(R.id.tvBadgeItem); // Sekarang tidak akan error lagi
        btnBack = findViewById(R.id.btnBack);
        btnBayarFinal = findViewById(R.id.btnBayarFinal);

        cardTunai = findViewById(R.id.cardTunai);
        cardQRIS = findViewById(R.id.cardQRIS);
        btnTunaiClick = findViewById(R.id.btnTunai);
        btnQRISClick = findViewById(R.id.btnQRIS);

        imgTunai = findViewById(R.id.imgTunai);
        imgQRIS = findViewById(R.id.imgQRIS);
        tvTunai = findViewById(R.id.tvTunai);
        tvQRIS = findViewById(R.id.tvQRIS);
    }

    private void setupPaymentSelection() {
        btnTunaiClick.setOnClickListener(v -> {
            metodeTerpilih = "Tunai";
            updateMetodeUI();
        });
        btnQRISClick.setOnClickListener(v -> {
            metodeTerpilih = "QRIS";
            updateMetodeUI();
        });
        updateMetodeUI();
    }

    private void updateMetodeUI() {
        if (metodeTerpilih.equals("Tunai")) {
            cardTunai.setStrokeWidth(4);
            tvTunai.setTextColor(Color.parseColor("#2155D3"));
            imgTunai.setColorFilter(Color.parseColor("#2155D3"));

            cardQRIS.setStrokeWidth(0);
            tvQRIS.setTextColor(Color.parseColor("#888888"));
            imgQRIS.setColorFilter(Color.parseColor("#888888"));
        } else {
            cardQRIS.setStrokeWidth(4);
            tvQRIS.setTextColor(Color.parseColor("#2155D3"));
            imgQRIS.setColorFilter(Color.parseColor("#2155D3"));

            cardTunai.setStrokeWidth(0);
            tvTunai.setTextColor(Color.parseColor("#888888"));
            imgTunai.setColorFilter(Color.parseColor("#888888"));
        }
    }

    private void updateTotalUI() {
        int totalHarga = 0, totalItem = 0;
        for (item_keranjang item : listBelanja) {
            totalHarga += item.getSubtotal();
            totalItem += item.getJumlah();
        }
        tvTotalBayar.setText("Rp " + String.format("%,d", (long) totalHarga).replace(',', '.'));
        tvBadgeItem.setText(totalItem + " Item");
    }

    private void tampilkanDialogQRIS() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.activity_dialog_qris, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageView imgQris = dialogView.findViewById(R.id.img_qris_view);
        String path = dbHelper.getQrisPath(emailUserLogin);
        if (path != null) imgQris.setImageURI(Uri.parse(path));

        dialogView.findViewById(R.id.btn_selesai_qris).setOnClickListener(v -> {
            dialog.dismiss();
            eksekusiPembayaran();
        });
        dialog.show();
    }

    private void eksekusiPembayaran() {
        int totalHarga = 0;
        for (item_keranjang item : listBelanja) {
            totalHarga += item.getSubtotal();
        }

        StringBuilder sbItems = new StringBuilder();
        for (int i = 0; i < listBelanja.size(); i++) {
            item_keranjang item = listBelanja.get(i);
            sbItems.append(item.getProduk().getNama()).append(" x").append(item.getJumlah());
            if (i < listBelanja.size() - 1) sbItems.append(", ");
        }
        String namaItems = sbItems.toString();

        Calendar cal = Calendar.getInstance();
        String dd   = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
        String mm   = String.format("%02d", cal.get(Calendar.MONTH) + 1);
        String yyyy = String.valueOf(cal.get(Calendar.YEAR));

        // Simpan ke SQLite
        dbHelper.simpanTransaksi(emailUserLogin, namaItems, metodeTerpilih, totalHarga, dd, mm, yyyy);

        // Simpan ke SharedPreferences untuk dashboard
        SharedPreferences revPref = getSharedPreferences("DATA_REVENUE_" + emailUserLogin, MODE_PRIVATE);
        String key = "REV_" + mm + "_" + yyyy;
        long revLama = revPref.getLong(key, 0);
        revPref.edit().putLong(key, revLama + totalHarga).apply();

        // Ambil nama toko
        String namaToko = dbHelper.getNamaBisnis(emailUserLogin);
        if (namaToko == null || namaToko.isEmpty()) namaToko = "Kasir Pro";

        // Buka halaman Struk
        Intent intent = new Intent(this, StrukActivity.class);
        intent.putExtra("NAMA_TOKO", namaToko);
        intent.putExtra("METODE_BAYAR", metodeTerpilih);
        intent.putExtra("TOTAL_BAYAR", totalHarga);
        intent.putExtra("DATA_BELANJA", listBelanja);
        startActivity(intent);
        finish();
    }

    private void setupRecyclerView() {
        keranjangAdapter = new KeranjangAdapter(listBelanja, this::updateTotalUI);
        rvKeranjang.setLayoutManager(new LinearLayoutManager(this));
        rvKeranjang.setAdapter(keranjangAdapter);
    }
}