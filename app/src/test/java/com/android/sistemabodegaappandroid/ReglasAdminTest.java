package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.android.sistemabodegaappandroid.util.ReglasAdmin;

import org.junit.Test;

/**
 * Las reglas que impiden dejar el sistema sin administrador.
 *
 * Son puras (sin Android ni base), así que se prueban directo: si alguna se
 * afloja, la app puede quedar bloqueada para siempre y no hay forma de entrar
 * a arreglarla desde la interfaz.
 */
public class ReglasAdminTest {

    // --- Cambio de rol ---

    @Test
    public void unAdminNoPuedeQuitarseElAdminASiMismo() {
        assertFalse(ReglasAdmin.puedeCambiarRol(1, 1, true, false));
        assertFalse(ReglasAdmin.puedeCambiarRol(1, 1, false, false));
    }

    @Test
    public void unAdminSiPuedeCambiarSuRolAMenosQueSigaSiendoAdmin() {
        assertTrue(ReglasAdmin.puedeCambiarRol(1, 1, true, true));
    }

    @Test
    public void elUltimoAdminNoPuedeSerDegradadoPorOtroUsuario() {
        // objetivo != sesión, pero es el único admin activo
        assertFalse(ReglasAdmin.puedeCambiarRol(7, 1, true, false));
    }

    @Test
    public void sePuedeDegradarAOtroAdminCuandoQuedaAlMenosUnAdmin() {
        assertTrue(ReglasAdmin.puedeCambiarRol(7, 1, false, false));
    }

    @Test
    public void sePuedeQuitarElAdminAOtroUsuario() {
        // El caso pedido: admin A le quita admin a admin B
        assertTrue(ReglasAdmin.puedeCambiarRol(9, 1, false, false));
    }

    // --- Eliminación de cuentas ---

    @Test
    public void nadiePuedeEliminarSuPropiaCuenta() {
        assertFalse(ReglasAdmin.puedeEliminar(3, 3, false));
    }

    @Test
    public void noSePuedeEliminarAlUltimoAdmin() {
        assertFalse(ReglasAdmin.puedeEliminar(3, 1, true));
    }

    @Test
    public void sePuedeEliminarAUnaCuentaComun() {
        assertTrue(ReglasAdmin.puedeEliminar(3, 1, false));
    }

    // --- Roles ---

    @Test
    public void noSePuedeEliminarElRolDeLaSesion() {
        assertFalse(ReglasAdmin.puedeEliminarRol(2, 2));
        assertTrue(ReglasAdmin.puedeEliminarRol(3, 2));
    }

    @Test
    public void elRolPropioNoPuedeQuedarseSinPermisoDeUsuariosSiEsElUnicoAdmin() {
        assertFalse(ReglasAdmin.puedeQuitarPermisoUsuarios(2, 2, false, false));
    }

    @Test
    public void elRolPropioPuedeEditarseSiConservaElPermisoDeUsuarios() {
        assertTrue(ReglasAdmin.puedeQuitarPermisoUsuarios(2, 2, true, false));
    }

    @Test
    public void elRolPropioPuedeQuedarseSinUsuariosSiQuedaOtroAdmin() {
        assertTrue(ReglasAdmin.puedeQuitarPermisoUsuarios(2, 2, false, true));
    }

    @Test
    public void otroRolSiempreSePuedeEditar() {
        assertTrue(ReglasAdmin.puedeQuitarPermisoUsuarios(5, 2, false, false));
    }
}
