package com.android.sistemabodegaappandroid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.ProductoFormActivity;
import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.db.ProductoDao;
import com.android.sistemabodegaappandroid.model.Categoria;
import com.android.sistemabodegaappandroid.model.Movimiento;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.ui.adapter.InformeStockAdapter;
import com.android.sistemabodegaappandroid.ui.adapter.MovimientoGeneralAdapter;
import com.android.sistemabodegaappandroid.util.AccesoModulo;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.android.sistemabodegaappandroid.util.Permisos;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Módulo de Informes de Stock y Consulta de Historial (CU-06 / RF-19 / RF-21 / RF-22).
 */
public class InformesActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ProductoDao productoDao;

    private InformeStockAdapter informeAdapter;
    private MovimientoGeneralAdapter movimientoAdapter;

    private TextView tvStatTotalProductos;
    private TextView tvStatStockBajo;
    private TextView tvStatSinStock;
    private TextView tvVacioInformes;
    private Spinner spinnerCategoria;
    private Spinner spinnerEstado;
    private TextInputEditText etBuscar;
    private TabLayout tabLayout;
    private RecyclerView rvInformes;
    private MaterialButton btnExportar;
    // Filtros y contadores que solo tienen sentido en el informe de stock: el
    // historial se filtra por producto, no por categoría ni por estado.
    private View filaFiltrosStock;
    private View containerResumenInformes;

    private final List<Categoria> listaCategorias = new ArrayList<>();
    private int idCategoriaSeleccionada = 0;
    private String estadoSeleccionado = "TODOS";
    private int tabActual = 0; // 0: Informe Stock, 1: Historial

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Guard de permisos RF-27
        if (!AccesoModulo.permitido(this, Permisos.INFORMES)) {
            return;
        }

        setContentView(R.layout.activity_informes);
        EdgeToEdgeInsets.apply(this, findViewById(R.id.rootInformes));

        dbHelper = new DatabaseHelper(this);
        productoDao = new ProductoDao(dbHelper);

        MaterialToolbar toolbar = findViewById(R.id.toolbarInformes);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tvStatTotalProductos = findViewById(R.id.tvStatTotalProductos);
        tvStatStockBajo = findViewById(R.id.tvStatStockBajo);
        tvStatSinStock = findViewById(R.id.tvStatSinStock);
        tvVacioInformes = findViewById(R.id.tvVacioInformes);
        spinnerCategoria = findViewById(R.id.spinnerCategoriaInforme);
        spinnerEstado = findViewById(R.id.spinnerEstadoInforme);
        etBuscar = findViewById(R.id.etBuscarInforme);
        tabLayout = findViewById(R.id.tabLayoutInformes);
        rvInformes = findViewById(R.id.rvInformes);
        btnExportar = findViewById(R.id.btnExportarInforme);
        filaFiltrosStock = findViewById(R.id.filaFiltrosStock);
        containerResumenInformes = findViewById(R.id.containerResumenInformes);

        informeAdapter = new InformeStockAdapter(producto -> {
            Intent intent = new Intent(InformesActivity.this, ProductoFormActivity.class);
            intent.putExtra(ProductoFormActivity.EXTRA_ID_PRODUCTO, producto.getIdProducto());
            startActivity(intent);
        });
        movimientoAdapter = new MovimientoGeneralAdapter();

        rvInformes.setLayoutManager(new LinearLayoutManager(this));
        rvInformes.setAdapter(informeAdapter);

        configurarSpinners();
        configurarBuscadorYTabs();

        btnExportar.setOnClickListener(v -> exportarInformeDialog());
    }

    private void configurarSpinners() {
        // Cargar categorías
        listaCategorias.clear();
        Categoria todas = new Categoria(0, getString(R.string.filter_categoria_todas), "", 1);
        listaCategorias.add(todas);
        listaCategorias.addAll(dbHelper.getCategoriasActivas());

        List<String> nombresCat = new ArrayList<>();
        for (Categoria c : listaCategorias) {
            nombresCat.add(c.getNombreCategoria());
        }

        ArrayAdapter<String> adapterCat = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, nombresCat);
        adapterCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapterCat);

        spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                idCategoriaSeleccionada = listaCategorias.get(position).getIdCategoria();
                actualizarVistaActual();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Cargar estados de stock
        String[] estados = {
                getString(R.string.filter_estado_todos),
                getString(R.string.filter_estado_ok),
                getString(R.string.filter_estado_bajo),
                getString(R.string.filter_estado_sin_stock)
        };
        ArrayAdapter<String> adapterEst = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, estados);
        adapterEst.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEstado.setAdapter(adapterEst);

        spinnerEstado.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1: estadoSeleccionado = "OK"; break;
                    case 2: estadoSeleccionado = "BAJO"; break;
                    case 3: estadoSeleccionado = "SIN_STOCK"; break;
                    default: estadoSeleccionado = "TODOS"; break;
                }
                actualizarVistaActual();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void configurarBuscadorYTabs() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                actualizarVistaActual();
            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabActual = tab.getPosition();
                boolean esInformeStock = tabActual == 0;
                if (esInformeStock) {
                    rvInformes.setAdapter(informeAdapter);
                } else {
                    rvInformes.setAdapter(movimientoAdapter);
                }
                btnExportar.setVisibility(esInformeStock ? View.VISIBLE : View.GONE);
                // El estado del stock y la categoría no filtran un movimiento:
                // si se dejan a la vista, elegir "Stock bajo" vaciaba el
                // historial porque ese texto viajaba como tipo de movimiento.
                filaFiltrosStock.setVisibility(esInformeStock ? View.VISIBLE : View.GONE);
                containerResumenInformes.setVisibility(esInformeStock ? View.VISIBLE : View.GONE);
                actualizarVistaActual();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void actualizarVistaActual() {
        if (tabActual == 0) {
            cargarInformeStock();
        } else {
            cargarHistorialMovimientos();
        }
    }

    private void cargarInformeStock() {
        String busqueda = etBuscar.getText() != null ? etBuscar.getText().toString() : "";
        List<Producto> productos = productoDao.getInformeProductos(
                idCategoriaSeleccionada, estadoSeleccionado, busqueda);

        informeAdapter.updateData(productos);

        // Calcular métricas
        int totalProductos = productos.size();
        int stockBajoCount = 0;
        int sinStockCount = 0;

        for (Producto p : productos) {
            if (p.getStockActual() <= 0) {
                sinStockCount++;
            } else if (p.getStockActual() <= p.getStockMinimo()) {
                stockBajoCount++;
            }
        }

        tvStatTotalProductos.setText(String.valueOf(totalProductos));
        tvStatStockBajo.setText(String.valueOf(stockBajoCount));
        tvStatSinStock.setText(String.valueOf(sinStockCount));

        boolean vacio = productos.isEmpty();
        tvVacioInformes.setVisibility(vacio ? View.VISIBLE : View.GONE);
        if (vacio) {
            tvVacioInformes.setText(R.string.empty_informes);
        }
    }

    private void cargarHistorialMovimientos() {
        // Se filtra por el texto del buscador (nombre o SKU) y nunca por el
        // estado del stock: ese combo era el que dejaba la lista vacía.
        String busqueda = etBuscar.getText() != null ? etBuscar.getText().toString() : "";
        List<Movimiento> movimientos = productoDao.getHistorialMovimientosGeneral(0, null, busqueda);
        movimientoAdapter.updateData(movimientos);

        boolean vacio = movimientos.isEmpty();
        tvVacioInformes.setVisibility(vacio ? View.VISIBLE : View.GONE);
        if (vacio) {
            tvVacioInformes.setText(R.string.empty_historial_general);
        }
    }

    private void exportarInformeDialog() {
        String busqueda = etBuscar.getText() != null ? etBuscar.getText().toString() : "";
        List<Producto> productos = productoDao.getInformeProductos(
                idCategoriaSeleccionada, estadoSeleccionado, busqueda);

        StringBuilder reporte = new StringBuilder();
        reporte.append("=== INFORME DE STOCK - BODEGA EXPRESS ===\n");
        reporte.append("Total productos listados: ").append(productos.size()).append("\n\n");

        for (Producto p : productos) {
            String estado = p.getStockActual() <= 0 ? "[SIN STOCK]" :
                    (p.getStockActual() <= p.getStockMinimo() ? "[STOCK BAJO]" : "[DISPONIBLE]");
            reporte.append(p.getNombre())
                   .append(" (SKU: ").append(p.getSkuCodigo()).append(") ")
                   .append(estado)
                   .append("\n  Stock Actual: ").append(p.getStockActual())
                   .append(" | Stock Mínimo: ").append(p.getStockMinimo())
                   .append("\n\n");
        }

        String textoFinal = reporte.toString();

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reporte_exportado_titulo)
                .setMessage(R.string.reporte_exportado_mensaje)
                .setPositiveButton("Compartir / Exportar", (dialog, which) -> {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, textoFinal);
                    sendIntent.setType("text/plain");
                    Intent shareIntent = Intent.createChooser(sendIntent, "Exportar Informe de Stock");
                    startActivity(shareIntent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarVistaActual();
    }
}
