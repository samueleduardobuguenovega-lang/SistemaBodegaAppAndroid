package com.android.sistemabodegaappandroid.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.model.Proveedor;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adaptador del listado de proveedores.
 *
 * Expone dos acciones separadas: tocar la tarjeta para editar y pulsar el
 * botón de deshabilitar, que solo apaga el registro sin perder sus datos.
 */
public class ProveedorAdapter extends RecyclerView.Adapter<ProveedorAdapter.ProveedorViewHolder> {

    /** Acciones disponibles sobre cada fila del listado. */
    public interface OnProveedorActionListener {
        void onProveedorClick(Proveedor proveedor);

        void onDeshabilitar(Proveedor proveedor);
    }

    private List<Proveedor> proveedores;
    private final OnProveedorActionListener listener;

    public ProveedorAdapter(List<Proveedor> proveedores, OnProveedorActionListener listener) {
        this.proveedores = proveedores;
        this.listener = listener;
    }

    public void updateData(List<Proveedor> nuevos) {
        this.proveedores = nuevos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProveedorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_proveedor, parent, false);
        return new ProveedorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProveedorViewHolder holder, int position) {
        Proveedor proveedor = proveedores.get(position);

        holder.tvRazonSocial.setText(proveedor.getRazonSocial());
        holder.tvRut.setText(proveedor.getRut());
        holder.tvEmail.setText(proveedor.getEmail());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProveedorClick(proveedor);
            }
        });
        holder.btnDeshabilitar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeshabilitar(proveedor);
            }
        });
    }

    @Override
    public int getItemCount() {
        return proveedores != null ? proveedores.size() : 0;
    }

    static class ProveedorViewHolder extends RecyclerView.ViewHolder {
        TextView tvRazonSocial;
        TextView tvRut;
        TextView tvEmail;
        MaterialButton btnDeshabilitar;

        public ProveedorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRazonSocial = itemView.findViewById(R.id.tvRazonSocialProveedor);
            tvRut = itemView.findViewById(R.id.tvRutProveedor);
            tvEmail = itemView.findViewById(R.id.tvEmailProveedor);
            btnDeshabilitar = itemView.findViewById(R.id.btnDeshabilitarProveedor);
        }
    }
}
