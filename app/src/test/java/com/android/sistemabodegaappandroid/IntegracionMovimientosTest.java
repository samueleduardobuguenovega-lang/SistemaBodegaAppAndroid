package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.android.sistemabodegaappandroid.ui.MovimientoStockDialog;

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
 * Integración de CU-04 / CU-05 (entrada y salida de stock con constancia
 * documental) entre el listado de productos, el formulario y la base.
 *
 * Son pruebas JVM puras, como el resto de la suite de conformidad: leen las
 * fuentes como texto porque validan estructura y contratos entre archivos, no
 * comportamiento en runtime. Lo único que se ejecuta de verdad es el parseo de
 * la cantidad, que es justo el que antes cerraba la app.
 */
public class IntegracionMovimientosTest {

    // --- Un solo diálogo de movimiento ---

    /**
     * La duplicación fue el defecto de la integración anterior: el listado tenía
     * su propia copia del diálogo y esa copia guardaba siempre id_proveedor = 0,
     * así que una entrada registrada desde ahí perdía el proveedor aunque el
     * campo se viera en pantalla. Con dos copias, cualquier arreglo se aplica a
     * una sola. Este test falla si vuelve a aparecer una segunda.
     */
    @Test
    public void unSoloArchivoArmaElDialogoDeMovimiento() throws IOException {
        List<String> archivos = archivosQueMencionan("R.layout.dialog_movimiento_stock");
        assertEquals("Solo MovimientoStockDialog debe inflar el layout del diálogo: " + archivos,
                1, archivos.size());
        assertTrue(archivos.get(0).endsWith("MovimientoStockDialog.java"));
    }

    /** Los dos puntos de entrada usan esa implementación única. */
    @Test
    public void listadoYFormularioUsanElMismoDialogo() throws IOException {
        assertTrue("El listado debe abrir el diálogo unificado",
                leer("ProductosActivity.java").contains("MovimientoStockDialog.mostrar("));
        assertTrue("El formulario debe abrir el diálogo unificado",
                leer("ProductoFormActivity.java").contains("MovimientoStockDialog.mostrar("));
    }

    // --- El movimiento viaja con documento, motivo y proveedor ---

    /** El INSERT de MOVIMIENTO escribe las dos columnas de la v8. */
    @Test
    public void elMovimientoPersisteDocumentoYMotivo() throws IOException {
        String dao = leer("ProductoDao.java");
        String insert = entre(dao, "private boolean aplicarMovimientoEnTransaccionAbierta",
                "public List<Movimiento> getMovimientosByProducto");
        assertTrue("El movimiento debe guardar el documento",
                insert.contains("COLUMN_MOV_DOCUMENTO"));
        assertTrue("El movimiento debe guardar el motivo",
                insert.contains("COLUMN_MOV_MOTIVO"));
        assertTrue("El proveedor de la entrada se sigue guardando",
                insert.contains("COLUMN_MOV_ID_PROVEEDOR"));
    }

    /** El historial lee las dos columnas y las entrega al modelo. */
    @Test
    public void elHistorialLeeDocumentoYMotivo() throws IOException {
        String dao = leer("ProductoDao.java");
        String lectura = entre(dao, "private List<Movimiento> leerMovimientos",
                "/** Productos habilitados cuyo stock igualó o bajó del mínimo");
        assertTrue(lectura.contains("COLUMN_MOV_DOCUMENTO"));
        assertTrue(lectura.contains("COLUMN_MOV_MOTIVO"));
        // Una base sin migrar no puede dejar la pantalla sin historial.
        assertTrue("El historial debe degradar a esquemas anteriores",
                lectura.contains("conNuevosCampos"));
    }

    /**
     * El alta con stock inicial audita ese stock como ENTRADA con su documento,
     * en vez de dejarlo sin constancia: si el movimiento falla, el producto no
     * se guarda (sigue siendo una sola transacción).
     */
    @Test
    public void elAltaConStockInicialDejaConstancia() throws IOException {
        String dao = leer("ProductoDao.java");
        String alta = entre(dao, "public long insertarProductoConStockInicial",
                "public boolean updateProducto");
        assertTrue(alta.contains("DOCUMENTO_STOCK_INICIAL"));
        assertTrue(alta.contains("MOTIVO_STOCK_INICIAL"));
        assertTrue("El alta debe seguir siendo transaccional",
                alta.contains("setTransactionSuccessful()"));
    }

    /** La v8 migra con ALTER TABLE: los movimientos viejos no se borran. */
    @Test
    public void laMigracionOchoNoBorraMovimientos() throws IOException {
        String helper = leer("DatabaseHelper.java");
        String migracion = entre(helper, "if (oldVersion < 8) {", "private void insertInitialSeedData");
        assertFalse("La v8 no puede recrear la tabla", migracion.contains("DROP TABLE"));
        assertTrue(migracion.contains("agregarColumnaSiFalta"));
        assertTrue(migracion.contains("COLUMN_MOV_DOCUMENTO"));
        assertTrue(migracion.contains("COLUMN_MOV_MOTIVO"));
    }

    // --- El listado no bloquea el hilo principal ---

    /**
     * La integración anterior consultaba los productos deshabilitados en el hilo
     * de la interfaz (`productoDao.getProductosInactivos()` dentro del listener
     * del interruptor y del buscador). Con una bodega real eso es un ANR
     * esperando su turno: ahora la lista llega por LiveData desde el executor.
     */
    @Test
    public void elListadoNoConsultaLaBaseEnElHiloPrincipal() throws IOException {
        String actividad = leer("ProductosActivity.java");
        assertFalse("El listado no debe invocar el DAO directamente",
                actividad.contains("productoDao.getProductosInactivos("));
        assertFalse("El listado no debe invocar el DAO directamente",
                actividad.contains("productoDao.buscarProductos("));
        assertTrue("Los deshabilitados llegan por LiveData",
                actividad.contains("observarProductosInactivos"));
        assertTrue("El filtro de deshabilitados se pide al helper",
                actividad.contains("refrescarProductosInactivos"));
    }

    // --- La cantidad no puede cerrar la app ---

    /**
     * Antes: {@code Integer.parseInt(texto)} directo sobre lo que escribió el
     * usuario. Un texto no numérico lanzaba NumberFormatException y la app se
     * cerraba. Ahora el parseo es tolerante y devuelve -1.
     */
    @Test
    public void unaCantidadNoNumericaNoRompeNada() {
        assertEquals("un texto no numérico no es una cantidad",
                -1, MovimientoStockDialog.leerCantidad("dos"));
        assertEquals("un número fuera de rango no es una cantidad",
                -1, MovimientoStockDialog.leerCantidad("99999999999"));
        assertEquals("el campo vacío no es una cantidad",
                -1, MovimientoStockDialog.leerCantidad(""));
        assertEquals("null no es una cantidad",
                -1, MovimientoStockDialog.leerCantidad(null));
        assertEquals("un entero válido se respeta",
                12, MovimientoStockDialog.leerCantidad("12"));
        assertEquals("los espacios no rompen el parseo",
                7, MovimientoStockDialog.leerCantidad("  7 "));
    }

    /** El diálogo valida la salida antes de tocar la base. */
    @Test
    public void laSalidaSeValidaAntesDeEscribir() throws IOException {
        String dialogo = leer("MovimientoStockDialog.java");
        assertTrue("La salida no puede superar el stock disponible",
                dialogo.contains("msg_stock_insuficiente"));
        assertTrue("La salida exige motivo para ser auditable",
                dialogo.contains("msg_motivo_requerido"));
        assertTrue("El proveedor se resuelve por id, no se deja en cero",
                dialogo.contains("idProveedorElegido"));
    }

    // --- No queda una segunda pantalla de edición ---

    /**
     * La pantalla de edición duplicada (EditarProductoActivity) no estaba
     * declarada en el manifiesto y nadie la abría: era un formulario paralelo
     * con menos campos que el real, con Toasts de depuración y un control de
     * acceso por nombre de rol en vez de por permisos. Se eliminó.
     */
    @Test
    public void noQuedaUnSegundoFormularioDeEdicion() throws IOException {
        assertFalse("EditarProductoActivity no debe existir",
                existe("EditarProductoActivity.java"));
        assertFalse("Su layout tampoco",
                existe("activity_editar_producto.xml"));
    }

    // --- helpers ---

    /** Archivos .java del módulo que contienen el texto buscado. */
    private List<String> archivosQueMencionan(String buscado) throws IOException {
        List<String> encontrados = new ArrayList<>();
        Path raiz = Paths.get(System.getProperty("user.dir")).resolve("src/main/java");
        try (Stream<Path> recorrido = Files.walk(raiz)) {
            for (Path archivo : (Iterable<Path>) recorrido.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String fuente = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
                if (fuente.contains(buscado)) {
                    encontrados.add(archivo.toString());
                }
            }
        }
        return encontrados;
    }

    /** true si el archivo aparece en cualquier punto del árbol del módulo. */
    private boolean existe(String nombre) throws IOException {
        Path raiz = Paths.get(System.getProperty("user.dir")).resolve("src");
        try (Stream<Path> recorrido = Files.walk(raiz)) {
            return recorrido.anyMatch(p -> p.getFileName() != null
                    && p.getFileName().toString().equals(nombre));
        }
    }

    private String entre(String fuente, String inicio, String fin) {
        int i = fuente.indexOf(inicio);
        assertTrue("No se encontró el marcador de inicio: " + inicio, i >= 0);
        int j = fuente.indexOf(fin, i + inicio.length());
        assertTrue("No se encontró el marcador de fin: " + fin, j > i);
        return fuente.substring(i, j);
    }

    private String leer(String nombre) throws IOException {
        Path raiz = Paths.get(System.getProperty("user.dir")).resolve("src/main/java");
        try (Stream<Path> recorrido = Files.walk(raiz)) {
            Path archivo = recorrido
                    .filter(p -> p.getFileName().toString().equals(nombre))
                    .findFirst().orElse(null);
            assertNotNull("Archivo no encontrado: " + nombre, archivo);
            return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
        }
    }
}
