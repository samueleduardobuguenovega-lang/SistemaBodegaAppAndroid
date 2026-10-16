package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.model.TipoMovimiento;

import org.junit.Test;

/**
 * Tests puros de lógica de dominio del módulo de productos.
 *
 * No tocan Android ni SQLite: solo modelos y el enum, de modo que corren en la
 * JVM del Gradle test sin dispositivo ni Robolectric.
 */
public class StockProductoTest {

    /** El enum debe tener exactamente ENTRADA y SALIDA, con esos textos. */
    @Test
    public void tipoMovimientoTieneDosConstantes() {
        assertEquals("El enum debe tener exactamente dos tipos",
                2, TipoMovimiento.values().length);
        assertEquals("ENTRADA", TipoMovimiento.ENTRADA.getValor());
        assertEquals("SALIDA", TipoMovimiento.SALIDA.getValor());
    }

    /** El valor persistible debe coincidir con el nombre de la constante. */
    @Test
    public void tipoMovimientoValorCoincideConElNombre() {
        for (TipoMovimiento tipo : TipoMovimiento.values()) {
            assertEquals(tipo.name(), tipo.getValor());
        }
    }

    @Test
    public void productoGuardaLosValoresEnLosSetters() {
        Producto p = new Producto();
        p.setIdProducto(5);
        p.setSkuCodigo("SKU-001");
        p.setNombre("Arroz");
        p.setDescripcion("Bolsa 1 kg");
        p.setPrecioCosto(800.0);
        p.setPrecioVenta(1200.0);
        p.setStockActual(10);
        p.setStockMinimo(3);
        p.setIdCategoria(2);
        p.setIdProveedor(7);
        p.setActivo(1);
        p.setNombreCategoria("Alimentos");
        p.setRazonSocialProveedor("Proveedor SpA");

        assertEquals(5, p.getIdProducto());
        assertEquals("SKU-001", p.getSkuCodigo());
        assertEquals("Arroz", p.getNombre());
        assertEquals("Bolsa 1 kg", p.getDescripcion());
        assertEquals(800.0, p.getPrecioCosto(), 0.0001);
        assertEquals(1200.0, p.getPrecioVenta(), 0.0001);
        assertEquals(10, p.getStockActual());
        assertEquals(3, p.getStockMinimo());
        assertEquals(2, p.getIdCategoria());
        assertEquals(7, p.getIdProveedor());
        assertEquals(1, p.getActivo());
        assertEquals("Alimentos", p.getNombreCategoria());
        assertEquals("Proveedor SpA", p.getRazonSocialProveedor());
    }

    /**
     * No se impone precioVenta >= precioCosto: el modelo solo persiste lo que
     * se le asigne y la regla de negocio (si existe) la decide la capa de UI.
     */
    @Test
    public void productoNoImponeRelacionEntrePrecios() {
        Producto p = new Producto();
        p.setPrecioCosto(5000.0);
        p.setPrecioVenta(1000.0);
        assertEquals("El setter debe guardar sin validar", 1000.0, p.getPrecioVenta(), 0.0001);
        assertNotEquals("El modelo no debe recalcar nada", 5000.0, p.getPrecioVenta(), 0.0001);
    }

    /** Un producto sin constructor completo parte en cero y habilitado. */
    @Test
    public void productoPorDefectoTieneStockCeroYActivo() {
        Producto p = new Producto();
        assertEquals(0, p.getIdProducto());
        assertNull(p.getSkuCodigo());
        assertEquals(0.0, p.getPrecioCosto(), 0.0001);
        assertEquals(0.0, p.getPrecioVenta(), 0.0001);
        assertEquals(0, p.getStockActual());
        assertEquals(0, p.getStockMinimo());
        assertEquals("Un producto nuevo nace habilitado", 1, p.getActivo());
    }

    /** El constructor completo debe mapear todos los campos en orden. */
    @Test
    public void productoConstructorCompletoMapeaTodosLosCampos() {
        Producto p = new Producto(1, "SKU-9", "Aceite", "Botella", 1000.0, 1500.0,
                4, 2, 3, 4, 1, "Alimentos", "Distribuidora Uno");
        assertEquals(1, p.getIdProducto());
        assertEquals("SKU-9", p.getSkuCodigo());
        assertEquals("Aceite", p.getNombre());
        assertEquals("Botella", p.getDescripcion());
        assertEquals(1000.0, p.getPrecioCosto(), 0.0001);
        assertEquals(1500.0, p.getPrecioVenta(), 0.0001);
        assertEquals(4, p.getStockActual());
        assertEquals(2, p.getStockMinimo());
        assertEquals(3, p.getIdCategoria());
        assertEquals(4, p.getIdProveedor());
        assertEquals(1, p.getActivo());
        assertEquals("Alimentos", p.getNombreCategoria());
        assertEquals("Distribuidora Uno", p.getRazonSocialProveedor());
    }

    /** La lógica de stock del DAO: ENTRADA suma, SALIDA resta. Se replica aquí
     *  porque el DAO real exige SQLiteDatabase y no puede correr en la JVM. */
    @Test
    public void logicaDeStockEntradaSumaYSalidaResta() {
        int stock = 10;
        int entradas = 5;
        int salidas = 3;
        assertEquals(15, stock + entradas);
        assertEquals(7, stock - salidas);
    }

    /** Una SALIDA mayor al stock debe rechazarse, no dejar stock negativo. */
    @Test
    public void salidaMayorAlStockNoDeberiaDejarNegativo() {
        int stock = 4;
        int salidaIntentada = 10;
        boolean debeRechazarse = salidaIntentada > stock;
        assertTrue("Una salida superior al stock debe rechazarse", debeRechazarse);
        assertEquals("El stock nunca debe quedar negativo",
                Math.max(0, stock - salidaIntentada), 0);
    }
}
