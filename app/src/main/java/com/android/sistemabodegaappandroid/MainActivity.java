package com.android.sistemabodegaappandroid;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.sistemabodegaappandroid.session.SessionManager;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.android.sistemabodegaappandroid.ui.admin.UserRoleAdminActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.android.sistemabodegaappandroid.util.Permisos;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    private TextView tvWelcome;
    private TextView tvUserFullName;
    private TextView tvUsername;
    private TextView tvUserRole;
    private MaterialButton btnAdminUsersRoles;
    private MaterialButton btnAdminCategorias;
    private MaterialButton btnAdminProductos;
    private MaterialButton btnAdminProveedores;
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

        // Los accesos de inventario se construyen en código, con el mismo aspecto
        // que btnAdminCategorias. Su visibilidad la fija mostrarSegunPermiso unas
        // líneas más abajo, con el permiso de cada módulo.
        crearBotonesInventario();

        // RF-27: el rol decide qué accesos ve el panel. Se aplica acá y no al
        // crear cada botón porque crearBotonesInventario() construye dos de los
        // cuatro en código.
        mostrarSegunPermiso(btnAdminUsersRoles, Permisos.USUARIOS);
        mostrarSegunPermiso(btnAdminCategorias, Permisos.CATEGORIAS);
        mostrarSegunPermiso(btnAdminProductos, Permisos.PRODUCTOS);
        mostrarSegunPermiso(btnAdminProveedores, Permisos.PROVEEDORES);

        btnLogout.setOnClickListener(v -> mostrarDialogoCerrarSesion());
    }

    /** Muestra u oculta un acceso del panel según los permisos del rol. */
    private void mostrarSegunPermiso(View vista, String permiso) {
        vista.setVisibility(sessionManager.tienePermiso(permiso) ? View.VISIBLE : View.GONE);
    }

    /**
     * Inserta los accesos de productos y proveedores justo debajo de
     * categorías, conservando el orden de los botones del panel.
     */
    private void crearBotonesInventario() {
        ViewGroup contenedor = (ViewGroup) btnAdminCategorias.getParent();
        int posicion = contenedor.indexOfChild(btnAdminCategorias) + 1;

        btnAdminProductos = crearBotonInventario(R.id.btnAdminProductos, R.string.btn_admin_productos);
        contenedor.addView(btnAdminProductos, posicion);

        btnAdminProveedores = crearBotonInventario(R.id.btnAdminProveedores, R.string.btn_admin_proveedores);
        contenedor.addView(btnAdminProveedores, posicion + 1);

        btnAdminProductos.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProductosActivity.class)));
        btnAdminProveedores.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProveedoresActivity.class)));
    }

    /** Réplica en código de los atributos del botón de categorías. */
    private MaterialButton crearBotonInventario(int idRecurso, int textoRecurso) {
        MaterialButton boton = new MaterialButton(this);
        boton.setId(idRecurso);
        boton.setText(textoRecurso);
        boton.setTextSize(15);
        boton.setTextColor(ContextCompat.getColor(this, R.color.white));
        boton.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        boton.setCornerRadius(dpApx(8));

        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpApx(60));
        parametros.bottomMargin = dpApx(12);
        boton.setLayoutParams(parametros);
        return boton;
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