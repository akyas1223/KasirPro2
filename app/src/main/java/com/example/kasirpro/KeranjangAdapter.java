package com.example.kasirpro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class KeranjangAdapter extends RecyclerView.Adapter<KeranjangAdapter.ViewHolder> {

    private List<item_keranjang> listKeranjang;
    private OnCartChangeListener listener;

    // Interface agar Activity tahu saat ada perubahan harga (klik + atau -)
    public interface OnCartChangeListener {
        void onUpdate();
    }

    public KeranjangAdapter(List<item_keranjang> listKeranjang, OnCartChangeListener listener) {
        this.listKeranjang = listKeranjang;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Menghubungkan ke layout XML item yang sudah kamu edit
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_keranjang, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        item_keranjang item = listKeranjang.get(position);
        Produk p = item.getProduk();

        holder.tvNama.setText(p.getNama());
        holder.tvQty.setText(String.valueOf(item.getJumlah()));

        // Format angka ke Rupiah agar terlihat profesional
        String hargaSatuan = String.format("%,d", (long) p.getHarga()).replace(',', '.');
        String subTotal = String.format("%,d", (long) item.getSubtotal()).replace(',', '.');

        holder.tvHargaXQty.setText("Rp " + hargaSatuan + " X " + item.getJumlah());
        holder.tvSubtotal.setText("Rp " + subTotal);

        // --- Logika Tombol Tambah (+) ---
        holder.btnPlus.setOnClickListener(v -> {
            item.setJumlah(item.getJumlah() + 1);
            notifyItemChanged(position);
            if (listener != null) listener.onUpdate();
        });

        // --- Logika Tombol Kurang (-) ---
        holder.btnMinus.setOnClickListener(v -> {
            if (item.getJumlah() > 1) {
                item.setJumlah(item.getJumlah() - 1);
                notifyItemChanged(position);
                if (listener != null) listener.onUpdate();
            }
        });

        // --- Logika Tombol Hapus (Ikon Tong Sampah) ---
        holder.btnHapus.setOnClickListener(v -> {
            listKeranjang.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, listKeranjang.size());
            if (listener != null) listener.onUpdate();
        });
    }

    @Override
    public int getItemCount() {
        return listKeranjang.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama, tvHargaXQty, tvSubtotal, tvQty, btnPlus, btnMinus;
        ImageView btnHapus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Menghubungkan variabel dengan ID yang ada di activity_item_keranjang.xml
            tvNama = itemView.findViewById(R.id.tv_nama_item);
            tvHargaXQty = itemView.findViewById(R.id.tv_harga_kali_qty);
            tvSubtotal = itemView.findViewById(R.id.tv_subtotal_item);
            tvQty = itemView.findViewById(R.id.tv_qty_item);
            btnPlus = itemView.findViewById(R.id.btn_plus);
            btnMinus = itemView.findViewById(R.id.btn_minus);
            btnHapus = itemView.findViewById(R.id.btn_hapus_item);
        }
    }
}