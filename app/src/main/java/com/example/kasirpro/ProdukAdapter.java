package com.example.kasirpro;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.util.ArrayList;
import java.util.List;

public class ProdukAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnTambahClickListener {
        void onTambahClick(Produk produk);
    }

    public interface OnProductLongClickListener {
        void onProductLongClick(Produk produk);
    }

    // Callback khusus saat card "Tambah Produk" diklik
    public interface OnTambahProdukBaruListener {
        void onTambahProdukBaru();
    }

    private Context context;
    private List<Produk> produkList;
    private String emailUserLogin;
    private OnTambahClickListener listener;
    private OnProductLongClickListener longClickListener;
    private OnTambahProdukBaruListener tambahProdukBaruListener;

    private int selectedPosition = -1;

    private static final int TYPE_ADD = 0;
    private static final int TYPE_ITEM = 1;

    public ProdukAdapter(Context context, List<Produk> produkList, String emailUserLogin,
                         OnTambahClickListener listener, OnProductLongClickListener longClickListener,
                         OnTambahProdukBaruListener tambahProdukBaruListener) {
        this.context = context;
        this.produkList = produkList;
        this.emailUserLogin = emailUserLogin;
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.tambahProdukBaruListener = tambahProdukBaruListener;
    }

    public void resetSelectedPosition() {
        int prev = this.selectedPosition;
        this.selectedPosition = -1;
        if (prev != -1) notifyItemChanged(prev);
    }

    public void setSelectedPosition(int position) {
        int previousSelected = this.selectedPosition;
        this.selectedPosition = position;
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected);
        }
        if (this.selectedPosition != -1) {
            notifyItemChanged(this.selectedPosition);
        }
    }

    // FIX: filterList tidak lagi mengganti referensi list (mencegah crash IndexOutOfBounds)
    public void filterList(List<Produk> filteredList) {
        this.produkList = new ArrayList<>(filteredList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? TYPE_ADD : TYPE_ITEM;
    }

    // FIX: Stabilkan item ID agar RecyclerView tidak salah recycling view
    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD) {
            View view = LayoutInflater.from(context).inflate(R.layout.activity_item_tambah, parent, false);
            return new AddViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.activity_item_produk, parent, false);
            return new ProdukViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                if (tambahProdukBaruListener != null) {
                    tambahProdukBaruListener.onTambahProdukBaru();
                }
            });
        } else if (holder instanceof ProdukViewHolder) {
            // FIX: Guard agar tidak crash jika position di luar range (bisa terjadi saat filter)
            int dataIndex = position - 1;
            if (dataIndex < 0 || dataIndex >= produkList.size()) return;

            Produk produk = produkList.get(dataIndex);
            ProdukViewHolder pVh = (ProdukViewHolder) holder;

            pVh.tvNama.setText(produk.getNama());
            pVh.tvHarga.setText("Rp " + String.format("%,d", (long) produk.getHarga()).replace(',', '.'));
            pVh.tvKategori.setText(produk.getKategori());

            // Warna Kategori Dinamis
            String kategori = produk.getKategori();
            if (kategori != null && !kategori.isEmpty()) {
                int warnaDinamis = generateColorFromText(kategori);
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setCornerRadius(15f);
                shape.setColor(warnaDinamis);
                pVh.tvKategori.setBackground(shape);
                pVh.tvKategori.setTextColor(Color.WHITE);
            }

            // Efek visual saat item dipilih
            if (selectedPosition == position) {
                pVh.itemView.setAlpha(0.5f);
                pVh.itemView.setScaleX(0.95f);
                pVh.itemView.setScaleY(0.95f);
            } else {
                pVh.itemView.setAlpha(1.0f);
                pVh.itemView.setScaleX(1.0f);
                pVh.itemView.setScaleY(1.0f);
            }

            pVh.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    setSelectedPosition(position);
                    longClickListener.onProductLongClick(produk);
                }
                return true;
            });

            pVh.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onTambahClick(produk);
            });

            pVh.btnTambahKeranjang.setOnClickListener(v -> {
                if (listener != null) listener.onTambahClick(produk);
            });

            // Muat gambar dengan Glide secara aman
            if (produk.getGambar() != null && !produk.getGambar().trim().isEmpty()) {
                try {
                    Object imageSource;
                    String gambarPath = produk.getGambar().trim();
                    if (gambarPath.startsWith("/")) {
                        // Path file lokal — load sebagai File
                        imageSource = new java.io.File(gambarPath);
                    } else {
                        // URI string lama
                        imageSource = android.net.Uri.parse(gambarPath);
                    }
                    Glide.with(context)
                            .load(imageSource)
                            .override(400, 400)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .centerCrop()
                            .into(pVh.imgProduk);
                } catch (Exception e) {
                    pVh.imgProduk.setImageResource(android.R.drawable.ic_menu_report_image);
                }
            } else {
                pVh.imgProduk.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }

    private int generateColorFromText(String text) {
        int hash = text.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = (hash & 0x0000FF);
        float[] hsv = new float[3];
        Color.RGBToHSV(r, g, b, hsv);
        hsv[1] = 0.5f;
        hsv[2] = 0.8f;
        return Color.HSVToColor(hsv);
    }

    @Override
    public int getItemCount() {
        return produkList.size() + 1;
    }

    public static class ProdukViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama, tvHarga, tvKategori;
        ImageView imgProduk;
        MaterialCardView btnTambahKeranjang; // FIX: Sesuaikan tipe dengan XML (MaterialCardView, bukan ImageView)

        public ProdukViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNama = itemView.findViewById(R.id.tv_nama_produk);
            tvHarga = itemView.findViewById(R.id.tv_harga_produk);
            tvKategori = itemView.findViewById(R.id.tv_kategori_produk);
            imgProduk = itemView.findViewById(R.id.img_produk);
            btnTambahKeranjang = itemView.findViewById(R.id.btn_tambah_ke_keranjang);
        }
    }

    public static class AddViewHolder extends RecyclerView.ViewHolder {
        public AddViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}