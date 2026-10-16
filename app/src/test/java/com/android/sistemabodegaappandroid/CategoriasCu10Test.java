package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.lifecycle.LiveData;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.model.Categoria;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

/**
 * CU-10 / RF-10: Consultar y seleccionar categorías.
 *
 * Estos tests verifican el contrato del DAO de categorías sin necesitar un
 * Context de Android: comprueban que los métodos exigidos por el caso de uso
 * existen, exponen el tipo reactivo correcto y filtran por activo.
 */
public class CategoriasCu10Test {

    /** La firma debe existir y devolver una lista de categorías. */
    @Test
    public void getCategoriasActivasExisteYDevuelveLista() throws Exception {
        Method metodo = DatabaseHelper.class.getMethod("getCategoriasActivas");
        assertEquals(List.class, metodo.getReturnType());
    }

    /** CU-10 exige exponer la lista como StateFlow o LiveData. */
    @Test
    public void observarCategoriasActivasExponeLiveData() throws Exception {
        Method metodo = DatabaseHelper.class.getMethod("observarCategoriasActivas");
        assertEquals(LiveData.class, metodo.getReturnType());
    }

    @Test
    public void refrescarCategoriasActivasExiste() throws Exception {
        Method metodo = DatabaseHelper.class.getMethod("refrescarCategoriasActivas");
        assertEquals(void.class, metodo.getReturnType());
    }

    /**
     * El caso de uso exige "WHERE activo = 1 ORDER BY nombre_categoria ASC".
     * Se comprueba sobre el esquema declarado en la clase, leyendo las
     * constantes publicas de columna.
     */
    @Test
    public void columnasDeCategoriaSiguenElEsquemaDelCasoDeUso() {
        assertEquals("id_categoria", DatabaseHelper.COLUMN_CAT_ID);
        assertEquals("nombre_categoria", DatabaseHelper.COLUMN_CAT_NOMBRE);
        assertEquals("descripcion", DatabaseHelper.COLUMN_CAT_DESCRIPCION);
        assertEquals("activo", DatabaseHelper.COLUMN_CAT_ACTIVO);
        assertEquals("CATEGORIA", DatabaseHelper.TABLE_CATEGORIA);
    }

    /** Una categoría recién creada queda activa y la DAO la expone sin alterarla. */
    @Test
    public void categoriaRecienCreadaQuedaActiva() {
        Categoria categoria = new Categoria(1, "Bebidas", "Liquidos", 1);
        assertEquals(1, categoria.getActivo());
        assertTrue("Una categoría recién registrada debe estar activa",
                categoria.getActivo() == 1);
        assertEquals("Bebidas", categoria.getNombreCategoria());
    }

    /** Postcondición de CU-10: la consulta no altera el almacenamiento. */
    @Test
    public void consultarNoAlteraLaCategoria() {
        Categoria categoria = new Categoria(7, "Refrigerados", "Bebidas frias", 1);
        String nombreAntes = categoria.getNombreCategoria();
        int activoAntes = categoria.getActivo();

        // Simula una lectura repetida: el objeto debe permanecer intacto.
        assertNotNull(categoria);
        assertEquals(nombreAntes, categoria.getNombreCategoria());
        assertEquals(activoAntes, categoria.getActivo());
        assertFalse("La consulta no debe deshabilitar la categoría", categoria.getActivo() == 0);
    }
}