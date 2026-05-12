package com.example.kasirpro;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.view.View;

public class StrukActivity extends AppCompatActivity {

    private TextView tvNamaToko, tvTanggalStruk, tvNomorStruk;
    private TextView tvSubtotalStruk, tvMetodeStruk, tvTotalStruk;
    private LinearLayout llItemStruk;
    private MaterialButton btnBagikanStruk, btnSelesai;
    private MaterialCardView cardStruk;

    private String namaToko, metodeBayar;
    private int totalBayar;
    private ArrayList<item_keranjang> listBelanja;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_struk);

        initViews();
        ambilDataDariIntent();
        tampilkanStruk();

        btnSelesai.setOnClickListener(v -> {
            // Kembali ke beranda dan kosongkan keranjang
            Intent intent = new Intent(this, beranda.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("CLEAR_CART", true);
            startActivity(intent);
            finish();
        });

        btnBagikanStruk.setOnClickListener(v -> cetakStruk());
    }

    private void initViews() {
        tvNamaToko       = findViewById(R.id.tvNamaToko);
        tvTanggalStruk   = findViewById(R.id.tvTanggalStruk);
        tvNomorStruk     = findViewById(R.id.tvNomorStruk);
        tvSubtotalStruk  = findViewById(R.id.tvSubtotalStruk);
        tvMetodeStruk    = findViewById(R.id.tvMetodeStruk);
        tvTotalStruk     = findViewById(R.id.tvTotalStruk);
        llItemStruk      = findViewById(R.id.llItemStruk);
        btnBagikanStruk  = findViewById(R.id.btnBagikanStruk);
        btnSelesai       = findViewById(R.id.btnSelesai);
        cardStruk        = findViewById(R.id.cardStruk);
    }

    @SuppressWarnings("unchecked")
    private void ambilDataDariIntent() {
        namaToko    = getIntent().getStringExtra("NAMA_TOKO");
        metodeBayar = getIntent().getStringExtra("METODE_BAYAR");
        totalBayar  = getIntent().getIntExtra("TOTAL_BAYAR", 0);
        listBelanja = (ArrayList<item_keranjang>) getIntent().getSerializableExtra("DATA_BELANJA");

        if (namaToko   == null) namaToko   = "Kasir Pro";
        if (metodeBayar== null) metodeBayar= "Tunai";
        if (listBelanja== null) listBelanja= new ArrayList<>();
    }

    private void tampilkanStruk() {
        // Nama toko & waktu
        tvNamaToko.setText(namaToko);

        SimpleDateFormat sdfTanggal = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id", "ID"));
        tvTanggalStruk.setText(sdfTanggal.format(new Date()));

        SimpleDateFormat sdfNomor = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        tvNomorStruk.setText("No. " + sdfNomor.format(new Date()));

        // Daftar item
        llItemStruk.removeAllViews();
        int subtotal = 0;
        for (item_keranjang item : listBelanja) {
            subtotal += item.getSubtotal();
            View baris = getLayoutInflater().inflate(R.layout.item_struk_baris, llItemStruk, false);

            TextView tvNama   = baris.findViewById(R.id.tvItemNama);
            TextView tvQty    = baris.findViewById(R.id.tvItemQty);
            TextView tvHarga  = baris.findViewById(R.id.tvItemHarga);

            tvNama.setText(item.getProduk().getNama());
            tvQty.setText("x" + item.getJumlah());
            tvHarga.setText(formatRupiah(item.getSubtotal()));

            llItemStruk.addView(baris);
        }

        // Total
        tvSubtotalStruk.setText(formatRupiah(subtotal));
        tvMetodeStruk.setText(metodeBayar);
        tvTotalStruk.setText(formatRupiah(totalBayar));
    }

    private String formatRupiah(int nominal) {
        return "Rp " + String.format("%,d", (long) nominal).replace(',', '.');
    }

    private void cetakStruk() {
        try {
            PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);
            if (printManager == null) {
                Toast.makeText(this, "Printer tidak tersedia di perangkat ini", Toast.LENGTH_SHORT).show();
                return;
            }

            String jobName = "Struk_" + namaToko + "_" + new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());

            PrintDocumentAdapter adapter = new PrintDocumentAdapter() {
                Bitmap bitmap;

                @Override
                public void onLayout(PrintAttributes oldAttr, PrintAttributes newAttr,
                                     CancellationSignal cancel, LayoutResultCallback callback,
                                     Bundle extras) {
                    if (cancel.isCanceled()) { callback.onLayoutCancelled(); return; }
                    // Render cardStruk ke bitmap
                    cardStruk.setDrawingCacheEnabled(true);
                    bitmap = Bitmap.createBitmap(cardStruk.getWidth(), cardStruk.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    cardStruk.draw(canvas);
                    cardStruk.setDrawingCacheEnabled(false);

                    PrintDocumentInfo info = new PrintDocumentInfo.Builder(jobName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build();
                    callback.onLayoutFinished(info, !newAttr.equals(oldAttr));
                }

                @Override
                public void onWrite(PageRange[] pages, ParcelFileDescriptor dest,
                                    CancellationSignal cancel, WriteResultCallback callback) {
                    PdfDocument pdf = new PdfDocument();
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                            bitmap.getWidth(), bitmap.getHeight(), 1).create();
                    PdfDocument.Page page = pdf.startPage(pageInfo);
                    page.getCanvas().drawBitmap(bitmap, 0, 0, null);
                    pdf.finishPage(page);
                    try {
                        pdf.writeTo(new FileOutputStream(dest.getFileDescriptor()));
                    } catch (Exception e) {
                        callback.onWriteFailed(e.toString());
                        return;
                    } finally {
                        pdf.close();
                    }
                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                }
            };

            printManager.print(jobName, adapter, new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                    .build());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal mencetak struk: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}