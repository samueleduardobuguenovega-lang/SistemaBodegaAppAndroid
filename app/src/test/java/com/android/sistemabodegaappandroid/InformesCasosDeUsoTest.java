package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.android.sistemabodegaappandroid.util.Permisos;

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
 * Tests JVM que verifican el cumplimiento de los requerimientos RF-19, RF-20, RF-21, RF-22 y RF-24
 * correspondientes a los casos de uso CU-06 (Informe de stock), CU-07 (Escáner de código de barras) y CU-08 (Ubicación Google Maps).
 */
public class InformesCasosDeUsoTest {

    private static final String[] ACTIVIDADES_NUEVAS = {
            "InformesActivity.java",
            "BarcodeScannerActivity.java",
            "UbicacionEmpresaActivity.java"
    };

    /** RF-19 / CU-06: Permiso INFORMES definido en Permisos.java. */
    @Test
    public void permisoInformesEstaDefinido() {
        assertTrue("Permisos.INFORMES debe valer INFORMES",
                "INFORMES".equalsIgnoreCase(Permisos.INFORMES));
    }

    /** CU-06, CU-07, CU-08: Las tres actividades nuevas deben existir en las fuentes. */
    @Test
    public void lasTresActividadesExisten() throws IOException {
        for (String nombre : ACTIVIDADES_NUEVAS) {
            Path archivo = buscarArchivo(nombre);
            assertNotNull("No se encontró el archivo de actividad: " + nombre, archivo);
            assertFalse("El archivo de actividad está vacío: " + nombre,
                    leer(archivo).trim().isEmpty());
        }
    }

    /** CU-06, CU-07, CU-08: Las tres actividades deben estar declaradas en AndroidManifest.xml. */
    @Test
    public void lasTresActividadesEstanEnElManifiesto() throws IOException {
        String manifiesto = leer(buscarArchivo("AndroidManifest.xml"));
        for (String nombre : ACTIVIDADES_NUEVAS) {
            String clase = nombre.replace(".java", "");
            assertTrue(clase + " debe estar declarada en AndroidManifest.xml",
                    manifiesto.contains("." + clase));
        }
    }

    /** CU-07 y CU-08: El manifiesto declara los permisos de cámara y ubicación. */
    @Test
    public void elManifiestoDeclaraPermisosCamaraYUbicacion() throws IOException {
        String manifiesto = leer(buscarArchivo("AndroidManifest.xml"));
        assertTrue("Debe declarar permiso CAMERA para escaneo de código de barras",
                manifiesto.contains("android.permission.CAMERA"));
        assertTrue("Debe declarar permiso ACCESS_FINE_LOCATION para ruta a la empresa",
                manifiesto.contains("android.permission.ACCESS_FINE_LOCATION"));
    }

    /** CU-06 y CU-07: ProductoDao expone las consultas necesarias para el informe y código de barras. */
    @Test
    public void productoDaoExponeMetodosDeInformeYCodigoBarras() throws IOException {
        String dao = leer(buscarArchivo("ProductoDao.java"));
        assertTrue("ProductoDao debe exponer getInformeProductos",
                dao.contains("getInformeProductos"));
        assertTrue("ProductoDao debe exponer getProductoByCodigoBarras",
                dao.contains("getProductoByCodigoBarras"));
        assertTrue("ProductoDao debe exponer getHistorialMovimientosGeneral",
                dao.contains("getHistorialMovimientosGeneral"));
    }

    /** CU-06 y CU-08: MainActivity incluye accesos al informe y ubicación de la empresa. */
    @Test
    public void mainActivityExponeInformesYUbicacion() throws IOException {
        String main = leer(buscarArchivo("MainActivity.java"));
        assertTrue("MainActivity debe referenciar btnAdminInformes",
                main.contains("btnAdminInformes"));
        assertTrue("MainActivity debe referenciar btnUbicacionEmpresa",
                main.contains("btnUbicacionEmpresa"));
        assertTrue("MainActivity debe abrir InformesActivity",
                main.contains("InformesActivity"));
        assertTrue("MainActivity debe abrir UbicacionEmpresaActivity",
                main.contains("UbicacionEmpresaActivity"));
    }

    /** CU-06, CU-07, CU-08: Las actividades aplican EdgeToEdgeInsets.apply. */
    @Test
    public void lasActividadesAplicanLosInsets() throws IOException {
        for (String nombre : ACTIVIDADES_NUEVAS) {
            String contenido = leer(buscarArchivo(nombre));
            assertTrue(nombre + " debe llamar a EdgeToEdgeInsets.apply",
                    contenido.contains("EdgeToEdgeInsets.apply"));
        }
    }

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

    private Path buscarEn(Path raiz, String nombre) throws IOException {
        if (!Files.isDirectory(raiz)) {
            return null;
        }
        try (Stream<Path> flujo = Files.walk(raiz)) {
            return flujo
                    .filter(Files::isRegularFile)
                    .filter(ruta -> ruta.getFileName().toString().equals(nombre))
                    .filter(ruta -> !ruta.toString().replace('\\', '/').contains("/build/"))
                    .filter(ruta -> !ruta.toString().replace('\\', '/').contains("/.git/"))
                    .filter(ruta -> !ruta.toString().replace('\\', '/').contains("/.gradle/"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private String leer(Path archivo) throws IOException {
        assertNotNull("Archivo no encontrado", archivo);
        return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
    }
}
