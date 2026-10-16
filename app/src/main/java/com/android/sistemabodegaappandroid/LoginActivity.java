package com.android.sistemabodegaappandroid;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.model.Usuario;
import com.android.sistemabodegaappandroid.session.SessionManager;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        // Si ya hay una sesión activa, redirigir al Panel Principal
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // El notch y las barras del sistema no deben tapar los campos.
        EdgeToEdgeInsets.apply(this, findViewById(R.id.scrollLogin).getRootView());

        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> intentarIniciarSesion());
    }

    private void intentarIniciarSesion() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar si el usuario existe para distinguir estado inactivo de credenciales incorrectas
        Usuario usuarioExistente = dbHelper.getUsuarioByUsername(username);

        if (usuarioExistente == null) {
            Toast.makeText(this, R.string.error_credenciales, Toast.LENGTH_LONG).show();
            // CU-01 flujo alternativo 4a: se limpia la contraseña y el foco
            // vuelve al usuario para corregir el error sin reescribir todo.
            limpiarPasswordYEnfocarUsuario();
            return;
        }

        // Validación: Solo pueden ingresar usuarios activos (activo = 1)
        if (!usuarioExistente.isActivo()) {
            Toast.makeText(this, R.string.error_inactive_user, Toast.LENGTH_LONG).show();
            limpiarPasswordYEnfocarUsuario();
            return;
        }

        // Verificar la concordancia de credenciales
        Usuario usuarioAutenticado = dbHelper.autenticar(username, password);

        if (usuarioAutenticado != null) {
            // Asociar el rol asignado (id_rol) a la sesión activa
            sessionManager.createLoginSession(usuarioAutenticado);

            Toast.makeText(this, R.string.msg_sesion_iniciada, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, R.string.error_credenciales, Toast.LENGTH_LONG).show();
            limpiarPasswordYEnfocarUsuario();
        }
    }

    /**
     * CU-01 flujo alternativo 4a: tras credenciales inválidas el mensaje se
     * muestra y, además, el campo de contraseña se limpia y el foco regresa al
     * usuario para corregir el error sin reescribir todo el formulario.
     */
    private void limpiarPasswordYEnfocarUsuario() {
        etPassword.setText("");
        etUsername.requestFocus();
    }
}
