package com.android.sistemabodegaappandroid.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.model.Usuario;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class UsuarioAdapter extends RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder> {

    public interface OnUserStatusChangeListener {
        void onStatusChange(Usuario usuario, boolean isChecked);
    }

    public interface OnUserActionListener {
        void onEdit(Usuario usuario);
        void onDelete(Usuario usuario);
    }

    private List<Usuario> usuarios;
    private final OnUserStatusChangeListener statusChangeListener;
    private final OnUserActionListener actionListener;

    public UsuarioAdapter(List<Usuario> usuarios, OnUserStatusChangeListener statusChangeListener, OnUserActionListener actionListener) {
        this.usuarios = usuarios;
        this.statusChangeListener = statusChangeListener;
        this.actionListener = actionListener;
    }

    public void updateData(List<Usuario> newUsuarios) {
        this.usuarios = newUsuarios;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usuario, parent, false);
        return new UsuarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        Usuario u = usuarios.get(position);
        holder.tvNombreCompleto.setText(u.getNombreCompleto());
        holder.tvUsername.setText("@" + u.getUsername());
        holder.tvRolName.setText("Rol: " + (u.getNombreRol() != null ? u.getNombreRol() : "Sin Rol"));

        // Evitar activar el listener durante la vinculación
        holder.switchActivo.setOnCheckedChangeListener(null);
        holder.switchActivo.setChecked(u.isActivo());

        holder.switchActivo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (statusChangeListener != null) {
                statusChangeListener.onStatusChange(u, isChecked);
            }
        });

        holder.btnEditUsuario.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEdit(u);
            }
        });

        holder.btnDeleteUsuario.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(u);
            }
        });
    }

    @Override
    public int getItemCount() {
        return usuarios != null ? usuarios.size() : 0;
    }

    static class UsuarioViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreCompleto;
        TextView tvUsername;
        TextView tvRolName;
        MaterialSwitch switchActivo;
        ImageView btnEditUsuario;
        ImageView btnDeleteUsuario;

        public UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreCompleto = itemView.findViewById(R.id.tvNombreCompleto);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvRolName = itemView.findViewById(R.id.tvRolName);
            switchActivo = itemView.findViewById(R.id.switchActivo);
            btnEditUsuario = itemView.findViewById(R.id.btnEditUsuario);
            btnDeleteUsuario = itemView.findViewById(R.id.btnDeleteUsuario);
        }
    }
}
