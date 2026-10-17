package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * RF-03: el alta de producto registra el stock inicial.
 *
 * El contrato lista `stock_actual` entre los datos a registrar y pide validar
 * que no sea negativo. Fijarlo en cero por código dejaba el requerimiento sin
 * cumplir: la invariante se sostenía sola, pero el dato no se ingresaba ni se
 * validaba.
 *
 * Son pruebas JVM puras que leen las fuentes como texto: validan estructura, no
 * comportamiento en ejecución, igual que el resto de la suite.
 */
public class StockInicialTest {

    private static final String RAIZ = "src/main/";
    private static final String PAQUETE = RAIZ + "java/com/android/sistemabodegaappandroid/";

    /** El formulario declara, lee, valida y usa el stock inicial. */
    @Test
    public void elAltaRegistraElStockInicialIngresado() throws IOException {
        String fuente = leer(PAQUETE + "ProductoFormActivity.java");

        assertTrue("El formulario debe tomar el stock inicial del campo",
                fuente.contains("etStockInicial"));
        assertFalse("El stock inicial no puede quedar fijo en 0",
                fuente.contains("nuevo.setStockActual(0)"));
        assertTrue("El stock inicial debe validarse junto con los otros valores",
                fuente.contains("stockInicial < 0"));
        assertTrue("El alta debe asentar el stock inicial como movimiento de entrada",
                fuente.contains("stockInicial, TipoMovimiento.ENTRADA"));
    }

    /** El stock inicial no se edita: el stock se mueve con movimientos (RF-07). */
    @Test
    public void laEdicionNoExponeElStockInicial() throws IOException {
        String fuente = leer(PAQUETE + "ProductoFormActivity.java");
        assertTrue("El campo debe ocultarse al editar un producto existente",
                fuente.contains("tilStockInicial.setVisibility(View.GONE)"));
    }

    /** El campo tiene que existir en el layout, con entrada numérica. */
    @Test
    public void elFormularioDeclaraElCampoDeStockInicial() throws IOException {
        String layout = leer(RAIZ + "res/layout/activity_producto_form.xml");

        assertTrue("Falta etStockInicial en el formulario", layout.contains("etStockInicial"));
        assertTrue("El campo debe estar dentro de su contenedor", layout.contains("tilStockInicial"));
        assertTrue("El stock inicial debe ser numérico", layout.contains("android:inputType=\"number\""));
    }

    /** El mensaje de validación debe cubrir el stock y no solo el mínimo. */
    @Test
    public void elMensajeDeValidacionCubreElStock() throws IOException {
        String strings = leer(RAIZ + "res/values/strings.xml");
        assertTrue("Debe existir el rótulo del campo",
                strings.contains("hint_stock_inicial"));
        assertTrue("Los precios y el stock no pueden ser negativos",
                strings.contains("Los precios y el stock no pueden ser negativos"));
    }

    private String leer(String rutaRelativa) throws IOException {
        Path archivo = Paths.get(System.getProperty("user.dir")).resolve(rutaRelativa);
        assertTrue("No existe el archivo: " + archivo, Files.isRegularFile(archivo));
        return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
    }
}
