package com.android.sistemabodegaappandroid;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.ui.adapter.ProductoAdapter;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Collections;

/**
 * Listado de productos con búsqueda por nombre o SKU (CU-11 / RF-04 y RF-05).
 *
 * El catálogo se expone como LiveData desde {@link DatabaseHelper}: la
 * consulta corre en el executor del helper, nunca en el hilo principal, y la
 * pantalla solo pide un refresco cuando cambia el texto del buscador.
 */
public class ProductosActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ProductoAdapter adapter;
    private RecyclerView rvProductos;
    private TextView tvVacioProductos;
    private ImageView ivVacioProductos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productos);

        // El notch y las barras del sistema no deben tapar el contenido.
        EdgeToEdgeInsets.apply(this, findViewById(R.id.rootProductos));

        dbHelper = new DatabaseHelper(this);
        rvProductos = findViewById(R.id.rvProductos);
        tvVacioProductos = findViewById(R.id.tvVacioProductos);
        ivVacioProductos = findViewById(R.id.ivVacioProductos);
        TextInputEditText etBuscar = findViewById(R.id.etBuscarProducto);
        MaterialToolbar toolbar = findViewById(R.id.toolbarProductos);
        FloatingActionButton fabAgregar = findViewById(R.id.fabAgregarProducto);

        // Navegación: la flecha del toolbar vuelve al panel principal.
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        adapter = new ProductoAdapter(Collections.emptyList(), producto -> {
            Intent intent = new Intent(ProductosActivity.this, ProductoFormActivity.class);
            intent.putExtra(ProductoFormActivity.EXTRA_ID_PRODUCTO, producto.getIdProducto());
            startActivity(intent);
        });
        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        rvProductos.setAdapter(adapter);

        // RF-04/RF-05: la lista observa el LiveData del helper. Cada actualización
        // llega ya resuelta al hilo principal, con el filtro de nombre o SKU aplicado.
        dbHelper.observarProductos(null).observe(this, productos -> {
            if (productos == null) {
                return;
            }
            adapter.updateData(productos);
            boolean vacio = productos.isEmpty();
            tvVacioProductos.setVisibility(vacio ? View.VISIBLE : View.GONE);
            ivVacioProductos.setVisibility(vacio ? View.VISIBLE : View.GONE);
            rvProductos.setVisibility(vacio ? View.GONE : View.VISIBLE);
        });

        // Búsqueda en vivo: cada carácter solo pide un refresco; la consulta
        // la ejecuta el executor de DatabaseHelper fuera del hilo principal.
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                dbHelper.refrescarProductos(s != null ? s.toString() : null);
            }
        });

        fabAgregar.setOnClickListener(v ->
                startActivity(new Intent(this, ProductoFormActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Vuelve a leer al regresar del formulario: ahí se creó o se movió stock.
        dbHelper.refrescarProductos(busquedaActual());
    }

    /** Texto que tiene el campo de búsqueda en este momento. */
    private String busquedaActual() {
        TextInputEditText etBuscar = findViewById(R.id.etBuscarProducto);
        return etBuscar.getText() != null ? etBuscar.getText().toString() : null;
    }
}
