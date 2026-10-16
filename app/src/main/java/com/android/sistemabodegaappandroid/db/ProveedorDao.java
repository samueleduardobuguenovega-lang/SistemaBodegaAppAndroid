package com.android.sistemabodegaappandroid.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.android.sistemabodegaappandroid.model.Proveedor;

import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de PROVEEDOR.
 *
 * Separado de {@link DatabaseHelper} para mantener el helper acotado y poder
 * evolucionar el módulo de proveedores sin tocar roles/usuarios/categorías.
 */
public class ProveedorDao {

    private final DatabaseHelper helper;

    public ProveedorDao(DatabaseHelper helper) {
        this.helper = helper;
    }

    /** Proveedores habilitados, ordenados por razón social. */
    public List<Proveedor> getAllProveedores() {
        List<Proveedor> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_PROV_ID
                        + ", " + DatabaseHelper.COLUMN_PROV_RAZON_SOCIAL
                        + ", " + DatabaseHelper.COLUMN_PROV_RUT
                        + ", " + DatabaseHelper.COLUMN_PROV_EMAIL
                        + ", " + DatabaseHelper.COLUMN_PROV_TELEFONO
                        + ", " + DatabaseHelper.COLUMN_PROV_DIRECCION
                        + ", " + DatabaseHelper.COLUMN_PROV_ACTIVO
                        + " FROM " + DatabaseHelper.TABLE_PROVEEDOR
                        + " WHERE " + DatabaseHelper.COLUMN_PROV_ACTIVO + " = 1"
                        + " ORDER BY " + DatabaseHelper.COLUMN_PROV_RAZON_SOCIAL + " ASC",
                null);
        if (cursor.moveToFirst()) {
            do {
                list.add(mapearProveedor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /** Devuelve null si el id no existe. */
    public Proveedor getProveedorById(int idProveedor) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_PROV_ID
                        + ", " + DatabaseHelper.COLUMN_PROV_RAZON_SOCIAL
                        + ", " + DatabaseHelper.COLUMN_PROV_RUT
                        + ", " + DatabaseHelper.COLUMN_PROV_EMAIL
                        + ", " + DatabaseHelper.COLUMN_PROV_TELEFONO
                        + ", " + DatabaseHelper.COLUMN_PROV_DIRECCION
                        + ", " + DatabaseHelper.COLUMN_PROV_ACTIVO
                        + " FROM " + DatabaseHelper.TABLE_PROVEEDOR
                        + " WHERE " + DatabaseHelper.COLUMN_PROV_ID + " = ?",
                new String[]{String.valueOf(idProveedor)});
        Proveedor proveedor = null;
        if (cursor.moveToFirst()) {
            proveedor = mapearProveedor(cursor);
        }
        cursor.close();
        return proveedor;
    }

    /** Consulta de unicidad del RUT ignorando mayúsculas/minúsculas. */
    public boolean isRutExists(String rut) {
        if (rut == null) {
            return false;
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM " + DatabaseHelper.TABLE_PROVEEDOR
                        + " WHERE " + DatabaseHelper.COLUMN_PROV_RUT + " = ? COLLATE NOCASE",
                new String[]{rut.trim()});
        boolean existe = cursor.getCount() > 0;
        cursor.close();
        return existe;
    }

    /**
     * Inserta un proveedor; devuelve -1 si el RUT ya está registrado.
     * La unicidad la respalda el índice UNIQUE de la tabla.
     */
    public long insertProveedor(Proveedor p) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_PROV_RAZON_SOCIAL, p.getRazonSocial() != null ? p.getRazonSocial().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROV_RUT, p.getRut() != null ? p.getRut().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROV_EMAIL, p.getEmail() != null ? p.getEmail().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROV_TELEFONO, p.getTelefono() != null ? p.getTelefono().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROV_DIRECCION, p.getDireccion() != null ? p.getDireccion().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROV_ACTIVO, p.getActivo());
        try {
            return db.insert(DatabaseHelper.TABLE_PROVEEDOR, null, cv);
        } catch (SQLiteConstraintException e) {
            return -1;
        }
    }

    /** Actualiza los datos del proveedor; false si no había fila con ese id. */
    public boolean updateProveedor(Proveedor p) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_PROV_RAZON_SOCIAL, p.getRazonSocial() != null ? p.getRazonSocial().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROV_RUT, p.getRut() != null ? p.getRut().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROV_EMAIL, p.getEmail() != null ? p.getEmail().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROV_TELEFONO, p.getTelefono() != null ? p.getTelefono().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROV_DIRECCION, p.getDireccion() != null ? p.getDireccion().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROV_ACTIVO, p.getActivo());
        int filas = db.update(DatabaseHelper.TABLE_PROVEEDOR, cv,
                DatabaseHelper.COLUMN_PROV_ID + " = ?",
                new String[]{String.valueOf(p.getIdProveedor())});
        return filas > 0;
    }

    /** Habilita (1) o deshabilita (0) un proveedor sin alterar sus datos. */
    public boolean updateProveedorEstado(int idProveedor, int activo) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_PROV_ACTIVO, activo);
        int filas = db.update(DatabaseHelper.TABLE_PROVEEDOR, cv,
                DatabaseHelper.COLUMN_PROV_ID + " = ?",
                new String[]{String.valueOf(idProveedor)});
        return filas > 0;
    }

    /** Mapea la fila actual del cursor al modelo Proveedor, en el orden del SELECT. */
    private Proveedor mapearProveedor(Cursor cursor) {
        Proveedor p = new Proveedor();
        p.setIdProveedor(cursor.getInt(0));
        p.setRazonSocial(cursor.getString(1));
        p.setRut(cursor.getString(2));
        p.setEmail(cursor.getString(3));
        p.setTelefono(cursor.getString(4));
        p.setDireccion(cursor.getString(5));
        p.setActivo(cursor.getInt(6));
        return p;
    }
}
