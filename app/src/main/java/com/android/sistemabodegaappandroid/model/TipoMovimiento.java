package com.android.sistemabodegaappandroid.model;

/**
 * Tipos de movimiento de stock admitidos.
 *
 * El valor de cada constante es lo que se persiste en la columna
 * MOVIMIENTO.tipo, de modo que la base quede auto-descriptiva y una lectura
 * directa del SQL sea interpretable sin traducción.
 */
public enum TipoMovimiento {

    ENTRADA("ENTRADA"),
    SALIDA("SALIDA");

    private final String valor;

    TipoMovimiento(String valor) {
        this.valor = valor;
    }

    /** Texto que se guarda en la columna MOVIMIENTO.tipo. */
    public String getValor() {
        return valor;
    }
}
