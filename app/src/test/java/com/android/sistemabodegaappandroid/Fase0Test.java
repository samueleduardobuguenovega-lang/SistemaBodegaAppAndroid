package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * FASE 0 del plan de segunda vuelta (Create + Read del módulo de productos):
 * alta transaccional con stock inicial, guard de permiso del formulario,
 * búsqueda acotada, fechas ordenables y esquema v7.
 *
 * Son pruebas JVM puras que leen las fuentes como texto, igual que el resto de
 * la suite: validan estructura, no comportamiento en ejecución, porque el
 * proyecto no corre Robolectric ni instrumentación en esta tarea.
 */
public class Fase0Test {

    /**
     * CU-04: el alta con stock inicial es UNA sola transacción. Si el
     * movimiento falla, el producto no puede quedar guardado sin la entrada
     * que explique su stock, así que el alta del formulario no puede mover el
     * stock por separado (eso era justamente lo que la dejaba a medias).
     */
    @Test
    public void altaEsUnaSolaTransaccion() throws IOException {
        String dao = leer("ProductoDao.java");
        assertTrue("El DAO debe exponer el alta transaccional",
                dao.contains("insertarProductoConStockInicial"));
        assertTrue("El alta debe abrir su propia transacción",
                dao.contains("beginTransaction"));

        String formulario = leer("ProductoFormActivity.java");
        assertTrue("El formulario debe persistir por el alta transaccional",
                formulario.contains("insertarProductoConStockInicial"));
        // El diálogo de movimientos sí usa actualizarStock (CU-12); lo que no
        // puede hacer el alta es mover el stock fuera de su transacción.
        String alta = entre(formulario, "private void guardarAlta(", "private void guardarEdicion(");
        assertFalse("El alta ya no debe mover el stock por fuera de su transacción",
                alta.contains("actualizarStock"));
    }

    /** RF-27: el formulario revalida el permiso, no solo el listado. */
    @Test
    public void formularioTieneGuardDePermisos() throws IOException {
        String listado = leer("ProductosActivity.java");
        assertTrue("El listado de productos debe usar el guard",
                listado.contains("AccesoModulo"));
        assertTrue("El listado de productos exige el permiso PRODUCTOS",
                listado.contains("Permisos.PRODUCTOS"));

        String formulario = leer("ProductoFormActivity.java");
        assertTrue("El formulario de producto debe usar el guard",
                formulario.contains("AccesoModulo"));
        assertTrue("El formulario de producto exige el permiso PRODUCTOS",
                formulario.contains("Permisos.PRODUCTOS"));
    }

    /** #render: la búsqueda acota el resultado para no degradar el catálogo. */
    @Test
    public void busquedaLimitada() throws IOException {
        String dao = leer("ProductoDao.java");
        assertTrue("Las consultas de catálogo y búsqueda deben acotarse", dao.contains("LIMIT "));
        assertTrue("El tope debe estar en una constante con nombre", dao.contains("LIMITE_BUSQUEDA"));
    }

    /**
     * #fecha-texto: las fechas deben ordenar lexicográficamente y no depender
     * del idioma del dispositivo, así que el formato es fijo y la zona UTC.
     * Los índices acompañan a la búsqueda y a los historiales por producto.
     */
    @Test
    public void fechasOrdenables() throws IOException {
        String helper = leer("DatabaseHelper.java");
        int indices = contar(helper, "CREATE INDEX IF NOT EXISTS");
        assertTrue("Deben declararse los tres índices (sku, barras, movimiento.producto)",
                indices >= 3);

        String dao = leer("ProductoDao.java");
        assertTrue("El formato persistido debe ser ordenable",
                dao.contains("yyyy-MM-dd HH:mm:ss"));
        assertTrue("La fecha debe escribirse en UTC", dao.contains("UTC"));

        int inicio = dao.indexOf("private String fechaActual");
        assertTrue("No se encontró fechaActual()", inicio >= 0);
        assertFalse("La fecha no puede depender del Locale del dispositivo",
                dao.substring(inicio).contains("Locale.getDefault()"));
    }

    /**
     * PRESERVE: extraer el cuerpo del movimiento a un método privado no puede
     * cambiar el comportamiento del movimiento suelto (CU-12): sigue abriendo y
     * cerrando su propia transacción, y el diálogo lo sigue usando.
     */
    @Test
    public void movimientoSueltoSigueSiendoTransaccional() throws IOException {
        String dao = leer("ProductoDao.java");
        String movimiento = entre(dao, "public boolean actualizarStock(int idProducto, int cantidad,",
                "private boolean aplicarMovimientoEnTransaccionAbierta");
        assertTrue("actualizarStock debe abrir su transacción",
                movimiento.contains("beginTransaction()"));
        assertTrue("actualizarStock debe marcar el éxito",
                movimiento.contains("setTransactionSuccessful()"));
        assertTrue("actualizarStock debe cerrar la transacción",
                movimiento.contains("endTransaction()"));

        assertTrue("El diálogo unificado de movimientos debe registrar por actualizarStock",
                leer("MovimientoStockDialog.java").contains("productoDao.actualizarStock("));
        assertTrue("El formulario del producto debe usar el diálogo unificado",
                leer("ProductoFormActivity.java").contains("MovimientoStockDialog.mostrar("));
    }

    /**
     * v9: permiso INFORMES para los roles de inventario (RF-19). Los pasos v7 y
     * v8 siguen existiendo y no se pueden perder al subir la versión.
     */
    @Test
    public void dbVersionNueve() throws IOException {
        String helper = leer("DatabaseHelper.java");
        assertTrue("La base debe estar en la versión 9",
                helper.matches("(?s).*DATABASE_VERSION\\s*=\\s*9\\s*;.*"));
        assertTrue("La migración debe cubrir el paso a la versión 7",
                helper.contains("oldVersion < 7"));
        assertTrue("La migración debe cubrir el paso a la versión 8",
                helper.contains("oldVersion < 8"));
        assertTrue("La migración debe cubrir el paso a la versión 9",
                helper.contains("oldVersion < 9"));
    }

    // --- helpers ---

    /** Cuenta apariciones no solapadas de un literal. */    private int contar(String fuente, String buscado) {
        int total = 0;
        int desde = 0;
        int encontrado = fuente.indexOf(buscado, desde);
        while (encontrado >= 0) {
            total++;
            desde = encontrado + buscado.length();
            encontrado = fuente.indexOf(buscado, desde);
        }
        return total;
    }

    /** Extrae el texto entre dos marcadores únicos; falla si alguno falta. */
    private String entre(String fuente, String inicio, String fin) {
        int i = fuente.indexOf(inicio);
        assertTrue("No se encontró el marcador de inicio: " + inicio, i >= 0);
        int j = fuente.indexOf(fin, i + inicio.length());
        assertTrue("No se encontró el marcador de fin: " + fin, j > i);
        return fuente.substring(i, j);
    }

    /** Localiza un archivo por nombre recorriendo el árbol del módulo. */
    private Path buscarArchivo(String nombre) throws IOException {
        Path directorio = Paths.get(System.getProperty("user.dir"));
        List<Path> raices = new ArrayList<>();
        Path src = directorio.resolve("src");
        if (Files.isDirectory(src)) {
            raices.add(src);
        }
        raices.add(directorio);
        if (directorio.getParent() != null) {
            raices.add(directorio.getParent());
        }

        for (Path raiz : raices) {
            Path encontrado = buscarEn(raiz, nombre);
            if (encontrado != null) {
                return encontrado;
            }
        }
        return null;
    }

    /** Recorre una raíz con Files.walk y devuelve la primera coincidencia exacta. */
    private Path buscarEn(Path raiz, String nombre) throws IOException {
        if (!Files.isDirectory(raiz)) {
            return null;
        }
        try (Stream<Path> flujo = Files.walk(raiz)) {
            return flujo
                    .filter(Files::isRegularFile)
                    .filter(ruta -> ruta.getFileName().toString().equals(nombre))
                    // Evita confundir la fuente con los artefactos generados.
                    .filter(ruta -> !ruta.toString().replace('\\', '/').contains("/build/"))
                    .filter(ruta -> !ruta.toString().replace('\\', '/').contains("/.git/"))
                    .filter(ruta -> !ruta.toString().replace('\\', '/').contains("/.gradle/"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private String leer(String nombre) throws IOException {        Path archivo = buscarArchivo(nombre);
        assertNotNull("Archivo no encontrado: " + nombre, archivo);
        return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
    }
}
