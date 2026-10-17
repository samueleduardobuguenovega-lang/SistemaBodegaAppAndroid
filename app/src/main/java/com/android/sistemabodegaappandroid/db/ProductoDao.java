package com.android.sistemabodegaappandroid.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.android.sistemabodegaappandroid.model.Movimiento;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.model.TipoMovimiento;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Acceso a datos de PRODUCTO y MOVIMIENTO.
 *
 * Está separado de {@link DatabaseHelper} a propósito: el helper ya concentra
 * roles, usuarios y categorías, y aislar el módulo de productos evita que
 * dos frentes de trabajo editen el mismo archivo y choquen en la fusión.
 * Solo el puente reactivo RF-04/RF-05 ({@code observarProductos}) vive en el
 * helper; las consultas, incluida la búsqueda, siguen aquí.
 * Recibe el helper en el constructor y usa sus propias constantes de tabla.
 */
public class ProductoDao {

    /** Formato de fecha persistido en MOVIMIENTO.fecha (ISO-8601). */
    private static final String FORMATO_FECHA = "yyyy-MM-dd HH:mm:ss";

    private final DatabaseHelper helper;

    public ProductoDao(DatabaseHelper helper) {
        this.helper = helper;
    }

    /** JOIN con CATEGORIA y PROVEEDOR para traer los nombres resueltos en una sola consulta. */
    private static final String SELECT_CON_JOINS =
            "SELECT p." + DatabaseHelper.COLUMN_PROD_ID
                    + ", p." + DatabaseHelper.COLUMN_PROD_SKU
                    + ", p." + DatabaseHelper.COLUMN_PROD_NOMBRE
                    + ", p." + DatabaseHelper.COLUMN_PROD_DESCRIPCION
                    + ", p." + DatabaseHelper.COLUMN_PROD_PRECIO_COSTO
                    + ", p." + DatabaseHelper.COLUMN_PROD_PRECIO_VENTA
                    + ", p." + DatabaseHelper.COLUMN_PROD_STOCK_ACTUAL
                    + ", p." + DatabaseHelper.COLUMN_PROD_STOCK_MINIMO
                    + ", p." + DatabaseHelper.COLUMN_PROD_ID_CATEGORIA
                    + ", p." + DatabaseHelper.COLUMN_PROD_ID_PROVEEDOR
                    + ", p." + DatabaseHelper.COLUMN_PROD_ACTIVO
                    + ", c." + DatabaseHelper.COLUMN_CAT_NOMBRE
                    + ", pr." + DatabaseHelper.COLUMN_PROV_RAZON_SOCIAL
                    + " FROM " + DatabaseHelper.TABLE_PRODUCTO + " p"
                    + " LEFT JOIN " + DatabaseHelper.TABLE_CATEGORIA + " c"
                    + " ON p." + DatabaseHelper.COLUMN_PROD_ID_CATEGORIA
                    + " = c." + DatabaseHelper.COLUMN_CAT_ID
                    + " LEFT JOIN " + DatabaseHelper.TABLE_PROVEEDOR + " pr"
                    + " ON p." + DatabaseHelper.COLUMN_PROD_ID_PROVEEDOR
                    + " = pr." + DatabaseHelper.COLUMN_PROV_ID;

    /** Índices de columnas del SELECT_CON_JOINS, en el mismo orden. */
    private static final String[] COLUMNAS_SELECT = {
            DatabaseHelper.COLUMN_PROD_ID,
            DatabaseHelper.COLUMN_PROD_SKU,
            DatabaseHelper.COLUMN_PROD_NOMBRE,
            DatabaseHelper.COLUMN_PROD_DESCRIPCION,
            DatabaseHelper.COLUMN_PROD_PRECIO_COSTO,
            DatabaseHelper.COLUMN_PROD_PRECIO_VENTA,
            DatabaseHelper.COLUMN_PROD_STOCK_ACTUAL,
            DatabaseHelper.COLUMN_PROD_STOCK_MINIMO,
            DatabaseHelper.COLUMN_PROD_ID_CATEGORIA,
            DatabaseHelper.COLUMN_PROD_ID_PROVEEDOR,
            DatabaseHelper.COLUMN_PROD_ACTIVO,
            DatabaseHelper.COLUMN_CAT_NOMBRE,
            DatabaseHelper.COLUMN_PROV_RAZON_SOCIAL
    };

    /** Listado de productos habilitados, ordenado alfabéticamente. */
    public List<Producto> getAllProductos() {
        return consultarProductos(SELECT_CON_JOINS
                + " WHERE p." + DatabaseHelper.COLUMN_PROD_ACTIVO + " = 1"
                + " ORDER BY p." + DatabaseHelper.COLUMN_PROD_NOMBRE + " ASC", null);
    }

    /**
     * Búsqueda por nombre o SKU entre los productos habilitados.
     * Un texto nulo o vacío degrada al listado completo para no filtrar de más.
     */
    public List<Producto> buscarProductos(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return getAllProductos();
        }
        String patron = "%" + texto.trim() + "%";
        return consultarProductos(SELECT_CON_JOINS
                + " WHERE (p." + DatabaseHelper.COLUMN_PROD_NOMBRE + " LIKE ?"
                + " OR p." + DatabaseHelper.COLUMN_PROD_SKU + " LIKE ?)"
                + " AND p." + DatabaseHelper.COLUMN_PROD_ACTIVO + " = 1"
                + " ORDER BY p." + DatabaseHelper.COLUMN_PROD_NOMBRE + " ASC",
                new String[]{patron, patron});
    }

    /** Devuelve null cuando el id no existe o el producto está deshabilitado. */
    public Producto getProductoById(int idProducto) {
        List<Producto> resultado = consultarProductos(SELECT_CON_JOINS
                        + " WHERE p." + DatabaseHelper.COLUMN_PROD_ID + " = ?"
                        + " AND p." + DatabaseHelper.COLUMN_PROD_ACTIVO + " = 1",
                new String[]{String.valueOf(idProducto)});
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    /**
     * RF-03: comprueba si el SKU ya pertenece a OTRO producto, sin filtrar por
     * {@code activo}.
     *
     * El índice UNIQUE de {@code sku_codigo} es global: un producto deshabilitado
     * sigue ocupando su SKU, así que validarlo solo entre los habilitados
     * (como hacía {@code buscarProductos}) deja pasar una violación de la
     * restricción y la app cae. La comparación usa {@code =} con collación
     * binaria, igual que el propio índice UNIQUE, para no marcar como duplicado
     * un SKU que la base sí aceptaría. El SKU viaja como argumento parametrizado,
     * nunca concatenado en el SQL.
     *
     * @return true si otro producto (habilitado o no) ya usa ese SKU.
     */
    public boolean existeSkuEnOtroProducto(String sku, int idProductoExcluido) {
        if (sku == null) {
            return false;
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_PRODUCTO
                        + " WHERE " + DatabaseHelper.COLUMN_PROD_SKU + " = ?"
                        + " AND " + DatabaseHelper.COLUMN_PROD_ID + " <> ?",
                new String[]{sku.trim(), String.valueOf(idProductoExcluido)});
        boolean existe = cursor.moveToFirst() && cursor.getInt(0) > 0;
        cursor.close();
        return existe;
    }

    /**
     * Inserta un producto nuevo.
     * Devuelve -1 si el SKU ya está registrado: la unicidad la garantiza el
     * índice UNIQUE de la tabla y aquí se traduce a un código amigable.
     */
    public long insertProducto(Producto p) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_PROD_SKU, p.getSkuCodigo() != null ? p.getSkuCodigo().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_NOMBRE, p.getNombre() != null ? p.getNombre().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_DESCRIPCION, p.getDescripcion() != null ? p.getDescripcion().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_PRECIO_COSTO, p.getPrecioCosto());
        cv.put(DatabaseHelper.COLUMN_PROD_PRECIO_VENTA, p.getPrecioVenta());
        cv.put(DatabaseHelper.COLUMN_PROD_STOCK_ACTUAL, p.getStockActual());
        cv.put(DatabaseHelper.COLUMN_PROD_STOCK_MINIMO, p.getStockMinimo());
        cv.put(DatabaseHelper.COLUMN_PROD_ID_CATEGORIA, p.getIdCategoria());
        // El formulario manda 0 cuando el usuario elige "Sin proveedor". 0 no es
        // NULL y no existe ninguna fila PROVEEDOR con ese id (AUTOINCREMENT
        // arranca en 1), asi que con las claves foraneas activas el alta fallaba
        // siempre con "FOREIGN KEY constraint failed". El proveedor es opcional
        // en el esquema (columna nullable): se guarda NULL, nunca 0.
        if (p.getIdProveedor() > 0) {
            cv.put(DatabaseHelper.COLUMN_PROD_ID_PROVEEDOR, p.getIdProveedor());
        } else {
            cv.putNull(DatabaseHelper.COLUMN_PROD_ID_PROVEEDOR);
        }
        cv.put(DatabaseHelper.COLUMN_PROD_ACTIVO, p.getActivo());
        try {
            return db.insert(DatabaseHelper.TABLE_PRODUCTO, null, cv);
        } catch (SQLiteConstraintException e) {
            // El UNIQUE de sku_codigo ya existía: el caller debe mostrar el error.
            return -1;
        }
    }

    /**
     * Actualiza los datos editables del producto; devuelve false si no había fila
     * o si el SKU choca con el UNIQUE global de la tabla.
     */
    public boolean updateProducto(Producto p) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_PROD_SKU, p.getSkuCodigo() != null ? p.getSkuCodigo().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_NOMBRE, p.getNombre() != null ? p.getNombre().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_DESCRIPCION, p.getDescripcion() != null ? p.getDescripcion().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_PRECIO_COSTO, p.getPrecioCosto());
        cv.put(DatabaseHelper.COLUMN_PROD_PRECIO_VENTA, p.getPrecioVenta());
        cv.put(DatabaseHelper.COLUMN_PROD_STOCK_MINIMO, p.getStockMinimo());
        cv.put(DatabaseHelper.COLUMN_PROD_ID_CATEGORIA, p.getIdCategoria());
        // Misma razon que en el alta: "Sin proveedor" llega como 0 y la clave
        // foranea lo rechaza. 0 significa "sin proveedor", que en el esquema es
        // NULL.
        if (p.getIdProveedor() > 0) {
            cv.put(DatabaseHelper.COLUMN_PROD_ID_PROVEEDOR, p.getIdProveedor());
        } else {
            cv.putNull(DatabaseHelper.COLUMN_PROD_ID_PROVEEDOR);
        }
        cv.put(DatabaseHelper.COLUMN_PROD_ACTIVO, p.getActivo());
        try {
            int filas = db.update(DatabaseHelper.TABLE_PRODUCTO, cv,
                    DatabaseHelper.COLUMN_PROD_ID + " = ?",
                    new String[]{String.valueOf(p.getIdProducto())});
            return filas > 0;
        } catch (SQLiteConstraintException e) {
            // Respaldo del pre-chequeo: si el SKU sigue ocupado por otro producto
            // (por una carrera o por un dato que el pre-chequeo no vio), el UNIQUE
            // global lo rechaza. Se traduce a false en vez de dejar volar la
            // excepción y que la app se caiga; el caller muestra el error.
            return false;
        }
    }

    /**
     * RF-08: baja lógica de un producto (activo = 0).
     * No se borra la fila para no comprometer la integridad referencial de los
     * movimientos pasados: el producto solo deja de listarse y de ofrecerse en
     * nuevas operaciones, pero su historial sigue siendo auditable.
     */
    public boolean updateProductoEstado(int idProducto, int activo) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_PROD_ACTIVO, activo);
        int filas = db.update(DatabaseHelper.TABLE_PRODUCTO, cv,
                DatabaseHelper.COLUMN_PROD_ID + " = ?",
                new String[]{String.valueOf(idProducto)});
        return filas > 0;
    }

    /**
     * Aplica un movimiento de stock sin registrar proveedor.
     * Se conserva para los llamados que no asocian proveedor y delega en la
     * sobrecarga completa con idProveedor = 0.
     */
    public boolean actualizarStock(int idProducto, int cantidad, TipoMovimiento tipo, String usuario) {
        return actualizarStock(idProducto, cantidad, tipo, usuario, 0);
    }

    /**
     * Aplica un movimiento de stock dentro de una transacción y deja constancia
     * del proveedor de la entrada (Gestión de Proveedores: soporte a compras).
     * Una SALIDA que dejaría el stock en negativo se rechaza sin escribir nada,
     * de modo que el inventario nunca quede inconsistentemente bajo cero.
     *
     * @param idProveedor proveedor de la ENTRADA; 0 o negativo persiste como NULL.
     * @return true si el stock y el MOVIMIENTO quedaron registrados.
     */
    public boolean actualizarStock(int idProducto, int cantidad, TipoMovimiento tipo, String usuario, int idProveedor) {
        if (tipo == null || cantidad <= 0) {
            return false;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            int stockActual = leerStockActual(db, idProducto);
            if (stockActual < 0) {
                // Producto inexistente: no hay nada que mover.
                return false;
            }
            int stockNuevo;
            if (tipo == TipoMovimiento.ENTRADA) {
                stockNuevo = stockActual + cantidad;
            } else {
                if (cantidad > stockActual) {
                    // Salida mayor al stock disponible: se aborta antes de escribir.
                    return false;
                }
                stockNuevo = stockActual - cantidad;
            }

            ContentValues cvStock = new ContentValues();
            cvStock.put(DatabaseHelper.COLUMN_PROD_STOCK_ACTUAL, stockNuevo);
            int filas = db.update(DatabaseHelper.TABLE_PRODUCTO, cvStock,
                    DatabaseHelper.COLUMN_PROD_ID + " = ?",
                    new String[]{String.valueOf(idProducto)});
            if (filas == 0) {
                return false;
            }

            ContentValues cvMov = new ContentValues();
            cvMov.put(DatabaseHelper.COLUMN_MOV_ID_PRODUCTO, idProducto);
            cvMov.put(DatabaseHelper.COLUMN_MOV_TIPO, tipo.getValor());
            cvMov.put(DatabaseHelper.COLUMN_MOV_CANTIDAD, cantidad);
            cvMov.put(DatabaseHelper.COLUMN_MOV_USUARIO, usuario != null ? usuario : "");
            cvMov.put(DatabaseHelper.COLUMN_MOV_FECHA, fechaActual());
            // Solo la entrada registra proveedor; 0 o negativo se guarda como NULL.
            if (idProveedor > 0) {
                cvMov.put(DatabaseHelper.COLUMN_MOV_ID_PROVEEDOR, idProveedor);
            } else {
                cvMov.putNull(DatabaseHelper.COLUMN_MOV_ID_PROVEEDOR);
            }
            long idMov = db.insert(DatabaseHelper.TABLE_MOVIMIENTO, null, cvMov);
            if (idMov == -1) {
                return false;
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    /** Historial de movimientos de un producto, del más reciente al más antiguo. */
    public List<Movimiento> getMovimientosByProducto(int idProducto) {
        try {
            return leerMovimientos(idProducto, true);
        } catch (android.database.SQLException columnaSinMigrar) {
            // Base que aún no migró a v4: el historial sigue siendo consultable,
            // solo que sin el proveedor de la entrada.
            return leerMovimientos(idProducto, false);
        }
    }

    /**
     * Lee el historial; conProveedor=false omite la columna agregada en v4.
     *
     * La entrada con proveedor se resuelve con un LEFT JOIN a PROVEEDOR para
     * mostrar la razón social en el historial: los movimientos sin proveedor
     * quedan con el nombre vacío, sin romper la lectura.
     */
    private List<Movimiento> leerMovimientos(int idProducto, boolean conProveedor) {
        List<Movimiento> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        // Los alias "m" y "pr" son necesarios: id_proveedor existe en ambas
        // tablas y sin calificar la columna el SQL sería ambiguo.
        String sql = "SELECT m." + DatabaseHelper.COLUMN_MOV_ID
                + ", m." + DatabaseHelper.COLUMN_MOV_ID_PRODUCTO
                + ", m." + DatabaseHelper.COLUMN_MOV_TIPO
                + ", m." + DatabaseHelper.COLUMN_MOV_CANTIDAD
                + ", m." + DatabaseHelper.COLUMN_MOV_USUARIO
                + ", m." + DatabaseHelper.COLUMN_MOV_FECHA
                + (conProveedor
                        ? ", m." + DatabaseHelper.COLUMN_MOV_ID_PROVEEDOR
                                + ", pr." + DatabaseHelper.COLUMN_PROV_RAZON_SOCIAL
                        : "")
                + " FROM " + DatabaseHelper.TABLE_MOVIMIENTO + " m"
                + (conProveedor
                        ? " LEFT JOIN " + DatabaseHelper.TABLE_PROVEEDOR + " pr"
                                + " ON m." + DatabaseHelper.COLUMN_MOV_ID_PROVEEDOR
                                + " = pr." + DatabaseHelper.COLUMN_PROV_ID
                        : "")
                + " WHERE m." + DatabaseHelper.COLUMN_MOV_ID_PRODUCTO + " = ?"
                + " ORDER BY m." + DatabaseHelper.COLUMN_MOV_FECHA + " DESC";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idProducto)});
        // Si la columna no está, getColumnIndex devuelve -1 y el id queda en 0.
        int indiceProveedor = cursor.getColumnIndex(DatabaseHelper.COLUMN_MOV_ID_PROVEEDOR);
        int indiceRazonSocial = cursor.getColumnIndex(DatabaseHelper.COLUMN_PROV_RAZON_SOCIAL);
        if (cursor.moveToFirst()) {
            do {
                Movimiento movimiento = new Movimiento(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        indiceProveedor >= 0 ? cursor.getInt(indiceProveedor) : 0,
                        // El LEFT JOIN deja NULL cuando no hubo proveedor: nunca
                        // se muestra "null" en el historial.
                        (indiceRazonSocial >= 0 && !cursor.isNull(indiceRazonSocial))
                                ? cursor.getString(indiceRazonSocial) : "");
                list.add(movimiento);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /** Productos habilitados cuyo stock igualó o bajó del mínimo: alertas de reposición. */
    public List<Producto> getProductosStockBajo() {
        return consultarProductos(SELECT_CON_JOINS
                + " WHERE p." + DatabaseHelper.COLUMN_PROD_ACTIVO + " = 1"
                + " AND p." + DatabaseHelper.COLUMN_PROD_STOCK_ACTUAL
                + " <= p." + DatabaseHelper.COLUMN_PROD_STOCK_MINIMO
                + " ORDER BY p." + DatabaseHelper.COLUMN_PROD_NOMBRE + " ASC", null);
    }

    // --- helpers internos ---

    /** Ejecuta el SELECT con JOINs y mapea cada fila a un Producto. */
    private List<Producto> consultarProductos(String sql, String[] args) {
        List<Producto> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, args);
        if (cursor.moveToFirst()) {
            do {
                Producto p = new Producto();
                p.setIdProducto(cursor.getInt(0));
                p.setSkuCodigo(cursor.getString(1));
                p.setNombre(cursor.getString(2));
                p.setDescripcion(cursor.getString(3));
                p.setPrecioCosto(cursor.getDouble(4));
                p.setPrecioVenta(cursor.getDouble(5));
                p.setStockActual(cursor.getInt(6));
                p.setStockMinimo(cursor.getInt(7));
                p.setIdCategoria(cursor.getInt(8));
                p.setIdProveedor(cursor.getInt(9));
                p.setActivo(cursor.getInt(10));
                p.setNombreCategoria(cursor.getString(11));
                p.setRazonSocialProveedor(cursor.getString(12));
                list.add(p);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /** Lee el stock actual; devuelve -1 si el producto no existe. */
    private int leerStockActual(SQLiteDatabase db, int idProducto) {
        Cursor cursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_PROD_STOCK_ACTUAL
                        + " FROM " + DatabaseHelper.TABLE_PRODUCTO
                        + " WHERE " + DatabaseHelper.COLUMN_PROD_ID + " = ?",
                new String[]{String.valueOf(idProducto)});
        int stock = -1;
        if (cursor.moveToFirst()) {
            stock = cursor.getInt(0);
        }
        cursor.close();
        return stock;
    }

    /** Marca de tiempo local en el formato que espera MOVIMIENTO.fecha. */
    private String fechaActual() {
        return new SimpleDateFormat(FORMATO_FECHA, Locale.getDefault()).format(new Date());
    }
}
