package com.android.sistemabodegaappandroid;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.db.ProductoDao;
import com.android.sistemabodegaappandroid.model.Categoria;
import com.android.sistemabodegaappandroid.model.Movimiento;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.model.Proveedor;
import com.android.sistemabodegaappandroid.model.TipoMovimiento;
import com.android.sistemabodegaappandroid.session.SessionManager;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Alta, edición y movimiento de stock de un producto.
 *
 * Una sola pantalla para los tres casos: con EXTRA_ID_PRODUCTO = 0 se da de
 * alta, con un id distinto se edita y la sección de movimientos queda a mano.
 * Reutilizar el formulario evita que los campos de alta y de edición se
 * desincronicen cuando cambia un requisito.
 */
public class ProductoFormActivity extends AppCompatActivity {

    /** id del producto a editar; 0 (o ausente) significa producto nuevo. */
    public static final String EXTRA_ID_PRODUCTO = "EXTRA_ID_PRODUCTO";

    /**
     * Hilo dedicado al historial de movimientos del detalle (lista que se
     * consultaba al abrir la pantalla en el hilo principal). Single-thread y
     * estático, igual que los executors de DatabaseHelper: serializa los
     * pedidos para que el último resultado publicado corresponda al último
     * producto pedido.
     */
    private static final ExecutorService movimientosExecutor = Executors.newSingleThreadExecutor();

    private DatabaseHelper dbHelper;
    private ProductoDao productoDao;
    private SessionManager sessionManager;

    private TextInputEditText etSku;
    private TextInputEditText etNombre;
    private TextInputEditText etDescripcion;
    private TextInputEditText etPrecioCosto;
    private TextInputEditText etPrecioVenta;
    private TextInputEditText etStockMinimo;
    private Spinner spinnerCategoria;
    private Spinner spinnerProveedor;
    private TextView tvStockActual;
    private TextView tvHistorialMovimientos;
    private View layoutMovimientos;
    private MaterialButton btnGuardar;
    // RF-08: se crea en código porque el layout del formulario no forma parte
    // de este frente de trabajo; el id vive en strings.xml como recurso.
    private MaterialButton btnDeshabilitarProducto;

    private int idProducto;
    /** Producto cargado desde la base; null cuando es alta. */
    private Producto producto;
    private List<Categoria> categorias = Collections.emptyList();
    private List<Proveedor> proveedores = Collections.emptyList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_producto_form);

        // El notch y las barras del sistema no deben tapar el contenido.
        EdgeToEdgeInsets.apply(this, findViewById(R.id.rootProductoForm));

        dbHelper = new DatabaseHelper(this);
        productoDao = new ProductoDao(dbHelper);
        sessionManager = new SessionManager(this);
        idProducto = getIntent().getIntExtra(EXTRA_ID_PRODUCTO, 0);

        etSku = findViewById(R.id.etSkuProducto);
        etNombre = findViewById(R.id.etNombreProducto);
        etDescripcion = findViewById(R.id.etDescripcionProducto);
        etPrecioCosto = findViewById(R.id.etPrecioCosto);
        etPrecioVenta = findViewById(R.id.etPrecioVenta);
        etStockMinimo = findViewById(R.id.etStockMinimo);
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        spinnerProveedor = findViewById(R.id.spinnerProveedor);
        tvStockActual = findViewById(R.id.tvStockActual);
        tvHistorialMovimientos = findViewById(R.id.tvHistorialMovimientos);
        layoutMovimientos = findViewById(R.id.layoutMovimientos);
        btnGuardar = findViewById(R.id.btnGuardarProducto);

        MaterialToolbar toolbar = findViewById(R.id.toolbarProductoForm);
        MaterialButton btnMovimiento = findViewById(R.id.btnRegistrarMovimiento);

        if (toolbar != null) {
            toolbar.setTitle(idProducto > 0
                    ? R.string.title_producto_editar : R.string.title_producto_nuevo);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        cargarCategorias();
        cargarProveedores();

        if (idProducto > 0) {
            cargarProductoExistente();
            // RF-08: la baja lógica solo aplica a un producto ya guardado.
            crearBotonDeshabilitar();
        } else {
            // Sin id todavía no hay historial que mostrar.
            layoutMovimientos.setVisibility(View.GONE);
        }

        btnGuardar.setOnClickListener(v -> guardar());
        btnMovimiento.setOnClickListener(v -> mostrarDialogoMovimiento());
    }

    /**
     * Carga las categorías activas en el selector; sin ninguna, no se puede guardar.
     *
     * La lista llega por LiveData (CU-10): la consulta corre en el executor de
     * DatabaseHelper y el observer rellena el spinner en el hilo principal.
     */
    private void cargarCategorias() {
        dbHelper.observarCategoriasActivas().observe(this, lista -> {
            if (lista == null) {
                return;
            }
            // Al recrear el adapter el spinner vuelve a la posición 0, así que
            // se recuerda la elegida para no perderla en un refresco.
            int seleccionPrevia = spinnerCategoria.getSelectedItemPosition();
            categorias = lista;
            List<String> nombres = new ArrayList<>();
            for (Categoria categoria : categorias) {
                nombres.add(categoria.getNombreCategoria());
            }
            ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, nombres);
            adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategoria.setAdapter(adaptador);
            // En edición la categoría del producto se restaura aquí mismo: el
            // LiveData entrega la lista después de cargarProductoExistente().
            if (producto != null) {
                spinnerCategoria.setSelection(indiceCategoria(producto.getIdCategoria()));
            } else if (seleccionPrevia >= 0 && seleccionPrevia < categorias.size()) {
                spinnerCategoria.setSelection(seleccionPrevia);
            }
        });
    }

    /**
     * El proveedor es opcional: la primera opción siempre es "Sin proveedor".
     *
     * La lista llega por LiveData (CU-11), con el mismo patrón que las
     * categorías: la consulta corre en el executor de DatabaseHelper y el
     * observer rellena el spinner en el hilo principal.
     */
    private void cargarProveedores() {
        dbHelper.observarProveedores().observe(this, lista -> {
            if (lista == null) {
                return;
            }
            // Al recrear el adapter el spinner vuelve a la posición 0, así que
            // se recuerda la elegida para no perderla en un refresco.
            int seleccionPrevia = spinnerProveedor.getSelectedItemPosition();
            proveedores = lista;
            List<String> nombres = new ArrayList<>();
            nombres.add(getString(R.string.sin_proveedor));
            for (Proveedor proveedor : proveedores) {
                nombres.add(proveedor.getRazonSocial());
            }
            ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, nombres);
            adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerProveedor.setAdapter(adaptador);
            // En edición el proveedor del producto se restaura aquí mismo: el
            // LiveData entrega la lista después de cargarProductoExistente().
            if (producto != null) {
                spinnerProveedor.setSelection(indiceProveedor(producto.getIdProveedor()));
            } else if (seleccionPrevia > 0 && seleccionPrevia <= proveedores.size()) {
                spinnerProveedor.setSelection(seleccionPrevia);
            }
        });
    }

    /** Rellena el formulario con el producto existente y su historial de stock. */
    private void cargarProductoExistente() {
        producto = productoDao.getProductoById(idProducto);
        if (producto == null) {
            // Entre abrir la pantalla y leer la base el producto pudo desactivarse.
            Toast.makeText(this, R.string.msg_producto_no_encontrado, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etSku.setText(producto.getSkuCodigo());
        etNombre.setText(producto.getNombre());
        etDescripcion.setText(producto.getDescripcion());
        etPrecioCosto.setText(formatearDecimalEdicion(producto.getPrecioCosto()));
        etPrecioVenta.setText(formatearDecimalEdicion(producto.getPrecioVenta()));
        etStockMinimo.setText(String.valueOf(producto.getStockMinimo()));

        spinnerCategoria.setSelection(indiceCategoria(producto.getIdCategoria()));
        spinnerProveedor.setSelection(indiceProveedor(producto.getIdProveedor()));

        layoutMovimientos.setVisibility(View.VISIBLE);
        mostrarStockYHistorial();
    }

    /**
     * RF-08: agrega el botón de baja lógica bajo el botón de guardar.
     * El botón se construye en código para no editar el layout del formulario,
     * que pertenece a otro frente de trabajo; el id del recurso permite seguir
     * localizando la vista desde las pruebas y desde el sistema.
     */
    private void crearBotonDeshabilitar() {
        float densidad = getResources().getDisplayMetrics().density;
        btnDeshabilitarProducto = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnDeshabilitarProducto.setId(R.id.btnDeshabilitarProducto);
        btnDeshabilitarProducto.setText(R.string.btn_deshabilitar_producto);
        btnDeshabilitarProducto.setTextSize(14);
        // colors.xml no define un token "error": accent es el rojo del tema.
        btnDeshabilitarProducto.setTextColor(ContextCompat.getColor(this, R.color.accent));
        btnDeshabilitarProducto.setStrokeColorResource(R.color.accent);
        btnDeshabilitarProducto.setCornerRadius((int) (8 * densidad));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (8 * densidad);
        params.bottomMargin = (int) (8 * densidad);
        btnDeshabilitarProducto.setLayoutParams(params);

        ViewGroup contenedor = (ViewGroup) btnGuardar.getParent();
        contenedor.addView(btnDeshabilitarProducto);
        btnDeshabilitarProducto.setOnClickListener(v -> confirmarDeshabilitarProducto());
    }

    /**
     * RF-08: pide confirmación antes de la baja lógica. No se borra la fila:
     * el producto deja de listarse y de aceptar operaciones, pero sus
     * movimientos pasados quedan intactos para la auditoría.
     */
    private void confirmarDeshabilitarProducto() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_deshabilitar_producto)
                .setMessage(R.string.confirmar_deshabilitar_producto)
                .setPositiveButton(R.string.btn_deshabilitar, (dialog, which) -> {
                    if (productoDao.updateProductoEstado(idProducto, 0)) {
                        Toast.makeText(this, R.string.msg_producto_deshabilitado,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.msg_producto_error,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Valida, construye el modelo y persiste según sea alta o edición. */
    private void guardar() {
        String sku = texto(etSku);
        String nombre = texto(etNombre);

        if (TextUtils.isEmpty(sku) || TextUtils.isEmpty(nombre)) {
            Toast.makeText(this, R.string.msg_nombre_sku_requerido, Toast.LENGTH_SHORT).show();
            return;
        }

        double precioCosto = parseDecimal(texto(etPrecioCosto));
        double precioVenta = parseDecimal(texto(etPrecioVenta));
        int stockMinimo = parseEntero(texto(etStockMinimo));

        if (precioCosto < 0 || precioVenta < 0 || stockMinimo < 0) {
            Toast.makeText(this, R.string.msg_valores_invalidos, Toast.LENGTH_SHORT).show();
            return;
        }

        // Sin categoría la fila quedaría huerfana en el inventario.
        if (categorias.isEmpty()) {
            Toast.makeText(this, R.string.msg_categoria_requerida, Toast.LENGTH_LONG).show();
            return;
        }

        int posicionCategoria = spinnerCategoria.getSelectedItemPosition();
        if (posicionCategoria < 0 || posicionCategoria >= categorias.size()) {
            posicionCategoria = 0;
        }
        int idCategoria = categorias.get(posicionCategoria).getIdCategoria();

        int posicionProveedor = spinnerProveedor.getSelectedItemPosition();
        int idProveedor = posicionProveedor > 0 && posicionProveedor <= proveedores.size()
                ? proveedores.get(posicionProveedor - 1).getIdProveedor() : 0;

        if (idProducto > 0) {
            guardarEdicion(sku, nombre, texto(etDescripcion), precioCosto, precioVenta,
                    stockMinimo, idCategoria, idProveedor);
        } else {
            guardarAlta(sku, nombre, texto(etDescripcion), precioCosto, precioVenta,
                    stockMinimo, idCategoria, idProveedor);
        }
    }

    private void guardarAlta(String sku, String nombre, String descripcion,
                             double precioCosto, double precioVenta, int stockMinimo,
                             int idCategoria, int idProveedor) {
        Producto nuevo = new Producto();
        nuevo.setSkuCodigo(sku);
        nuevo.setNombre(nombre);
        nuevo.setDescripcion(descripcion);
        nuevo.setPrecioCosto(precioCosto);
        nuevo.setPrecioVenta(precioVenta);
        // El stock inicial es cero: las existencias entran con movimientos,
        // así el primer registro queda auditado en vez de aparecer de la nada.
        nuevo.setStockActual(0);
        nuevo.setStockMinimo(stockMinimo);
        nuevo.setIdCategoria(idCategoria);
        nuevo.setIdProveedor(idProveedor);

        long resultado = productoDao.insertProducto(nuevo);
        if (resultado == -1) {
            Toast.makeText(this, R.string.msg_sku_duplicado, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, R.string.msg_producto_guardado, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void guardarEdicion(String sku, String nombre, String descripcion,
                                double precioCosto, double precioVenta, int stockMinimo,
                                int idCategoria, int idProveedor) {
        // El UNIQUE de sku_codigo es global: comprobamos contra TODOS los productos
        // (también deshabilitados) antes de escribir, porque buscarProductos solo
        // mira los activos y una reasignación a un SKU de un producto dado de baja
        // acabaría en SQLiteConstraintException sin capturar.
        if (productoDao.existeSkuEnOtroProducto(sku, idProducto)) {
            Toast.makeText(this, R.string.msg_sku_duplicado, Toast.LENGTH_LONG).show();
            return;
        }

        producto.setSkuCodigo(sku);
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setPrecioCosto(precioCosto);
        producto.setPrecioVenta(precioVenta);
        producto.setStockMinimo(stockMinimo);
        producto.setIdCategoria(idCategoria);
        producto.setIdProveedor(idProveedor);

        if (productoDao.updateProducto(producto)) {
            Toast.makeText(this, R.string.msg_producto_guardado, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.msg_producto_error, Toast.LENGTH_LONG).show();
        }
    }

    /** Abre el diálogo de entrada/salida de stock del producto en edición. */
    private void mostrarDialogoMovimiento() {
        if (producto == null || idProducto <= 0) {
            return;
        }

        View vista = getLayoutInflater().inflate(R.layout.dialog_movimiento_stock, null);
        TextView tvStockDialogo = vista.findViewById(R.id.tvStockActualDialog);
        TextInputEditText etCantidad = vista.findViewById(R.id.etCantidadMovimiento);
        RadioGroup rgTipo = vista.findViewById(R.id.rgTipoMovimiento);
        tvStockDialogo.setText(getString(R.string.label_stock, producto.getStockActual()));

        // El proveedor solo se asocia a entradas: en salidas el selector se oculta.
        View contenedorProveedor = vista.findViewById(R.id.contenedorProveedorMovimiento);
        AutoCompleteTextView selectorProveedor = vista.findViewById(R.id.spinnerProveedorMovimiento);
        final List<Proveedor> proveedoresMovimiento = poblarSelectorProveedor(selectorProveedor);
        RadioButton rbEntrada = vista.findViewById(R.id.rbEntrada);
        rbEntrada.setOnCheckedChangeListener((buttonView, isChecked)
                -> contenedorProveedor.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_movimiento_titulo)
                .setView(vista)
                .setPositiveButton(R.string.btn_confirmar_movimiento, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialogo.show();

        // El botón positivo se sobreescribe para poder validar la cantidad sin
        // cerrar el diálogo: si el dato es inválido el usuario lo corrige ahí.
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int cantidad = parseEntero(texto(etCantidad));
            if (cantidad <= 0) {
                Toast.makeText(this, R.string.msg_cantidad_invalida, Toast.LENGTH_SHORT).show();
                return;
            }

            TipoMovimiento tipo = rgTipo.getCheckedRadioButtonId() == R.id.rbSalida
                    ? TipoMovimiento.SALIDA : TipoMovimiento.ENTRADA;

            // Solo la entrada registra proveedor; la salida y "Sin proveedor" quedan en 0.
            int idProveedorMovimiento = 0;
            if (tipo == TipoMovimiento.ENTRADA) {
                // AutoCompleteTextView no es un AdapterView: no expone la
                // posición seleccionada, así que se resuelve por el texto elegido.
                String seleccion = selectorProveedor.getText() != null
                        ? selectorProveedor.getText().toString().trim() : "";
                if (!seleccion.isEmpty() && !seleccion.equals(getString(R.string.sin_proveedor))) {
                    for (Proveedor proveedor : proveedoresMovimiento) {
                        if (proveedor.getRazonSocial() != null
                                && proveedor.getRazonSocial().equals(seleccion)) {
                            idProveedorMovimiento = proveedor.getIdProveedor();
                            break;
                        }
                    }
                }
            }

            boolean ok = productoDao.actualizarStock(
                    idProducto, cantidad, tipo, sessionManager.getUsername(), idProveedorMovimiento);

            if (ok) {
                Toast.makeText(this, R.string.msg_movimiento_ok, Toast.LENGTH_SHORT).show();
                dialogo.dismiss();
                recargarProducto();
            } else {
                // Salida mayor que el stock disponible: el DAO la rechaza.
                Toast.makeText(this, R.string.msg_movimiento_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Puebla el selector del diálogo con los proveedores activos y devuelve la
     * lista en el mismo orden que las opciones. La primera opción es siempre
     * "Sin proveedor": en una entrada el proveedor es opcional.
     *
     * No consulta la base: reutiliza la lista que ya entregó el observer de
     * observarProveedores() (mismo contenido, mismo orden), así el diálogo se
     * arma sin tocar SQLite en el hilo principal.
     */
    private List<Proveedor> poblarSelectorProveedor(AutoCompleteTextView selector) {
        List<Proveedor> lista = proveedores;
        List<String> nombres = new ArrayList<>();
        nombres.add(getString(R.string.sin_proveedor));
        for (Proveedor proveedor : lista) {
            nombres.add(proveedor.getRazonSocial());
        }
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, nombres);
        selector.setAdapter(adaptador);
        return lista;
    }

    /** Vuelve a leer el producto para mostrar el stock ya actualizado. */
    private void recargarProducto() {
        producto = productoDao.getProductoById(idProducto);
        if (producto == null) {
            Toast.makeText(this, R.string.msg_producto_no_encontrado, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mostrarStockYHistorial();
    }

    /**
     * Dibuja el stock actual y el historial de movimientos, recientes primero.
     *
     * El stock ya viene en memoria (se leyó junto con el producto), pero la
     * lista de movimientos es una consulta: sale del hilo principal. El
     * executor la ejecuta y el resultado vuelve por runOnUiThread. Ese camino
     * es el adecuado acá porque es un pedido único atado a esta pantalla y a un
     * producto concreto; el patrón LiveData de DatabaseHelper queda para las
     * listas compartidas que varias pantallas refrescan, que necesitan caché.
     */
    private void mostrarStockYHistorial() {
        tvStockActual.setText(getString(R.string.label_stock, producto.getStockActual()));

        final int idConsultado = idProducto;
        movimientosExecutor.execute(() -> {
            final List<Movimiento> movimientos = productoDao.getMovimientosByProducto(idConsultado);
            runOnUiThread(() -> {
                // La pantalla pudo destruirse mientras se consultaba (rotación
                // o salida): no se toca una vista que ya no existe.
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                mostrarHistorial(movimientos);
            });
        });
    }

    /** Escribe el historial en el TextView. Solo se llama en el hilo principal. */
    private void mostrarHistorial(List<Movimiento> movimientos) {
        if (movimientos.isEmpty()) {
            tvHistorialMovimientos.setText(R.string.sin_movimientos);
            return;
        }

        StringBuilder textoHistorial = new StringBuilder();
        for (Movimiento movimiento : movimientos) {
            if (textoHistorial.length() > 0) {
                textoHistorial.append('\n');
            }
            textoHistorial.append(lineaDeMovimiento(movimiento));
        }
        tvHistorialMovimientos.setText(textoHistorial.toString());
    }

    /**
     * Una línea del historial de movimientos.
     *
     * Una ENTRADA con proveedor agrega la razón social de quien entregó la
     * mercancía; las salidas y las entradas sin proveedor mantienen el formato
     * de siempre para que la lista no se llene de "Proveedor: " vacíos.
     */
    private String lineaDeMovimiento(Movimiento movimiento) {
        String fecha = movimiento.getFecha() != null ? movimiento.getFecha() : "";
        String tipo = movimiento.getTipo() != null ? movimiento.getTipo() : "";
        String usuario = movimiento.getUsuario() != null ? movimiento.getUsuario() : "";
        String proveedor = movimiento.getRazonSocialProveedor();

        boolean esEntradaConProveedor = TipoMovimiento.ENTRADA.getValor().equals(tipo)
                && !TextUtils.isEmpty(proveedor);
        if (!esEntradaConProveedor) {
            return getString(R.string.linea_movimiento, fecha, tipo,
                    movimiento.getCantidad(), usuario);
        }
        return getString(R.string.linea_movimiento_proveedor, fecha, tipo,
                movimiento.getCantidad(), usuario,
                getString(R.string.label_proveedor, proveedor));
    }

    /** Posición del spinner para el id de categoría, o 0 si ya no existe. */
    private int indiceCategoria(int idCategoriaBuscada) {
        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).getIdCategoria() == idCategoriaBuscada) {
                return i;
            }
        }
        return 0;
    }

    /** Posición en el spinner de proveedores, considerando la opción vacía. */
    private int indiceProveedor(int idProveedorBuscado) {
        if (idProveedorBuscado <= 0) {
            return 0;
        }
        for (int i = 0; i < proveedores.size(); i++) {
            if (proveedores.get(i).getIdProveedor() == idProveedorBuscado) {
                return i + 1;
            }
        }
        return 0;
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() != null ? campo.getText().toString().trim() : "";
    }

    private double parseDecimal(String valor) {
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseEntero(String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Punto decimal fijo: el campo es de tipo numberDecimal y no admite comas. */
    private String formatearDecimalEdicion(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }
}
