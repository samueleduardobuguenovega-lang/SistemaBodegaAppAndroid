package com.android.sistemabodegaappandroid.model;

/**
 * Producto de la bodega.
 *
 * Se modela como POJO plano (sin anotaciones ni Room) para mantenerse
 * compatible con el SQLiteOpenHelper ya existente en el proyecto. Los campos
 * "nombreCategoria" y "razonSocialProveedor" no viven en la tabla PRODUCTO:
 * se resuelven con JOIN contra CATEGORIA y PROVEEDOR para mostrarlos en la
 * interfaz sin forzar consultas adicionales por fila.
 */
public class Producto {

    private int idProducto;
    private String skuCodigo;
    private String nombre;
    private String descripcion;
    private double precioCosto;
    private double precioVenta;
    private int stockActual;
    private int stockMinimo;
    private int idCategoria;
    private int idProveedor;
    private int activo;
    private String nombreCategoria;
    private String razonSocialProveedor;

    public Producto() {
        // Un producto recién construido se asume habilitado: el caller solo
        // lo deshabilita de forma explícita, así no se olvida el flag al darlo
        // de alta y el registro no queda invisible en los listados.
        this.activo = 1;
    }

    public Producto(int idProducto, String skuCodigo, String nombre, String descripcion,
                    double precioCosto, double precioVenta, int stockActual, int stockMinimo,
                    int idCategoria, int idProveedor, int activo,
                    String nombreCategoria, String razonSocialProveedor) {
        this.idProducto = idProducto;
        this.skuCodigo = skuCodigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioCosto = precioCosto;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.idCategoria = idCategoria;
        this.idProveedor = idProveedor;
        this.activo = activo;
        this.nombreCategoria = nombreCategoria;
        this.razonSocialProveedor = razonSocialProveedor;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getSkuCodigo() {
        return skuCodigo;
    }

    public void setSkuCodigo(String skuCodigo) {
        this.skuCodigo = skuCodigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecioCosto() {
        return precioCosto;
    }

    public void setPrecioCosto(double precioCosto) {
        this.precioCosto = precioCosto;
    }

    public double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public int getStockActual() {
        return stockActual;
    }

    public void setStockActual(int stockActual) {
        this.stockActual = stockActual;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public int getActivo() {
        return activo;
    }

    public void setActivo(int activo) {
        this.activo = activo;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public String getRazonSocialProveedor() {
        return razonSocialProveedor;
    }

    public void setRazonSocialProveedor(String razonSocialProveedor) {
        this.razonSocialProveedor = razonSocialProveedor;
    }

    @Override
    public String toString() {
        return "Producto{idProducto=" + idProducto
                + ", skuCodigo='" + skuCodigo + '\''
                + ", nombre='" + nombre + '\''
                + ", stockActual=" + stockActual
                + ", stockMinimo=" + stockMinimo
                + ", activo=" + activo
                + "}";
    }
}
