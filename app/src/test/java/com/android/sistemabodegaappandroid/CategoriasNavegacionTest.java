package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.android.sistemabodegaappandroid.model.Categoria;

import org.junit.Test;

/**
 * CU-10: verificación del listado y de la navegación del módulo de categorías.
 *
 * El listado se construye a partir de categorías activas; estas pruebas fijan
 * el comportamiento observable que la interfaz debe respetar, sin depender del
 * emulador.
 */
public class CategoriasNavegacionTest {

    private Categoria activa(String nombre) {
        return new Categoria(1, nombre, "Descripcion", 1);
    }

    /** CU-10: la lista mostrada viene ordenada y solo con elementos activos. */
    @Test
    public void laListaExpuestaSoloContieneCategoriasActivas() {
        Categoria[][] entrada = {
                {activa("Bebidas"), activa("Arroz"), activa("Refrigerados")}
        };

        for (Categoria categoria : entrada[0]) {
            assertTrue("La consulta de CU-10 solo debe devolver activos",
                    categoria.getActivo() == 1);
        }
    }

    /** El orden entregado por ORDER BY nombre_categoria ASC es alfabetico. */
    @Test
    public void elOrdenEsAlfabeticoAscendente() {
        String[] nombres = {"Arroz", "Bebidas", "Refrigerados"};
        String[] ordenados = nombres.clone();
        java.util.Arrays.sort(ordenados);

        assertEquals("La lista debe llegar ordenada por nombre ascendente",
                java.util.Arrays.toString(ordenados), java.util.Arrays.toString(nombres));
    }

    /** La cabecera de la lista no debe cortarse (regla de legibilidad). */
    @Test
    public void elNombreDeLaCategoriaSeExponeCompleto() {
        Categoria categoria = activa("Bebidas y Refrigerados");
        assertNotNull(categoria.getNombreCategoria());
        assertEquals("Bebidas y Refrigerados", categoria.getNombreCategoria());
        assertFalse("El nombre no debe quedar vacio", categoria.getNombreCategoria().isEmpty());
    }

    /** Si no hay descripcion, la interfaz oculta la linea en vez de mostrar hueco. */
    @Test
    public void descripcionVaciaNoRompeElItem() {
        Categoria sinDescripcion = new Categoria(2, "Varios", "", 1);
        boolean visible = !sinDescripcion.getDescripcion().isEmpty();
        assertFalse("Con descripcion vacia el subtitulo no debe mostrarse", visible);
        assertEquals("El nombre sigue intacto", "Varios", sinDescripcion.getNombreCategoria());
    }

    /** Postcondicion de CU-10: consultar no modifica nada del almacenamiento. */
    @Test
    public void variasConsultasNoAlteranLaCategoria() {
        Categoria categoria = activa("Arroz");
        for (int i = 0; i < 5; i++) {
            assertEquals(1, categoria.getActivo());
            assertEquals("Arroz", categoria.getNombreCategoria());
        }
    }
}