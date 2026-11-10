package com.android.sistemabodegaappandroid;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.db.ProductoDao;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.model.Proveedor;
import com.android.sistemabodegaappandroid.session.SessionManager;
import com.android.sistemabodegaappandroid.ui.MovimientoStockDialog;
import com.android.sistemabodegaappandroid.ui.adapter.ProductoAdapter;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import com.android.sistemabodegaappandroid.util.AccesoModulo;
import com.android.sistemabodegaappandroid.util.Permisos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Listado de productos con búsqueda por nombre o SKU (CU-11 / RF-04 y RF-05).
 *
 * El catálogo se expone como LiveData desde {@link DatabaseHelper}: la
 * consulta corre en el executor del helper, nunca en el hilo principal, y la
 * pantalla solo pide un refresco cuando cambia el texto del buscador.
 *
 * Desde acá también se registra una entrada o salida de stock (CU-04 / CU-05)
 * sin entrar al formulario, y se alterna entre el catálogo activo y los
 * productos dados de baja lógica (RF-08). Las dos listas son LiveData
 * independientes: el interruptor decide cuál se dibuja, no lanza consultas.
 */
public class ProductosActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ProductoDao productoDao;
    private SessionManager sessionManager;
    private ProductoAdapter adapter;
    private RecyclerView rvProductos;
    private TextView tvVacioProductos;
    private TextView tvHeroConteo;
    private ImageView ivVacioProductos;
    private MaterialSwitch switchVerInactivos;

    /** Proveedores activos para el diálogo de movimiento; los entrega el helper. */
    private final List<Proveedor> proveedores = new ArrayList<>();

    /** Última lista publicada por cada LiveData; el interruptor elige cuál se ve. */
    private List<Producto> productosActivos = Collections.emptyList();
    private List<Producto> productosInactivos = Collections.emptyList();

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
        sessionManager = new SessionManager(this);
        rvProductos = findViewById(R.id.rvProductos);
        tvVacioProductos = findViewById(R.id.tvVacioProductos);
        tvHeroConteo = findViewById(R.id.tvHeroConteo);
        ivVacioProductos = findViewById(R.id.ivVacioProductos);
        switchVerInactivos = findViewById(R.id.switchVerInactivos);
        TextInputEditText etBuscar = findViewById(R.id.etBuscarProducto);
        MaterialToolbar toolbar = findViewById(R.id.toolbarProductos);
        FloatingActionButton fabAgregar = findViewById(R.id.fabAgregarProducto);

        // Navegación: la flecha del toolbar vuelve al panel principal.
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        adapter = new ProductoAdapter(this::mostrarOpcionesDelProducto);
        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        rvProductos.setAdapter(adapter);
        // Entrada escalonada: cada tarjeta sube y aparece con ~70 ms de
        // desfase respecto de la anterior. Se reprograda cada vez que cambia
        // el dataset visible para que la animación no solo luzca al abrir.
        rvProductos.setLayoutAnimation(new LayoutAnimationController(
                AnimationUtils.loadAnimation(this, R.anim.item_fall), 0.07f));

        // RF-04/RF-05: la lista observa el LiveData del helper. Cada actualización
        // llega ya resuelta al hilo principal, con el filtro de nombre o SKU aplicado.
        dbHelper.observarProductos(null).observe(this, productos -> {
            productosActivos = productos != null ? productos : Collections.emptyList();
            if (!estaViendoInactivos()) {
                dibujarLista(productosActivos);
            }
        });

        // RF-08: los deshabilitados son otro LiveData, con el filtro resuelto en
        // SQL. Así el interruptor no ejecuta ninguna consulta en el hilo principal.
        dbHelper.observarProductosInactivos(null).observe(this, productos -> {
            productosInactivos = productos != null ? productos : Collections.emptyList();
            if (estaViendoInactivos()) {
                dibujarLista(productosInactivos);
            }
        });

        // CU-04: el diálogo de movimiento ofrece el proveedor de la entrada. La
        // lista llega por observer, igual que en el formulario del producto.
        dbHelper.observarProveedores().observe(this, lista -> {
            if (lista == null) {
                return;
            }
            proveedores.clear();
            proveedores.addAll(lista);
        });

        // El interruptor alterna entre el catálogo activo y los deshabilitados.
        if (switchVerInactivos != null) {
            switchVerInactivos.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    dbHelper.refrescarProductosInactivos(busquedaActual());
                    Toast.makeText(this, R.string.msg_mostrando_deshabilitados, Toast.LENGTH_SHORT).show();
                } else {
                    dbHelper.refrescarProductos(busquedaActual());
                    Toast.makeText(this, R.string.msg_mostrando_activos, Toast.LENGTH_SHORT).show();
                }
                dibujarLista(isChecked ? productosInactivos : productosActivos);
            });
        }

        // Búsqueda en vivo: cada carácter solo pide un refresco; la consulta
        // la ejecuta el executor de DatabaseHelper fuera del hilo principal.
        // Se refresca la lista que se está viendo, no las dos.
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refrescarListaVisible();
            }
        });

        fabAgregar.setOnClickListener(v ->
                abrirFormulario(new Intent(this, ProductoFormActivity.class)));
    }

    /**
     * Menú de acciones sobre un producto del catálogo (CU-04 / CU-05 y edición).
     *
     * Registrar un movimiento desde la lista evita entrar al formulario solo
     * para mover stock, que es la operación más frecuente en una bodega. La
     * edición sigue siendo el formulario completo: es donde viven la validación
     * de SKU, la categoría y el proveedor.
     */
    private void mostrarOpcionesDelProducto(Producto producto) {
        CharSequence[] opciones = {
                getString(R.string.opcion_registrar_movimiento),
                getString(R.string.opcion_editar_producto)
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.titulo_opciones_producto, producto.getNombre()))
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        MovimientoStockDialog.mostrar(this, producto, proveedores,
                                productoDao, sessionManager, this::refrescarListaVisible);
                        return;
                    }
                    Intent intent = new Intent(ProductosActivity.this, ProductoFormActivity.class);
                    intent.putExtra(ProductoFormActivity.EXTRA_ID_PRODUCTO, producto.getIdProducto());
                    abrirFormulario(intent);
                })
                .show();
    }

    /** true cuando el interruptor pide ver los productos deshabilitados. */
    private boolean estaViendoInactivos() {
        return switchVerInactivos != null && switchVerInactivos.isChecked();
    }

    /** Pide al helper el refresco de la lista que está en pantalla. */
    private void refrescarListaVisible() {
        String filtro = busquedaActual();
        if (estaViendoInactivos()) {
            dbHelper.refrescarProductosInactivos(filtro);
        } else {
            dbHelper.refrescarProductos(filtro);
        }
    }

    /**
     * Dibuja una de las dos listas con su propio conteo y su propio texto de
     * "vacío": decir "todavía no hay productos registrados" cuando lo que no
     * hay son deshabilitados confunde y hace dudar de si la baja funcionó.
     */
    private void dibujarLista(List<Producto> productos) {
        adapter.updateData(productos);
        boolean viendoInactivos = estaViendoInactivos();
        tvHeroConteo.setText(getString(
                viendoInactivos ? R.string.label_total_deshabilitados : R.string.label_total_productos,
                productos.size()));
        boolean vacio = productos.isEmpty();
        tvVacioProductos.setText(viendoInactivos
                ? R.string.empty_deshabilitados : R.string.empty_productos);
        tvVacioProductos.setVisibility(vacio ? View.VISIBLE : View.GONE);
        ivVacioProductos.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rvProductos.setVisibility(vacio ? View.GONE : View.VISIBLE);
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
        // Vuelve a leer al regresar del formulario: ahí se creó, se editó o se
        // movió stock, y las dos listas pueden haber cambiado.
        dbHelper.refrescarProductos(busquedaActual());
        dbHelper.refrescarProductosInactivos(busquedaActual());
    }

    /** Texto que tiene el campo de búsqueda en este momento. */
    private String busquedaActual() {
        TextInputEditText etBuscar = findViewById(R.id.etBuscarProducto);
        return etBuscar.getText() != null ? etBuscar.getText().toString() : null;
    }
}