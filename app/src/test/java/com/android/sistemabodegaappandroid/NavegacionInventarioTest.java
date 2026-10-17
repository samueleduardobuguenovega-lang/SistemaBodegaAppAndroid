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
 * Navegación e invariantes visuales del módulo de inventario.
 *
 * Lee los archivos del proyecto como texto desde el directorio de trabajo del
 * módulo app, por lo que corre en JVM sin emulador. Los archivos se localizan
 * con Files.walk por nombre, sin asumir rutas absolutas del repositorio.
 */
public class NavegacionInventarioTest {

    private static final String[] ACTIVIDADES = {
            "ProductosActivity.java",
            "ProductoFormActivity.java",
            "ProveedoresActivity.java",
            "ProveedorFormActivity.java"
    };

    private static final String[] LAYOUTS_NUEVOS = {
            "activity_productos.xml",
            "activity_producto_form.xml",
            "item_producto.xml",
            "activity_proveedores.xml",
            "activity_proveedor_form.xml",
            "item_proveedor.xml",
            "dialog_movimiento_stock.xml"
    };

    /** Las cuatro pantallas nuevas deben existir como fuentes compilables. */
    @Test
    public void lasCuatroActivitiesExisten() throws IOException {
        for (String nombre : ACTIVIDADES) {
            Path archivo = buscarArchivo(nombre);
            assertNotNull("No se encontró el archivo: " + nombre, archivo);
            assertFalse("El archivo está vacío: " + nombre,
                    leer(archivo).trim().isEmpty());
        }
    }

    /** Cada pantalla nueva debe estar declarada en el manifiesto. */
    @Test
    public void lasCuatroActivitiesEstanEnElManifiesto() throws IOException {
        String manifiesto = leer(buscarArchivo("AndroidManifest.xml"));
        for (String nombre : ACTIVIDADES) {
            String clase = nombre.replace(".java", "");
            assertTrue(clase + " debe estar declarada en AndroidManifest.xml",
                    manifiesto.contains("." + clase));
        }
    }

    /** Ninguna pantalla nueva puede quedar exportada a otras aplicaciones. */
    @Test
    public void lasActivitiesNuevasNoEstanExportadas() throws IOException {
        String manifiesto = leer(buscarArchivo("AndroidManifest.xml"));
        for (String nombre : ACTIVIDADES) {
            String clase = "." + nombre.replace(".java", "");
            int inicio = manifiesto.indexOf(clase);
            assertTrue(clase + " debe existir en el manifiesto", inicio > 0);
            int fin = manifiesto.indexOf("/>", inicio);
            String bloque = manifiesto.substring(inicio, fin > 0 ? fin : manifiesto.length());
            assertFalse(clase + " no debe ser accesible desde otras apps",
                    bloque.contains("android:exported=\"true\""));
        }
    }

    /** Las cuatro pantallas deben aplicar las áreas seguras del sistema. */
    @Test
    public void lasCuatroActivitiesAplicanLosInsets() throws IOException {
        for (String nombre : ACTIVIDADES) {
            String contenido = leer(buscarArchivo(nombre));
            assertTrue(nombre + " debe llamar a EdgeToEdgeInsets.apply",
                    contenido.contains("EdgeToEdgeInsets.apply"));
        }
    }

    /** Los colores heredados del tema no aparecen en ningún layout nuevo. */
    @Test
    public void losLayoutsNuevosNoHeredanColoresDelTema() throws IOException {
        List<String> conAttrs = new ArrayList<>();
        for (String nombre : LAYOUTS_NUEVOS) {
            Path archivo = buscarArchivo(nombre);
            assertNotNull("No se encontró el layout: " + nombre, archivo);
            if (leer(archivo).contains("?attr/color")) {
                conAttrs.add(nombre);
            }
        }
        assertTrue("Layouts nuevos con ?attr/color heredado: " + conAttrs,
                conAttrs.isEmpty());
    }

    /** El estilo Elevated hereda el fondo del tema y pinta las tarjetas en negro. */
    @Test
    public void losLayoutsNuevosNoUsanCardViewElevated() throws IOException {
        List<String> conElevated = new ArrayList<>();
        for (String nombre : LAYOUTS_NUEVOS) {
            Path archivo = buscarArchivo(nombre);
            assertNotNull("No se encontró el layout: " + nombre, archivo);
            if (leer(archivo).contains("Widget.Material3.CardView.Elevated")) {
                conElevated.add(nombre);
            }
        }
        assertTrue("Layouts nuevos con CardView.Elevated: " + conElevated,
                conElevated.isEmpty());
    }

    /** El panel principal debe exponer los dos accesos de inventario. */
    @Test
    public void mainActivityExponeLosAccesosDeInventario() throws IOException {
        String main = leer(buscarArchivo("MainActivity.java"));
        assertTrue("MainActivity debe referenciar btnAdminProductos",
                main.contains("btnAdminProductos"));
        assertTrue("MainActivity debe referenciar btnAdminProveedores",
                main.contains("btnAdminProveedores"));
        assertTrue("btnAdminProductos debe apuntar a ProductosActivity",
                main.contains("ProductosActivity"));
        assertTrue("btnAdminProveedores debe apuntar a ProveedoresActivity",
                main.contains("ProveedoresActivity"));
    }

    /**
     * El guard de usuarios y roles no puede cubrir el inventario.
     *
     * Antes el panel preguntaba por el rol Administrador y mostraba el
     * inventario a todos; ahora cada acceso depende de su propio permiso
     * (RF-27), así que la comprobación pasa por el permiso de cada botón y no
     * por el nombre del rol.
     */
    @Test
    public void losAccesosDeInventarioNoEstanDetrasDelGuardDeAdmin() throws IOException {
        String main = leer(buscarArchivo("MainActivity.java"));

        assertTrue("El panel ya no debe decidir por el nombre del rol",
                main.indexOf("esAdministrador()") < 0);
        assertTrue("Usuarios y roles dependen del permiso USUARIOS",
                main.contains("mostrarSegunPermiso(btnAdminUsersRoles, Permisos.USUARIOS)"));
        assertTrue("Productos depende de su propio permiso",
                main.contains("mostrarSegunPermiso(btnAdminProductos, Permisos.PRODUCTOS)"));
        assertTrue("Proveedores depende de su propio permiso",
                main.contains("mostrarSegunPermiso(btnAdminProveedores, Permisos.PROVEEDORES)"));
        assertTrue("Categorías depende de su propio permiso",
                main.contains("mostrarSegunPermiso(btnAdminCategorias, Permisos.CATEGORIAS)"));
        assertTrue("Los accesos de inventario deben seguir existiendo",
                main.contains("crearBotonesInventario()"));
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

    private String leer(Path archivo) throws IOException {
        assertNotNull("Archivo no encontrado", archivo);
        return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
    }
}
