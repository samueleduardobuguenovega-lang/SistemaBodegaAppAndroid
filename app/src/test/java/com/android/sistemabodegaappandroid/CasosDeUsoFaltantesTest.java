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
 * Verifica las tres brechas de cumplimiento cerradas contra el documento de
 * casos de uso: RF-08 (baja lógica de productos), la asociación de proveedores
 * a los movimientos de entrada y el flujo alternativo 4a del CU-01 (login).
 *
 * Son tests JVM puros que leen las fuentes como texto con Files.walk desde el
 * directorio de trabajo del módulo app: no necesitan dispositivo ni Robolectric
 * porque validan estructura, no comportamiento en runtime.
 */
public class CasosDeUsoFaltantesTest {

    // --- RF-08: baja lógica de productos ---

    /** v5 existe justamente para esta columna de permisos; sin ella el esquema no cambió. */
    @Test
    public void databaseVersionEsCinco() throws IOException {
        String fuente = leer("DatabaseHelper.java");
        assertTrue("DATABASE_VERSION debe valer 5",
                fuente.matches("(?s).*DATABASE_VERSION\\s*=\\s*5\\s*;.*"));
    }

    /** La migración v4 debe usar ALTER TABLE incremental, nunca DROP TABLE. */
    @Test
    public void onUpgradeAplicaLaMigracionDeLaVersionCuatroSinBorrarTablas() throws IOException {
        String fuente = leer("DatabaseHelper.java");
        String cuerpo = entre(fuente, "public void onUpgrade(", "private void insertInitialSeedData");
        assertFalse("La migración no debe contener DROP TABLE", cuerpo.contains("DROP TABLE"));
        assertTrue("La migración debe cubrir la versión 4", cuerpo.contains("oldVersion < 4"));
        assertTrue("La migración debe agregar la columna con ALTER TABLE",
                cuerpo.contains("ALTER TABLE"));
        assertTrue("La migración debe sumar id_proveedor a MOVIMIENTO",
                cuerpo.contains("COLUMN_MOV_ID_PROVEEDOR"));
    }

    /** El DDL compartido por onCreate y onUpgrade debe incluir la columna. */
    @Test
    public void elMovimientoRegistraElProveedorEnElDdl() throws IOException {
        String fuente = leer("DatabaseHelper.java");
        int inicio = fuente.indexOf("\"CREATE TABLE IF NOT EXISTS \" + TABLE_MOVIMIENTO");
        assertTrue("Falta el CREATE TABLE de MOVIMIENTO", inicio >= 0);
        int fin = fuente.indexOf("CREATE TABLE IF NOT EXISTS", inicio + 10);
        String bloque = fuente.substring(inicio, fin > 0 ? fin : fuente.length());
        assertTrue("MOVIMIENTO debe declarar id_proveedor",
                bloque.contains("COLUMN_MOV_ID_PROVEEDOR"));
    }

    /** RF-08: el DAO necesita el método de baja lógica, no solo updateProducto. */
    @Test
    public void productoDaoTieneUpdateProductoEstado() throws IOException {
        String dao = leer("ProductoDao.java");
        assertTrue("ProductoDao debe exponer updateProductoEstado",
                dao.contains("updateProductoEstado"));
        assertTrue("La baja lógica debe escribir activo",
                dao.contains("COLUMN_PROD_ACTIVO"));
    }

    /** El formulario debe exponer la acción de deshabilitar solo en edición. */
    @Test
    public void productoFormActivityReferenciaElBotonDeDeshabilitar() throws IOException {
        String activity = leer("ProductoFormActivity.java");
        assertTrue("El formulario debe referenciar btnDeshabilitarProducto",
                activity.contains("btnDeshabilitarProducto"));
        assertTrue("La baja lógica debe pasar por updateProductoEstado",
                activity.contains("updateProductoEstado"));
    }

    // --- Asociación de proveedores a los movimientos de entrada ---

    /** El DAO debe persistir el proveedor en el INSERT del MOVIMIENTO. */
    @Test
    public void actualizarStockRegistraElProveedorDeLaEntrada() throws IOException {
        String dao = leer("ProductoDao.java");
        assertTrue("actualizarStock debe aceptar el id del proveedor",
                dao.contains("int idProveedor"));
        assertTrue("El INSERT del movimiento debe guardar COLUMN_MOV_ID_PROVEEDOR",
                dao.contains("COLUMN_MOV_ID_PROVEEDOR"));
    }

    /** El modelo debe exponer el proveedor para leerlo del historial. */
    @Test
    public void movimientoExponeElProveedor() throws IOException {
        String modelo = leer("Movimiento.java");
        assertTrue("Movimiento debe tener el campo idProveedor",
                modelo.contains("private int idProveedor"));
        assertTrue("Movimiento debe leer id_proveedor en el SELECT",
                leer("ProductoDao.java").contains("indiceProveedor"));
    }

    /** El diálogo debe ofrecer el selector de proveedor para la entrada. */
    @Test
    public void elDialogoDeMovimientoTieneSelectorDeProveedor() throws IOException {
        String dialogo = leer("dialog_movimiento_stock.xml");
        assertTrue("El diálogo debe incluir spinnerProveedorMovimiento",
                dialogo.contains("spinnerProveedorMovimiento"));
        assertTrue("El selector debe vivir en un contenedor ocultable",
                dialogo.contains("contenedorProveedorMovimiento"));
        String activity = leer("ProductoFormActivity.java");
        assertTrue("La actividad debe alternar la visibilidad según el tipo",
                activity.contains("contenedorProveedorMovimiento"));
    }

    // --- CU-01, flujo alternativo 4a ---

    /** Tras credenciales inválidas el foco debe volver al campo de usuario. */
    @Test
    public void loginDevuelveElFocoAlUsuarioTrasElError() throws IOException {
        String login = leer("LoginActivity.java");
        assertTrue("Login debe devolver el foco con requestFocus",
                login.contains("requestFocus"));
        assertTrue("Login debe limpiar la contraseña con setText",
                login.contains("setText(\"\")"));
    }

    // --- helpers ---

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

    private String leer(String nombre) throws IOException {
        Path archivo = buscarArchivo(nombre);
        assertNotNull("Archivo no encontrado: " + nombre, archivo);
        return new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
    }

    /** Extrae el texto entre dos marcadores únicos; falla si alguno falta. */
    private String entre(String fuente, String inicio, String fin) {
        int i = fuente.indexOf(inicio);
        assertTrue("No se encontró el marcador de inicio: " + inicio, i >= 0);
        int j = fuente.indexOf(fin, i + inicio.length());
        assertTrue("No se encontró el marcador de fin: " + fin, j > i);
        return fuente.substring(i, j);
    }
}
