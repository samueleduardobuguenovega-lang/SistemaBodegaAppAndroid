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
import java.util.TimeZone;

public class ProductoDao {

    static final String FORMATO_FECHA = "yyyy-MM-dd HH:mm:ss";
    private static final Locale LOCALE_FECHA = Locale.US;
    private static final TimeZone ZONA_FECHA = TimeZone.getTimeZone("UTC");
    private static final int LIMITE_BUSQUEDA = 200;

    private final DatabaseHelper helper;

    public ProductoDao(DatabaseHelper helper) {
        this.helper = helper;
    }

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
                    + ", p." + DatabaseHelper.COLUMN_PROD_CODIGO_BARRAS
                    + ", p." + DatabaseHelper.COLUMN_PROD_UNIDAD_MEDIDA
                    + " FROM " + DatabaseHelper.TABLE_PRODUCTO + " p"
                    + " LEFT JOIN " + DatabaseHelper.TABLE_CATEGORIA + " c"
                    + " ON p." + DatabaseHelper.COLUMN_PROD_ID_CATEGORIA
                    + " = c." + DatabaseHelper.COLUMN_CAT_ID
                    + " LEFT JOIN " + DatabaseHelper.TABLE_PROVEEDOR + " pr"
                    + " ON p." + DatabaseHelper.COLUMN_PROD_ID_PROVEEDOR
                    + " = pr." + DatabaseHelper.COLUMN_PROV_ID;

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
            DatabaseHelper.COLUMN_PROV_RAZON_SOCIAL,
            DatabaseHelper.COLUMN_PROD_CODIGO_BARRAS,
            DatabaseHelper.COLUMN_PROD_UNIDAD_MEDIDA
    };

    public List<Producto> getAllProductos() {
        return consultarProductos(SELECT_CON_JOINS
                + " WHERE p." + DatabaseHelper.COLUMN_PROD_ACTIVO + " = 1"
                + " ORDER BY p." + DatabaseHelper.COLUMN_PROD_NOMBRE + " ASC"
                + " LIMIT " + LIMITE_BUSQUEDA, null);
    }

    public List<Producto> getProductosInactivos() {
        return consultarProductos(SELECT_CON_JOINS
                + " WHERE p." + DatabaseHelper.COLUMN_PROD_ACTIVO + " = 0"
                + " ORDER BY p." + DatabaseHelper.COLUMN_PROD_NOMBRE + " ASC"
                + " LIMIT " + LIMITE_BUSQUEDA, null);
    }

    public List<Producto> buscarProductos(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return getAllProductos();
        }
        String patron = "%" + texto.trim() + "%";
        return consultarProductos(SELECT_CON_JOINS
                        + " WHERE (p." + DatabaseHelper.COLUMN_PROD_NOMBRE + " LIKE ?"
                        + " OR p." + DatabaseHelper.COLUMN_PROD_SKU + " LIKE ?)"
                        + " AND p." + DatabaseHelper.COLUMN_PROD_ACTIVO + " = 1"
                        + " ORDER BY p." + DatabaseHelper.COLUMN_PROD_NOMBRE + " ASC"
                        + " LIMIT " + LIMITE_BUSQUEDA,
                new String[]{patron, patron});
    }

    public Producto getProductoById(int idProducto) {
        List<Producto> resultado = consultarProductos(SELECT_CON_JOINS
                        + " WHERE p." + DatabaseHelper.COLUMN_PROD_ID + " = ?"
                        + " AND p." + DatabaseHelper.COLUMN_PROD_ACTIVO + " = 1",
                new String[]{String.valueOf(idProducto)});
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public boolean existeCodigoBarrasEnOtroProducto(String codigoBarras, int idProductoExcluido) {
        String sql = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_PRODUCTO
                + " WHERE " + DatabaseHelper.COLUMN_PROD_CODIGO_BARRAS + " = ?"
                + " AND " + DatabaseHelper.COLUMN_PROD_ID + " <> ?";
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(sql,
                new String[]{codigoBarras, String.valueOf(idProductoExcluido)})) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) > 0;
            }
        }
        return false;
    }

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

    public long insertProducto(Producto p) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_PROD_SKU, p.getSkuCodigo() != null ? p.getSkuCodigo().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_NOMBRE, p.getNombre() != null ? p.getNombre().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_DESCRIPCION, p.getDescripcion() != null ? p.getDescripcion().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_PRECIO_COSTO, p.getPrecioCosto());
        cv.put(DatabaseHelper.COLUMN_PROD_PRECIO_VENTA, p.getPrecioVenta());
        if (p.getCodigoBarras() != null && !p.getCodigoBarras().trim().isEmpty()) {
            cv.put(DatabaseHelper.COLUMN_PROD_CODIGO_BARRAS, p.getCodigoBarras().trim());
        } else {
            cv.putNull(DatabaseHelper.COLUMN_PROD_CODIGO_BARRAS);
        }
        if (p.getUnidadMedida() != null && !p.getUnidadMedida().trim().isEmpty()) {
            cv.put(DatabaseHelper.COLUMN_PROD_UNIDAD_MEDIDA, p.getUnidadMedida().trim());
        }
        cv.put(DatabaseHelper.COLUMN_PROD_STOCK_ACTUAL, p.getStockActual());
        cv.put(DatabaseHelper.COLUMN_PROD_STOCK_MINIMO, p.getStockMinimo());
        cv.put(DatabaseHelper.COLUMN_PROD_ID_CATEGORIA, p.getIdCategoria());
        if (p.getIdProveedor() > 0) {
            cv.put(DatabaseHelper.COLUMN_PROD_ID_PROVEEDOR, p.getIdProveedor());
        } else {
            cv.putNull(DatabaseHelper.COLUMN_PROD_ID_PROVEEDOR);
        }
        cv.put(DatabaseHelper.COLUMN_PROD_ACTIVO, p.getActivo());
        try {
            return db.insert(DatabaseHelper.TABLE_PRODUCTO, null, cv);
        } catch (SQLiteConstraintException e) {
            return -1;
        }
    }

    public long insertarProductoConStockInicial(Producto p, int stockInicial, String usuario,
                                                int idProveedor) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long id = insertProducto(p);
            if (id == -1) {
                return -1;
            }
            if (stockInicial > 0
                    && !aplicarMovimientoEnTransaccionAbierta(db, (int) id, stockInicial,
                    TipoMovimiento.ENTRADA, usuario, idProveedor, "INVENTARIO INICIAL", "Stock de apertura")) {
                return -1;
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public boolean updateProducto(Producto p) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_PROD_SKU, p.getSkuCodigo() != null ? p.getSkuCodigo().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_NOMBRE, p.getNombre() != null ? p.getNombre().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_DESCRIPCION, p.getDescripcion() != null ? p.getDescripcion().trim() : "");
        cv.put(DatabaseHelper.COLUMN_PROD_PRECIO_COSTO, p.getPrecioCosto());
        cv.put(DatabaseHelper.COLUMN_PROD_PRECIO_VENTA, p.getPrecioVenta());
        if (p.getCodigoBarras() != null && !p.getCodigoBarras().trim().isEmpty()) {
            cv.put(DatabaseHelper.COLUMN_PROD_CODIGO_BARRAS, p.getCodigoBarras().trim());
        } else {
            cv.putNull(DatabaseHelper.COLUMN_PROD_CODIGO_BARRAS);
        }
        if (p.getUnidadMedida() != null && !p.getUnidadMedida().trim().isEmpty()) {
            cv.put(DatabaseHelper.COLUMN_PROD_UNIDAD_MEDIDA, p.getUnidadMedida().trim());
        }
        cv.put(DatabaseHelper.COLUMN_PROD_STOCK_MINIMO, p.getStockMinimo());
        cv.put(DatabaseHelper.COLUMN_PROD_ID_CATEGORIA, p.getIdCategoria());
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
            return false;
        }
    }

    public boolean updateProductoEstado(int idProducto, int activo) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_PROD_ACTIVO, activo);
        int filas = db.update(DatabaseHelper.TABLE_PRODUCTO, cv,
                DatabaseHelper.COLUMN_PROD_ID + " = ?",
                new String[]{String.valueOf(idProducto)});
        return filas > 0;
    }

    // Sobrecarga de compatibilidad original (sin proveedor, sin documento, sin motivo)
    public boolean actualizarStock(int idProducto, int cantidad, TipoMovimiento tipo, String usuario) {
        return actualizarStock(idProducto, cantidad, tipo, usuario, 0, "", "");
    }

    // Sobrecarga de compatibilidad anterior (con proveedor, sin documento, sin motivo)
    public boolean actualizarStock(int idProducto, int cantidad, TipoMovimiento tipo, String usuario, int idProveedor) {
        return actualizarStock(idProducto, cantidad, tipo, usuario, idProveedor, "", "");
    }

    // NUEVO: Método que recibe documento y motivo para CU-04 / CU-05
    public boolean actualizarStock(int idProducto, int cantidad, TipoMovimiento tipo, String usuario, int idProveedor, String documento, String motivo) {
        if (tipo == null || cantidad <= 0) {
            return false;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (!aplicarMovimientoEnTransaccionAbierta(db, idProducto, cantidad, tipo, usuario, idProveedor, documento, motivo)) {
                return false;
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // NUEVO: Método adaptado con documento y motivo
    private boolean aplicarMovimientoEnTransaccionAbierta(SQLiteDatabase db, int idProducto,
                                                          int cantidad, TipoMovimiento tipo,
                                                          String usuario, int idProveedor,
                                                          String documento, String motivo) {
        if (tipo == null || cantidad <= 0) {
            return false;
        }
        int stockActual = leerStockActual(db, idProducto);
        if (stockActual < 0) {
            return false;
        }
        int stockNuevo;
        if (tipo == TipoMovimiento.ENTRADA) {
            stockNuevo = stockActual + cantidad;
        } else {
            if (cantidad > stockActual) {
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
        cvMov.put(DatabaseHelper.COLUMN_MOV_DOCUMENTO, documento != null ? documento : "");
        cvMov.put(DatabaseHelper.COLUMN_MOV_MOTIVO, motivo != null ? motivo : "");

        if (idProveedor > 0) {
            cvMov.put(DatabaseHelper.COLUMN_MOV_ID_PROVEEDOR, idProveedor);
        } else {
            cvMov.putNull(DatabaseHelper.COLUMN_MOV_ID_PROVEEDOR);
        }
        long idMov = db.insert(DatabaseHelper.TABLE_MOVIMIENTO, null, cvMov);
        return idMov != -1;
    }

    public List<Movimiento> getMovimientosByProducto(int idProducto) {
        try {
            return leerMovimientos(idProducto, true, true);
        } catch (android.database.SQLException columnaSinMigrar) {
            // Manejo de migración incremental por si fallan las columnas
            try {
                return leerMovimientos(idProducto, true, false); // Intenta con proveedor pero sin doc/motivo
            } catch (android.database.SQLException e2) {
                return leerMovimientos(idProducto, false, false); // Base muy antigua
            }
        }
    }

    // NUEVO: Agregado parámetro conNuevosCampos para manejar lectura de documento y motivo
    private List<Movimiento> leerMovimientos(int idProducto, boolean conProveedor, boolean conNuevosCampos) {
        List<Movimiento> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
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
                + (conNuevosCampos
                ? ", m." + DatabaseHelper.COLUMN_MOV_DOCUMENTO
                  + ", m." + DatabaseHelper.COLUMN_MOV_MOTIVO
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

        int indiceProveedor = cursor.getColumnIndex(DatabaseHelper.COLUMN_MOV_ID_PROVEEDOR);
        int indiceRazonSocial = cursor.getColumnIndex(DatabaseHelper.COLUMN_PROV_RAZON_SOCIAL);
        int indiceDocumento = cursor.getColumnIndex(DatabaseHelper.COLUMN_MOV_DOCUMENTO);
        int indiceMotivo = cursor.getColumnIndex(DatabaseHelper.COLUMN_MOV_MOTIVO);

        if (cursor.moveToFirst()) {
            do {
                String doc = (indiceDocumento >= 0 && !cursor.isNull(indiceDocumento)) ? cursor.getString(indiceDocumento) : "";
                String mot = (indiceMotivo >= 0 && !cursor.isNull(indiceMotivo)) ? cursor.getString(indiceMotivo) : "";

                Movimiento movimiento = new Movimiento(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        indiceProveedor >= 0 ? cursor.getInt(indiceProveedor) : 0,
                        (indiceRazonSocial >= 0 && !cursor.isNull(indiceRazonSocial)) ? cursor.getString(indiceRazonSocial) : "",
                        doc,
                        mot);
                list.add(movimiento);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public List<Producto> getProductosStockBajo() {
        return consultarProductos(SELECT_CON_JOINS
                + " WHERE p." + DatabaseHelper.COLUMN_PROD_ACTIVO + " = 1"
                + " AND p." + DatabaseHelper.COLUMN_PROD_STOCK_ACTUAL
                + " <= p." + DatabaseHelper.COLUMN_PROD_STOCK_MINIMO
                + " ORDER BY p." + DatabaseHelper.COLUMN_PROD_NOMBRE + " ASC", null);
    }

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
                p.setCodigoBarras(cursor.getString(13));
                p.setUnidadMedida(cursor.getString(14));
                list.add(p);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

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

    private String fechaActual() {
        SimpleDateFormat formato = new SimpleDateFormat(FORMATO_FECHA, LOCALE_FECHA);
        formato.setTimeZone(ZONA_FECHA);
        return formato.format(new Date());
    }
}