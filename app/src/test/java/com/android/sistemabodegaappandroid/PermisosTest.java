package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.android.sistemabodegaappandroid.util.Permisos;

import org.junit.Test;

/**
 * Semántica de ROL.permisos: la lista se evalúa por token completo.
 *
 * Es el corazón de RF-27. Antes de esto la columna se guardaba y se volvía a
 * mostrar, pero ninguna clase la consultaba para autorizar.
 */
public class PermisosTest {

    private static final String ADMIN = "USUARIOS,PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS";
    private static final String OPERADOR = "PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS";
    private static final String VENDEDOR = "PRODUCTOS,CATEGORIAS";

    @Test
    public void elAdministradorHabilitaLosCincoModulos() {
        assertTrue(Permisos.tiene(ADMIN, Permisos.USUARIOS));
        assertTrue(Permisos.tiene(ADMIN, Permisos.PRODUCTOS));
        assertTrue(Permisos.tiene(ADMIN, Permisos.CATEGORIAS));
        assertTrue(Permisos.tiene(ADMIN, Permisos.PROVEEDORES));
        assertTrue(Permisos.tiene(ADMIN, Permisos.MOVIMIENTOS));
    }

    @Test
    public void unRolSinUsuariosNoAdministraUsuariosNiRoles() {
        assertFalse("el operador no define usuarios ni permisos",
                Permisos.tiene(OPERADOR, Permisos.USUARIOS));
        assertFalse(Permisos.tiene(VENDEDOR, Permisos.USUARIOS));
    }

    @Test
    public void unRolSinProveedoresNoEntraAlModuloDeProveedores() {
        assertFalse("el vendedor no mantiene proveedores",
                Permisos.tiene(VENDEDOR, Permisos.PROVEEDORES));
        assertFalse(Permisos.tiene(VENDEDOR, Permisos.MOVIMIENTOS));
        assertTrue(Permisos.tiene(VENDEDOR, Permisos.CATEGORIAS));
    }

    @Test
    public void laComparacionNoEsPorSubcadena() {
        assertFalse(Permisos.tiene("PRODUCTOS", "PRODUCTO"));
        assertFalse(Permisos.tiene("USUARIOS", "USUARIO"));
        assertFalse(Permisos.tiene("MOVIMIENTOS", "MOVIMIENTO"));
    }

    @Test
    public void toleraEspaciosYMinusculas() {
        assertTrue(Permisos.tiene(" productos , categorias ", Permisos.CATEGORIAS));
        assertTrue(Permisos.tiene("productos", Permisos.PRODUCTOS));
    }

    @Test
    public void unaListaVaciaONulaNoHabilitaNada() {
        assertFalse(Permisos.tiene("", Permisos.PRODUCTOS));
        assertFalse(Permisos.tiene("   ", Permisos.PRODUCTOS));
        assertFalse(Permisos.tiene(null, Permisos.PRODUCTOS));
        assertFalse(Permisos.tiene(",", Permisos.PRODUCTOS));
        assertFalse(Permisos.tiene(", ,", Permisos.PRODUCTOS));
    }

    @Test
    public void unTokenVacioNoCoincideConLosSeparadores() {
        assertFalse(Permisos.tiene(", ,", ""));
        assertFalse(Permisos.tiene(ADMIN, ""));
        assertFalse(Permisos.tiene(ADMIN, null));
    }
}
