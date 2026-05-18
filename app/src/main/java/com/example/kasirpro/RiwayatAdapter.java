package com.example.kasirpro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RiwayatAdapter extends RecyclerView.Adapter<RiwayatAdapter.RiwayatViewHolder> {

    private List<Transaksi> transaksiList;

    public RiwayatAdapter(List<Transaksi> transaksiList) {
        this.transaksiList = transaksiList;
    }

    @NonNull
    @Override
    public RiwayatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_riwayat, parent, false);
        return new RiwayatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RiwayatViewHolder holder, int position) {
        Transaksi transaksi = transaksiList.get(position);

        holder.tvNamaItem.setText(transaksi.getNamaItems());
        holder.tvMetode.setText(transaksi.getMetodeBayar());

        String formattedHarga = String.format("%,d", (long) transaksi.getTotalBayar()).replace(',', '.');
        holder.tvTotal.setText("Rp " + formattedHarga);

        if (holder.tvWaktu != null) {
            holder.tvWaktu.setText(transaksi.getJam());
        }
    }

    @Override
    public int getItemCount() {
        return transaksiList != null ? transaksiList.size() : 0;
    }

    public void updateList(List<Transaksi> newList) {
        this.transaksiList = newList;
        notifyDataSetChanged();
    }

    public static class RiwayatViewHolder extends RecyclerView.ViewHolder {
        TextView tvNamaItem, tvMetode, tvTotal, tvWaktu;

        public RiwayatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNamaItem = itemView.findViewById(R.id.tv_nama_item);
            tvMetode   = itemView.findViewById(R.id.tv_metode);
            tvTotal    = itemView.findViewById(R.id.tv_total_harga);
            tvWaktu    = itemView.findViewById(R.id.tv_waktu);
        }
    }
}