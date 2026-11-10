package com.android.sistemabodegaappandroid;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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

import com.android.sistemabodegaappandroid.util.AccesoModulo;
import com.android.sistemabodegaappandroid.util.Permisos;

import java.util.List;

/**
 * Listado de productos con búsqueda por nombre o SKU (CU-11 / RF-04 y RF-05).
 *
 * El catálogo se expone como LiveData desde {@link DatabaseHelper}: la
 * consulta corre en el executor del helper, nunca en el hilo principal, y la
 * pantalla solo pide un refresco cuando cambia el texto del buscador.
 */
public class ProductosActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ProductoDao productoDao;
    private ProductoAdapter adapter;
    private RecyclerView rvProductos;
    private TextView tvVacioProductos;
    private TextView tvHeroConteo;
    private ImageView ivVacioProductos;
    private Switch switchVerInactivos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // RF-27: el módulo solo se abre si el rol tiene el permiso. Ocultar el
        // botón en el panel no alcanza: la actividad también puede abrirse con
        // un intent directo o quedar en una tarea restaurada.
        if (!AccesoModulo.permitido(this, Permisos.PRODUCTOS)) {
            return;
        }

        setContentView(R.layout.activity_productos);

        // El notch y las barras del sistema no deben tapar el contenido.
        EdgeToEdgeInsets.apply(this, findViewById(R.id.rootProductos));

        dbHelper = new DatabaseHelper(this);
        productoDao = new ProductoDao(dbHelper);
        rvProductos = findViewById(R.id.rvProductos);
        tvVacioProductos = findViewById(R.id.tvVacioProductos);
        tvHeroConteo = findViewById(R.id.tvHeroConteo);
        ivVacioProductos = findViewById(R.id.ivVacioProductos);
        switchVerInactivos = findViewById(R.id.switchVerInactivos);
        TextInputEditText etBuscar = findViewById(R.id.etBuscarProducto);
        com.google.android.material.textfield.TextInputLayout tilBuscar = findViewById(R.id.tilBuscarProducto);
        MaterialToolbar toolbar = findViewById(R.id.toolbarProductos);
        FloatingActionButton fabAgregar = findViewById(R.id.fabAgregarProducto);

        if (tilBuscar != null) {
            tilBuscar.setEndIconOnClickListener(v ->
                    startActivity(new Intent(ProductosActivity.this, com.android.sistemabodegaappandroid.ui.BarcodeScannerActivity.class)));
        }

        // Navegación: la flecha del toolbar vuelve al panel principal.
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        adapter = new ProductoAdapter(producto -> {
            Intent intent = new Intent(ProductosActivity.this, ProductoFormActivity.class);
            intent.putExtra(ProductoFormActivity.EXTRA_ID_PRODUCTO, producto.getIdProducto());
            abrirFormulario(intent);
        });
        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        rvProductos.setAdapter(adapter);
        // Entrada escalonada: cada tarjeta sube y aparece con ~70 ms de
        // desfase respecto a la anterior. Se reprograda cada vez que cambia
        // el dataset visible para que la animación no solo luzca al abrir.
        rvProductos.setLayoutAnimation(new LayoutAnimationController(
                AnimationUtils.loadAnimation(this, R.anim.item_fall), 0.07f));

        // RF-04/RF-05: la lista observa el LiveData del helper. Cada actualización
        // llega ya resuelta al hilo principal, con el filtro de nombre o SKU aplicado.
        dbHelper.observarProductos(null).observe(this, productos -> {
            if (switchVerInactivos != null && switchVerInactivos.isChecked()) {
                return; // Si estamos viendo los inactivos, ignoramos temporalmente el LiveData activo
            }
            if (productos == null) {
                return;
            }
            adapter.updateData(productos);
            tvHeroConteo.setText(getString(R.string.label_total_productos, productos.size()));
            boolean vacio = productos.isEmpty();
            tvVacioProductos.setVisibility(vacio ? View.VISIBLE : View.GONE);
            ivVacioProductos.setVisibility(vacio ? View.VISIBLE : View.GONE);
            rvProductos.setVisibility(vacio ? View.GONE : View.VISIBLE);
        });

        // Listener del Switch para alternar vistas
        if (switchVerInactivos != null) {
            switchVerInactivos.setOnCheckedChangeListener((buttonView, isChecked) -> {
                cargarListaProductosSegunSwitch();
                if (isChecked) {
                    Toast.makeText(this, "Mostrando productos deshabilitados", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Mostrando productos activos", Toast.LENGTH_SHORT).show();
                }
            });
        }

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
                cargarListaProductosSegunSwitch();
            }
        });

        fabAgregar.setOnClickListener(v ->
                abrirFormulario(new Intent(this, ProductoFormActivity.class)));
    }

    /** Vuelve a ejecutar el DAO o consulta los inactivos según el estado del switch. */
    private void cargarListaProductosSegunSwitch() {
        if (switchVerInactivos != null && switchVerInactivos.isChecked()) {
            List<Producto> inactivos = productoDao.getProductosInactivos();
            String filtro = busquedaActual();
            if (filtro != null && !filtro.trim().isEmpty()) {
                String query = filtro.toLowerCase();
                inactivos.removeIf(p ->
                        (p.getNombre() == null || !p.getNombre().toLowerCase().contains(query)) &&
                                (p.getSkuCodigo() == null || !p.getSkuCodigo().toLowerCase().contains(query))
                );
            }
            adapter.updateData(inactivos);
            tvHeroConteo.setText(getString(R.string.label_total_productos, inactivos.size()));
            boolean vacio = inactivos.isEmpty();
            tvVacioProductos.setVisibility(vacio ? View.VISIBLE : View.GONE);
            ivVacioProductos.setVisibility(vacio ? View.VISIBLE : View.GONE);
            rvProductos.setVisibility(vacio ? View.GONE : View.VISIBLE);
        } else {
            dbHelper.refrescarProductos(busquedaActual());
        }
    }

    /** Transición fluida al abrir el formulario (slide, estilo Expressive). */
    private void abrirFormulario(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Vuelve a leer al regresar del formulario: ahí se creó o se movió stock.
        cargarListaProductosSegunSwitch();
    }

    /** Texto que tiene el campo de búsqueda en este momento. */
    private String busquedaActual() {
        TextInputEditText etBuscar = findViewById(R.id.etBuscarProducto);
        return etBuscar.getText() != null ? etBuscar.getText().toString() : null;
    }
}