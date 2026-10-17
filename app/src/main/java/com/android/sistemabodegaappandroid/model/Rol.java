package com.android.sistemabodegaappandroid.model;

import androidx.annotation.NonNull;

public class Rol {
    private int idRol;
    private String nombreRol;
    private String descripcion;
    private String permisos;

    public Rol() {
    }

    public Rol(int idRol, String nombreRol, String descripcion) {
        this.idRol = idRol;
        this.nombreRol = nombreRol;
        this.descripcion = descripcion;
    }

    public Rol(int idRol, String nombreRol, String descripcion, String permisos) {
        this.idRol = idRol;
        this.nombreRol = nombreRol;
        this.descripcion = descripcion;
        this.permisos = permisos;
    }

    public Rol(String nombreRol, String descripcion) {
        this.nombreRol = nombreRol;
        this.descripcion = descripcion;
    }

    public Rol(String nombreRol, String descripcion, String permisos) {
        this.nombreRol = nombreRol;
        this.descripcion = descripcion;
        this.permisos = permisos;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPermisos() {
        return permisos;
    }

    public void setPermisos(String permisos) {
        this.permisos = permisos;
    }

    @NonNull
    @Override
    public String toString() {
        return nombreRol;
    }
}
