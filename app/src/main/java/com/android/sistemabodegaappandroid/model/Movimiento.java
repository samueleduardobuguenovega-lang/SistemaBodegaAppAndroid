package com.android.sistemabodegaappandroid.model;

/**
 * Registro de entrada o salida de stock de un producto.
 *
 * Se guarda como POJO plano para leerlo con Cursor directamente, sin Room.
 * El campo "tipo" persiste como texto ("ENTRADA"/"SALIDA") para que el dato
 * sea legible en la base y sobreviva cambios de enumeración en el futuro.
 */
public class Movimiento {

    private int idMovimiento;
    private int idProducto;
    private String tipo;
    private int cantidad;
    private String usuario;
    private String fecha;
    // Proveedor que entregó la mercancía en una ENTRADA (v4). 0 = sin proveedor;
    // las salidas nunca lo usan, por eso no lleva clave foránea.
    private int idProveedor;
    // Razón social resuelta del proveedor para mostrarla en el historial.
    // Vacío cuando el movimiento no tiene proveedor (salidas y entradas sin él).
    private String razonSocialProveedor;

    public Movimiento() {
    }

    public Movimiento(int idMovimiento, int idProducto, String tipo, int cantidad,
                      String usuario, String fecha, int idProveedor) {
        this(idMovimiento, idProducto, tipo, cantidad, usuario, fecha, idProveedor, "");
    }

    public Movimiento(int idMovimiento, int idProducto, String tipo, int cantidad,
                      String usuario, String fecha, int idProveedor,
                      String razonSocialProveedor) {
        this.idMovimiento = idMovimiento;
        this.idProducto = idProducto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.usuario = usuario;
        this.fecha = fecha;
        this.idProveedor = idProveedor;
        this.razonSocialProveedor = razonSocialProveedor != null ? razonSocialProveedor : "";
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getRazonSocialProveedor() {
        return razonSocialProveedor != null ? razonSocialProveedor : "";
    }

    public void setRazonSocialProveedor(String razonSocialProveedor) {
        this.razonSocialProveedor = razonSocialProveedor != null ? razonSocialProveedor : "";
    }

    @Override
    public String toString() {
        return "Movimiento{idMovimiento=" + idMovimiento
                + ", idProducto=" + idProducto
                + ", tipo='" + tipo + '\''
                + ", cantidad=" + cantidad
                + ", fecha='" + fecha + '\''
                + ", idProveedor=" + idProveedor
                + ", razonSocialProveedor='" + getRazonSocialProveedor() + '\''
                + "}";
    }
}
