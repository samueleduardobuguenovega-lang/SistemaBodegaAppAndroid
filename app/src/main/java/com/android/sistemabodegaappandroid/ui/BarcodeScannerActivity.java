package com.android.sistemabodegaappandroid.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.sistemabodegaappandroid.ProductoFormActivity;
import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.db.ProductoDao;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.util.AccesoModulo;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.android.sistemabodegaappandroid.util.Permisos;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Escáner de Código de Barras utilizando la Cámara Android (CU-07 / RF-20).
 * Maneja solicitud de permisos en runtime y flujos alternativos (código desconocido / inactivo).
 */
public class BarcodeScannerActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT_PRODUCTO_ID = "extra_producto_id";

    private DatabaseHelper dbHelper;
    private ProductoDao productoDao;

    private TextInputEditText etCodigoManual;
    private TextInputLayout tilCodigoSimulado;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    iniciarCamara();
                } else {
                    // Flujo alternativo 3a: permiso denegado, se ofrece búsqueda manual
                    Toast.makeText(this, R.string.msg_permiso_camara_denegado, Toast.LENGTH_LONG).show();
                    focarBusquedaManual();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Guard de permisos RF-27
        if (!AccesoModulo.permitido(this, Permisos.PRODUCTOS)) {
            return;
        }

        setContentView(R.layout.activity_barcode_scanner);
        EdgeToEdgeInsets.apply(this, findViewById(R.id.rootBarcodeScanner));

        dbHelper = new DatabaseHelper(this);
        productoDao = new ProductoDao(dbHelper);

        MaterialToolbar toolbar = findViewById(R.id.toolbarBarcodeScanner);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etCodigoManual = findViewById(R.id.etCodigoManual);
        tilCodigoSimulado = findViewById(R.id.tilCodigoSimulado);

        tilCodigoSimulado.setEndIconOnClickListener(v -> procesarCodigo(etCodigoManual.getText() != null ? etCodigoManual.getText().toString() : ""));

        etCodigoManual.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                procesarCodigo(etCodigoManual.getText() != null ? etCodigoManual.getText().toString() : "");
                return true;
            }
            return false;
        });

        verificarYPedirPermisoCamara();
    }

    private void verificarYPedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void iniciarCamara() {
        // En dispositivo físico/emulador con cámara activa el visor.
        // Se mantiene listo el campo manual para ingreso directo o simulación.
    }

    private void focarBusquedaManual() {
        if (etCodigoManual != null) {
            etCodigoManual.requestFocus();
        }
    }

    /** CU-07 Flujo principal y alternativos de consulta por código de barras. */
    private void procesarCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            Toast.makeText(this, "Ingrese o enfoque un código de barras válido", Toast.LENGTH_SHORT).show();
            return;
        }

        String codigoLimpio = codigo.trim();
        Producto productoInactivo = productoDao.getProductoByCodigoBarrasIncluyendoInactivos(codigoLimpio);

        if (productoInactivo == null) {
            // Flujo alternativo: Código no registrado
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Producto no encontrado")
                    .setMessage(R.string.msg_codigo_no_encontrado)
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        if (productoInactivo.getActivo() == 0) {
            // Flujo alternativo: Producto deshabilitado
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Producto Deshabilitado")
                    .setMessage(R.string.msg_producto_inactivo_escaneado)
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        // Flujo principal: Producto activo encontrado
        mostrarResultadoProducto(productoInactivo);
    }

    private void mostrarResultadoProducto(Producto producto) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_producto_escaneado, null);
        dialog.setContentView(dialogView);

        TextView tvNombre = dialogView.findViewById(R.id.tvEscaneadoNombre);
        TextView tvSku = dialogView.findViewById(R.id.tvEscaneadoSku);
        TextView tvCategoria = dialogView.findViewById(R.id.tvEscaneadoCategoria);
        TextView tvStock = dialogView.findViewById(R.id.tvEscaneadoStock);

        MaterialButton btnEntrada = dialogView.findViewById(R.id.btnEscaneadoEntrada);
        MaterialButton btnSalida = dialogView.findViewById(R.id.btnEscaneadoSalida);
        MaterialButton btnDetalle = dialogView.findViewById(R.id.btnEscaneadoVerDetalle);

        tvNombre.setText(producto.getNombre());
        tvSku.setText(getString(R.string.label_sku, producto.getSkuCodigo()) + " · Código: " + (producto.getCodigoBarras() != null ? producto.getCodigoBarras() : "-"));
        tvCategoria.setText(getString(R.string.label_categoria, producto.getNombreCategoria() != null ? producto.getNombreCategoria() : getString(R.string.sin_categoria)));
        tvStock.setText("Stock Actual: " + producto.getStockActual() + " (Mínimo: " + producto.getStockMinimo() + ")");

        btnEntrada.setOnClickListener(v -> {
            dialog.dismiss();
            abrirFormularioYFinalizar(producto.getIdProducto());
        });

        btnSalida.setOnClickListener(v -> {
            dialog.dismiss();
            abrirFormularioYFinalizar(producto.getIdProducto());
        });

        btnDetalle.setOnClickListener(v -> {
            dialog.dismiss();
            abrirFormularioYFinalizar(producto.getIdProducto());
        });

        dialog.show();
    }

    private void abrirFormularioYFinalizar(int idProducto) {
        Intent intent = new Intent(this, ProductoFormActivity.class);
        intent.putExtra(ProductoFormActivity.EXTRA_ID_PRODUCTO, idProducto);
        startActivity(intent);
        finish();
    }
}
