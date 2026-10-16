package com.android.sistemabodegaappandroid.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.MainActivity;
import com.android.sistemabodegaappandroid.session.SessionManager;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class UserRoleAdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Guarda real: ocultar el boton no basta, la pantalla puede abrirse directo.
        // El rol sale de la sesion, que a su vez viene del registro en la base.
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || !session.esAdministrador()) {
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
}
