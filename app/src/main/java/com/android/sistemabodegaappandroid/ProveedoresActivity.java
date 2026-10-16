package com.android.sistemabodegaappandroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.db.ProveedorDao;
import com.android.sistemabodegaappandroid.model.Proveedor;
import com.android.sistemabodegaappandroid.ui.adapter.ProveedorAdapter;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Collections;

/**
 * Listado de proveedores habilitados.
 *
 * Sigue el mismo esqueleto que la pantalla de productos, pero sin búsqueda:
 * el DAO de proveedores no expone filtro y agregarlo exigiría tocar la capa
 * de datos, que está acotada a este módulo.
 *
 * La lista se expone como LiveData desde {@link DatabaseHelper}: la consulta
 * corre en el executor del helper, nunca en el hilo principal.
 */
public class ProveedoresActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ProveedorDao proveedorDao;
    private ProveedorAdapter adapter;
    private RecyclerView rvProveedores;
    private TextView tvVacioProveedores;
    private ImageView ivVacioProveedores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proveedores);

        // El notch y las barras del sistema no deben tapar el contenido.
        EdgeToEdgeInsets.apply(this, findViewById(R.id.rootProveedores));

        dbHelper = new DatabaseHelper(this);
        proveedorDao = new ProveedorDao(dbHelper);
        rvProveedores = findViewById(R.id.rvProveedores);
        tvVacioProveedores = findViewById(R.id.tvVacioProveedores);
        ivVacioProveedores = findViewById(R.id.ivVacioProveedores);
        MaterialToolbar toolbar = findViewById(R.id.toolbarProveedores);
        FloatingActionButton fabAgregar = findViewById(R.id.fabAgregarProveedor);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        adapter = new ProveedorAdapter(Collections.emptyList(), new ProveedorAdapter.OnProveedorActionListener() {
            @Override
            public void onProveedorClick(Proveedor proveedor) {
                Intent intent = new Intent(ProveedoresActivity.this, ProveedorFormActivity.class);
                intent.putExtra(ProveedorFormActivity.EXTRA_ID_PROVEEDOR, proveedor.getIdProveedor());
                startActivity(intent);
            }

            @Override
            public void onDeshabilitar(Proveedor proveedor) {
                confirmarDeshabilitar(proveedor);
            }
        });
        rvProveedores.setLayoutManager(new LinearLayoutManager(this));
        rvProveedores.setAdapter(adapter);

        // CU-11: la lista observa el LiveData del helper. La consulta corre en
        // el executor, nunca en el hilo principal, y cada actualización llega
        // ya resuelta al hilo de UI junto con el estado vacío.
        dbHelper.observarProveedores().observe(this, proveedores -> {
            if (proveedores == null) {
                return;
            }
            adapter.updateData(proveedores);
            boolean vacio = proveedores.isEmpty();
            tvVacioProveedores.setVisibility(vacio ? View.VISIBLE : View.GONE);
            ivVacioProveedores.setVisibility(vacio ? View.VISIBLE : View.GONE);
            rvProveedores.setVisibility(vacio ? View.GONE : View.VISIBLE);
        });

        fabAgregar.setOnClickListener(v ->
                startActivity(new Intent(this, ProveedorFormActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Vuelve a leer al regresar del formulario para reflejar altas y cambios.
        dbHelper.refrescarProveedores();
    }

    /**
     * Deshabilitar no borra: solo cambia el flag, de modo que el historial de
     * movimientos conserva la referencia al proveedor original.
     */
    private void confirmarDeshabilitar(Proveedor proveedor) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_deshabilitar)
                .setMessage(R.string.confirmar_deshabilitar)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    boolean ok = proveedorDao.updateProveedorEstado(proveedor.getIdProveedor(), 0);
                    Toast.makeText(this, ok
                                    ? R.string.msg_proveedor_deshabilitado
                                    : R.string.msg_proveedor_error,
                            Toast.LENGTH_SHORT).show();
                    dbHelper.refrescarProveedores();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}
