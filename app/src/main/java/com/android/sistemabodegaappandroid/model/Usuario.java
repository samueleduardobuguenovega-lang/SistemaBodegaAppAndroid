package com.android.sistemabodegaappandroid.model;

public class Usuario {
    private int idUsuario;
    private String username;
    private String nombreCompleto;
    private String passwordHash;
    private int idRol;
    private String nombreRol;
    private int activo; // 1 = activo, 0 = inactivo

    public Usuario() {
    }

    public Usuario(int idUsuario, String username, String nombreCompleto, String passwordHash, int idRol, String nombreRol, int activo) {
        this.idUsuario = idUsuario;
        this.username = username;
        this.nombreCompleto = nombreCompleto;
        this.passwordHash = passwordHash;
        this.idRol = idRol;
        this.nombreRol = nombreRol;
        this.activo = activo;
    }

    public Usuario(String username, String nombreCompleto, String passwordHash, int idRol, int activo) {
        this.username = username;
        this.nombreCompleto = nombreCompleto;
        this.passwordHash = passwordHash;
        this.idRol = idRol;
        this.activo = activo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public int getActivo() {
        return activo;
    }

    public void setActivo(int activo) {
        this.activo = activo;
    }

    public boolean isActivo() {
        return activo == 1;
    }
}
