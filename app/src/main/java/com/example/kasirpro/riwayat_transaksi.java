package com.example.kasirpro;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;

public class riwayat_transaksi extends AppCompatActivity {

    private EditText etDD, etMM, etYYYY;
    private RecyclerView rvRiwayat;
    private RiwayatAdapter adapter;
    private ArrayList<Transaksi> listTransaksi = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private String emailUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat_transaksi);

        dbHelper = new DatabaseHelper(this);

        // --- AMBIL EMAIL USER (FIX BUG 1) ---
        // Coba ambil dari Intent dulu
        emailUser = getIntent().getStringExtra("EMAIL_USER");

        // Jika kosong, ambil dari SharedPreferences
        if (emailUser == null || emailUser.isEmpty()) {
            SharedPreferences session = getSharedPreferences("SESSION_KASIR", MODE_PRIVATE);
            emailUser = session.getString("logged_in_user", "");
        }

        // Jika masih kosong, kembali ke login
        if (emailUser == null || emailUser.isEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login kembali", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupDefaultDate();
        setupRecyclerView();
        setupListeners();

        muatDataRiwayat();
    }

    private void initViews() {
        etDD = findViewById(R.id.et_dd);
        etMM = findViewById(R.id.et_mm);
        etYYYY = findViewById(R.id.et_yyyy);
        rvRiwayat = findViewById(R.id.rv_riwayat);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupDefaultDate() {
        Calendar cal = Calendar.getInstance();
        etDD.setText(String.format("%02d", cal.get(Calendar.DAY_OF_MONTH)));
        etMM.setText(String.format("%02d", cal.get(Calendar.MONTH) + 1));
        etYYYY.setText(String.valueOf(cal.get(Calendar.YEAR)));
    }

    private void setupRecyclerView() {
        rvRiwayat.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RiwayatAdapter(listTransaksi);
        rvRiwayat.setAdapter(adapter);
    }

    private void setupListeners() {
        TextWatcher filterWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                muatDataRiwayat();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etDD.addTextChangedListener(filterWatcher);
        etMM.addTextChangedListener(filterWatcher);
        etYYYY.addTextChangedListener(filterWatcher);
    }

    private void muatDataRiwayat() {
        String dd = etDD.getText().toString().trim();
        String mm = etMM.getText().toString().trim();
        String yyyy = etYYYY.getText().toString().trim();

        // Validasi tanggal tidak boleh kosong
        if (dd.isEmpty() || mm.isEmpty() || yyyy.isEmpty()) {
            return;
        }

        try {
            Cursor cursor = dbHelper.getRiwayat(emailUser, dd, mm, yyyy);
            listTransaksi.clear();

            // FIX BUG 3: Cek cursor tidak null dan ada datanya
            if (cursor != null && cursor.getCount() > 0) {
                // Cari nama kolom (FIX BUG 4)
                int idxItems = cursor.getColumnIndex(DatabaseHelper.COL_NAMA_ITEM_TRANS);
                int idxMetode = cursor.getColumnIndex(DatabaseHelper.COL_METODE_BAYAR);
                int idxTotal = cursor.getColumnIndex(DatabaseHelper.COL_TOTAL_BAYAR);

                // Jika kolom tidak ditemukan, coba dengan nama alternatif
                if (idxItems == -1) idxItems = cursor.getColumnIndex("items");
                if (idxMetode == -1) idxMetode = cursor.getColumnIndex("metode_bayar");
                if (idxTotal == -1) idxTotal = cursor.getColumnIndex("total");

                // Jika masih tidak ditemukan, kasih nilai default
                while (cursor.moveToNext()) {
                    String items = (idxItems != -1) ? cursor.getString(idxItems) : "-";
                    String metode = (idxMetode != -1) ? cursor.getString(idxMetode) : "-";
                    int total = (idxTotal != -1) ? cursor.getInt(idxTotal) : 0;

                    listTransaksi.add(new Transaksi(items, metode, total));
                }
            }

            if (cursor != null) {
                cursor.close();
            }

            adapter.notifyDataSetChanged();

            // Kasih tahu user kalau tidak ada data
            if (listTransaksi.isEmpty()) {
                Toast.makeText(this, "Tidak ada transaksi pada tanggal ini", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("RIWAYAT_ERROR", "Error: " + e.getMessage());
            Toast.makeText(this, "Gagal memuat riwayat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}