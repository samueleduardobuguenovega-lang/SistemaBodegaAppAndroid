package com.android.sistemabodegaappandroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.android.sistemabodegaappandroid.session.SessionManager;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.android.sistemabodegaappandroid.util.Permisos;
import com.android.sistemabodegaappandroid.ui.admin.UserRoleAdminActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    /** Desfase entre tarjetas en la entrada escalonada del panel. */
    private static final long DESFASE_TARJETAS_MS = 60L;

    private SessionManager sessionManager;

    private TextView tvWelcome;
    private TextView tvUserFullName;
    private TextView tvUsername;
    private TextView tvUserRole;
    private MaterialCardView btnAdminUsersRoles;
    private MaterialCardView btnAdminCategorias;
    private MaterialCardView btnAdminProductos;
    private MaterialCardView btnAdminProveedores;
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

        // Un solo lugar que aplica las areas seguras del sistema.
        EdgeToEdgeInsets.apply(this, findViewById(R.id.main));

        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserFullName = findViewById(R.id.tvUserFullName);
        tvUsername = findViewById(R.id.tvUsername);
        tvUserRole = findViewById(R.id.tvUserRole);
        btnAdminUsersRoles = findViewById(R.id.btnAdminUsersRoles);
        btnAdminCategorias = findViewById(R.id.btnAdminCategorias);
        btnAdminProductos = findViewById(R.id.btnAdminProductos);
        btnAdminProveedores = findViewById(R.id.btnAdminProveedores);
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

        // Los accesos de inventario ya son tarjetas del layout; acá se conectan
        // con su pantalla igual que los dos accesos anteriores.
        crearBotonesInventario();

        // RF-27: el rol decide qué accesos ve el panel. Se aplica sobre las
        // cuatro tarjetas, sin importar si vinieron del layout o se conectaron
        // en código.
        mostrarSegunPermiso(btnAdminUsersRoles, Permisos.USUARIOS);
        mostrarSegunPermiso(btnAdminCategorias, Permisos.CATEGORIAS);
        mostrarSegunPermiso(btnAdminProductos, Permisos.PRODUCTOS);
        mostrarSegunPermiso(btnAdminProveedores, Permisos.PROVEEDORES);

        btnLogout.setOnClickListener(v -> mostrarDialogoCerrarSesion());

        // Primero se resuelve la visibilidad por permiso y después se anima: así
        // la entrada escalonada no gasta desfase en tarjetas que el rol no ve.
        aplicarFeedbackTactil(btnAdminUsersRoles);
        aplicarFeedbackTactil(btnAdminCategorias);
        aplicarFeedbackTactil(btnAdminProductos);
        aplicarFeedbackTactil(btnAdminProveedores);
        animarEntradaTarjetas();
    }

    /** Muestra u oculta un acceso del panel según los permisos del rol. */
    private void mostrarSegunPermiso(View vista, String permiso) {
        vista.setVisibility(sessionManager.tienePermiso(permiso) ? View.VISIBLE : View.GONE);
    }

    /** Conecta las tarjetas de productos y proveedores con su pantalla. */
    private void crearBotonesInventario() {
        btnAdminProductos.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProductosActivity.class)));
        btnAdminProveedores.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProveedoresActivity.class)));
    }

    /**
     * Entrada escalonada de las tarjetas de menú: cada una sube 24dp mientras
     * aparece, con 60 ms de desfase entre tarjetas. Solo se animan las que el
     * rol ve; las ocultas no consumen turno.
     */
    private void animarEntradaTarjetas() {
        MaterialCardView[] tarjetas = {
                btnAdminUsersRoles,
                btnAdminCategorias,
                btnAdminProductos,
                btnAdminProveedores
        };
        long desfase = 0L;
        for (MaterialCardView tarjeta : tarjetas) {
            if (tarjeta == null || tarjeta.getVisibility() != View.VISIBLE) {
                continue;
            }
            tarjeta.setAlpha(0f);
            tarjeta.setTranslationY(dpApx(24));
            tarjeta.animate().alpha(1f).translationY(0f)
                    .setStartDelay(desfase)
                    .setDuration(280)
                    .setInterpolator(new DecelerateInterpolator(1.4f))
                    .start();
            desfase += DESFASE_TARJETAS_MS;
        }
    }

    /**
     * Feedback táctil expresivo, igual al del listado de productos: la tarjeta
     * se encoge levemente al presionar y vuelve con rebote al soltar. Devuelve
     * false para no consumir el toque: el clic sigue llegando al listener.
     */
    private void aplicarFeedbackTactil(View vista) {
        vista.setOnTouchListener((v, evento) -> {
            int accion = evento.getActionMasked();
            if (accion == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
            } else if (accion == MotionEvent.ACTION_UP || accion == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(180)
                        .setInterpolator(new OvershootInterpolator(1.6f)).start();
            }
            return false;
        });
    }

    /** Convierte dp a píxeles con la densidad real del dispositivo. */
    private int dpApx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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
