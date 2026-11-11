package com.android.sistemabodegaappandroid.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas de la lista de permisos de ROL.
 *
 * El editor de roles armaba el CSV sumando los tokens uno por uno, así que un
 * permiso nuevo quedaba fuera sin que nada avisara. Estas pruebas fijan el
 * contrato de {@link Permisos#unir} y {@link Permisos#agregar}.
 */
public class PermisosCsvTest {

    @Test
    public void unirIncluyeTodosLosModulosMarcados() {
        String csv = Permisos.unir(true, true, true, true, true, true);

        assertEquals("USUARIOS,PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS,INFORMES", csv);
        assertTrue(Permisos.tiene(csv, Permisos.INFORMES));
    }

    @Test
    public void unirOmiteLosModulosDesmarcados() {
        String csv = Permisos.unir(false, true, true, false, false, false);

        assertEquals("PRODUCTOS,CATEGORIAS", csv);
        assertFalse("un rol de ventas no debería recibir informes de stock",
                Permisos.tiene(csv, Permisos.INFORMES));
    }

    @Test
    public void unirConInformesSolamenteNoArrastraOtrosPermisos() {
        String csv = Permisos.unir(false, false, false, false, false, true);

        assertEquals("INFORMES", csv);
    }

    @Test
    public void agregarSumaElTokenSinDuplicarlo() {
        String conInformes = Permisos.agregar("USUARIOS,MOVIMIENTOS", Permisos.INFORMES);
        assertEquals("USUARIOS,MOVIMIENTOS,INFORMES", conInformes);

        String otraVez = Permisos.agregar(conInformes, Permisos.INFORMES);
        assertEquals("agregar dos veces el mismo permiso no debe duplicarlo",
                conInformes, otraVez);
    }

    @Test
    public void agregarSobreUnaListaVaciaDevuelveSoloElToken() {
        assertEquals("INFORMES", Permisos.agregar(null, Permisos.INFORMES));
        assertEquals("INFORMES", Permisos.agregar("   ", Permisos.INFORMES));
    }
}
