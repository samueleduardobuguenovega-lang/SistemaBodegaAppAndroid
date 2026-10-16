package com.android.sistemabodegaappandroid;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.model.Categoria;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Collections;
import java.util.List;

public class CategoriasActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView lvCategorias;
    private TextView tvVacioCategorias;
    private FloatingActionButton fabAgregarCategoria;
    private ArrayAdapter<Categoria> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categorias);

        // El notch y las barras del sistema no deben tapar el contenido.
        EdgeToEdgeInsets.apply(this, findViewById(R.id.toolbarCategorias).getRootView());

        dbHelper = new DatabaseHelper(this);
        lvCategorias = findViewById(R.id.lvCategorias);
        tvVacioCategorias = findViewById(R.id.tvVacioCategorias);
        fabAgregarCategoria = findViewById(R.id.fabAgregarCategoria);
        MaterialToolbar toolbar = findViewById(R.id.toolbarCategorias);

        // Navegación: la flecha del toolbar vuelve al panel principal.
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        adapter = new ArrayAdapter<Categoria>(this, R.layout.item_categoria,
                new java.util.ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_categoria, parent, false);
                }

                Categoria categoria = getItem(position);

                TextView tvNombre = convertView.findViewById(R.id.tvNombreCategoriaItem);
                TextView tvDesc = convertView.findViewById(R.id.tvDescCategoriaItem);
                ImageView ivActiva = convertView.findViewById(R.id.ivActiva);

                if (categoria != null) {
                    tvNombre.setText(categoria.getNombreCategoria());
                    // CU-10: solo llegan categorías activas; el check lo confirma en pantalla.
                    ivActiva.setVisibility(categoria.getActivo() == 1 ? View.VISIBLE : View.GONE);
                    tvDesc.setText(categoria.getDescripcion());
                    tvDesc.setVisibility(TextUtils.isEmpty(categoria.getDescripcion())
                            ? View.GONE : View.VISIBLE);
                }

                return convertView;
            }
        };
        lvCategorias.setAdapter(adapter);

        // CU-10: el DAO expone las categorías activas como LiveData y la
        // interfaz se suscribe. Cada actualización llega al hilo principal.
        dbHelper.observarCategoriasActivas().observe(this, categorias -> {
            if (categorias == null) {
                return;
            }
            adapter.clear();
            adapter.addAll(categorias);
            adapter.notifyDataSetChanged();
            tvVacioCategorias.setVisibility(categorias.isEmpty() ? View.VISIBLE : View.GONE);
            lvCategorias.setVisibility(categorias.isEmpty() ? View.GONE : View.VISIBLE);
        });

        fabAgregarCategoria.setOnClickListener(v -> mostrarDialogoRegistro());
    }

    /** Vuelve a ejecutar el DAO para reflejar los cambios tras crear una categoría. */
    private void cargarListaCategorias() {
        dbHelper.refrescarCategoriasActivas();
    }

    private void mostrarDialogoRegistro() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_categoria, null);

        TextInputEditText etNombre = view.findViewById(R.id.etNombreCategoria);
        TextInputEditText etDesc = view.findViewById(R.id.etDescripcionCategoria);

        builder.setView(view)
                .setTitle(R.string.dialog_title_registrar_categoria)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
                    String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(nombre)) {
                        Toast.makeText(this, R.string.msg_categoria_nombre_vacio, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long resultado = dbHelper.insertCategoria(nombre, desc);
                    if (resultado != -1) {
                        Toast.makeText(this, R.string.msg_categoria_registrada, Toast.LENGTH_SHORT).show();
                        cargarListaCategorias();
                    } else {
                        Toast.makeText(this, R.string.msg_categoria_duplicada, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }
}