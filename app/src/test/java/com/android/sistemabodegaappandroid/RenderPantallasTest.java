package com.android.sistemabodegaappandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Renderiza los LAYOUTS REALES de la app a PNG para la presentación.
 *
 * No levanta las Activities a propósito: SessionManager usa preferencias
 * cifradas con keystore y en la JVM no están disponibles — y degradarlas a
 * texto plano sería romper una decisión de seguridad documentada. Se infla el
 * layout con el tema real, se cargan datos en las vistas y se dibuja a
 * 390x844 dp @2x: es el layout verdadero, con sus colores, tipografías y
 * componentes, no un mockup.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 33, qualifiers = "w390dp-h844dp-xhdpi")
public class RenderPantallasTest {

    private static final File SALIDA = new File("build/screens");

    @Before
    public void preparar() {
        SALIDA.mkdirs();
    }

    private View inflar(int layout) {
        Context tema = new android.view.ContextThemeWrapper(
                RuntimeEnvironment.getApplication(), R.style.Theme_SistemaBodegaAppAndroid);
        return LayoutInflater.from(tema).inflate(layout, null, false);
    }

    private static void texto(View raiz, int id, String valor) {
        TextView tv = raiz.findViewById(id);
        if (tv != null) {
            tv.setText(valor);
        }
    }

    private void guardar(View root, String nombre) {
        int w = 780, h = 1688;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bmp));
        File destino = new File(SALIDA, nombre + ".png");
        try (FileOutputStream out = new FileOutputStream(destino)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            throw new AssertionError("no se pudo escribir " + nombre, e);
        }
        System.out.println("RENDER " + nombre + " " + destino.length() + " bytes");
    }

    @Test
    public void login() {
        View v = inflar(R.layout.activity_login);
        // Sin texto en los campos: el hint flotante y el valor se superponen al
        // dibujar en Robolectric y ensucian la captura.
        guardar(v, "01-login");
    }

    @Test
    public void panel() {
        View v = inflar(R.layout.activity_main);
        texto(v, R.id.tvUserFullName, "Samuel Bugueño");
        texto(v, R.id.tvUserRole, "Administrador");
        guardar(v, "02-panel");
    }

    @Test
    public void catalogo() {
        View v = inflar(R.layout.activity_productos);
        texto(v, R.id.tvHeroConteo, "12");
        // El ícono de fin del buscador se dibuja fuera de su caja en Robolectric
        // y aparece como un círculo oscuro sobre el placeholder.
        com.google.android.material.textfield.TextInputLayout tilBuscar = v.findViewById(R.id.tilBuscarProducto);
        if (tilBuscar != null) {
            tilBuscar.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_NONE);
            tilBuscar.setStartIconDrawable(null);
        }
        RecyclerView rv = v.findViewById(R.id.rvProductos);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(v.getContext()));
            rv.setAdapter(new AdapterDemo());
        }
        guardar(v, "03-productos");
    }

    @Test
    public void formulario() {
        View v = inflar(R.layout.activity_producto_form);
        // Igual que el buscador: el ícono de fin del campo de código de barras se
        // dibuja fuera de su caja al renderizar y tapa el placeholder.
        int[] conIcono = {R.id.tilCodigoBarras};
        for (int id : conIcono) {
            com.google.android.material.textfield.TextInputLayout til = v.findViewById(id);
            if (til != null) {
                til.setEndIconMode(com.google.android.material.textfield.TextInputLayout.END_ICON_NONE);
            }
        }
        guardar(v, "04-formulario");
    }


    @Test
    public void categorias() {
        View v = inflar(R.layout.activity_categorias);
        android.widget.ListView lv = v.findViewById(R.id.lvCategorias);
        if (lv != null) {
            lv.setAdapter(new android.widget.ArrayAdapter<String>(v.getContext(),
                    R.layout.item_categoria, R.id.tvNombreCategoriaItem,
                    new String[]{"Bebidas", "Lácteos", "Abarrotes", "Snacks", "Limpieza"}) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View fila = super.getView(position, convertView, parent);
                    TextView desc = fila.findViewById(R.id.tvDescCategoriaItem);
                    if (desc != null) {
                        desc.setText("Categoría activa");
                    }
                    return fila;
                }
            });
        }
        guardar(v, "05-categorias");
    }

    @Test
    public void informes() {
        View v = inflar(R.layout.activity_informes);
        String[] cats = {"Todas las categorías", "Bebidas", "Lácteos"};
        android.widget.Spinner spCat = v.findViewById(R.id.spinnerCategoriaInforme);
        if (spCat != null) {
            spCat.setAdapter(new android.widget.ArrayAdapter<>(v.getContext(),
                    android.R.layout.simple_spinner_item, cats));
        }
        android.widget.Spinner spEst = v.findViewById(R.id.spinnerEstadoInforme);
        if (spEst != null) {
            spEst.setAdapter(new android.widget.ArrayAdapter<>(v.getContext(),
                    android.R.layout.simple_spinner_item,
                    new String[]{"Todos los estados", "Disponible", "Stock bajo"}));
        }
        com.google.android.material.textfield.TextInputLayout tilBuscarInf = v.findViewById(R.id.tilBuscarInforme);
        if (tilBuscarInf != null) {
            tilBuscarInf.setStartIconDrawable(null);
        }
        texto(v, R.id.tvStatTotalProductos, "12");
        texto(v, R.id.tvStatStockBajo, "2");
        texto(v, R.id.tvStatSinStock, "1");
        guardar(v, "06-informes");
    }

    /** Adapter mínimo que infla el item real del catálogo con datos de ejemplo. */
    private static class AdapterDemo extends RecyclerView.Adapter<AdapterDemo.VH> {

        private final String[][] filas = {
                {"Agua mineral 1.5 L", "BEB-001", "Bebidas", "$1.190", "48 UN"},
                {"Bebida cola 1.5 L", "BEB-002", "Bebidas", "$1.990", "36 UN"},
                {"Leche entera 1 L", "LAC-001", "Lácteos", "$1.250", "8 UN"},
                {"Queso gauda 250 g", "LAC-003", "Lácteos", "$2.790", "0 UN"},
        };

        static class VH extends RecyclerView.ViewHolder {
            VH(View v) {
                super(v);
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_producto, parent, false));
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            String[] f = filas[position];
            View v = holder.itemView;
            texto(v, R.id.tvNombreProducto, f[0]);
            texto(v, R.id.tvSkuProducto, f[1]);
            texto(v, R.id.tvCategoriaProducto, f[2]);
            texto(v, R.id.tvPrecioProducto, f[3]);
            texto(v, R.id.tvStockProducto, f[4]);
            texto(v, R.id.tvProveedorProducto, "Distribuidora Andina");
        }

        @Override
        public int getItemCount() {
            return filas.length;
        }
    }
}
