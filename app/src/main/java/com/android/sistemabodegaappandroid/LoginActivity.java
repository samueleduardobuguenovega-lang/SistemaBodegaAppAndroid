package com.android.sistemabodegaappandroid;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.model.Usuario;
import com.android.sistemabodegaappandroid.session.SessionManager;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    /** No existe el usuario o no coincide la contraseña (mismo mensaje para ambos). */
    private static final int RESULTADO_CREDENCIALES_INVALIDAS = 0;
    /** El usuario existe pero está deshabilitado (activo = 0). */
    private static final int RESULTADO_USUARIO_INACTIVO = 1;
    /** Contraseña verificada: toca persistir la sesión y entrar. */
    private static final int RESULTADO_EXITO = 2;

    /**
     * Hilo dedicado a la verificación de credenciales. getUsuarioByUsername()
     * y, sobre todo, autenticar() con PBKDF2 (120.000 iteraciones) cuestan
     * decenas de milisegundos y no deben correr en el hilo principal. Es
     * single-thread: dos pulsaciones no verifican a la vez.
     */
    private static final ExecutorService loginExecutor = Executors.newSingleThreadExecutor();

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private View indicadorProgreso;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    /** true mientras hay una verificación en curso: evita dobles pulsaciones. */
    private boolean verificando = false;

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
        indicadorProgreso = findViewById(R.id.progressLogin);

        btnLogin.setOnClickListener(v -> intentarIniciarSesion());
    }

    private void intentarIniciarSesion() {
        if (verificando) {
            return; // Ya hay una verificación en curso: segunda pulsación.
        }

        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarVerificacionEnCurso();

        // La verificación entera (consulta de usuario y PBKDF2) sale del hilo
        // principal; el resultado vuelve por runOnUiThread.
        loginExecutor.execute(() -> {
            // Verificar si el usuario existe para distinguir estado inactivo
            // de credenciales incorrectas
            Usuario usuarioExistente = dbHelper.getUsuarioByUsername(username);

            if (usuarioExistente == null) {
                publicarResultado(RESULTADO_CREDENCIALES_INVALIDAS, null);
                return;
            }

            // Validación: Solo pueden ingresar usuarios activos (activo = 1)
            if (!usuarioExistente.isActivo()) {
                publicarResultado(RESULTADO_USUARIO_INACTIVO, null);
                return;
            }

            // Verificar la concordancia de credenciales
            Usuario usuarioAutenticado = dbHelper.autenticar(username, password);
            publicarResultado(usuarioAutenticado != null
                    ? RESULTADO_EXITO : RESULTADO_CREDENCIALES_INVALIDAS, usuarioAutenticado);
        });
    }

    /**
     * Publica el resultado de la verificación en el hilo principal.
     *
     * La pantalla pudo destruirse mientras se verificaba (rotación o salida
     * del usuario): antes de tocar cualquier vista se comprueba que siga viva.
     * La sesión, en cambio, es negocio y no UI, así que se persiste igual si
     * el resultado llegó a buen puerto, salvo que el usuario haya cancelado la
     * salida a propósito.
     */
    private void publicarResultado(final int codigo, final Usuario usuario) {
        runOnUiThread(() -> {
            if (codigo == RESULTADO_EXITO && !isFinishing()) {
                // Asociar el rol asignado (id_rol) a la sesión activa
                sessionManager.createLoginSession(usuario);
            }

            if (isFinishing() || isDestroyed()) {
                return; // No se toca la interfaz de una pantalla que ya no existe.
            }

            finalizarVerificacion();

            switch (codigo) {
                case RESULTADO_EXITO:
                    Toast.makeText(this, R.string.msg_sesion_iniciada, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                case RESULTADO_USUARIO_INACTIVO:
                    Toast.makeText(this, R.string.error_inactive_user, Toast.LENGTH_LONG).show();
                    limpiarPasswordYEnfocarUsuario();
                    break;
                default:
                    Toast.makeText(this, R.string.error_credenciales, Toast.LENGTH_LONG).show();
                    limpiarPasswordYEnfocarUsuario();
                    break;
            }
        });
    }

    /**
     * Bloquea el formulario y muestra el indicador mientras se verifica, para
     * que el usuario vea que algo pasa y no pueda pulsar de nuevo.
     */
    private void mostrarVerificacionEnCurso() {
        verificando = true;
        btnLogin.setEnabled(false);
        btnLogin.setText(R.string.login_verificando);
        indicadorProgreso.setVisibility(View.VISIBLE);
    }

    /** Devuelve el formulario a su estado inicial tras recibir el resultado. */
    private void finalizarVerificacion() {
        verificando = false;
        btnLogin.setEnabled(true);
        btnLogin.setText(R.string.btn_login);
        indicadorProgreso.setVisibility(View.GONE);
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
