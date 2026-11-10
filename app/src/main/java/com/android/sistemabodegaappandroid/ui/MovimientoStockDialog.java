package com.android.sistemabodegaappandroid.ui;

import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.db.ProductoDao;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.model.Proveedor;
import com.android.sistemabodegaappandroid.model.TipoMovimiento;
import com.android.sistemabodegaappandroid.session.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo único de entrada/salida de stock (CU-04 / CU-05).
 *
 * Existe para que el listado de productos y el formulario del producto no
 * tengan cada uno su propia versión del mismo diálogo: cuando eso pasó, una
 * copia registraba el proveedor y la otra no, y una de las dos reventaba con
 * una cantidad no numérica. Acá vive una sola implementación y los dos puntos
 * de entrada la llaman.
 *
 * Qué registra: tipo (entrada/salida), cantidad, proveedor (solo en entradas),
 * documento (guía, factura o vale) y motivo u observación. Las validaciones se
 * hacen antes de tocar la base y el diálogo NO se cierra cuando el dato es
 * inválido, para que el usuario lo corrija en el mismo lugar.
 */
public final class MovimientoStockDialog {

    /** Aviso de que el movimiento quedó registrado, para que la pantalla se refresque. */
    public interface AlRegistrarMovimiento {
        void alRegistrarMovimiento();
    }

    private MovimientoStockDialog() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Abre el diálogo sobre el producto indicado.
     *
     * @param actividad  pantalla que lo abre; se usa para inflar y mostrar.
     * @param producto   producto cuyo stock se mueve.
     * @param proveedores proveedores activos para el selector de la entrada. La
     *                    lista llega ya resuelta (observer de proveedores): el
     *                    diálogo no consulta SQLite en el hilo principal.
     * @param productoDao DAO con el que se registra el movimiento.
     * @param sesion     sesión actual; el usuario queda en el historial.
     * @param callback   se invoca solo si el movimiento se guardó.
     */
    public static void mostrar(final AppCompatActivity actividad, final Producto producto,
                               final List<Proveedor> proveedores, final ProductoDao productoDao,
                               final SessionManager sesion,
                               final AlRegistrarMovimiento callback) {
        if (producto == null) {
            return;
        }

        View vista = actividad.getLayoutInflater().inflate(R.layout.dialog_movimiento_stock, null);

        TextView tvStockActual = vista.findViewById(R.id.tvStockActualDialog);
        RadioGroup rgTipo = vista.findViewById(R.id.rgTipoMovimiento);
        RadioButton rbEntrada = vista.findViewById(R.id.rbEntrada);
        RadioButton rbSalida = vista.findViewById(R.id.rbSalida);
        View contenedorProveedor = vista.findViewById(R.id.contenedorProveedorMovimiento);
        AutoCompleteTextView selectorProveedor = vista.findViewById(R.id.spinnerProveedorMovimiento);
        TextInputEditText etCantidad = vista.findViewById(R.id.etCantidadMovimiento);
        TextInputEditText etDocumento = vista.findViewById(R.id.etDocumentoMovimiento);
        TextInputEditText etMotivo = vista.findViewById(R.id.etMotivoMovimiento);

        tvStockActual.setText(actividad.getString(R.string.label_stock, producto.getStockActual()));

        // El proveedor solo tiene sentido en una entrada: en una salida la
        // mercancía sale del almacén y no la entrega nadie.
        final List<Proveedor> proveedoresMovimiento = proveedores != null ? proveedores : new ArrayList<>();
        poblarSelectorProveedor(actividad, selectorProveedor, proveedoresMovimiento);
        rbEntrada.setChecked(true);
        contenedorProveedor.setVisibility(View.VISIBLE);
        rgTipo.setOnCheckedChangeListener((group, checkedId) ->
                contenedorProveedor.setVisibility(checkedId == R.id.rbSalida ? View.GONE : View.VISIBLE));

        final AlertDialog dialogo = new AlertDialog.Builder(actividad)
                .setTitle(actividad.getString(R.string.dialog_movimiento_titulo)
                        + ": " + producto.getNombre())
                .setView(vista)
                .setPositiveButton(R.string.btn_confirmar_movimiento, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialogo.show();

        // El botón positivo se sobreescribe para poder validar sin cerrar el
        // diálogo: si el dato es inválido el usuario lo corrige ahí.
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String textoCantidad = textoDe(etCantidad);
            String documento = textoDe(etDocumento);
            String motivo = textoDe(etMotivo);

            int cantidad = leerCantidad(textoCantidad);
            if (cantidad <= 0) {
                etCantidad.setError(actividad.getString(R.string.msg_cantidad_invalida));
                return;
            }

            boolean esSalida = rbSalida.isChecked();
            if (esSalida && cantidad > producto.getStockActual()) {
                etCantidad.setError(actividad.getString(
                        R.string.msg_stock_insuficiente, producto.getStockActual()));
                return;
            }
            // Una salida sin motivo no es auditable: no se sabe si fue venta,
            // merma o devolución.
            if (esSalida && motivo.isEmpty()) {
                etMotivo.setError(actividad.getString(R.string.msg_motivo_requerido));
                return;
            }

            TipoMovimiento tipo = esSalida ? TipoMovimiento.SALIDA : TipoMovimiento.ENTRADA;
            int idProveedor = esSalida ? 0 : idProveedorElegido(actividad, selectorProveedor, proveedoresMovimiento);

            String usuario = sesion != null ? sesion.getUsername() : null;
            if (TextUtils.isEmpty(usuario)) {
                usuario = actividad.getString(R.string.usuario_sin_sesion);
            }

            boolean registrado = productoDao.actualizarStock(
                    producto.getIdProducto(), cantidad, tipo, usuario, idProveedor, documento, motivo);

            if (registrado) {
                Toast.makeText(actividad, R.string.msg_movimiento_ok, Toast.LENGTH_SHORT).show();
                dialogo.dismiss();
                if (callback != null) {
                    callback.alRegistrarMovimiento();
                }
            } else {
                // Salida mayor al stock disponible o error de escritura: el DAO
                // ya revirtió la transacción.
                Toast.makeText(actividad, R.string.msg_movimiento_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Puebla el selector con los proveedores activos. La primera opción es
     * siempre "Sin proveedor": en una entrada el proveedor es opcional.
     */
    private static void poblarSelectorProveedor(AppCompatActivity actividad,
                                                AutoCompleteTextView selector,
                                                List<Proveedor> proveedores) {
        List<String> nombres = new ArrayList<>();
        nombres.add(actividad.getString(R.string.sin_proveedor));
        for (Proveedor proveedor : proveedores) {
            if (!TextUtils.isEmpty(proveedor.getRazonSocial())) {
                nombres.add(proveedor.getRazonSocial());
            }
        }
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                actividad, android.R.layout.simple_list_item_1, nombres);
        selector.setAdapter(adaptador);
    }

    /**
     * Traduce el texto del selector al id del proveedor.
     *
     * El selector es un AutoCompleteTextView y no expone la posición elegida,
     * además de que el usuario puede escribir: se resuelve por el nombre
     * exacto contra la lista que se mostró, y cualquier texto libre que no
     * coincida equivale a "sin proveedor" en vez de inventar un id.
     */
    private static int idProveedorElegido(AppCompatActivity actividad,
                                          AutoCompleteTextView selector,
                                          List<Proveedor> proveedores) {
        String seleccion = selector.getText() != null ? selector.getText().toString().trim() : "";
        if (seleccion.isEmpty() || seleccion.equals(actividad.getString(R.string.sin_proveedor))) {
            return 0;
        }
        for (Proveedor proveedor : proveedores) {
            if (seleccion.equals(proveedor.getRazonSocial())) {
                return proveedor.getIdProveedor();
            }
        }
        return 0;
    }

    /**
     * Convierte la cantidad escrita a entero sin lanzar.
     *
     * La versión anterior hacía {@code Integer.parseInt} directo: escribir
     * "dos" o un número más largo que un int cerraba la app con
     * NumberFormatException. Un texto inválido devuelve -1 y el llamador lo
     * trata como cantidad inválida.
     */
    public static int leerCantidad(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException cantidadNoNumerica) {
            return -1;
        }
    }

    /** Texto recortado del campo, o cadena vacía si está vacío. */
    private static String textoDe(TextInputEditText campo) {
        return campo.getText() != null ? campo.getText().toString().trim() : "";
    }
}
