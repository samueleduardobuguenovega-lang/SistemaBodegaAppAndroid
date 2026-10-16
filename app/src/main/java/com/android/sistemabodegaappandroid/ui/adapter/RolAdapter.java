package com.android.sistemabodegaappandroid.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.model.Rol;

import java.util.List;

public class RolAdapter extends RecyclerView.Adapter<RolAdapter.RolViewHolder> {

    private List<Rol> roles;

    public RolAdapter(List<Rol> roles) {
        this.roles = roles;
    }

    public void updateData(List<Rol> newRoles) {
        this.roles = newRoles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rol, parent, false);
        return new RolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RolViewHolder holder, int position) {
        Rol rol = roles.get(position);
        holder.tvNombreRol.setText(rol.getNombreRol());
        holder.tvDescripcionRol.setText(rol.getDescripcion() != null && !rol.getDescripcion().isEmpty()
                ? rol.getDescripcion() : "Sin descripción");
    }

    @Override
    public int getItemCount() {
        return roles != null ? roles.size() : 0;
    }

    static class RolViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreRol;
        TextView tvDescripcionRol;

        public RolViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreRol = itemView.findViewById(R.id.tvNombreRol);
            tvDescripcionRol = itemView.findViewById(R.id.tvDescripcionRol);
        }
    }
}
