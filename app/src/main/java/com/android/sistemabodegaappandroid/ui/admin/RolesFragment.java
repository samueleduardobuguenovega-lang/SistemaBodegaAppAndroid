package com.android.sistemabodegaappandroid.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        adapter = new RolAdapter(rolesList);
        rvRoles.setAdapter(adapter);

        fabAddRole.setOnClickListener(v -> showAddRoleDialog());

        loadRoles();
    }

    public void loadRoles() {
        rolesList = dbHelper.getAllRoles();
        if (rolesList.isEmpty()) {
            tvEmptyRoles.setVisibility(View.VISIBLE);
            rvRoles.setVisibility(View.GONE);
        } else {
            tvEmptyRoles.setVisibility(View.GONE);
            rvRoles.setVisibility(View.VISIBLE);
            adapter.updateData(rolesList);
        }
    }

    private void showAddRoleDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_rol, null);
        EditText etNombreRol = dialogView.findViewById(R.id.etNombreRol);
        EditText etDescripcionRol = dialogView.findViewById(R.id.etDescripcionRol);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_role)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String nombreRol = etNombreRol.getText() != null ? etNombreRol.getText().toString().trim() : "";
                    String descripcion = etDescripcionRol.getText() != null ? etDescripcionRol.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(nombreRol)) {
                        Toast.makeText(requireContext(), "El nombre del rol es obligatorio", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (dbHelper.isRoleNameExists(nombreRol)) {
                        Toast.makeText(requireContext(), "El nombre del rol ya existe (debe ser único)", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Rol nuevoRol = new Rol(nombreRol, descripcion);
                    long result = dbHelper.insertRol(nuevoRol);
                    if (result != -1) {
                        Toast.makeText(requireContext(), "Rol registrado con éxito", Toast.LENGTH_SHORT).show();
                        loadRoles();
                    } else {
                        Toast.makeText(requireContext(), "Error al registrar el rol", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
