package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.db.ProductoDao;
import com.android.sistemabodegaappandroid.model.Movimiento;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.model.TipoMovimiento;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * Historial general de movimientos (RF-21 / RF-22) contra SQLite de verdad.
 *
 * Cubre lo que la pantalla de informes muestra: el nombre del producto en cada
 * fila, la búsqueda por nombre o SKU y que el estado del stock no se cuele como
 * si fuera un tipo de movimiento.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class HistorialMovimientosTest {

    private DatabaseHelper helper;
    private ProductoDao dao;
    private int idArroz;
    private int idFideos;

    @Before
    public void setUp() {
        helper = new DatabaseHelper(RuntimeEnvironment.getApplication());
        dao = new ProductoDao(helper);
        int idCategoria = (int) helper.insertCategoria("Abarrotes", "prueba");

        idArroz = (int) dao.insertarProductoConStockInicial(
                producto("SKU-ARROZ", "Arroz 1kg", idCategoria), 10, "admin", 0);
        idFideos = (int) dao.insertarProductoConStockInicial(
                producto("SKU-FIDEOS", "Fideos 400g", idCategoria), 4, "admin", 0);
    }

    private Producto producto(String sku, String nombre, int idCategoria) {
        Producto p = new Producto();
        p.setSkuCodigo(sku);
        p.setNombre(nombre);
        p.setPrecioCosto(800);
        p.setPrecioVenta(1200);
        p.setStockMinimo(5);
        p.setIdCategoria(idCategoria);
        p.setUnidadMedida("UNIDAD");
        p.setActivo(1);
        return p;
    }

    @Test
    public void elHistorialTraeElNombreYElSkuDelProducto() {
        List<Movimiento> movimientos = dao.getHistorialMovimientosGeneral(0, null, null);

        assertEquals("las dos altas con stock inicial dejan un movimiento cada una", 2, movimientos.size());
        for (Movimiento mov : movimientos) {
            assertTrue("cada fila debe traer el nombre del producto, vino: '" + mov.getNombreProducto() + "'",
                    mov.getNombreProducto().length() > 0);
            assertTrue("cada fila debe traer el SKU, vino: '" + mov.getSkuCodigo() + "'",
                    mov.getSkuCodigo().length() > 0);
        }
    }

    @Test
    public void laBusquedaFiltraElHistorialPorNombreYSku() {
        List<Movimiento> porNombre = dao.getHistorialMovimientosGeneral(0, null, "arroz");
        List<Movimiento> porSku = dao.getHistorialMovimientosGeneral(0, null, "SKU-FIDEOS");
        List<Movimiento> sinResultados = dao.getHistorialMovimientosGeneral(0, null, "cemento");

        assertEquals(1, porNombre.size());
        assertEquals("Arroz 1kg", porNombre.get(0).getNombreProducto());
        assertEquals(1, porSku.size());
        assertEquals("Fideos 400g", porSku.get(0).getNombreProducto());
        assertEquals("un texto que no existe no debe devolver filas", 0, sinResultados.size());
    }

    @Test
    public void elTipoDeMovimientoSigueFiltrando() {
        assertTrue(dao.actualizarStock(idArroz, 3, TipoMovimiento.SALIDA, "cajero1"));

        List<Movimiento> entradas = dao.getHistorialMovimientosGeneral(0, "ENTRADA", null);
        List<Movimiento> salidas = dao.getHistorialMovimientosGeneral(0, "SALIDA", null);
        List<Movimiento> todos = dao.getHistorialMovimientosGeneral(0, "TODOS", null);

        assertEquals(2, entradas.size());
        assertEquals(1, salidas.size());
        assertEquals(3, todos.size());
        assertEquals("cajero1", salidas.get(0).getUsuario());
    }

    @Test
    public void elEstadoDelStockNoDevuelveFilas() {
        // Así se comportaba el filtro de la pantalla: "OK" viajaba como tipo de
        // movimiento. Ya no se usa, y el historial no debe vaciarse por eso.
        List<Movimiento> conEstadoOk = dao.getHistorialMovimientosGeneral(0, "OK", null);

        assertEquals("un estado de stock no es un tipo de movimiento", 0, conEstadoOk.size());
        assertEquals("el historial completo no se ve afectado", 2,
                dao.getHistorialMovimientosGeneral(0, null, null).size());
    }

    @Test
    public void elHistorialSePuedeAcotarAUnProducto() {
        List<Movimiento> soloFideos = dao.getHistorialMovimientosGeneral(idFideos, null, null);

        assertEquals(1, soloFideos.size());
        assertEquals("Fideos 400g", soloFideos.get(0).getNombreProducto());
    }
}
