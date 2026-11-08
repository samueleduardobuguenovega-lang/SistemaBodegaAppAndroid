package com.android.sistemabodegaappandroid.ui.adapter;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.model.Producto;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador del listado de productos (RF-04).
 *
 * Es un ListAdapter con DiffUtil: cada refresco de LiveData compara la lista
 * anterior con la nueva y solo anima lo que cambió, en vez de repintar todo la
 * fila apagada de golpe. Eso es lo que hace que la búsqueda "en vivo" se sienta
 * fluida: escribir letra a letra refresca el catálogo sin parpadeos.
 *
 * La señal de inventario vive aquí: la píldora de stock se colorea como un
 * semáforo (verde disponible, ámbar al llegar al mínimo, rojo sin stock), de
 * modo que quien revisa el listado ve el faltante sin abrir cada producto.
 */
public class ProductoAdapter extends ListAdapter<Producto, ProductoAdapter.ProductoViewHolder> {

    /** Abre el formulario de edición cuando el usuario toca la tarjeta. */
    public interface OnProductoClickListener {
        void onProductoClick(Producto producto);
    }

    private static final DiffUtil.ItemCallback<Producto> DIFF = new DiffUtil.ItemCallback<Producto>() {
        @Override
        public boolean areItemsTheSame(@NonNull Producto a, @NonNull Producto b) {
            return a.getIdProducto() == b.getIdProducto();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Producto a, @NonNull Producto b) {
            return a.getStockActual() == b.getStockActual()
                    && Double.compare(a.getPrecioVenta(), b.getPrecioVenta()) == 0
                    && Double.compare(a.getPrecioCosto(), b.getPrecioCosto()) == 0
                    && a.getStockMinimo() == b.getStockMinimo()
                    && a.getActivo() == b.getActivo()
                    && igual(a.getNombre(), b.getNombre())
                    && igual(a.getSkuCodigo(), b.getSkuCodigo())
                    && igual(a.getNombreCategoria(), b.getNombreCategoria())
                    && igual(a.getRazonSocialProveedor(), b.getRazonSocialProveedor())
                    && igual(a.getUnidadMedida(), b.getUnidadMedida());
        }

        private boolean igual(Object uno, Object otro) {
            return uno == null ? otro == null : uno.equals(otro);
        }
    };

    private final OnProductoClickListener clickListener;

    public ProductoAdapter(OnProductoClickListener clickListener) {
        super(DIFF);
        this.clickListener = clickListener;
    }

    /**
     * Entrada por submitList (heredado de ListAdapter): el Diff calcula las
     * diferencias en un hilo aparte y anima solo los ítems afectados.
     */
    public void updateData(@Nullable List<Producto> nuevos) {
        submitList(nuevos != null ? nuevos : Collections.emptyList());
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_producto, parent, false);
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        Producto producto = getItem(position);

        holder.tvNombreProducto.setText(producto.getNombre());
        holder.tvSkuProducto.setText(holder.itemView.getContext().getString(
                R.string.label_sku, producto.getSkuCodigo()));

        holder.tvCategoriaProducto.setText(holder.itemView.getContext().getString(
                R.string.label_categoria,
                vacioATexto(producto.getNombreCategoria(),
                        holder.itemView.getContext().getString(R.string.sin_categoria))));
        holder.tvProveedorProducto.setText(holder.itemView.getContext().getString(
                R.string.label_proveedor,
                vacioATexto(producto.getRazonSocialProveedor(),
                        holder.itemView.getContext().getString(R.string.sin_proveedor))));

        // Moneda formateada según la locale del dispositivo, no con coma fija.
        String precio = String.format(Locale.getDefault(), "$%,.2f", producto.getPrecioVenta());
        holder.tvPrecioProducto.setText(holder.itemView.getContext().getString(
                R.string.label_precio_venta, precio));

        // Semáforo de stock: la píldora muestra la cifra con su unidad de
        // medida; el color depende del estado respecto al stock mínimo.
        boolean sinStock = producto.getStockActual() <= 0;
        boolean stockBajo = producto.getStockActual() <= producto.getStockMinimo();
        String textoPildora = sinStock
                ? holder.itemView.getContext().getString(R.string.stock_agotado)
                : textoStock(producto, holder);
        holder.tvStockProducto.setText(textoPildora);
        int colorPildora;
        TextView etiqueta = holder.tvStockBajo;
        if (sinStock) {
            colorPildora = R.color.stock_critico;
        } else if (stockBajo) {
            colorPildora = R.color.accent;
        } else {
            colorPildora = R.color.stock_ok;
        }
        holder.tvStockProducto.setBackgroundTintList(ResourcesCompat.getColorStateList(
                holder.itemView.getResources(), colorPildora, null));
        etiqueta.setText(holder.itemView.getContext().getString(
                sinStock ? R.string.stock_agotado : R.string.stock_bajo));
        etiqueta.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), stockBajo ? R.color.accent : R.color.text_primary));
        // La etiqueta solo complementa: con la píldora roja "Sin stock" el
        // texto ya está en la píldora, así que no se repite.
        etiqueta.setVisibility(!sinStock && stockBajo ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onProductoClick(producto);
            }
        });

        // Feedback táctil "expresivo": la tarjeta se encoge levemente al
        // presionar y vuelve con rebote al soltar.
        holder.itemView.setOnTouchListener((v, evento) -> {
            int accion = evento.getActionMasked();
            if (accion == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
            } else if (accion == MotionEvent.ACTION_UP || accion == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(180)
                        .setInterpolator(new OvershootInterpolator(1.6f)).start();
            }
            return false;
        });
    }

    /** "12 unidad" con la unidad registrada; si no la hay, el formato clásico. */
    private String textoStock(Producto producto, ProductoViewHolder holder) {
        if (producto.getUnidadMedida() != null && !producto.getUnidadMedida().trim().isEmpty()) {
            return holder.itemView.getContext().getString(R.string.label_stock_unidad,
                    producto.getStockActual(), producto.getUnidadMedida());
        }
        return holder.itemView.getContext().getString(R.string.label_stock, producto.getStockActual());
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    /** Un JOIN con LEFT JOIN puede devolver null: se traduce a un texto visible. */
    private String vacioATexto(String valor, String alternativa) {
        return valor == null || valor.trim().isEmpty() ? alternativa : valor;
    }

    static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreProducto;
        TextView tvSkuProducto;
        TextView tvCategoriaProducto;
        TextView tvProveedorProducto;
        TextView tvPrecioProducto;
        TextView tvStockProducto;
        TextView tvStockBajo;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreProducto = itemView.findViewById(R.id.tvNombreProducto);
            tvSkuProducto = itemView.findViewById(R.id.tvSkuProducto);
            tvCategoriaProducto = itemView.findViewById(R.id.tvCategoriaProducto);
            tvProveedorProducto = itemView.findViewById(R.id.tvProveedorProducto);
            tvPrecioProducto = itemView.findViewById(R.id.tvPrecioProducto);
            tvStockProducto = itemView.findViewById(R.id.tvStockProducto);
            tvStockBajo = itemView.findViewById(R.id.tvStockBajo);
        }
    }
}
