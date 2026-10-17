package com.android.sistemabodegaappandroid.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.MainActivity;
import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.model.Usuario;
import com.android.sistemabodegaappandroid.session.SessionManager;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.android.sistemabodegaappandroid.util.Permisos;

public class UserRoleAdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Guarda real: ocultar el boton no basta, la pantalla puede abrirse
        // directo. La preferencia de sesión es una copia y basta con
        // restaurarla o manipularla para que diga "Administrador", asi que el
        // rol se reconsulta en la base antes de mostrar u operar la pantalla.
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || !rolRealPuedeAdministrarUsuarios(session.getUserId())) {
            Toast.makeText(this, R.string.msg_acceso_admin_denegado, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_user_role_admin);

        // El notch y las barras del sistema no deben tapar el toolbar ni las pestañas.
        EdgeToEdgeInsets.apply(this, findViewById(R.id.viewPager).getRootView());

        MaterialToolbar toolbar = findViewById(R.id.toolbarAdmin);
        // No usar setSupportActionBar: descarta el titleTextColor del layout y deja
        // el titulo con el color por defecto del tema. Se maneja la flecha directo.
        toolbar.setTitleTextColor(getColor(R.color.white));
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return new UsuariosFragment();
                } else {
                    return new RolesFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.tab_users);
            } else {
                tab.setText(R.string.tab_roles);
            }
        }).attach();
    }

    /**
     * Consulta el rol REAL en la base para el usuario de la sesión.
     *
     * La preferencia solo guarda una copia del rol del login: si fue
     * restaurada, manipulada o el usuario fue degradado/inactivo después,
     * sigue diciendo "Administrador". La autoridad es la fila de USUARIO y,
     * para el acceso, el permiso USUARIOS de su rol (RF-27): el nombre del rol
     * no autoriza, porque entonces un rol nuevo con el permiso marcado quedaría
     * afuera y cualquiera llamado "Administrador" entraría.
     *
     * @param idUsuario id del usuario en sesión.
     * @return true solo si el usuario existe, está activo y su rol declara el
     *         permiso de administrar usuarios.
     */
    private boolean rolRealPuedeAdministrarUsuarios(int idUsuario) {
        if (idUsuario < 0) {
            return false;
        }
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        Usuario usuario = dbHelper.getUsuarioById(idUsuario);
        return usuario != null
                && usuario.isActivo()
                && Permisos.tiene(dbHelper.getPermisosDeRol(usuario.getIdRol()), Permisos.USUARIOS);
    }
}
