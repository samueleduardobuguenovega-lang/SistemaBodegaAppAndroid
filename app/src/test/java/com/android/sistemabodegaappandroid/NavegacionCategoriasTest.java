package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Navegación del módulo de categorías (CU-10).
 *
 * Verifica que la pantalla de categorías está declarada en el manifiesto y que
 * su navegación de regreso está conectada. Lee los archivos del proyecto desde
 * la raíz del módulo, por lo que corre en JVM sin emulador.
 */
public class NavegacionCategoriasTest {

    private String leer(String rutaRelativa) throws IOException {
        File base = new File(System.getProperty("user.dir"));
        // Gradle ejecuta los tests desde app/, el manifest esta en app/src/main
        File f = new File(base, rutaRelativa);
        if (!f.exists()) {
            f = new File(base, "src/main/" + rutaRelativa);
        }
        assertNotNull("No se encontro el archivo: " + rutaRelativa, f);
        assertTrue("No se encontro el archivo: " + rutaRelativa + " en " + f.getAbsolutePath(), f.exists());
        try (InputStream in = new FileInputStream(f)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int leidos;
            while ((leidos = in.read(buffer)) != -1) {
                bos.write(buffer, 0, leidos);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /** La pantalla de categorías debe estar registrada en el manifiesto. */
    @Test
    public void categoriasActivityEstaDeclaradaEnElManifiesto() throws IOException {
        String manifest = leer("AndroidManifest.xml");
        assertTrue("CategoriasActivity debe estar declarada en el manifiesto",
                manifest.contains(".CategoriasActivity"));
    }

    /** No debe ser exportada: solo se abre desde el panel principal. */
    @Test
    public void categoriasActivityNoEsExportada() throws IOException {
        String manifest = leer("AndroidManifest.xml");
        int i = manifest.indexOf(".CategoriasActivity");
        assertTrue("CategoriasActivity debe existir en el manifiesto", i > 0);
        int fin = manifest.indexOf("/>", i);
        String bloque = manifest.substring(i, fin > 0 ? fin : manifest.length());
        assertFalse("CategoriasActivity no debe ser accesible desde otras apps",
                bloque.contains("android:exported=\"true\""));
    }

    /** La pantalla principal debe ser la que abre categorías. */
    @Test
    public void mainActivityNavegaACategorias() throws IOException {
        String main = leer("java/com/android/sistemabodegaappandroid/MainActivity.java");
        assertTrue("MainActivity debe abrir CategoriasActivity",
                main.contains("CategoriasActivity"));
    }

    /** El toolbar de categorías debe tener acción de retroceso conectada. */
    @Test
    public void toolbarTieneNavegacionDeRegreso() throws IOException {
        String activity = leer("java/com/android/sistemabodegaappandroid/CategoriasActivity.java");
        assertTrue("El toolbar debe conectar su accion de navegacion",
                activity.contains("setNavigationOnClickListener"));
    }

    /** La pantalla consume el DAO reactivo exigido por CU-10. */
    @Test
    public void laPantallaObservaElLiveDataDeCategorias() throws IOException {
        String activity = leer("java/com/android/sistemabodegaappandroid/CategoriasActivity.java");
        assertTrue("La pantalla debe observar el LiveData de CU-10",
                activity.contains("observarCategoriasActivas"));
        assertTrue("La pantalla debe suscribirse con observe",
                activity.contains(".observe(this"));
    }

    /** Las tres pantallas del flujo existen como clases compilables. */
    @Test
    public void lasPantallasDelFlujoExisten() throws IOException {
        String[] pantallas = {
                "java/com/android/sistemabodegaappandroid/LoginActivity.java",
                "java/com/android/sistemabodegaappandroid/MainActivity.java",
                "java/com/android/sistemabodegaappandroid/CategoriasActivity.java"
        };
        for (String ruta : pantallas) {
            String contenido = leer(ruta);
            assertFalse("El archivo no debe estar vacio: " + ruta, contenido.trim().isEmpty());
            assertTrue(ruta + " debe declarar una Activity",
                    contenido.contains("extends AppCompatActivity"));
        }
    }

    /** CU-09 + CU-10 comparten pantalla: el FAB registra y la lista consulta. */
    @Test
    public void laPantallaSoportaRegistroYConsulta() throws IOException {
        String activity = leer("java/com/android/sistemabodegaappandroid/CategoriasActivity.java");
        assertTrue("Debe permitir registrar una categoria",
                activity.contains("insertCategoria"));
        assertEquals("Debe ser la unica pantalla de categorias",
                1, countOcurrences(activity, "class CategoriasActivity"));
    }

    private int countOcurrences(String texto, String busqueda) {
        int n = 0, i = 0;
        while ((i = texto.indexOf(busqueda, i)) != -1) {
            n++;
            i += busqueda.length();
        }
        return n;
    }
}