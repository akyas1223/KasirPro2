package com.example.kasirpro;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class beranda extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private RecyclerView rvProduk;
    private ProdukAdapter produkAdapter;
    private List<Produk> produkList;
    private List<Produk> fullProdukList;
    private DatabaseHelper dbHelper;
    private String emailUserLogin;

    private int totalHarga = 0;
    private int totalItem = 0;

    // UI Components
    private TextView tvTotalBayar, tvTotalItem;
    private EditText etCariProduk;
    private View btnBayarUtama;

    private ArrayList<item_keranjang> listBelanja = new ArrayList<>();
    private final int REQ_BAYAR = 101;
    private Produk selectedProduk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_beranda);

        dbHelper = new DatabaseHelper(this);

        // Inisialisasi View
        initViews();

        // Ambil Session Login
        SharedPreferences session = getSharedPreferences("SESSION_KASIR", MODE_PRIVATE);
        emailUserLogin = session.getString("logged_in_user", null);

        if (emailUserLogin == null || emailUserLogin.isEmpty()) {
            keHalamanLogin();
            return;
        }

        // Setup Header Navigation Drawer
        setupNavigationDrawer(session);

        // --- RECYCLERVIEW SETUP ---
        produkList = new ArrayList<>();
        fullProdukList = new ArrayList<>();

        produkAdapter = new ProdukAdapter(this, produkList, emailUserLogin,
                this::updateKeranjang, this::onProductLongClick,
                () -> {
                    // Saat card Tambah Produk diklik, tandai perlu refresh lalu buka activity
                    tandaiPerluRefresh();
                    Intent intent = new Intent(beranda.this, TambahProduk.class);
                    intent.putExtra("EMAIL_USER", emailUserLogin);
                    startActivity(intent);
                });
        dataNeedsRefresh = true;

        if (rvProduk != null) {
            // 2 kolom untuk produk, item "Tambah Produk" (posisi 0) span penuh 2 kolom
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    // Posisi 0 = Tambah Produk -> span 2 (penuh 1 baris sendiri, lebih besar)
                    return (position == 0) ? 2 : 1;
                }
            });
            rvProduk.setLayoutManager(gridLayoutManager);
            rvProduk.setAdapter(produkAdapter);
        }

        // --- SEARCH FILTER ---
        if (etCariProduk != null) {
            etCariProduk.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // --- BUTTON BAYAR ---
        if (btnBayarUtama != null) {
            btnBayarUtama.setOnClickListener(v -> {
                if (listBelanja.isEmpty()) {
                    Toast.makeText(this, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show();
                } else {
                    dataNeedsRefresh = false; // ke halaman bayar, gambar tidak perlu reload
                    Intent intent = new Intent(beranda.this, activity_ringkasan_pembayaran.class);
                    intent.putExtra("DATA_BELANJA", listBelanja);
                    intent.putExtra("EMAIL_USER", emailUserLogin);
                    startActivityForResult(intent, REQ_BAYAR);
                }
            });
        }

        // Handle Back Press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        etCariProduk = findViewById(R.id.etCariProduk);
        tvTotalBayar = findViewById(R.id.tvTotalBayar);
        tvTotalItem = findViewById(R.id.tvTotalItem);
        rvProduk = findViewById(R.id.rv_produk);
        btnBayarUtama = findViewById(R.id.btnBayarUtama);

        ImageView btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null && drawerLayout != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
    }

    private void setupNavigationDrawer(SharedPreferences session) {
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            String namaLogin = dbHelper.getNamaBisnis(emailUserLogin);
            View headerView = navigationView.getHeaderView(0);
            TextView tvNama = headerView.findViewById(R.id.tv_nama_profil);
            TextView tvEmail = headerView.findViewById(R.id.tv_email_profil);

            if (tvNama != null) tvNama.setText(namaLogin != null ? namaLogin : "Kasir");
            if (tvEmail != null) tvEmail.setText(emailUserLogin);

            // Klik profil/foto di header drawer → buka EditProfil
            View cardProfile = headerView.findViewById(R.id.cardProfile);
            if (cardProfile != null) {
                cardProfile.setOnClickListener(v -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    startActivityForResult(new Intent(beranda.this, EditProfilActivity.class), 200);
                });
            }

            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_logout) {
                    SharedPreferences.Editor editor = session.edit();
                    editor.clear();
                    editor.apply();
                    keHalamanLogin();
                } else if (id == R.id.nav_dashboard) {
                    Intent intent = new Intent(this, dashboard.class);
                    intent.putExtra("EMAIL_USER", emailUserLogin);
                    startActivity(intent);
                } else if (id == R.id.nav_riwayat) {
                    Intent intent = new Intent(this, riwayat_transaksi.class);
                    intent.putExtra("EMAIL_USER", emailUserLogin);
                    startActivity(intent);
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }
    }

    // --- LOGIKA EDIT & DELETE dengan Bottom Sheet ---
    private void onProductLongClick(Produk produk) {
        selectedProduk = produk;
        tampilkanBottomSheetOpsi(produk);
    }

    private void tampilkanBottomSheetOpsi(Produk produk) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_produk_opsi, null);
        bottomSheet.setContentView(sheetView);

        TextView tvNama = sheetView.findViewById(R.id.tvNamaProdukSheet);
        tvNama.setText(produk.getNama());

        LinearLayout btnEdit = sheetView.findViewById(R.id.btnSheetEdit);
        btnEdit.setOnClickListener(v -> {
            bottomSheet.dismiss();
            tandaiPerluRefresh();
            Intent intent = new Intent(beranda.this, TambahProduk.class);
            intent.putExtra("IS_EDIT", true);
            intent.putExtra("NAMA_PRODUK", produk.getNama());
            startActivity(intent);
        });

        LinearLayout btnHapus = sheetView.findViewById(R.id.btnSheetHapus);
        btnHapus.setOnClickListener(v -> {
            bottomSheet.dismiss();
            konfirmasiHapus(produk);
        });

        View btnBatal = sheetView.findViewById(R.id.btnSheetBatal);
        btnBatal.setOnClickListener(v -> bottomSheet.dismiss());

        // FIX: Reset efek visual (alpha & scale) saat bottom sheet ditutup dengan cara apapun
        bottomSheet.setOnDismissListener(dialog -> produkAdapter.resetSelectedPosition());

        bottomSheet.show();
    }

    private void konfirmasiHapus(Produk produk) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Produk")
                .setMessage("Yakin ingin menghapus \"" + produk.getNama() + "\"?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    if (dbHelper.hapusProduk(produk.getNama(), emailUserLogin)) {
                        Toast.makeText(this, produk.getNama() + " berhasil dihapus", Toast.LENGTH_SHORT).show();
                        loadDataProduk(); // langsung reload sekarang
                        dataNeedsRefresh = false; // onResume tidak perlu reload lagi
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // --- SISTEM KERANJANG ---
    private void updateKeranjang(Produk produk) {
        boolean sdhAda = false;
        for (item_keranjang item : listBelanja) {
            if (item.getProduk().getNama().equals(produk.getNama())) {
                item.setJumlah(item.getJumlah() + 1);
                sdhAda = true;
                break;
            }
        }
        if (!sdhAda) listBelanja.add(new item_keranjang(produk, 1));
        refreshBottomBar();
    }

    private void refreshBottomBar() {
        totalHarga = 0;
        totalItem = 0;
        for (item_keranjang item : listBelanja) {
            totalHarga += item.getSubtotal();
            totalItem += item.getJumlah();
        }

        String formattedHarga = String.format("%,d", (long) totalHarga).replace(',', '.');
        if (tvTotalBayar != null) tvTotalBayar.setText("Rp " + formattedHarga);
        if (tvTotalItem != null) tvTotalItem.setText(totalItem + " Item");

        if (btnBayarUtama != null) {
            btnBayarUtama.setAlpha(totalItem == 0 ? 0.5f : 1.0f);
            btnBayarUtama.setEnabled(totalItem != 0);
        }
    }

    private void loadDataProduk() {
        produkList.clear();
        fullProdukList.clear();
        Cursor cursor = dbHelper.getProdukByUser(emailUserLogin);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String kategori = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_KATEGORI));
                String nama = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAMA_PRODUK));
                int harga = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HARGA_PRODUK));
                String gambar = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GAMBAR_PRODUK));
                Produk p = new Produk(kategori, nama, harga, gambar);
                produkList.add(p);
                fullProdukList.add(p);
            }
            cursor.close();
        }
        produkAdapter.notifyDataSetChanged();
    }

    private void filter(String text) {
        List<Produk> filteredList = new ArrayList<>();
        for (Produk item : fullProdukList) {
            if (item.getNama().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        produkAdapter.filterList(filteredList);
    }

    private void keHalamanLogin() {
        Intent intent = new Intent(this, login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK) {
            // Profil diperbarui — refresh nama di header drawer
            NavigationView navigationView = findViewById(R.id.nav_view);
            if (navigationView != null) {
                View headerView = navigationView.getHeaderView(0);
                TextView tvNama = headerView.findViewById(R.id.tv_nama_profil);
                String namaBaru = dbHelper.getNamaBisnis(emailUserLogin);
                if (tvNama != null) tvNama.setText(namaBaru);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("CLEAR_CART", false)) {
            kosongkanKeranjang();
        }
    }

    private boolean dataNeedsRefresh = true;

    @Override
    protected void onResume() {
        super.onResume();
        // Reset highlight menu drawer ke Home setiap kali beranda aktif
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_home);
        }

        if (dataNeedsRefresh) {
            loadDataProduk();
            dataNeedsRefresh = false;
        }
    }

    // Dipanggil saat kembali dari StrukActivity — kosongkan keranjang
    public void kosongkanKeranjang() {
        listBelanja.clear();
        refreshBottomBar();
    }

    // Panggil ini setelah edit/hapus/tambah agar data di-refresh
    private void tandaiPerluRefresh() {
        dataNeedsRefresh = true;
    }
}