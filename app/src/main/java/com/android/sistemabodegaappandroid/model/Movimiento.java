package com.android.sistemabodegaappandroid.model;

/**
 * Registro de entrada o salida de stock de un producto.
 */
public class Movimiento {

    private int idMovimiento;
    private int idProducto;
    private String tipo;
    private int cantidad;
    private String usuario; // Idealmente el nombre de usuario (username)
    private String fecha;
    private int idProveedor;
    private String razonSocialProveedor;

    // Nuevos campos CU-04 / CU-05
    private String documento;
    private String motivo;

    public Movimiento() {
    }

    public Movimiento(int idMovimiento, int idProducto, String tipo, int cantidad,
                      String usuario, String fecha, int idProveedor) {
        this(idMovimiento, idProducto, tipo, cantidad, usuario, fecha, idProveedor, "", null, null);
    }

    public Movimiento(int idMovimiento, int idProducto, String tipo, int cantidad,
                      String usuario, String fecha, int idProveedor,
                      String razonSocialProveedor, String documento, String motivo) {
        this.idMovimiento = idMovimiento;
        this.idProducto = idProducto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.usuario = usuario;
        this.fecha = fecha;
        this.idProveedor = idProveedor;
        this.razonSocialProveedor = razonSocialProveedor != null ? razonSocialProveedor : "";
        this.documento = documento != null ? documento : "";
        this.motivo = motivo != null ? motivo : "";
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

    public String getDocumento() {
        return documento != null ? documento : "";
    }

    public void setDocumento(String documento) {
        this.documento = documento != null ? documento : "";
    }

    public String getMotivo() {
        return motivo != null ? motivo : "";
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo != null ? motivo : "";
    }

    @Override
    public String toString() {
        return "Movimiento{idMovimiento=" + idMovimiento
                + ", idProducto=" + idProducto
                + ", tipo='" + tipo + '\''
                + ", cantidad=" + cantidad
                + ", fecha='" + fecha + '\''
                + ", idProveedor=" + idProveedor
                + ", documento='" + getDocumento() + '\''
                + ", motivo='" + getMotivo() + '\''
                + ", razonSocialProveedor='" + getRazonSocialProveedor() + '\''
                + "}";
    }
}