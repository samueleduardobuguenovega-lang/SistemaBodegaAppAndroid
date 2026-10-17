package com.android.sistemabodegaappandroid.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.model.Rol;
import com.android.sistemabodegaappandroid.model.Usuario;
import com.android.sistemabodegaappandroid.ui.adapter.UsuarioAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class UsuariosFragment extends Fragment {

    private RecyclerView rvUsuarios;
    private TextView tvEmptyUsuarios;
    private FloatingActionButton fabAddUser;
    private DatabaseHelper dbHelper;
    private UsuarioAdapter adapter;
    private List<Usuario> usuariosList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_usuarios, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        rvUsuarios = view.findViewById(R.id.rvUsuarios);
        tvEmptyUsuarios = view.findViewById(R.id.tvEmptyUsuarios);
        fabAddUser = view.findViewById(R.id.fabAddUser);

        rvUsuarios.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UsuarioAdapter(usuariosList, (usuario, isChecked) -> {
            boolean updated = dbHelper.updateUsuarioEstado(usuario.getIdUsuario(), isChecked ? 1 : 0);
            if (updated) {
                usuario.setActivo(isChecked ? 1 : 0);
                Toast.makeText(requireContext(), R.string.msg_estado_usuario_actualizado, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), R.string.msg_error_actualizar_estado, Toast.LENGTH_SHORT).show();
            }
        }, new UsuarioAdapter.OnUserActionListener() {
            @Override
            public void onEdit(Usuario usuario) {
                showEditUserDialog(usuario);
            }

            @Override
            public void onDelete(Usuario usuario) {
                confirmDeleteUser(usuario);
            }
        });
        rvUsuarios.setAdapter(adapter);

        fabAddUser.setOnClickListener(v -> showAddUserDialog());

        dbHelper.observarUsuarios().observe(getViewLifecycleOwner(), lista -> {
            if (lista == null) {
                return;
            }
            usuariosList = lista;
            adapter.updateData(usuariosList);
            tvEmptyUsuarios.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            rvUsuarios.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    public void loadUsuarios() {
        dbHelper.refrescarUsuarios();
    }

    private Observer<List<Rol>> observerRolesDialogo;

    private void showAddUserDialog() {
        if (observerRolesDialogo != null) {
            return;
        }
        observerRolesDialogo = lista -> {
            dbHelper.observarRoles().removeObserver(observerRolesDialogo);
            observerRolesDialogo = null;
            abrirDialogoUsuario(null, lista != null ? lista : new ArrayList<>());
        };
        dbHelper.observarRoles().observe(getViewLifecycleOwner(), observerRolesDialogo);
    }

    private void showEditUserDialog(Usuario usuario) {
        if (observerRolesDialogo != null) {
            return;
        }
        observerRolesDialogo = lista -> {
            dbHelper.observarRoles().removeObserver(observerRolesDialogo);
            observerRolesDialogo = null;
            abrirDialogoUsuario(usuario, lista != null ? lista : new ArrayList<>());
        };
        dbHelper.observarRoles().observe(getViewLifecycleOwner(), observerRolesDialogo);
    }

    private void abrirDialogoUsuario(@Nullable Usuario usuarioEditar, List<Rol> roles) {
        if (roles.isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_roles_requeridos, Toast.LENGTH_LONG).show();
            return;
        }

        boolean esEdicion = usuarioEditar != null;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_usuario, null);
        TextInputLayout tilUsername = dialogView.findViewById(R.id.tilUsername);
        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etNombreCompleto = dialogView.findViewById(R.id.etNombreCompleto);
        TextInputLayout tilPassword = dialogView.findViewById(R.id.tilPassword);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);
        Spinner spinnerRoles = dialogView.findViewById(R.id.spinnerRoles);
        MaterialSwitch switchActivo = dialogView.findViewById(R.id.switchActivoDialog);

        if (esEdicion) {
            etUsername.setText(usuarioEditar.getUsername());
            etNombreCompleto.setText(usuarioEditar.getNombreCompleto());
            tilPassword.setHint("Contraseña (dejar en blanco para no cambiar)");
            switchActivo.setChecked(usuarioEditar.isActivo());
        }

        tilUsername.setError(null);
        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilUsername.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ArrayAdapter<Rol> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoles.setAdapter(spinnerAdapter);

        if (esEdicion) {
            for (int i = 0; i < roles.size(); i++) {
                if (roles.get(i).getIdRol() == usuarioEditar.getIdRol()) {
                    spinnerRoles.setSelection(i);
                    break;
                }
            }
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(esEdicion ? "Editar Usuario" : getString(R.string.add_user))
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String nombreCompleto = etNombreCompleto.getText() != null ? etNombreCompleto.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            Rol selectedRol = (Rol) spinnerRoles.getSelectedItem();
            boolean activo = switchActivo.isChecked();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(nombreCompleto)) {
                Toast.makeText(requireContext(), R.string.msg_campos_obligatorios, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!esEdicion && TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), R.string.msg_campos_obligatorios, Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedRol == null) {
                Toast.makeText(requireContext(), R.string.msg_rol_invalido, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!esEdicion && dbHelper.isUsernameExists(username)) {
                tilUsername.setError(getString(R.string.error_username_duplicado));
                etUsername.requestFocus();
                return;
            }

            if (esEdicion && !username.equalsIgnoreCase(usuarioEditar.getUsername()) && dbHelper.isUsernameExists(username)) {
                tilUsername.setError(getString(R.string.error_username_duplicado));
                etUsername.requestFocus();
                return;
            }

            if (esEdicion) {
                usuarioEditar.setUsername(username);
                usuarioEditar.setNombreCompleto(nombreCompleto);
                usuarioEditar.setIdRol(selectedRol.getIdRol());
                usuarioEditar.setActivo(activo ? 1 : 0);

                boolean updated = dbHelper.updateUsuario(usuarioEditar, password);
                if (updated) {
                    Toast.makeText(requireContext(), "Usuario actualizado con éxito", Toast.LENGTH_SHORT).show();
                    loadUsuarios();
                    dialog.dismiss();
                } else {
                    Toast.makeText(requireContext(), "Error al actualizar el usuario", Toast.LENGTH_SHORT).show();
                }
            } else {
                Usuario nuevoUsuario = new Usuario(username, nombreCompleto, "", selectedRol.getIdRol(), activo ? 1 : 0);
                long result = dbHelper.insertUsuario(nuevoUsuario, password);
                if (result != -1) {
                    Toast.makeText(requireContext(), R.string.msg_usuario_registrado, Toast.LENGTH_SHORT).show();
                    loadUsuarios();
                    dialog.dismiss();
                } else {
                    Toast.makeText(requireContext(), R.string.msg_error_registrar_usuario, Toast.LENGTH_SHORT).show();
                }
            }
        }));

        dialog.show();
    }

    private void confirmDeleteUser(Usuario usuario) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar Usuario")
                .setMessage("¿Estás seguro de que deseas eliminar al usuario @" + usuario.getUsername() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    boolean deleted = dbHelper.deleteUsuario(usuario.getIdUsuario());
                    if (deleted) {
                        Toast.makeText(requireContext(), "Usuario eliminado con éxito", Toast.LENGTH_SHORT).show();
                        loadUsuarios();
                    } else {
                        Toast.makeText(requireContext(), "Error al eliminar el usuario", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
