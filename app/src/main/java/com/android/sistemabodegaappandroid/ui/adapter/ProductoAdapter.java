package com.android.sistemabodegaappandroid.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.model.Producto;

import java.util.List;
import java.util.Locale;

/**
 * Adaptador del listado de productos.
 *
 * La señal de inventario vive aquí: cuando el stock llegó al mínimo, la cifra
 * se pinta en naranja y aparece la etiqueta "Stock bajo", de modo que quien
 * revisa el listado ve el faltante sin entrar a cada producto.
 */
public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder> {

    /** Abre el formulario de edición cuando el usuario toca la tarjeta. */
    public interface OnProductoClickListener {
        void onProductoClick(Producto producto);
    }

    private List<Producto> productos;
    private final OnProductoClickListener clickListener;

    public ProductoAdapter(List<Producto> productos, OnProductoClickListener clickListener) {
        this.productos = productos;
        this.clickListener = clickListener;
    }

    /** Reemplaza la lista completa; el listado no usa diff porque es de pocas filas. */
    public void updateData(List<Producto> nuevos) {
        this.productos = nuevos;
        notifyDataSetChanged();
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
        Producto producto = productos.get(position);

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

        holder.tvStockProducto.setText(holder.itemView.getContext().getString(
                R.string.label_stock, producto.getStockActual()));

        boolean stockBajo = producto.getStockActual() <= producto.getStockMinimo();
        holder.tvStockProducto.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), stockBajo ? R.color.accent : R.color.text_primary));
        holder.tvStockBajo.setVisibility(stockBajo ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onProductoClick(producto);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productos != null ? productos.size() : 0;
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
