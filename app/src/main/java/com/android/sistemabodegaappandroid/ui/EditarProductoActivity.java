package com.android.sistemabodegaappandroid.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.db.ProductoDao;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.session.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

public class EditarProductoActivity extends AppCompatActivity {

    private TextInputEditText etSku, etNombre, etDescripcion, etStockMinimo, etPrecioCosto, etPrecioVenta;
    private Switch switchActivo;
    private Button btnGuardar;

    private ProductoDao productoDao;
    private int idProducto = -1;
    private Producto productoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- CONTROL DE ACCESO CON DEPURACIÓN DE ROL ---
        SessionManager sessionManager = new SessionManager(this);
        String rolUsuario = sessionManager.getRoleName();

        // Muestra qué rol exacto tiene el usuario logueado en un mensaje flotante
        Toast.makeText(this, "Rol detectado: " + rolUsuario, Toast.LENGTH_LONG).show();

        if (rolUsuario != null && rolUsuario.toLowerCase().contains("vendedor")) {
            Toast.makeText(this, "Acceso denegado: Los vendedores no tienen permisos", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // ----------------------------------------------

        setContentView(R.layout.activity_editar_producto);

        // Inicializar vistas
        etSku = findViewById(R.id.etSku);
        etNombre = findViewById(R.id.etNombre);
        etDescripcion = findViewById(R.id.etDescripcion);
        etStockMinimo = findViewById(R.id.etStockMinimo);
        etPrecioCosto = findViewById(R.id.etPrecioCosto);
        etPrecioVenta = findViewById(R.id.etPrecioVenta);
        switchActivo = findViewById(R.id.switchActivo);
        btnGuardar = findViewById(R.id.btnGuardar);

        // Inicializar DAO
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        productoDao = new ProductoDao(dbHelper);

        // Recibir el ID del producto enviado desde la lista de consulta
        idProducto = getIntent().getIntExtra("PRODUCTO_ID", -1);
        if (idProducto != -1) {
            cargarDatosProducto();
        } else {
            Toast.makeText(this, "Error: Producto no encontrado", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Evento del botón Guardar
        btnGuardar.setOnClickListener(v -> guardarCambios());
    }

    private void cargarDatosProducto() {
        productoActual = productoDao.getProductoById(idProducto);
        if (productoActual != null) {
            etSku.setText(productoActual.getSkuCodigo());
            etNombre.setText(productoActual.getNombre());
            etDescripcion.setText(productoActual.getDescripcion());
            etStockMinimo.setText(String.valueOf(productoActual.getStockMinimo()));
            etPrecioCosto.setText(String.valueOf(productoActual.getPrecioCosto()));
            etPrecioVenta.setText(String.valueOf(productoActual.getPrecioVenta()));

            // Si está activo (1) el switch va encendido, si es 0 va apagado
            switchActivo.setChecked(productoActual.getActivo() == 1);
        }
    }

    private void guardarCambios() {
        if (productoActual == null) return;

        String sku = etSku.getText().toString().trim();
        String nombre = etNombre.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String sStockMin = etStockMinimo.getText().toString().trim();
        String sPrecioCosto = etPrecioCosto.getText().toString().trim();
        String sPrecioVenta = etPrecioVenta.getText().toString().trim();

        // Validaciones básicas de campos obligatorios
        if (sku.isEmpty() || nombre.isEmpty() || sStockMin.isEmpty()) {
            Toast.makeText(this, "El SKU, nombre y stock mínimo son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar si el SKU ya pertenece a otro producto en la base de datos
        if (productoDao.existeSkuEnOtroProducto(sku, idProducto)) {
            Toast.makeText(this, "El código SKU ya está registrado en otro producto", Toast.LENGTH_LONG).show();
            return;
        }

        // Actualizar los datos en el objeto producto
        productoActual.setSkuCodigo(sku);
        productoActual.setNombre(nombre);
        productoActual.setDescripcion(descripcion);
        productoActual.setStockMinimo(Integer.parseInt(sStockMin));
        productoActual.setPrecioCosto(sPrecioCosto.isEmpty() ? 0.0 : Double.parseDouble(sPrecioCosto));
        productoActual.setPrecioVenta(sPrecioVenta.isEmpty() ? 0.0 : Double.parseDouble(sPrecioVenta));

        // Obtener el estado del switch para la baja lógica (1 = activo, 0 = inactivo)
        int estadoActivo = switchActivo.isChecked() ? 1 : 0;
        productoActual.setActivo(estadoActivo);

        // 1. Ejecutar la modificación general (RF-07)
        boolean actualizado = productoDao.updateProducto(productoActual);

        // 2. Ejecutar la baja lógica en caso de que cambie el estado (RF-08)
        productoDao.updateProductoEstado(idProducto, estadoActivo);

        if (actualizado) {
            Toast.makeText(this, "Producto actualizado correctamente", Toast.LENGTH_SHORT).show();
            finish(); // Cierra la pantalla y regresa al listado
        } else {
            Toast.makeText(this, "Error al actualizar el producto", Toast.LENGTH_SHORT).show();
        }
    }
}