package com.android.sistemabodegaappandroid.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.android.sistemabodegaappandroid.db.DatabaseHelper;

/**
 * Datos de demostración para la presentación.
 *
 * Reglas:
 *  - Solo siembra si las tablas de inventario están vacías: nunca pisa datos reales.
 *  - Deja una marca en SharedPreferences, así el usuario puede borrar los productos
 *    sin que reaparezcan en el siguiente arranque.
 *  - Se ejecuta en un hilo de fondo (ver LoginActivity): no bloquea la UI.
 */
public final class DemoSeeder {

    private static final String PREFS = "demo";
    private static final String CLAVE = "sembrado_v1";
    private static final String CLAVE_USUARIOS = "usuarios_sembrado_v1";
    private static final String TAG = "DemoSeeder";

    private DemoSeeder() {
    }

    /** Lanza la siembra en un hilo de fondo, una sola vez por instalación. */
    public static void sembrarEnSegundoPlano(final Context contexto) {
        final Context app = contexto.getApplicationContext();
        new Thread(() -> {
            try {
                sembrarSiHaceFalta(app);
            } catch (Exception e) {
                Log.w(TAG, "no se pudieron sembrar los datos de demo", e);
            }
        }, "demo-seeder").start();
    }

    private static void sembrarSiHaceFalta(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        DatabaseHelper helper = new DatabaseHelper(ctx);
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            // Usuarios y roles de demostración: tienen su propia marca, así una
            // instalación con inventario cargado igual recibe las cuentas.
            sembrarUsuariosSiHaceFalta(db, prefs);

            if (prefs.getBoolean(CLAVE, false)) {
                return;
            }
            if (contar(db, "PRODUCTO") > 0 || contar(db, "PROVEEDOR") > 0) {
                prefs.edit().putBoolean(CLAVE, true).apply();
                return;
            }
            db.beginTransaction();
            try {
                long[] categorias = sembrarCategorias(db);
                long[] proveedores = sembrarProveedores(db);
                sembrarProductos(db, categorias, proveedores);
                sembrarMovimientos(db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            prefs.edit().putBoolean(CLAVE, true).apply();
            Log.i(TAG, "datos de demostración cargados");
        } finally {
            db.close();
            helper.close();
        }
    }

    /**
     * Crea cuentas de demostración, una por rol, más una inactiva.
     * Solo actúa si la base tiene el admin por defecto y nada más.
     */
    private static void sembrarUsuariosSiHaceFalta(SQLiteDatabase db, SharedPreferences prefs) {
        if (prefs.getBoolean(CLAVE_USUARIOS, false)) {
            return;
        }
        if (contar(db, "USUARIO") > 1) {
            prefs.edit().putBoolean(CLAVE_USUARIOS, true).apply();
            return;
        }
        int rolAdmin = idRolPorNombre(db, "Administrador");
        int rolOperador = idRolPorNombre(db, "Operador");
        int rolVendedor = idRolPorNombre(db, "Vendedor");
        if (rolAdmin <= 0) {
            return;
        }
        db.beginTransaction();
        try {
            insertarUsuario(db, "supervisor", "Supervisora de Local", "3333", rolAdmin, 1);
            insertarUsuario(db, "bodega", "Bodeguero Turno Tarde", "2222",
                    rolOperador > 0 ? rolOperador : rolAdmin, 1);
            insertarUsuario(db, "cajero", "Cajera Turno Mañana", "1111",
                    rolVendedor > 0 ? rolVendedor : rolAdmin, 1);
            insertarUsuario(db, "temporal", "Refuerzo Fin de Semana", "4444",
                    rolVendedor > 0 ? rolVendedor : rolAdmin, 0);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        prefs.edit().putBoolean(CLAVE_USUARIOS, true).apply();
        Log.i(TAG, "usuarios de demostración cargados");
    }

    private static int idRolPorNombre(SQLiteDatabase db, String nombre) {
        try (Cursor c = db.rawQuery("SELECT id_rol FROM ROL WHERE nombre_rol = ?",
                new String[]{nombre})) {
            return c.moveToFirst() ? c.getInt(0) : -1;
        }
    }

    private static void insertarUsuario(SQLiteDatabase db, String usuario, String nombre,
                                        String clave, int idRol, int activo) {
        ContentValues v = new ContentValues();
        v.put("username", usuario);
        v.put("nombre_completo", nombre);
        v.put("password_hash", PasswordUtils.hashPassword(clave));
        v.put("id_rol", idRol);
        v.put("activo", activo);
        db.insert("USUARIO", null, v);
    }

    private static int contar(SQLiteDatabase db, String tabla) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + tabla, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private static long insertarCategoria(SQLiteDatabase db, String nombre, String descripcion) {
        ContentValues v = new ContentValues();
        v.put("nombre_categoria", nombre);
        v.put("descripcion", descripcion);
        v.put("activo", 1);
        return db.insert("CATEGORIA", null, v);
    }

    private static long insertarProveedor(SQLiteDatabase db, String razon, String rut,
                                          String email, String telefono, String direccion) {
        ContentValues v = new ContentValues();
        v.put("razon_social", razon);
        v.put("rut", rut);
        v.put("email", email);
        v.put("telefono", telefono);
        v.put("direccion", direccion);
        v.put("activo", 1);
        return db.insert("PROVEEDOR", null, v);
    }

    private static long[] sembrarCategorias(SQLiteDatabase db) {
        return new long[]{
                insertarCategoria(db, "Bebidas", "Aguas, jugos y bebidas gaseosas"),
                insertarCategoria(db, "Lácteos", "Leche, yogur y quesos"),
                insertarCategoria(db, "Abarrotes", "Arroz, fideos, aceite y conservas"),
                insertarCategoria(db, "Snacks", "Galletas, papas fritas y dulces"),
                insertarCategoria(db, "Limpieza", "Detergente, cloro y aseo del local")
        };
    }

    private static long[] sembrarProveedores(SQLiteDatabase db) {
        return new long[]{
                insertarProveedor(db, "Distribuidora Andina SpA", "76.543.210-8",
                        "ventas@andina.cl", "+56 9 8877 1122",
                        "Av. La Serena 1450, Coquimbo"),
                insertarProveedor(db, "Lácteos del Valle Ltda", "77.123.456-9",
                        "pedidos@lacteosdelvalle.cl", "+56 9 5544 3322",
                        "Camino El Romero 220, Ovalle"),
                insertarProveedor(db, "Comercial Ovalle Ltda", "78.987.654-3",
                        "contacto@comercialovalle.cl", "+56 53 234 5678",
                        "Benjamín Vicuña Mackenna 320, Ovalle"),
                insertarProveedor(db, "Snacks del Norte SpA", "79.456.123-5",
                        "rutas@snacksnorte.cl", "+56 9 6611 9900",
                        "Ruta D-43 km 4, Ovalle")
        };
    }

    private static void sembrarProductos(SQLiteDatabase db, long[] cat, long[] prov) {
        // nombre, sku, barra, unidad, costo, venta, stock, minimo, categoria, proveedor
        Object[][] filas = {
                {"Agua mineral 1.5 L", "BEB-001", "7801234500017", "UN", 690, 1190, 48, 12, cat[0], prov[0]},
                {"Bebida cola 1.5 L", "BEB-002", "7801234500024", "UN", 1150, 1990, 36, 12, cat[0], prov[0]},
                {"Jugo de naranja 1 L", "BEB-003", "7801234500031", "UN", 890, 1490, 8, 12, cat[0], prov[0]},
                {"Leche entera 1 L", "LAC-001", "7801234500048", "UN", 780, 1250, 60, 20, cat[1], prov[1]},
                {"Yogur natural 1 L", "LAC-002", "7801234500055", "UN", 990, 1690, 15, 10, cat[1], prov[1]},
                {"Queso gauda 250 g", "LAC-003", "7801234500062", "UN", 1850, 2790, 0, 6, cat[1], prov[1]},
                {"Arroz grado 1 1 kg", "ABA-001", "7801234500079", "KG", 1050, 1490, 40, 15, cat[2], prov[2]},
                {"Fideos spaghetti 400 g", "ABA-002", "7801234500086", "UN", 690, 990, 52, 15, cat[2], prov[2]},
                {"Aceite vegetal 900 ml", "ABA-003", "7801234500093", "UN", 1980, 2890, 6, 10, cat[2], prov[2]},
                {"Galletas surtidas 120 g", "SNK-001", "7801234500109", "UN", 550, 890, 70, 20, cat[3], prov[3]},
                {"Papas fritas 180 g", "SNK-002", "7801234500116", "UN", 990, 1590, 3, 12, cat[3], prov[3]},
                {"Detergente 900 ml", "LIM-001", "7801234500123", "UN", 1690, 2490, 18, 8, cat[4], prov[2]}
        };
        for (Object[] f : filas) {
            ContentValues v = new ContentValues();
            v.put("nombre", (String) f[0]);
            v.put("sku_codigo", (String) f[1]);
            v.put("codigo_barras", (String) f[2]);
            v.put("unidad_medida", (String) f[3]);
            v.put("precio_costo", (int) f[4]);
            v.put("precio_venta", (int) f[5]);
            v.put("stock_actual", (int) f[6]);
            v.put("stock_minimo", (int) f[7]);
            v.put("id_categoria", (long) f[8]);
            v.put("id_proveedor", (long) f[9]);
            v.put("activo", 1);
            db.insert("PRODUCTO", null, v);
        }
    }

    private static void sembrarMovimientos(SQLiteDatabase db) {
        // tipo, cantidad, documento, motivo, dias atras
        Object[][] movs = {
                {"ENTRADA", 48, "FACT-10231", "Recepción de proveedor", 6},
                {"ENTRADA", 60, "FACT-10244", "Recepción de proveedor", 5},
                {"SALIDA", 6, null, "Venta en caja", 4},
                {"ENTRADA", 40, "FACT-10265", "Recepción de proveedor", 3},
                {"SALIDA", 12, null, "Venta en caja", 3},
                {"SALIDA", 4, null, "Venta en caja", 2},
                {"ENTRADA", 70, "FACT-10280", "Recepción de proveedor", 1},
                {"SALIDA", 9, null, "Venta en caja", 1}
        };
        long ahora = System.currentTimeMillis();
        for (int i = 0; i < movs.length; i++) {
            ContentValues v = new ContentValues();
            v.put("id_producto", (i % 12) + 1L);
            v.put("tipo", (String) movs[i][0]);
            v.put("cantidad", (int) movs[i][1]);
            v.put("usuario", "admin");
            long dias = ((int) movs[i][4]) * 24L * 60L * 60L * 1000L;
            v.put("fecha", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.US).format(new java.util.Date(ahora - dias)));
            if (movs[i][2] != null) {
                v.put("documento", (String) movs[i][2]);
            }
            v.put("motivo", (String) movs[i][3]);
            db.insert("MOVIMIENTO", null, v);
        }
    }
}
