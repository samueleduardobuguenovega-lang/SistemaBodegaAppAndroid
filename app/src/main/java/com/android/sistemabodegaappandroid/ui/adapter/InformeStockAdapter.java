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
import com.android.sistemabodegaappandroid.model.Producto;

import java.util.Collections;
import java.util.List;

/**
 * Adaptador para el reporte de stock (CU-06 / RF-19 / RF-22).
 * Muestra existencias actuales, stock mínimo y estado de disponibilidad.
 */
public class InformeStockAdapter extends ListAdapter<Producto, InformeStockAdapter.InformeViewHolder> {

    public interface OnInformeClickListener {
        void onInformeClick(Producto producto);
    }

    private static final DiffUtil.ItemCallback<Producto> DIFF = new DiffUtil.ItemCallback<Producto>() {
        @Override
        public boolean areItemsTheSame(@NonNull Producto a, @NonNull Producto b) {
            return a.getIdProducto() == b.getIdProducto();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Producto a, @NonNull Producto b) {
            return a.getStockActual() == b.getStockActual()
                    && a.getStockMinimo() == b.getStockMinimo()
                    && a.getActivo() == b.getActivo()
                    && igual(a.getNombre(), b.getNombre())
                    && igual(a.getSkuCodigo(), b.getSkuCodigo())
                    && igual(a.getNombreCategoria(), b.getNombreCategoria());
        }

        private boolean igual(Object a, Object b) {
            return a == null ? b == null : a.equals(b);
        }
    };

    private final OnInformeClickListener clickListener;

    public InformeStockAdapter(@Nullable OnInformeClickListener clickListener) {
        super(DIFF);
        this.clickListener = clickListener;
    }

    public void updateData(@Nullable List<Producto> lista) {
        submitList(lista != null ? lista : Collections.emptyList());
    }

    @NonNull
    @Override
    public InformeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_informe_stock, parent, false);
        return new InformeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InformeViewHolder holder, int position) {
        Producto producto = getItem(position);

        holder.tvNombre.setText(producto.getNombre());
        holder.tvSku.setText(holder.itemView.getContext().getString(R.string.label_sku, producto.getSkuCodigo()));
        holder.tvCategoria.setText(holder.itemView.getContext().getString(R.string.label_categoria,
                producto.getNombreCategoria() != null ? producto.getNombreCategoria() : holder.itemView.getContext().getString(R.string.sin_categoria)));

        String stockTexto = holder.itemView.getContext().getString(R.string.label_stock, producto.getStockActual());
        holder.tvStockActual.setText(stockTexto);
        holder.tvStockMinimo.setText(holder.itemView.getContext().getString(R.string.hint_stock_minimo) + ": " + producto.getStockMinimo());

        // Clasificación de estado de disponibilidad (RF-19)
        int stockActual = producto.getStockActual();
        int stockMinimo = producto.getStockMinimo();

        if (stockActual <= 0) {
            holder.tvBadgeEstado.setText(R.string.filter_estado_sin_stock);
            holder.tvBadgeEstado.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F"))); // Rojo
        } else if (stockActual <= stockMinimo) {
            holder.tvBadgeEstado.setText(R.string.filter_estado_bajo);
            holder.tvBadgeEstado.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E65100"))); // Naranja/Ámbar
        } else {
            holder.tvBadgeEstado.setText(R.string.filter_estado_ok);
            holder.tvBadgeEstado.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32"))); // Verde
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onInformeClick(producto);
            }
        });
    }

    static class InformeViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        TextView tvSku;
        TextView tvCategoria;
        TextView tvBadgeEstado;
        TextView tvStockActual;
        TextView tvStockMinimo;

        public InformeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvInformeNombreProducto);
            tvSku = itemView.findViewById(R.id.tvInformeSku);
            tvCategoria = itemView.findViewById(R.id.tvInformeCategoria);
            tvBadgeEstado = itemView.findViewById(R.id.tvInformeBadgeEstado);
            tvStockActual = itemView.findViewById(R.id.tvInformeStockActual);
            tvStockMinimo = itemView.findViewById(R.id.tvInformeStockMinimo);
        }
    }
}
