package com.android.sistemabodegaappandroid;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.db.ProductoDao;
import com.android.sistemabodegaappandroid.model.Categoria;
import com.android.sistemabodegaappandroid.model.Movimiento;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.model.Proveedor;
import com.android.sistemabodegaappandroid.model.TipoMovimiento;
import com.android.sistemabodegaappandroid.session.SessionManager;
import com.android.sistemabodegaappandroid.ui.BarcodeScannerActivity;
import com.android.sistemabodegaappandroid.ui.MovimientoStockDialog;
import com.android.sistemabodegaappandroid.util.AccesoModulo;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.android.sistemabodegaappandroid.util.Permisos;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
    private TextInputEditText etStockInicial;
    // CU-03: código de barras opcional y unidad de medida de lista fija.
    private TextInputEditText etCodigoBarras;

    /**
     * Recibe el código leído por la cámara. El escáner se abre en modo captura,
     * así que devuelve el texto en vez de buscar el producto.
     */
    private final ActivityResultLauncher<Intent> escanerCodigoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                String codigo = result.getData().getStringExtra(BarcodeScannerActivity.EXTRA_RESULT_CODIGO);
                if (codigo == null || codigo.trim().isEmpty()) {
                    return;
                }
                etCodigoBarras.setText(codigo.trim());
                etCodigoBarras.setSelection(etCodigoBarras.getText().length());
            });
    private Spinner spinnerUnidadMedida;
    private List<String> unidadesMedida;
    private android.view.View tilStockInicial;
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

        // RF-27: el formulario también se abre por intent directo (o queda en
        // una tarea restaurada), así que revalida el permiso del módulo igual
        // que el listado. AccesoModulo ya avisa, vuelve al panel y cierra.
        if (!AccesoModulo.permitido(this, Permisos.PRODUCTOS)) {
            return;
        }

        idProducto = getIntent().getIntExtra(EXTRA_ID_PRODUCTO, 0);

        etSku = findViewById(R.id.etSkuProducto);
        etNombre = findViewById(R.id.etNombreProducto);
        etDescripcion = findViewById(R.id.etDescripcionProducto);
        etPrecioCosto = findViewById(R.id.etPrecioCosto);
        etPrecioVenta = findViewById(R.id.etPrecioVenta);
        etStockMinimo = findViewById(R.id.etStockMinimo);
        etStockInicial = findViewById(R.id.etStockInicial);
        tilStockInicial = findViewById(R.id.tilStockInicial);
        etCodigoBarras = findViewById(R.id.etCodigoBarras);
        configurarEscaneoCodigoBarras();
        spinnerUnidadMedida = findViewById(R.id.spinnerUnidadMedida);
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
        configurarUnidadesMedida();

        if (idProducto > 0) {
            cargarProductoExistente();
            // RF-08: la baja lógica solo aplica a un producto ya guardado.
            crearBotonDeshabilitar();
            // El stock actual no se edita (RF-07): se cambia con movimientos, y
            // el valor vigente ya se muestra en tvStockActual.
            tilStockInicial.setVisibility(View.GONE);
        } else {
            // Sin id todavía no hay historial que mostrar.
            layoutMovimientos.setVisibility(View.GONE);
        }

        btnGuardar.setOnClickListener(v -> guardar());
        btnMovimiento.setOnClickListener(v -> mostrarDialogoMovimiento());

        animarEntradaFormulario();
    }

    /**
     * El ícono del campo abre la cámara. El código leído se escribe en el campo
     * para que la validación de duplicados lo revise igual que si se tipeó.
     */
    private void configurarEscaneoCodigoBarras() {
        TextInputLayout tilCodigo = findViewById(R.id.tilCodigoBarras);
        if (tilCodigo == null) {
            return;
        }
        tilCodigo.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        tilCodigo.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(this, BarcodeScannerActivity.class);
            intent.putExtra(BarcodeScannerActivity.EXTRA_MODO_CAPTURA, true);
            escanerCodigoLauncher.launch(intent);
        });
    }

    /**
     * Entrada escalonada estilo Expressive: cada sección del formulario sube y
     * aparece con ~40 ms de desfase, en vez de aparecer todo de golpe.
     */
    private void animarEntradaFormulario() {
        ViewGroup raizScroll = findViewById(R.id.scrollProductoForm);
        if (raizScroll == null || raizScroll.getChildCount() == 0) {
            return;
        }
        ViewGroup contenido = (ViewGroup) raizScroll.getChildAt(0);
        long desfase = 0L;
        for (int i = 0; i < contenido.getChildCount(); i++) {
            View hijo = contenido.getChildAt(i);
            hijo.setAlpha(0f);
            hijo.setTranslationY(28f);
            hijo.animate().alpha(1f).translationY(0f)
                    .setStartDelay(desfase)
                    .setDuration(240)
                    .setInterpolator(new DecelerateInterpolator(1.4f))
                    .start();
            desfase += 40L;
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    /**
     * Carga las categorías activas en el selector; sin ninguna, no se puede guardar.
     *
     * La lista llega por LiveData (CU-10): la consulta corre en el executor de
     * DatabaseHelper y el observer rellena el spinner en el hilo principal.
     */
    /**
     * Rellena el selector de unidad de medida desde una lista fija del
     * proyecto (array unidades_medida), igual que las categorías fijas:
     * la unidad no se administra, se elige.
     */
    private void configurarUnidadesMedida() {
        unidadesMedida = new ArrayList<>();
        for (String unidad : getResources().getStringArray(R.array.unidades_medida)) {
            unidadesMedida.add(unidad);
        }
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, unidadesMedida);
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnidadMedida.setAdapter(adaptador);
    }

    /** Posición en el selector de la unidad guardada; 0 (primera) si no está. */
    private int indiceUnidad(String unidad) {
        if (unidadesMedida == null || unidad == null) {
            return 0;
        }
        int indice = unidadesMedida.indexOf(unidad);
        return indice >= 0 ? indice : 0;
    }

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
        etCodigoBarras.setText(producto.getCodigoBarras() != null ? producto.getCodigoBarras() : "");
        spinnerUnidadMedida.setSelection(indiceUnidad(producto.getUnidadMedida()));

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
        String nombre = texto(etNombre);
        if (TextUtils.isEmpty(nombre)) {
            Toast.makeText(this, "El nombre del producto no puede estar vacío", Toast.LENGTH_SHORT).show();
            etNombre.requestFocus();
            return;
        }

        String sku = texto(etSku);
        if (TextUtils.isEmpty(sku)) {
            sku = "SKU-" + (System.currentTimeMillis() % 10000000) + "-" + (int) (Math.random() * 1000);
            while (productoDao.existeSkuEnOtroProducto(sku, idProducto)) {
                sku = "SKU-" + (System.currentTimeMillis() % 10000000) + "-" + (int) (Math.random() * 1000);
            }
            etSku.setText(sku);
            Toast.makeText(this, getString(R.string.msg_sku_generado, sku),
                    Toast.LENGTH_SHORT).show();
        }

        double precioCosto = parseDecimal(texto(etPrecioCosto));
        double precioVenta = parseDecimal(texto(etPrecioVenta));
        int stockMinimo = parseEntero(texto(etStockMinimo));

        // RF-03: el stock inicial se registra en el alta. El campo vacío vale
        // cero y es de tipo numérico, así que no admite texto: parseEntero()
        // devuelve 0 si el texto no es un número.
        String textoStockInicial = texto(etStockInicial);
        int stockInicial = textoStockInicial.isEmpty() ? 0 : parseEntero(textoStockInicial);

        if (precioCosto < 0 || precioVenta < 0 || stockMinimo < 0
                || (idProducto <= 0 && stockInicial < 0)) {
            Toast.makeText(this, R.string.msg_valores_invalidos, Toast.LENGTH_SHORT).show();
            return;
        }

        // CU-03: el código de barras solo se valida cuando se informa; el
        // vacío es NULL y puede repetirse. La comprobación es igual a la del
        // SKU: contra todos los productos, deshabilitados incluidos.
        String codigoBarras = texto(etCodigoBarras).trim();
        if (!codigoBarras.isEmpty()
                && productoDao.existeCodigoBarrasEnOtroProducto(codigoBarras, idProducto)) {
            Toast.makeText(this, R.string.msg_codigo_barras_duplicado,
                    Toast.LENGTH_LONG).show();
            return;
        }
        String unidadMedida = unidadesMedida != null
                && spinnerUnidadMedida.getSelectedItemPosition() >= 0
                && spinnerUnidadMedida.getSelectedItemPosition() < unidadesMedida.size()
                ? unidadesMedida.get(spinnerUnidadMedida.getSelectedItemPosition())
                : "";

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
                    stockMinimo, idCategoria, idProveedor, codigoBarras, unidadMedida);
        } else {
            guardarAlta(sku, nombre, texto(etDescripcion), precioCosto, precioVenta,
                    stockMinimo, stockInicial, idCategoria, idProveedor,
                    codigoBarras, unidadMedida);
        }
    }

    private void guardarAlta(String skuParam, String nombre, String descripcion,
                             double precioCosto, double precioVenta, int stockMinimo,
                             int stockInicial, int idCategoria, int idProveedor,
                             String codigoBarras, String unidadMedida) {
        String sku = skuParam;
        Producto nuevo = new Producto();
        nuevo.setSkuCodigo(sku);
        nuevo.setNombre(nombre);
        nuevo.setDescripcion(descripcion);
        nuevo.setPrecioCosto(precioCosto);
        nuevo.setPrecioVenta(precioVenta);
        // El producto nace en cero y el stock inicial entra como movimiento:
        // así las existencias quedan auditadas en el historial en vez de
        // aparecer como un número sin origen (RF-03).
        nuevo.setStockMinimo(stockMinimo);
        nuevo.setIdCategoria(idCategoria);
        nuevo.setIdProveedor(idProveedor);
        nuevo.setCodigoBarras(codigoBarras.isEmpty() ? null : codigoBarras);
        nuevo.setUnidadMedida(unidadMedida);

        // El stock inicial se asienta como movimiento de entrada auditada (stockInicial, TipoMovimiento.ENTRADA)
        // pero dentro de la MISMA transacción que el INSERT del producto (CU-04):
        // o quedan los dos, o no queda ninguno de los dos.
        long resultado = productoDao.insertarProductoConStockInicial(nuevo, stockInicial,
                sessionManager.getUsername(), idProveedor);

        if (resultado == -1 && productoDao.existeSkuEnOtroProducto(sku, 0)) {
            // Reintento automático con SKU nuevo si hubo colisión imprevista.
            sku = "SKU-" + (System.currentTimeMillis() % 10000000) + "-" + (int) (Math.random() * 1000);
            nuevo.setSkuCodigo(sku);
            etSku.setText(sku);
            resultado = productoDao.insertarProductoConStockInicial(nuevo, stockInicial,
                    sessionManager.getUsername(), idProveedor);
        }

        if (resultado < 0) {
            // Un -1 nunca es éxito: o el SKU sigue chocando con el UNIQUE global
            // o la transacción revirtió el alta junto con su movimiento.
            boolean skuOcupado = productoDao.existeSkuEnOtroProducto(sku, 0);
            Toast.makeText(this,
                    skuOcupado ? R.string.msg_sku_duplicado : R.string.msg_producto_error,
                    Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, R.string.msg_producto_guardado, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void guardarEdicion(String sku, String nombre, String descripcion,
                                double precioCosto, double precioVenta, int stockMinimo,
                                int idCategoria, int idProveedor,
                                String codigoBarras, String unidadMedida) {
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
        producto.setCodigoBarras(codigoBarras.isEmpty() ? null : codigoBarras);
        producto.setUnidadMedida(unidadMedida);

        if (productoDao.updateProducto(producto)) {
            Toast.makeText(this, R.string.msg_producto_guardado, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.msg_producto_error, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Abre el diálogo de entrada/salida de stock del producto en edición.
     *
     * Desde la unificación del diálogo (CU-04 / CU-05) este método solo delega:
     * el listado de productos y el formulario muestran exactamente el mismo
     * diálogo, con proveedor en las entradas, documento y motivo. Antes había
     * dos copias y una de ellas guardaba siempre id_proveedor = 0.
     */
    private void mostrarDialogoMovimiento() {
        if (producto == null || idProducto <= 0) {
            return;
        }
        MovimientoStockDialog.mostrar(this, producto, proveedores, productoDao, sessionManager,
                this::recargarProducto);
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