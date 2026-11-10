package com.android.sistemabodegaappandroid;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.db.ProductoDao;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.model.TipoMovimiento;
import com.android.sistemabodegaappandroid.session.SessionManager;
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

        if (!AccesoModulo.permitido(this, Permisos.PRODUCTOS)) {
            return;
        }

        setContentView(R.layout.activity_productos);
        EdgeToEdgeInsets.apply(this, findViewById(R.id.rootProductos));

        dbHelper = new DatabaseHelper(this);
        productoDao = new ProductoDao(dbHelper);
        rvProductos = findViewById(R.id.rvProductos);
        tvVacioProductos = findViewById(R.id.tvVacioProductos);
        tvHeroConteo = findViewById(R.id.tvHeroConteo);
        ivVacioProductos = findViewById(R.id.ivVacioProductos);
        switchVerInactivos = findViewById(R.id.switchVerInactivos);
        TextInputEditText etBuscar = findViewById(R.id.etBuscarProducto);
        MaterialToolbar toolbar = findViewById(R.id.toolbarProductos);
        FloatingActionButton fabAgregar = findViewById(R.id.fabAgregarProducto);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // INTEGRACIÓN CU-04 / CU-05: Menú de opciones al hacer clic en un producto
        adapter = new ProductoAdapter(producto -> {
            CharSequence[] opciones = {"📦 Registrar Entrada/Salida", "✏️ Editar Producto"};

            new AlertDialog.Builder(ProductosActivity.this)
                    .setTitle("Opciones para: " + producto.getNombre())
                    .setItems(opciones, (dialog, which) -> {
                        if (which == 0) {
                            // Llama a TU requerimiento (Persona 3)
                            mostrarDialogoMovimiento(producto);
                        } else {
                            // Llama al requerimiento de Nico (Persona 2)
                            Intent intent = new Intent(ProductosActivity.this, ProductoFormActivity.class);
                            intent.putExtra(ProductoFormActivity.EXTRA_ID_PRODUCTO, producto.getIdProducto());
                            abrirFormulario(intent);
                        }
                    })
                    .show();
        });

        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        rvProductos.setAdapter(adapter);
        rvProductos.setLayoutAnimation(new LayoutAnimationController(
                AnimationUtils.loadAnimation(this, R.anim.item_fall), 0.07f));

        dbHelper.observarProductos(null).observe(this, productos -> {
            if (switchVerInactivos != null && switchVerInactivos.isChecked()) {
                return;
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

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                cargarListaProductosSegunSwitch();
            }
        });

        fabAgregar.setOnClickListener(v ->
                abrirFormulario(new Intent(this, ProductoFormActivity.class)));
    }

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
        cargarListaProductosSegunSwitch();
    }

    private String busquedaActual() {
        TextInputEditText etBuscar = findViewById(R.id.etBuscarProducto);
        return etBuscar.getText() != null ? etBuscar.getText().toString() : null;
    }

    /**
     * Muestra el diálogo para registrar una entrada o salida de stock (CU-04 / CU-05).
     * @param producto El producto al que se le modificará el stock.
     */
    public void mostrarDialogoMovimiento(Producto producto) {
        SessionManager sessionManager = new SessionManager(this);

        View view = getLayoutInflater().inflate(R.layout.dialog_movimiento_stock, null);

        // Variables declaradas correctamente para evitar errores en rojo
        TextView tvStockActual = view.findViewById(R.id.tvStockActualDialog);
        RadioGroup rgTipoMovimiento = view.findViewById(R.id.rgTipoMovimiento);
        RadioButton rbEntrada = view.findViewById(R.id.rbEntrada);
        RadioButton rbSalida = view.findViewById(R.id.rbSalida);
        View contenedorProveedor = view.findViewById(R.id.contenedorProveedorMovimiento);

        TextInputEditText etCantidad = view.findViewById(R.id.etCantidadMovimiento);
        TextInputEditText etDocumento = view.findViewById(R.id.etDocumentoMovimiento);
        TextInputEditText etMotivo = view.findViewById(R.id.etMotivoMovimiento);

        tvStockActual.setText("Stock actual: " + producto.getStockActual());

        rgTipoMovimiento.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSalida) {
                contenedorProveedor.setVisibility(View.GONE);
            } else {
                contenedorProveedor.setVisibility(View.VISIBLE);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Movimiento: " + producto.getNombre());
        builder.setView(view);

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton("Guardar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String strCantidad = etCantidad.getText() != null ? etCantidad.getText().toString().trim() : "";
            String documento = etDocumento.getText() != null ? etDocumento.getText().toString().trim() : "";
            String motivo = etMotivo.getText() != null ? etMotivo.getText().toString().trim() : "";

            if (strCantidad.isEmpty()) {
                etCantidad.setError("Ingresa una cantidad");
                return;
            }

            int cantidad = Integer.parseInt(strCantidad);
            if (cantidad <= 0) {
                etCantidad.setError("La cantidad debe ser mayor a 0");
                return;
            }

            TipoMovimiento tipo = rbEntrada.isChecked() ? TipoMovimiento.ENTRADA : TipoMovimiento.SALIDA;
            int idProveedorSeleccionado = 0;

            if (tipo == TipoMovimiento.SALIDA) {
                if (cantidad > producto.getStockActual()) {
                    etCantidad.setError("Stock insuficiente. Actual: " + producto.getStockActual());
                    return;
                }
                if (motivo.isEmpty()) {
                    etMotivo.setError("El motivo es obligatorio para salidas");
                    return;
                }
            }

            String nombreUsuario = sessionManager.getUsername();
            if (nombreUsuario == null || nombreUsuario.isEmpty()) {
                nombreUsuario = "Operador";
            }

            boolean exito = productoDao.actualizarStock(
                    producto.getIdProducto(),
                    cantidad,
                    tipo,
                    nombreUsuario,
                    idProveedorSeleccionado,
                    documento,
                    motivo
            );

            if (exito) {
                Toast.makeText(this, "Movimiento guardado exitosamente", Toast.LENGTH_SHORT).show();
                cargarListaProductosSegunSwitch(); // Refresca la lista automáticamente
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Error al guardar el movimiento", Toast.LENGTH_LONG).show();
            }
        });
    }
}