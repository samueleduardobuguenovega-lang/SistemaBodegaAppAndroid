package com.android.sistemabodegaappandroid.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.sistemabodegaappandroid.model.Categoria;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.model.Proveedor;
import com.android.sistemabodegaappandroid.model.Rol;
import com.android.sistemabodegaappandroid.model.Usuario;
import com.android.sistemabodegaappandroid.util.PasswordUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sistema_bodega.db";
    private static final int DATABASE_VERSION = 8; // 8: Agregados documento y motivo a MOVIMIENTO

    private static final String TAG = "DatabaseHelper";

    private static final ExecutorService categoriaExecutor = Executors.newSingleThreadExecutor();
    private MutableLiveData<List<Categoria>> categoriasActivas;

    // Tabla ROL
    public static final String TABLE_ROL = "ROL";
    public static final String COLUMN_ROL_ID = "id_rol";
    public static final String COLUMN_ROL_NOMBRE = "nombre_rol";
    public static final String COLUMN_ROL_DESCRIPCION = "descripcion";
    public static final String COLUMN_ROL_PERMISOS = "permisos";

    // Tabla USUARIO
    public static final String TABLE_USUARIO = "USUARIO";
    public static final String COLUMN_USUARIO_ID = "id_usuario";
    public static final String COLUMN_USUARIO_USERNAME = "username";
    public static final String COLUMN_USUARIO_NOMBRE_COMPLETO = "nombre_completo";
    public static final String COLUMN_USUARIO_PASSWORD_HASH = "password_hash";
    public static final String COLUMN_USUARIO_ID_ROL = "id_rol";
    public static final String COLUMN_USUARIO_ACTIVO = "activo";

    // Tabla CATEGORIA
    public static final String TABLE_CATEGORIA = "CATEGORIA";
    public static final String COLUMN_CAT_ID = "id_categoria";
    public static final String COLUMN_CAT_NOMBRE = "nombre_categoria";
    public static final String COLUMN_CAT_DESCRIPCION = "descripcion";
    public static final String COLUMN_CAT_ACTIVO = "activo";

    // Tabla PROVEEDOR
    public static final String TABLE_PROVEEDOR = "PROVEEDOR";
    public static final String COLUMN_PROV_ID = "id_proveedor";
    public static final String COLUMN_PROV_RAZON_SOCIAL = "razon_social";
    public static final String COLUMN_PROV_RUT = "rut";
    public static final String COLUMN_PROV_EMAIL = "email";
    public static final String COLUMN_PROV_TELEFONO = "telefono";
    public static final String COLUMN_PROV_DIRECCION = "direccion";
    public static final String COLUMN_PROV_ACTIVO = "activo";

    // Tabla PRODUCTO
    public static final String TABLE_PRODUCTO = "PRODUCTO";
    public static final String COLUMN_PROD_ID = "id_producto";
    public static final String COLUMN_PROD_SKU = "sku_codigo";
    public static final String COLUMN_PROD_NOMBRE = "nombre";
    public static final String COLUMN_PROD_DESCRIPCION = "descripcion";
    public static final String COLUMN_PROD_PRECIO_COSTO = "precio_costo";
    public static final String COLUMN_PROD_PRECIO_VENTA = "precio_venta";
    public static final String COLUMN_PROD_STOCK_ACTUAL = "stock_actual";
    public static final String COLUMN_PROD_STOCK_MINIMO = "stock_minimo";
    public static final String COLUMN_PROD_ID_CATEGORIA = "id_categoria";
    public static final String COLUMN_PROD_ID_PROVEEDOR = "id_proveedor";
    public static final String COLUMN_PROD_ACTIVO = "activo";
    public static final String COLUMN_PROD_CODIGO_BARRAS = "codigo_barras";
    public static final String COLUMN_PROD_UNIDAD_MEDIDA = "unidad_medida";

    // Tabla MOVIMIENTO
    public static final String TABLE_MOVIMIENTO = "MOVIMIENTO";
    public static final String COLUMN_MOV_ID = "id_movimiento";
    public static final String COLUMN_MOV_ID_PRODUCTO = "id_producto";
    public static final String COLUMN_MOV_TIPO = "tipo";
    public static final String COLUMN_MOV_CANTIDAD = "cantidad";
    public static final String COLUMN_MOV_USUARIO = "usuario";
    public static final String COLUMN_MOV_FECHA = "fecha";
    public static final String COLUMN_MOV_ID_PROVEEDOR = "id_proveedor";
    // NUEVOS CAMPOS (CU-04 / CU-05)
    public static final String COLUMN_MOV_DOCUMENTO = "documento";
    public static final String COLUMN_MOV_MOTIVO = "motivo";

    private static final String[] SQL_TABLAS_INVENTARIO = {
            "CREATE TABLE IF NOT EXISTS " + TABLE_PROVEEDOR + " ("
                    + COLUMN_PROV_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_PROV_RAZON_SOCIAL + " TEXT NOT NULL, "
                    + COLUMN_PROV_RUT + " TEXT NOT NULL UNIQUE, "
                    + COLUMN_PROV_EMAIL + " TEXT, "
                    + COLUMN_PROV_TELEFONO + " TEXT, "
                    + COLUMN_PROV_DIRECCION + " TEXT, "
                    + COLUMN_PROV_ACTIVO + " INTEGER NOT NULL DEFAULT 1"
                    + ");",
            "CREATE TABLE IF NOT EXISTS " + TABLE_PRODUCTO + " ("
                    + COLUMN_PROD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_PROD_SKU + " TEXT NOT NULL UNIQUE, "
                    + COLUMN_PROD_CODIGO_BARRAS + " TEXT UNIQUE, "
                    + COLUMN_PROD_UNIDAD_MEDIDA + " TEXT, "
                    + COLUMN_PROD_NOMBRE + " TEXT NOT NULL, "
                    + COLUMN_PROD_DESCRIPCION + " TEXT, "
                    + COLUMN_PROD_PRECIO_COSTO + " REAL NOT NULL DEFAULT 0, "
                    + COLUMN_PROD_PRECIO_VENTA + " REAL NOT NULL DEFAULT 0, "
                    + COLUMN_PROD_STOCK_ACTUAL + " INTEGER NOT NULL DEFAULT 0, "
                    + COLUMN_PROD_STOCK_MINIMO + " INTEGER NOT NULL DEFAULT 0, "
                    + COLUMN_PROD_ID_CATEGORIA + " INTEGER NOT NULL, "
                    + COLUMN_PROD_ID_PROVEEDOR + " INTEGER, "
                    + COLUMN_PROD_ACTIVO + " INTEGER NOT NULL DEFAULT 1, "
                    + "FOREIGN KEY (" + COLUMN_PROD_ID_CATEGORIA + ") REFERENCES " + TABLE_CATEGORIA + "(" + COLUMN_CAT_ID + "), "
                    + "FOREIGN KEY (" + COLUMN_PROD_ID_PROVEEDOR + ") REFERENCES " + TABLE_PROVEEDOR + "(" + COLUMN_PROV_ID + ")"
                    + ");",
            "CREATE TABLE IF NOT EXISTS " + TABLE_MOVIMIENTO + " ("
                    + COLUMN_MOV_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_MOV_ID_PRODUCTO + " INTEGER NOT NULL, "
                    + COLUMN_MOV_TIPO + " TEXT NOT NULL, "
                    + COLUMN_MOV_CANTIDAD + " INTEGER NOT NULL, "
                    + COLUMN_MOV_USUARIO + " TEXT, "
                    + COLUMN_MOV_FECHA + " TEXT NOT NULL, "
                    + COLUMN_MOV_ID_PROVEEDOR + " INTEGER, "
                    + COLUMN_MOV_DOCUMENTO + " TEXT, "
                    + COLUMN_MOV_MOTIVO + " TEXT, "
                    + "FOREIGN KEY (" + COLUMN_MOV_ID_PRODUCTO + ") REFERENCES " + TABLE_PRODUCTO + "(" + COLUMN_PROD_ID + ")"
                    + ");"
    };

    private static final String[] SQL_INDICES = {
            "CREATE INDEX IF NOT EXISTS idx_producto_sku ON " + TABLE_PRODUCTO
                    + "(" + COLUMN_PROD_SKU + ");",
            "CREATE INDEX IF NOT EXISTS idx_producto_barras ON " + TABLE_PRODUCTO
                    + "(" + COLUMN_PROD_CODIGO_BARRAS + ");",
            "CREATE INDEX IF NOT EXISTS idx_movimiento_producto ON " + TABLE_MOVIMIENTO
                    + "(" + COLUMN_MOV_ID_PRODUCTO + ");"
    };

    // Para la normalización de fechas (manteniendo compatibilidad original)
    private static final String FORMATO_FECHA_ORDENABLE = "yyyy-MM-dd HH:mm:ss";
    private static final Locale[] LOCALES_LEGACY = {
            Locale.forLanguageTag("ar"),
            Locale.forLanguageTag("fa"),
            Locale.forLanguageTag("th-TH-u-nu-thai"),
            Locale.forLanguageTag("hi-IN-u-nu-deva")
    };

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=ON;");

        String CREATE_ROL_TABLE = "CREATE TABLE " + TABLE_ROL + " ("
                + COLUMN_ROL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_ROL_NOMBRE + " TEXT UNIQUE NOT NULL, "
                + COLUMN_ROL_DESCRIPCION + " TEXT, "
                + COLUMN_ROL_PERMISOS + " TEXT"
                + ");";
        db.execSQL(CREATE_ROL_TABLE);

        String CREATE_USUARIO_TABLE = "CREATE TABLE " + TABLE_USUARIO + " ("
                + COLUMN_USUARIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USUARIO_USERNAME + " TEXT UNIQUE NOT NULL, "
                + COLUMN_USUARIO_NOMBRE_COMPLETO + " TEXT NOT NULL, "
                + COLUMN_USUARIO_PASSWORD_HASH + " TEXT NOT NULL, "
                + COLUMN_USUARIO_ID_ROL + " INTEGER NOT NULL, "
                + COLUMN_USUARIO_ACTIVO + " INTEGER NOT NULL DEFAULT 1, "
                + "FOREIGN KEY (" + COLUMN_USUARIO_ID_ROL + ") REFERENCES " + TABLE_ROL + "(" + COLUMN_ROL_ID + ")"
                + ");";
        db.execSQL(CREATE_USUARIO_TABLE);

        String CREATE_CATEGORIA_TABLE = "CREATE TABLE " + TABLE_CATEGORIA + " ("
                + COLUMN_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_CAT_NOMBRE + " TEXT UNIQUE NOT NULL, "
                + COLUMN_CAT_DESCRIPCION + " TEXT, "
                + COLUMN_CAT_ACTIVO + " INTEGER NOT NULL DEFAULT 1"
                + ");";
        db.execSQL(CREATE_CATEGORIA_TABLE);

        for (String sqlInventario : SQL_TABLAS_INVENTARIO) {
            db.execSQL(sqlInventario);
        }

        crearIndices(db);
        insertInitialSeedData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            for (String sqlInventario : SQL_TABLAS_INVENTARIO) {
                db.execSQL(sqlInventario);
            }
        }
        if (oldVersion < 4) {
            agregarColumnaSiFalta(db, TABLE_MOVIMIENTO, COLUMN_MOV_ID_PROVEEDOR,
                    "ALTER TABLE " + TABLE_MOVIMIENTO + " ADD COLUMN " + COLUMN_MOV_ID_PROVEEDOR + " INTEGER",
                    "v4: MOVIMIENTO." + COLUMN_MOV_ID_PROVEEDOR);
        }
        if (oldVersion < 5) {
            agregarColumnaSiFalta(db, TABLE_ROL, COLUMN_ROL_PERMISOS,
                    "ALTER TABLE " + TABLE_ROL + " ADD COLUMN " + COLUMN_ROL_PERMISOS + " TEXT",
                    "v5: ROL." + COLUMN_ROL_PERMISOS);
            rellenarPermisosDeRolesExistentes(db);
        }
        if (oldVersion < 6) {
            agregarColumnaSiFalta(db, TABLE_PRODUCTO, COLUMN_PROD_CODIGO_BARRAS,
                    "ALTER TABLE " + TABLE_PRODUCTO + " ADD COLUMN " + COLUMN_PROD_CODIGO_BARRAS + " TEXT",
                    "v6: PRODUCTO." + COLUMN_PROD_CODIGO_BARRAS);
            agregarColumnaSiFalta(db, TABLE_PRODUCTO, COLUMN_PROD_UNIDAD_MEDIDA,
                    "ALTER TABLE " + TABLE_PRODUCTO + " ADD COLUMN " + COLUMN_PROD_UNIDAD_MEDIDA + " TEXT",
                    "v6: PRODUCTO." + COLUMN_PROD_UNIDAD_MEDIDA);
        }
        if (oldVersion < 7) {
            crearIndices(db);
            normalizarFechasDeMovimiento(db);
        }
        if (oldVersion < 8) {
            // v8 agrega documento y motivo al historial de movimientos (CU-04, CU-05)
            agregarColumnaSiFalta(db, TABLE_MOVIMIENTO, COLUMN_MOV_DOCUMENTO,
                    "ALTER TABLE " + TABLE_MOVIMIENTO + " ADD COLUMN " + COLUMN_MOV_DOCUMENTO + " TEXT",
                    "v8: MOVIMIENTO." + COLUMN_MOV_DOCUMENTO);
            agregarColumnaSiFalta(db, TABLE_MOVIMIENTO, COLUMN_MOV_MOTIVO,
                    "ALTER TABLE " + TABLE_MOVIMIENTO + " ADD COLUMN " + COLUMN_MOV_MOTIVO + " TEXT",
                    "v8: MOVIMIENTO." + COLUMN_MOV_MOTIVO);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        repararPermisosDeRol(db);
    }

    private void repararPermisosDeRol(SQLiteDatabase db) {
        if (columnaExisteEnTabla(db, TABLE_ROL, COLUMN_ROL_PERMISOS)) {
            return;
        }
        agregarColumnaSiFalta(db, TABLE_ROL, COLUMN_ROL_PERMISOS,
                "ALTER TABLE " + TABLE_ROL + " ADD COLUMN " + COLUMN_ROL_PERMISOS + " TEXT",
                "reparacion: " + TABLE_ROL + "." + COLUMN_ROL_PERMISOS);
        rellenarPermisosDeRolesExistentes(db);
    }

    private void crearIndices(SQLiteDatabase db) {
        for (String sqlIndice : SQL_INDICES) {
            db.execSQL(sqlIndice);
        }
    }

    private void normalizarFechasDeMovimiento(SQLiteDatabase db) {
        SimpleDateFormat canonico = nuevoFormateadorFechas(Locale.US);
        List<SimpleDateFormat> candidatos = new ArrayList<>();
        candidatos.add(nuevoFormateadorFechas(Locale.getDefault()));
        for (Locale locale : LOCALES_LEGACY) {
            candidatos.add(nuevoFormateadorFechas(locale));
        }

        List<String[]> cambios = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_MOV_ID + ", " + COLUMN_MOV_FECHA
                        + " FROM " + TABLE_MOVIMIENTO, null)) {
            int indiceId = cursor.getColumnIndexOrThrow(COLUMN_MOV_ID);
            int indiceFecha = cursor.getColumnIndexOrThrow(COLUMN_MOV_FECHA);
            while (cursor.moveToNext()) {
                String fecha = cursor.getString(indiceFecha);
                String normalizada = normalizarFecha(fecha, canonico, candidatos);
                if (normalizada != null && !normalizada.equals(fecha)) {
                    cambios.add(new String[]{String.valueOf(cursor.getInt(indiceId)), normalizada});
                }
            }
        }

        if (cambios.isEmpty()) {
            return;
        }
        try {
            for (String[] cambio : cambios) {
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_MOV_FECHA, cambio[1]);
                db.update(TABLE_MOVIMIENTO, cv,
                        COLUMN_MOV_ID + " = ?", new String[]{cambio[0]});
            }
        } catch (SQLException e) {
            Log.e(TAG, "Fallo el paso de migracion v7", e);
            throw e;
        }
    }

    private String normalizarFecha(String fecha, SimpleDateFormat canonico,
                                   List<SimpleDateFormat> candidatos) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }
        String valor = fecha.trim();
        Date canonica = parsearFecha(canonico, valor);
        if (canonica != null && canonico.format(canonica).equals(valor)) {
            return valor;
        }
        for (SimpleDateFormat candidato : candidatos) {
            Date legacy = parsearFecha(candidato, valor);
            if (legacy != null) {
                return canonico.format(legacy);
            }
        }
        return null;
    }

    private Date parsearFecha(SimpleDateFormat formato, String valor) {
        try {
            return formato.parse(valor);
        } catch (ParseException e) {
            return null;
        }
    }

    private SimpleDateFormat nuevoFormateadorFechas(Locale locale) {
        SimpleDateFormat formato = new SimpleDateFormat(FORMATO_FECHA_ORDENABLE, locale);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        formato.setLenient(false);
        return formato;
    }

    private void rellenarPermisosDeRolesExistentes(SQLiteDatabase db) {
        String todos = "USUARIOS,PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS";
        String inventario = "PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS";
        String sinPermisos = " AND (" + COLUMN_ROL_PERMISOS + " IS NULL"
                + " OR TRIM(" + COLUMN_ROL_PERMISOS + ") = '')";
        db.execSQL("UPDATE " + TABLE_ROL + " SET " + COLUMN_ROL_PERMISOS + " = ?"
                        + " WHERE UPPER(TRIM(" + COLUMN_ROL_NOMBRE + ")) = 'ADMINISTRADOR'" + sinPermisos,
                new Object[]{todos});
        db.execSQL("UPDATE " + TABLE_ROL + " SET " + COLUMN_ROL_PERMISOS + " = ?"
                        + " WHERE UPPER(TRIM(" + COLUMN_ROL_NOMBRE + ")) <> 'ADMINISTRADOR'" + sinPermisos,
                new Object[]{inventario});
    }

    private boolean columnaExisteEnTabla(SQLiteDatabase db, String tabla, String columna) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + tabla + ")", null)) {
            int indiceNombre = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (columna.equalsIgnoreCase(cursor.getString(indiceNombre))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void agregarColumnaSiFalta(SQLiteDatabase db, String tabla, String columna,
                                       String alterTable, String paso) {
        if (columnaExisteEnTabla(db, tabla, columna)) {
            return;
        }
        try {
            db.execSQL(alterTable);
        } catch (SQLException e) {
            Log.e(TAG, "Fallo el paso de migracion " + paso, e);
            throw e;
        }
    }

    private void insertInitialSeedData(SQLiteDatabase db) {
        ContentValues cvAdminRol = new ContentValues();
        cvAdminRol.put(COLUMN_ROL_NOMBRE, "Administrador");
        cvAdminRol.put(COLUMN_ROL_DESCRIPCION, "Acceso total al sistema");
        cvAdminRol.put(COLUMN_ROL_PERMISOS, "USUARIOS,PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS");
        long adminRolId = db.insert(TABLE_ROL, null, cvAdminRol);

        ContentValues cvOperadorRol = new ContentValues();
        cvOperadorRol.put(COLUMN_ROL_NOMBRE, "Operador");
        cvOperadorRol.put(COLUMN_ROL_DESCRIPCION, "Acceso a operaciones de bodega");
        cvOperadorRol.put(COLUMN_ROL_PERMISOS, "PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS");
        db.insert(TABLE_ROL, null, cvOperadorRol);

        ContentValues cvVendedorRol = new ContentValues();
        cvVendedorRol.put(COLUMN_ROL_NOMBRE, "Vendedor");
        cvVendedorRol.put(COLUMN_ROL_DESCRIPCION, "Acceso a ventas y consultas");
        cvVendedorRol.put(COLUMN_ROL_PERMISOS, "PRODUCTOS,CATEGORIAS");
        db.insert(TABLE_ROL, null, cvVendedorRol);

        if (adminRolId != -1) {
            ContentValues cvAdminUser = new ContentValues();
            cvAdminUser.put(COLUMN_USUARIO_USERNAME, "admin");
            cvAdminUser.put(COLUMN_USUARIO_NOMBRE_COMPLETO, "Administrador del Sistema");
            cvAdminUser.put(COLUMN_USUARIO_PASSWORD_HASH, PasswordUtils.hashPassword("admin"));
            cvAdminUser.put(COLUMN_USUARIO_ID_ROL, adminRolId);
            cvAdminUser.put(COLUMN_USUARIO_ACTIVO, 1);
            db.insert(TABLE_USUARIO, null, cvAdminUser);
        }
    }

    public Usuario getUsuarioByUsername(String username) {
        return buscarUsuarioPor(COLUMN_USUARIO_USERNAME, username);
    }

    public Usuario getUsuarioById(int idUsuario) {
        return buscarUsuarioPor(COLUMN_USUARIO_ID, String.valueOf(idUsuario));
    }

    public String getPermisosDeRol(int idRol) {
        if (idRol <= 0) return "";
        String sql = "SELECT " + COLUMN_ROL_PERMISOS + " FROM " + TABLE_ROL
                + " WHERE " + COLUMN_ROL_ID + " = ?";
        try (Cursor cursor = getReadableDatabase().rawQuery(
                sql, new String[]{String.valueOf(idRol)})) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                String permisos = cursor.getString(0);
                return permisos != null ? permisos : "";
            }
        }
        return "";
    }

    private Usuario buscarUsuarioPor(String columna, String valor) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT u." + COLUMN_USUARIO_ID + ", u." + COLUMN_USUARIO_USERNAME + ", u."
                + COLUMN_USUARIO_NOMBRE_COMPLETO + ", u." + COLUMN_USUARIO_PASSWORD_HASH + ", u."
                + COLUMN_USUARIO_ID_ROL + ", u." + COLUMN_USUARIO_ACTIVO + ", r." + COLUMN_ROL_NOMBRE
                + " FROM " + TABLE_USUARIO + " u INNER JOIN " + TABLE_ROL + " r ON u."
                + COLUMN_USUARIO_ID_ROL + " = r." + COLUMN_ROL_ID
                + " WHERE u." + columna + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{valor});
        Usuario usuario = null;
        if (cursor.moveToFirst()) {
            usuario = new Usuario();
            usuario.setIdUsuario(cursor.getInt(0));
            usuario.setUsername(cursor.getString(1));
            usuario.setNombreCompleto(cursor.getString(2));
            usuario.setPasswordHash(cursor.getString(3));
            usuario.setIdRol(cursor.getInt(4));
            usuario.setActivo(cursor.getInt(5));
            usuario.setNombreRol(cursor.getString(6));
        }
        cursor.close();
        return usuario;
    }

    public Usuario autenticar(String username, String rawPassword) {
        Usuario usuario = getUsuarioByUsername(username);
        if (usuario == null) return null;

        String hashAlmacenado = usuario.getPasswordHash();
        if (!PasswordUtils.verificar(rawPassword, hashAlmacenado)) return null;

        if (PasswordUtils.necesitaRehash(hashAlmacenado)) {
            String hashNuevo = PasswordUtils.hashPassword(rawPassword);
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_USUARIO_PASSWORD_HASH, hashNuevo);
            this.getWritableDatabase().update(
                    TABLE_USUARIO, cv,
                    COLUMN_USUARIO_ID + " = ?",
                    new String[]{String.valueOf(usuario.getIdUsuario())});
            usuario.setPasswordHash(hashNuevo);
        }
        return usuario;
    }

    private static final ExecutorService rolExecutor = Executors.newSingleThreadExecutor();
    private MutableLiveData<List<Rol>> roles;

    public List<Rol> getAllRoles() {
        List<Rol> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ROL + " ORDER BY " + COLUMN_ROL_NOMBRE + " ASC", null);
        if (cursor.moveToFirst()) {
            do {
                Rol rol = new Rol(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROL_NOMBRE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROL_DESCRIPCION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROL_PERMISOS))
                );
                list.add(rol);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public long insertRol(Rol rol) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ROL_NOMBRE, rol.getNombreRol().trim());
        cv.put(COLUMN_ROL_DESCRIPCION, rol.getDescripcion() != null ? rol.getDescripcion().trim() : "");
        cv.put(COLUMN_ROL_PERMISOS, rol.getPermisos() != null ? rol.getPermisos().trim() : "");
        return db.insert(TABLE_ROL, null, cv);
    }

    public boolean updateRol(Rol rol) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ROL_NOMBRE, rol.getNombreRol().trim());
        cv.put(COLUMN_ROL_DESCRIPCION, rol.getDescripcion() != null ? rol.getDescripcion().trim() : "");
        cv.put(COLUMN_ROL_PERMISOS, rol.getPermisos() != null ? rol.getPermisos().trim() : "");
        int rows = db.update(TABLE_ROL, cv, COLUMN_ROL_ID + " = ?", new String[]{String.valueOf(rol.getIdRol())});
        return rows > 0;
    }

    public boolean deleteRol(int idRol) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_USUARIO + " WHERE " + COLUMN_USUARIO_ID_ROL + " = ?", new String[]{String.valueOf(idRol)});
        boolean inUse = cursor.getCount() > 0;
        cursor.close();
        if (inUse) return false;
        int rows = db.delete(TABLE_ROL, COLUMN_ROL_ID + " = ?", new String[]{String.valueOf(idRol)});
        return rows > 0;
    }

    public boolean isRoleNameExists(String nombreRol) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_ROL + " WHERE " + COLUMN_ROL_NOMBRE + " = ? COLLATE NOCASE", new String[]{nombreRol.trim()});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public LiveData<List<Rol>> observarRoles() {
        if (roles == null) {
            roles = new MutableLiveData<>();
            refrescarRoles();
        }
        return roles;
    }

    public void refrescarRoles() {
        final MutableLiveData<List<Rol>> destino = roles;
        if (destino == null) return;
        rolExecutor.execute(() -> destino.postValue(getAllRoles()));
    }

    public List<Usuario> getAllUsuarios() {
        List<Usuario> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT u." + COLUMN_USUARIO_ID + ", u." + COLUMN_USUARIO_USERNAME + ", u."
                + COLUMN_USUARIO_NOMBRE_COMPLETO + ", u." + COLUMN_USUARIO_PASSWORD_HASH + ", u."
                + COLUMN_USUARIO_ID_ROL + ", u." + COLUMN_USUARIO_ACTIVO + ", r." + COLUMN_ROL_NOMBRE
                + " FROM " + TABLE_USUARIO + " u INNER JOIN " + TABLE_ROL + " r ON u."
                + COLUMN_USUARIO_ID_ROL + " = r." + COLUMN_ROL_ID
                + " ORDER BY u." + COLUMN_USUARIO_ID + " DESC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                Usuario u = new Usuario();
                u.setIdUsuario(cursor.getInt(0));
                u.setUsername(cursor.getString(1));
                u.setNombreCompleto(cursor.getString(2));
                u.setPasswordHash(cursor.getString(3));
                u.setIdRol(cursor.getInt(4));
                u.setActivo(cursor.getInt(5));
                u.setNombreRol(cursor.getString(6));
                list.add(u);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public long insertUsuario(Usuario usuario, String rawPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_USUARIO_USERNAME, usuario.getUsername().trim());
        cv.put(COLUMN_USUARIO_NOMBRE_COMPLETO, usuario.getNombreCompleto().trim());
        cv.put(COLUMN_USUARIO_PASSWORD_HASH, PasswordUtils.hashPassword(rawPassword));
        cv.put(COLUMN_USUARIO_ID_ROL, usuario.getIdRol());
        cv.put(COLUMN_USUARIO_ACTIVO, usuario.getActivo());
        return db.insert(TABLE_USUARIO, null, cv);
    }

    public boolean updateUsuarioEstado(int idUsuario, int nuevoEstado) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_USUARIO_ACTIVO, nuevoEstado);
        int rows = db.update(TABLE_USUARIO, cv, COLUMN_USUARIO_ID + " = ?", new String[]{String.valueOf(idUsuario)});
        return rows > 0;
    }

    public boolean updateUsuario(Usuario usuario, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_USUARIO_USERNAME, usuario.getUsername().trim());
        cv.put(COLUMN_USUARIO_NOMBRE_COMPLETO, usuario.getNombreCompleto().trim());
        cv.put(COLUMN_USUARIO_ID_ROL, usuario.getIdRol());
        cv.put(COLUMN_USUARIO_ACTIVO, usuario.getActivo());
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            cv.put(COLUMN_USUARIO_PASSWORD_HASH, PasswordUtils.hashPassword(newPassword.trim()));
        }
        int rows = db.update(TABLE_USUARIO, cv, COLUMN_USUARIO_ID + " = ?", new String[]{String.valueOf(usuario.getIdUsuario())});
        return rows > 0;
    }

    public boolean deleteUsuario(int idUsuario) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_USUARIO, COLUMN_USUARIO_ID + " = ?", new String[]{String.valueOf(idUsuario)});
        return rows > 0;
    }

    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_USUARIO + " WHERE " + COLUMN_USUARIO_USERNAME + " = ? COLLATE NOCASE", new String[]{username.trim()});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean isCategoriaExists(String nombreCategoria) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_CATEGORIA + " WHERE " + COLUMN_CAT_NOMBRE + " = ? COLLATE NOCASE", new String[]{nombreCategoria.trim()});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public long insertCategoria(String nombreCategoria, String descripcion) {
        if (isCategoriaExists(nombreCategoria)) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CAT_NOMBRE, nombreCategoria.trim());
        cv.put(COLUMN_CAT_DESCRIPCION, descripcion != null ? descripcion.trim() : "");
        cv.put(COLUMN_CAT_ACTIVO, 1);
        return db.insert(TABLE_CATEGORIA, null, cv);
    }

    public List<Categoria> getCategoriasActivas() {
        List<Categoria> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_CAT_ID + ", " + COLUMN_CAT_NOMBRE + ", " + COLUMN_CAT_DESCRIPCION + ", " + COLUMN_CAT_ACTIVO
                        + " FROM " + TABLE_CATEGORIA
                        + " WHERE " + COLUMN_CAT_ACTIVO + " = 1"
                        + " ORDER BY " + COLUMN_CAT_NOMBRE + " ASC",
                null);
        if (cursor.moveToFirst()) {
            do {
                Categoria cat = new Categoria();
                cat.setIdCategoria(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAT_ID)));
                cat.setNombreCategoria(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_NOMBRE)));
                cat.setDescripcion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_DESCRIPCION)));
                cat.setActivo(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAT_ACTIVO)));
                list.add(cat);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public LiveData<List<Categoria>> observarCategoriasActivas() {
        if (categoriasActivas == null) {
            categoriasActivas = new MutableLiveData<>();
            refrescarCategoriasActivas();
        }
        return categoriasActivas;
    }

    public void refrescarCategoriasActivas() {
        final MutableLiveData<List<Categoria>> destino = categoriasActivas;
        if (destino == null) return;
        categoriaExecutor.execute(() -> destino.postValue(getCategoriasActivas()));
    }

    private static final ExecutorService usuarioExecutor = Executors.newSingleThreadExecutor();
    private MutableLiveData<List<Usuario>> usuarios;

    public LiveData<List<Usuario>> observarUsuarios() {
        if (usuarios == null) {
            usuarios = new MutableLiveData<>();
            refrescarUsuarios();
        }
        return usuarios;
    }

    public void refrescarUsuarios() {
        final MutableLiveData<List<Usuario>> destino = usuarios;
        if (destino == null) return;
        usuarioExecutor.execute(() -> destino.postValue(getAllUsuarios()));
    }

    private static final ExecutorService productoExecutor = Executors.newSingleThreadExecutor();
    private MutableLiveData<List<Producto>> productos;
    private final ProductoDao productoDaoReactivo = new ProductoDao(this);

    public LiveData<List<Producto>> observarProductos(String filtro) {
        if (productos == null) {
            productos = new MutableLiveData<>();
            refrescarProductos(filtro);
        }
        return productos;
    }

    public void refrescarProductos(String filtro) {
        final MutableLiveData<List<Producto>> destino = productos;
        if (destino == null) return;
        final String texto = filtro;
        productoExecutor.execute(() ->
                destino.postValue(productoDaoReactivo.buscarProductos(texto)));
    }

    private static final ExecutorService proveedorExecutor = Executors.newSingleThreadExecutor();
    private MutableLiveData<List<Proveedor>> proveedores;
    private final ProveedorDao proveedorDaoReactivo = new ProveedorDao(this);

    public LiveData<List<Proveedor>> observarProveedores() {
        if (proveedores == null) {
            proveedores = new MutableLiveData<>();
            refrescarProveedores();
        }
        return proveedores;
    }

    public void refrescarProveedores() {
        final MutableLiveData<List<Proveedor>> destino = proveedores;
        if (destino == null) return;
        proveedorExecutor.execute(() ->
                destino.postValue(proveedorDaoReactivo.getAllProveedores()));
    }
}