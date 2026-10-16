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
import com.android.sistemabodegaappandroid.db.ProductoDao;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.ui.adapter.ProductoAdapter;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Collections;
import java.util.List;

/**
 * Listado de productos con búsqueda por nombre o SKU (CU-11).
 *
 * La consulta se delega al DAO en cada pulsación para que el filtro aplique
 * sobre la base y no sobre una copia desactualizada en memoria.
 */
public class ProductosActivity extends AppCompatActivity {

    private ProductoDao productoDao;
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

        productoDao = new ProductoDao(new DatabaseHelper(this));
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

        // Búsqueda en vivo: cada carácter vuelve a consultar el DAO.
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                cargarProductos(s != null ? s.toString() : null);
            }
        });

        fabAgregar.setOnClickListener(v ->
                startActivity(new Intent(this, ProductoFormActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Vuelve a leer al regresar del formulario: ahí se creó o se movió stock.
        cargarProductos(busquedaActual());
    }

    /** Consulta al DAO y refleja el resultado en la lista y en el estado vacío. */
    private void cargarProductos(String texto) {
        List<Producto> productos = productoDao.buscarProductos(texto);
        adapter.updateData(productos);
        boolean vacio = productos.isEmpty();
        tvVacioProductos.setVisibility(vacio ? View.VISIBLE : View.GONE);
        ivVacioProductos.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rvProductos.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    /** Texto que tiene el campo de búsqueda en este momento. */
    private String busquedaActual() {
        TextInputEditText etBuscar = findViewById(R.id.etBuscarProducto);
        return etBuscar.getText() != null ? etBuscar.getText().toString() : null;
    }
}
