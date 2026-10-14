package com.android.sistemabodegaappandroid;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.model.Categoria;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class CategoriasActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView lvCategorias;
    private FloatingActionButton fabAgregarCategoria;
    private List<Categoria> listaCategorias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categorias);

        dbHelper = new DatabaseHelper(this);
        lvCategorias = findViewById(R.id.lvCategorias);
        fabAgregarCategoria = findViewById(R.id.fabAgregarCategoria);

        cargarListaCategorias();

        fabAgregarCategoria.setOnClickListener(v -> mostrarDialogoRegistro());
    }

    private void cargarListaCategorias() {
        listaCategorias = dbHelper.getAllCategorias();

        ArrayAdapter<Categoria> adapter = new ArrayAdapter<Categoria>(this, R.layout.item_categoria, listaCategorias) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_categoria, parent, false);
                }

                Categoria categoria = getItem(position);

                TextView tvNombre = convertView.findViewById(R.id.tvNombreCategoriaItem);
                TextView tvDesc = convertView.findViewById(R.id.tvDescCategoriaItem);

                if (categoria != null) {
                    tvNombre.setText(categoria.getNombreCategoria());
                    tvDesc.setText(categoria.getDescripcion());
                }

                return convertView;
            }
        };

        lvCategorias.setAdapter(adapter);
    }

    private void mostrarDialogoRegistro() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_categoria, null);

        TextInputEditText etNombre = view.findViewById(R.id.etNombreCategoria);
        TextInputEditText etDesc = view.findViewById(R.id.etDescripcionCategoria);

        builder.setView(view)
                .setTitle("Registrar Categoría")
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
                    String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(nombre)) {
                        Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long resultado = dbHelper.insertCategoria(nombre, desc);
                    if (resultado != -1) {
                        Toast.makeText(this, "Categoría registrada correctamente", Toast.LENGTH_SHORT).show();
                        cargarListaCategorias();
                    } else {
                        Toast.makeText(this, "Ya existe una categoría con ese nombre", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }
}