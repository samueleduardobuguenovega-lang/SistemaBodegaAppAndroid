package com.android.sistemabodegaappandroid;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.db.ProveedorDao;
import com.android.sistemabodegaappandroid.model.Proveedor;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Alta y edición de proveedores.
 *
 * El RUT es la clave natural de la tabla: se comprueba antes de escribir tanto
 * en alta como en edición, porque updateProveedor no captura la restricción
 * UNIQUE y una colisión lanzaría la excepción en caliente.
 */
public class ProveedorFormActivity extends AppCompatActivity {

    /** id del proveedor a editar; 0 (o ausente) significa proveedor nuevo. */
    public static final String EXTRA_ID_PROVEEDOR = "EXTRA_ID_PROVEEDOR";

    private ProveedorDao proveedorDao;
    private TextInputEditText etRazonSocial;
    private TextInputEditText etRut;
    private TextInputEditText etEmail;
    private TextInputEditText etTelefono;
    private TextInputEditText etDireccion;

    private int idProveedor;
    private Proveedor proveedor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proveedor_form);

        // El notch y las barras del sistema no deben tapar el contenido.
        EdgeToEdgeInsets.apply(this, findViewById(R.id.rootProveedorForm));

        proveedorDao = new ProveedorDao(new DatabaseHelper(this));
        idProveedor = getIntent().getIntExtra(EXTRA_ID_PROVEEDOR, 0);

        etRazonSocial = findViewById(R.id.etRazonSocial);
        etRut = findViewById(R.id.etRut);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        etDireccion = findViewById(R.id.etDireccion);

        MaterialToolbar toolbar = findViewById(R.id.toolbarProveedorForm);
        MaterialButton btnGuardar = findViewById(R.id.btnGuardarProveedor);

        if (toolbar != null) {
            toolbar.setTitle(idProveedor > 0
                    ? R.string.title_proveedor_editar : R.string.title_proveedor_nuevo);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        if (idProveedor > 0) {
            cargarProveedorExistente();
        }

        btnGuardar.setOnClickListener(v -> guardar());
    }

    /** Rellena el formulario con los datos del proveedor en edición. */
    private void cargarProveedorExistente() {
        proveedor = proveedorDao.getProveedorById(idProveedor);
        if (proveedor == null) {
            Toast.makeText(this, R.string.msg_proveedor_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        etRazonSocial.setText(proveedor.getRazonSocial());
        etRut.setText(proveedor.getRut());
        etEmail.setText(proveedor.getEmail());
        etTelefono.setText(proveedor.getTelefono());
        etDireccion.setText(proveedor.getDireccion());
    }

    /** Valida, construye el modelo y persiste según sea alta o edición. */
    private void guardar() {
        String razonSocial = texto(etRazonSocial);
        String rut = texto(etRut);
        String telefono = texto(etTelefono);

        if (TextUtils.isEmpty(razonSocial) || TextUtils.isEmpty(rut)) {
            Toast.makeText(this, R.string.msg_razon_rut_requeridos, Toast.LENGTH_SHORT).show();
            return;
        }

        // El contrato exige razón social Y número de contacto. La obligatoriedad
        // se valida aquí a propósito: reforzar el NOT NULL de PROVEEDOR obligaría
        // a reconstruir la tabla y a mover las claves foráneas de PRODUCTO y MOVIMIENTO.
        if (TextUtils.isEmpty(telefono)) {
            Toast.makeText(this, R.string.msg_telefono_requerido, Toast.LENGTH_SHORT).show();
            return;
        }

        // isRutExists ignora mayúsculas: evita duplicados "1-2" vs "1-2 " y mayúsculas.
        if (proveedorDao.isRutExists(rut) && !rut.equalsIgnoreCase(textoOriginal(proveedor))) {
            Toast.makeText(this, R.string.msg_rut_duplicado, Toast.LENGTH_LONG).show();
            return;
        }

        Proveedor modelo = proveedor != null ? proveedor : new Proveedor();
        modelo.setRazonSocial(razonSocial);
        modelo.setRut(rut);
        modelo.setEmail(texto(etEmail));
        modelo.setTelefono(texto(etTelefono));
        modelo.setDireccion(texto(etDireccion));

        boolean ok;
        if (idProveedor > 0) {
            ok = proveedorDao.updateProveedor(modelo);
        } else {
            long resultado = proveedorDao.insertProveedor(modelo);
            ok = resultado != -1;
            if (!ok) {
                // El índice UNIQUE responde aunque la comprobación previa falle.
                Toast.makeText(this, R.string.msg_rut_duplicado, Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (ok) {
            Toast.makeText(this, R.string.msg_proveedor_guardado, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.msg_proveedor_error, Toast.LENGTH_LONG).show();
        }
    }

    /** RUT original del proveedor en edición; vacío cuando es alta. */
    private String textoOriginal(Proveedor modelo) {
        return modelo != null && modelo.getRut() != null ? modelo.getRut().trim() : "";
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() != null ? campo.getText().toString().trim() : "";
    }
}
