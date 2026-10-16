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
        });
        rvUsuarios.setAdapter(adapter);

        fabAddUser.setOnClickListener(v -> showAddUserDialog());

        // CU-27: el listado es reactivo; el DAO publica y la interfaz se
        // suscribe. Cada actualización llega al hilo principal.
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

    /** Refresca el listado reactiva el DAO (el observer repinta la vista). */
    public void loadUsuarios() {
        dbHelper.refrescarUsuarios();
    }

    /**
     * Observer de una sola apertura del diálogo: recibe los roles y se
     * desuscribe solo. Es null mientras no hay ninguna apertura en curso.
     */
    private Observer<List<Rol>> observerRolesDialogo;

    /**
     * Los roles llegan por el mismo camino reactivo que el listado. Se abre el
     * diálogo cuando el LiveData entrega (valor ya cacheado, o recién consultado
     * en el executor): nunca se consulta la base en el hilo principal, y el
     * estado vacío solo se informa cuando realmente no hay roles.
     */
    private void showAddUserDialog() {
        if (observerRolesDialogo != null) {
            return; // Ya hay una apertura esperando los roles: doble clic.
        }
        observerRolesDialogo = lista -> {
            dbHelper.observarRoles().removeObserver(observerRolesDialogo);
            observerRolesDialogo = null;
            abrirDialogoUsuario(lista != null ? lista : new ArrayList<>());
        };
        dbHelper.observarRoles().observe(getViewLifecycleOwner(), observerRolesDialogo);
    }

    private void abrirDialogoUsuario(List<Rol> roles) {
        if (roles.isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_roles_requeridos, Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_usuario, null);
        TextInputLayout tilUsername = dialogView.findViewById(R.id.tilUsername);
        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etNombreCompleto = dialogView.findViewById(R.id.etNombreCompleto);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);
        Spinner spinnerRoles = dialogView.findViewById(R.id.spinnerRoles);
        MaterialSwitch switchActivo = dialogView.findViewById(R.id.switchActivoDialog);

        // CU-27: al reabrir el diálogo no debe quedar un error anterior.
        tilUsername.setError(null);
        // CU-27: el error del campo se limpia apenas el usuario reescribe.
        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilUsername.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ArrayAdapter<Rol> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRoles.setAdapter(spinnerAdapter);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_user)
                .setView(dialogView)
                // El clic se maneja en setOnShowListener para poder mantener el
                // diálogo abierto y mostrar el error dentro del campo.
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String nombreCompleto = etNombreCompleto.getText() != null ? etNombreCompleto.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            Rol selectedRol = (Rol) spinnerRoles.getSelectedItem();
            boolean activo = switchActivo.isChecked();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(nombreCompleto) || TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), R.string.msg_campos_obligatorios, Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedRol == null) {
                Toast.makeText(requireContext(), R.string.msg_rol_invalido, Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.isUsernameExists(username)) {
                // CU-27: el nombre duplicado se informa en el campo, sin Toast.
                tilUsername.setError(getString(R.string.error_username_duplicado));
                etUsername.requestFocus();
                return;
            }

            Usuario nuevoUsuario = new Usuario(username, nombreCompleto, "", selectedRol.getIdRol(), activo ? 1 : 0);
            long result = dbHelper.insertUsuario(nuevoUsuario, password);
            if (result != -1) {
                Toast.makeText(requireContext(), R.string.msg_usuario_registrado, Toast.LENGTH_SHORT).show();
                loadUsuarios();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), R.string.msg_error_registrar_usuario, Toast.LENGTH_SHORT).show();
            }
        }));

        dialog.show();
    }
}
