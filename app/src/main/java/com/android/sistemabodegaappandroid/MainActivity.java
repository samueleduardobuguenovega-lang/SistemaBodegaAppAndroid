package com.android.sistemabodegaappandroid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.sistemabodegaappandroid.session.SessionManager;
import com.android.sistemabodegaappandroid.ui.admin.UserRoleAdminActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    private TextView tvWelcome;
    private TextView tvUserFullName;
    private TextView tvUsername;
    private TextView tvUserRole;
    private MaterialButton btnAdminUsersRoles;
    private MaterialButton btnAdminCategorias;
    private MaterialButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Verificar si el usuario está autenticado
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserFullName = findViewById(R.id.tvUserFullName);
        tvUsername = findViewById(R.id.tvUsername);
        tvUserRole = findViewById(R.id.tvUserRole);
        btnAdminUsersRoles = findViewById(R.id.btnAdminUsersRoles);
        btnAdminCategorias = findViewById(R.id.btnAdminCategorias);
        btnLogout = findViewById(R.id.btnLogout);

        cargarDatosSesion();

        btnAdminUsersRoles.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserRoleAdminActivity.class);
            startActivity(intent);
        });

        btnAdminCategorias.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CategoriasActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> mostrarDialogoCerrarSesion());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            cargarDatosSesion();
        }
    }

    private void cargarDatosSesion() {
        String nombreCompleto = sessionManager.getNombreCompleto();
        String username = sessionManager.getUsername();
        String roleName = sessionManager.getRoleName();

        tvWelcome.setText("¡Bienvenido, " + nombreCompleto + "!");
        tvUserFullName.setText(nombreCompleto);
        tvUsername.setText("Usuario: @" + username);
        tvUserRole.setText("Rol: " + (roleName.isEmpty() ? "Sin Rol" : roleName));
    }

    /**
     * RF-02: Cerrar Sesión
     * Permite al usuario autenticado cerrar su sesión actual de forma segura.
     */
    private void mostrarDialogoCerrarSesion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.btn_logout)
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(R.string.yes, (dialog, which) -> sessionManager.logoutUser())
                .setNegativeButton(R.string.no, null)
                .show();
    }
}