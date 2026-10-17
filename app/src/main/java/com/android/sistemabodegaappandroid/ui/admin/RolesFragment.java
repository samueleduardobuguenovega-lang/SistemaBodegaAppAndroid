package com.android.sistemabodegaappandroid.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.model.Rol;
import com.android.sistemabodegaappandroid.ui.adapter.RolAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class RolesFragment extends Fragment {

    private RecyclerView rvRoles;
    private TextView tvEmptyRoles;
    private FloatingActionButton fabAddRole;
    private DatabaseHelper dbHelper;
    private RolAdapter adapter;
    private List<Rol> rolesList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_roles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        rvRoles = view.findViewById(R.id.rvRoles);
        tvEmptyRoles = view.findViewById(R.id.tvEmptyRoles);
        fabAddRole = view.findViewById(R.id.fabAddRole);

        rvRoles.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RolAdapter(rolesList, new RolAdapter.OnRolActionListener() {
            @Override
            public void onEdit(Rol rol) {
                showEditRoleDialog(rol);
            }

            @Override
            public void onDelete(Rol rol) {
                confirmDeleteRole(rol);
            }
        });
        rvRoles.setAdapter(adapter);

        fabAddRole.setOnClickListener(v -> showAddRoleDialog());

        dbHelper.observarRoles().observe(getViewLifecycleOwner(), lista -> {
            if (lista == null) {
                return;
            }
            rolesList = lista;
            if (rolesList.isEmpty()) {
                tvEmptyRoles.setVisibility(View.VISIBLE);
                rvRoles.setVisibility(View.GONE);
            } else {
                tvEmptyRoles.setVisibility(View.GONE);
                rvRoles.setVisibility(View.VISIBLE);
                adapter.updateData(rolesList);
            }
        });

        loadRoles();
    }

    public void loadRoles() {
        dbHelper.refrescarRoles();
    }

    private void showAddRoleDialog() {
        abrirDialogoRol(null);
    }

    private void showEditRoleDialog(Rol rol) {
        abrirDialogoRol(rol);
    }

    private void abrirDialogoRol(@Nullable Rol rolEditar) {
        boolean esEdicion = rolEditar != null;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_rol, null);
        EditText etNombreRol = dialogView.findViewById(R.id.etNombreRol);
        EditText etDescripcionRol = dialogView.findViewById(R.id.etDescripcionRol);
        CheckBox cbUsuarios = dialogView.findViewById(R.id.cbPermisoUsuarios);
        CheckBox cbProductos = dialogView.findViewById(R.id.cbPermisoProductos);
        CheckBox cbCategorias = dialogView.findViewById(R.id.cbPermisoCategorias);
        CheckBox cbProveedores = dialogView.findViewById(R.id.cbPermisoProveedores);
        CheckBox cbMovimientos = dialogView.findViewById(R.id.cbPermisoMovimientos);

        if (esEdicion) {
            etNombreRol.setText(rolEditar.getNombreRol());
            etDescripcionRol.setText(rolEditar.getDescripcion());
            String p = rolEditar.getPermisos() != null ? rolEditar.getPermisos() : "";
            cbUsuarios.setChecked(p.contains("USUARIOS"));
            cbProductos.setChecked(p.contains("PRODUCTOS"));
            cbCategorias.setChecked(p.contains("CATEGORIAS"));
            cbProveedores.setChecked(p.contains("PROVEEDORES"));
            cbMovimientos.setChecked(p.contains("MOVIMIENTOS"));
        } else {
            cbProductos.setChecked(true);
            cbCategorias.setChecked(true);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(esEdicion ? "Editar Rol" : getString(R.string.add_role))
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String nombreRol = etNombreRol.getText() != null ? etNombreRol.getText().toString().trim() : "";
                    String descripcion = etDescripcionRol.getText() != null ? etDescripcionRol.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(nombreRol)) {
                        Toast.makeText(requireContext(), "El nombre del rol es obligatorio", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!esEdicion && dbHelper.isRoleNameExists(nombreRol)) {
                        Toast.makeText(requireContext(), "El nombre del rol ya existe (debe ser único)", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (esEdicion && !nombreRol.equalsIgnoreCase(rolEditar.getNombreRol()) && dbHelper.isRoleNameExists(nombreRol)) {
                        Toast.makeText(requireContext(), "El nombre del rol ya existe (debe ser único)", Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<String> perms = new ArrayList<>();
                    if (cbUsuarios.isChecked()) perms.add("USUARIOS");
                    if (cbProductos.isChecked()) perms.add("PRODUCTOS");
                    if (cbCategorias.isChecked()) perms.add("CATEGORIAS");
                    if (cbProveedores.isChecked()) perms.add("PROVEEDORES");
                    if (cbMovimientos.isChecked()) perms.add("MOVIMIENTOS");
                    String permisosStr = TextUtils.join(",", perms);

                    if (esEdicion) {
                        rolEditar.setNombreRol(nombreRol);
                        rolEditar.setDescripcion(descripcion);
                        rolEditar.setPermisos(permisosStr);
                        boolean updated = dbHelper.updateRol(rolEditar);
                        if (updated) {
                            Toast.makeText(requireContext(), "Rol actualizado con éxito", Toast.LENGTH_SHORT).show();
                            loadRoles();
                        } else {
                            Toast.makeText(requireContext(), "Error al actualizar el rol", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Rol nuevoRol = new Rol(nombreRol, descripcion, permisosStr);
                        long result = dbHelper.insertRol(nuevoRol);
                        if (result != -1) {
                            Toast.makeText(requireContext(), "Rol registrado con éxito", Toast.LENGTH_SHORT).show();
                            loadRoles();
                        } else {
                            Toast.makeText(requireContext(), "Error al registrar el rol", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDeleteRole(Rol rol) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar Rol")
                .setMessage("¿Estás seguro de que deseas eliminar el rol \"" + rol.getNombreRol() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    boolean deleted = dbHelper.deleteRol(rol.getIdRol());
                    if (deleted) {
                        Toast.makeText(requireContext(), "Rol eliminado con éxito", Toast.LENGTH_SHORT).show();
                        loadRoles();
                    } else {
                        Toast.makeText(requireContext(), "No se puede eliminar el rol porque está asignado a uno o más usuarios", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
