package com.android.sistemabodegaappandroid.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.model.Movimiento;

import java.util.Collections;
import java.util.List;

/**
 * Adaptador para la lista general de historial de movimientos (RF-21 / CU-06).
 */
public class MovimientoGeneralAdapter extends ListAdapter<Movimiento, MovimientoGeneralAdapter.MovimientoViewHolder> {

    private static final DiffUtil.ItemCallback<Movimiento> DIFF = new DiffUtil.ItemCallback<Movimiento>() {
        @Override
        public boolean areItemsTheSame(@NonNull Movimiento a, @NonNull Movimiento b) {
            return a.getIdMovimiento() == b.getIdMovimiento();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Movimiento a, @NonNull Movimiento b) {
            return a.getIdProducto() == b.getIdProducto()
                    && a.getCantidad() == b.getCantidad()
                    && igual(a.getNombreProducto(), b.getNombreProducto())
                    && igual(a.getTipo(), b.getTipo())
                    && igual(a.getUsuario(), b.getUsuario())
                    && igual(a.getFecha(), b.getFecha());
        }

        private boolean igual(Object a, Object b) {
            return a == null ? b == null : a.equals(b);
        }
    };

    public MovimientoGeneralAdapter() {
        super(DIFF);
    }

    public void updateData(@Nullable List<Movimiento> lista) {
        submitList(lista != null ? lista : Collections.emptyList());
    }

    @NonNull
    @Override
    public MovimientoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movimiento_general, parent, false);
        return new MovimientoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovimientoViewHolder holder, int position) {
        Movimiento mov = getItem(position);

        boolean esEntrada = "ENTRADA".equalsIgnoreCase(mov.getTipo());
        holder.tvTipoBadge.setText(mov.getTipo() != null ? mov.getTipo().toUpperCase() : "MOVIMIENTO");
        holder.tvTipoBadge.setBackgroundTintList(ColorStateList.valueOf(
                esEntrada ? Color.parseColor("#2E7D32") : Color.parseColor("#D32F2F")));

        String signo = esEntrada ? "+" : "-";
        holder.tvCantidad.setText(signo + mov.getCantidad() + " unidades");
        holder.tvFecha.setText(mov.getFecha() != null ? mov.getFecha() : "");

        // El nombre sale del JOIN a PRODUCTO; si la fila quedó huérfana se
        // vuelve al id para no mostrar una línea vacía.
        String nombreProducto = mov.getNombreProducto();
        if (nombreProducto.isEmpty()) {
            holder.tvProducto.setText("Producto ID #" + mov.getIdProducto());
        } else {
            String sku = mov.getSkuCodigo();
            holder.tvProducto.setText(sku.isEmpty() ? nombreProducto : nombreProducto + " · " + sku);
        }
        holder.tvUsuario.setText("Usuario: @" + (mov.getUsuario() != null ? mov.getUsuario() : "sistema"));

        if (esEntrada && mov.getRazonSocialProveedor() != null && !mov.getRazonSocialProveedor().trim().isEmpty()) {
            holder.tvProveedor.setVisibility(View.VISIBLE);
            holder.tvProveedor.setText("Proveedor: " + mov.getRazonSocialProveedor());
        } else {
            holder.tvProveedor.setVisibility(View.GONE);
        }
    }

    static class MovimientoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipoBadge;
        TextView tvCantidad;
        TextView tvFecha;
        TextView tvProducto;
        TextView tvUsuario;
        TextView tvProveedor;

        public MovimientoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipoBadge = itemView.findViewById(R.id.tvMovTipoBadge);
            tvCantidad = itemView.findViewById(R.id.tvMovCantidad);
            tvFecha = itemView.findViewById(R.id.tvMovFecha);
            tvProducto = itemView.findViewById(R.id.tvMovProducto);
            tvUsuario = itemView.findViewById(R.id.tvMovUsuario);
            tvProveedor = itemView.findViewById(R.id.tvMovProveedor);
        }
    }
}
