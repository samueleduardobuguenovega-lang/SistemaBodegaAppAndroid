package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Verifica el esquema del módulo de inventario leyendo DatabaseHelper.java
 * como texto.
 *
 * Estos tests son JVM puros: no pueden instanciar DatabaseHelper porque ese
 * constructor exige un Context de Android. Parsear la fuente permite asegurar
 * que las tablas existen, que la migración no destruye datos y que las claves
 * foráneas quedan activas, sin depender de un dispositivo ni de Robolectric.
 */
public class EsquemaProductoTest {

    private static String fuente;

    @BeforeClass
    public static void cargarFuente() throws Exception {
        // El working directory de los tests unitarios es el modulo app.
        Path ruta = Paths.get("src", "main", "java",
                "com", "android", "sistemabodegaappandroid", "db", "DatabaseHelper.java");
        fuente = new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8);
    }

    /** Extrae el texto entre dos marcadores únicos; falla si alguno falta. */
    private static String entre(String inicio, String fin) {
        int i = fuente.indexOf(inicio);
        assertTrue("No se encontro el marcador de inicio: " + inicio, i >= 0);
        int j = fuente.indexOf(fin, i + inicio.length());
        assertTrue("No se encontro el marcador de fin: " + fin, j > i);
        return fuente.substring(i, j);
    }

    /** Devuelve el bloque de CREATE TABLE de una tabla por su constante. */
    private static String bloqueCreateTable(String constanteTabla) {
        String marca = "\"CREATE TABLE IF NOT EXISTS \" + " + constanteTabla;
        int i = fuente.indexOf(marca);
        assertTrue("Falta CREATE TABLE para " + constanteTabla, i >= 0);
        int fin = fuente.indexOf("\"CREATE TABLE IF NOT EXISTS \" +", i + marca.length());
        if (fin < 0) {
            fin = fuente.length();
        }
        return fuente.substring(i, fin);
    }

    @Test
    public void existeCreateTableDeProveedor() {
        String bloque = bloqueCreateTable("TABLE_PROVEEDOR");
        assertTrue(bloque.contains("TABLE_PROVEEDOR"));
        assertTrue("PROVEEDOR debe tener razon_social", bloque.contains("COLUMN_PROV_RAZON_SOCIAL"));
        assertTrue("El RUT debe ser unico", bloque.contains("UNIQUE"));
        assertTrue("PROVEEDOR debe tener email", bloque.contains("COLUMN_PROV_EMAIL"));
        assertTrue("PROVEEDOR debe tener telefono", bloque.contains("COLUMN_PROV_TELEFONO"));
        assertTrue("PROVEEDOR debe tener direccion", bloque.contains("COLUMN_PROV_DIRECCION"));
        assertTrue("PROVEEDOR debe tener la bandera activo", bloque.contains("COLUMN_PROV_ACTIVO"));
    }

    @Test
    public void existeCreateTableDeProducto() {
        String bloque = bloqueCreateTable("TABLE_PRODUCTO");
        assertTrue(bloque.contains("TABLE_PRODUCTO"));
        assertTrue("PRODUCTO debe tener SKU unico", bloque.contains("UNIQUE"));
        assertTrue("PRODUCTO debe tener nombre", bloque.contains("COLUMN_PROD_NOMBRE"));
        assertTrue("PRODUCTO debe tener precio_costo", bloque.contains("COLUMN_PROD_PRECIO_COSTO"));
        assertTrue("PRODUCTO debe tener precio_venta", bloque.contains("COLUMN_PROD_PRECIO_VENTA"));
        assertTrue("PRODUCTO debe tener stock_actual", bloque.contains("COLUMN_PROD_STOCK_ACTUAL"));
        assertTrue("PRODUCTO debe tener stock_minimo", bloque.contains("COLUMN_PROD_STOCK_MINIMO"));
        assertTrue("PRODUCTO debe referenciar CATEGORIA",
                bloque.contains("FOREIGN KEY") && bloque.contains("TABLE_CATEGORIA"));
        assertTrue("PRODUCTO debe referenciar PROVEEDOR", bloque.contains("TABLE_PROVEEDOR"));
    }

    @Test
    public void existeCreateTableDeMovimiento() {
        String bloque = bloqueCreateTable("TABLE_MOVIMIENTO");
        assertTrue(bloque.contains("TABLE_MOVIMIENTO"));
        assertTrue("MOVIMIENTO debe referenciar PRODUCTO",
                bloque.contains("FOREIGN KEY") && bloque.contains("TABLE_PRODUCTO"));
        assertTrue("MOVIMIENTO debe registrar el tipo", bloque.contains("COLUMN_MOV_TIPO"));
        assertTrue("MOVIMIENTO debe registrar la cantidad", bloque.contains("COLUMN_MOV_CANTIDAD"));
        assertTrue("MOVIMIENTO debe registrar el usuario", bloque.contains("COLUMN_MOV_USUARIO"));
        assertTrue("MOVIMIENTO debe registrar la fecha", bloque.contains("COLUMN_MOV_FECHA"));
    }

    /** El defecto corregido: la migracion ya no borra tablas con datos. */
    @Test
    public void onUpgradeNoHaceDropTable() {
        String bloque = entre("public void onUpgrade(", "private void insertInitialSeedData");
        assertFalse("La migracion no debe contener DROP TABLE", bloque.contains("DROP TABLE"));
        assertTrue("La migracion debe crear las tablas del inventario",
                bloque.contains("SQL_TABLAS_INVENTARIO"));
    }

    @Test
    public void databaseVersionEsTresOMas() {
        assertTrue("DATABASE_VERSION debe ser 3 o superior",
                fuente.matches("(?s).*DATABASE_VERSION\\s*=\\s*[3-9]\\d*.*"));
    }

    /** Sin esto, Android no valida las claves foraneas y las FK son decorativas. */
    @Test
    public void onConfigureActivaLasClavesForaneas() {
        String bloque = entre("public void onConfigure(", "public void onCreate(");
        assertTrue("onConfigure debe activar las claves foraneas",
                bloque.contains("setForeignKeyConstraintsEnabled"));
    }

    /** Las tablas del inventario deben existir tambien en una instalacion limpia. */
    @Test
    public void onCreateTambienCreaLasTablasDelInventario() {
        String bloque = entre("public void onCreate(", "public void onUpgrade(");
        assertTrue("onCreate debe crear las tablas del inventario",
                bloque.contains("SQL_TABLAS_INVENTARIO"));
    }
}
