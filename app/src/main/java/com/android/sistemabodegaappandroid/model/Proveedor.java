package com.android.sistemabodegaappandroid.model;

/**
 * Proveedor asociado a los productos de la bodega.
 *
 * POJO plano, mismo patrón que Categoria/Rol/Usuario: sin anotaciones ni Room,
 * para seguir usando SQLiteOpenHelper directamente.
 */
public class Proveedor {

    private int idProveedor;
    private String razonSocial;
    private String rut;
    private String email;
    private String telefono;
    private String direccion;
    private int activo;

    public Proveedor() {
        // Mismo criterio que Producto: un proveedor nuevo nace habilitado.
        this.activo = 1;
    }

    public Proveedor(int idProveedor, String razonSocial, String rut, String email,
                     String telefono, String direccion, int activo) {
        this.idProveedor = idProveedor;
        this.razonSocial = razonSocial;
        this.rut = rut;
        this.email = email;
        this.telefono = telefono;
        this.direccion = direccion;
        this.activo = activo;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getRut() {
        return rut;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public int getActivo() {
        return activo;
    }

    public void setActivo(int activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "Proveedor{idProveedor=" + idProveedor
                + ", razonSocial='" + razonSocial + '\''
                + ", rut='" + rut + '\''
                + ", activo=" + activo
                + "}";
    }
}
