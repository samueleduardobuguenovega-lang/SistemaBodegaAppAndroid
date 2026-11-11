package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

import android.database.sqlite.SQLiteDatabase;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.model.Rol;
import com.android.sistemabodegaappandroid.util.Permisos;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Migración v8 -> v9: los roles que ya existían tienen que recibir el permiso
 * INFORMES, porque el módulo se sumó cuando ROL.permisos ya estaba creado.
 *
 * La prueba simula una instalación vieja: guarda los permisos como los dejaba
 * la v8 (sin INFORMES), marca la base como versión 8 y vuelve a abrir el
 * helper, que es cuando corre el paso de migración.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MigracionV9Test {

    @Test
    public void unRolDeInventarioRecibeInformesAlMigrar() {
        DatabaseHelper helper = new DatabaseHelper(RuntimeEnvironment.getApplication());

        int idAdministrador = idDeRol(helper, "Administrador");
        int idVendedor = idDeRol(helper, "Vendedor");

        // Estado tal como lo dejaba la v8: cinco tokens, sin INFORMES.
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("UPDATE " + DatabaseHelper.TABLE_ROL + " SET "
                + DatabaseHelper.COLUMN_ROL_PERMISOS + " = ? WHERE "
                + DatabaseHelper.COLUMN_ROL_ID + " = ?",
                new Object[]{"USUARIOS,PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS", idAdministrador});
        db.execSQL("UPDATE " + DatabaseHelper.TABLE_ROL + " SET "
                + DatabaseHelper.COLUMN_ROL_PERMISOS + " = ? WHERE "
                + DatabaseHelper.COLUMN_ROL_ID + " = ?",
                new Object[]{"PRODUCTOS,CATEGORIAS", idVendedor});
        db.setVersion(8);
        assertEquals(8, db.getVersion());
        db.close();

        // Al reabrir corre onUpgrade(8 -> 9).
        DatabaseHelper migrado = new DatabaseHelper(RuntimeEnvironment.getApplication());
        String permisosAdministrador = migrado.getPermisosDeRol(idAdministrador);
        String permisosVendedor = migrado.getPermisosDeRol(idVendedor);

        assertTrue("el rol que opera inventario debe recibir INFORMES al migrar",
                Permisos.tiene(permisosAdministrador, Permisos.INFORMES));
        assertTrue("los permisos que ya tenía no se pierden",
                Permisos.tiene(permisosAdministrador, Permisos.USUARIOS));
        assertTrue(Permisos.tiene(permisosAdministrador, Permisos.MOVIMIENTOS));
        assertTrue("un rol de ventas no gana informes por la migración",
                !Permisos.tiene(permisosVendedor, Permisos.INFORMES));
        assertTrue(Permisos.tiene(permisosVendedor, Permisos.PRODUCTOS));
    }

    @Test
    public void laMigracionNoDuplicaElTokenSiYaEstaba() {
        DatabaseHelper helper = new DatabaseHelper(RuntimeEnvironment.getApplication());
        int idOperador = idDeRol(helper, "Operador");

        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("UPDATE " + DatabaseHelper.TABLE_ROL + " SET "
                + DatabaseHelper.COLUMN_ROL_PERMISOS + " = ? WHERE "
                + DatabaseHelper.COLUMN_ROL_ID + " = ?",
                new Object[]{"PRODUCTOS,MOVIMIENTOS,INFORMES", idOperador});
        db.setVersion(8);
        db.close();

        DatabaseHelper migrado = new DatabaseHelper(RuntimeEnvironment.getApplication());
        String permisos = migrado.getPermisosDeRol(idOperador);

        int apariciones = permisos.split("INFORMES", -1).length - 1;
        assertTrue("INFORMES debe quedar una sola vez, quedo: " + permisos, apariciones == 1);
    }

    private int idDeRol(DatabaseHelper helper, String nombre) {
        for (Rol rol : helper.getAllRoles()) {
            if (nombre.equalsIgnoreCase(rol.getNombreRol())) {
                return rol.getIdRol();
            }
        }
        fail("no existe el rol " + nombre + " en la semilla");
        return -1;
    }
}
