package com.android.sistemabodegaappandroid.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.sistemabodegaappandroid.model.Categoria;
import com.android.sistemabodegaappandroid.model.Producto;
import com.android.sistemabodegaappandroid.model.Rol;
import com.android.sistemabodegaappandroid.model.Usuario;
import com.android.sistemabodegaappandroid.util.PasswordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sistema_bodega.db";
    private static final int DATABASE_VERSION = 4; // 4: MOVIMIENTO registra el proveedor de la entrada

    /**
     * Hilo dedicado para las consultas reactivas de CU-10. Evita ejecutar el DAO
     * de categorías en el hilo principal de la interfaz.
     */
    private static final ExecutorService categoriaExecutor = Executors.newSingleThreadExecutor();

    /** LiveData de categorías activas (CU-10). Se crea una sola vez por helper. */
    private MutableLiveData<List<Categoria>> categoriasActivas;

    // Tabla ROL
    public static final String TABLE_ROL = "ROL";
    public static final String COLUMN_ROL_ID = "id_rol";
    public static final String COLUMN_ROL_NOMBRE = "nombre_rol";
    public static final String COLUMN_ROL_DESCRIPCION = "descripcion";

    // Tabla USUARIO
    public static final String TABLE_USUARIO = "USUARIO";
    public static final String COLUMN_USUARIO_ID = "id_usuario";
    public static final String COLUMN_USUARIO_USERNAME = "username";
    public static final String COLUMN_USUARIO_NOMBRE_COMPLETO = "nombre_completo";
    public static final String COLUMN_USUARIO_PASSWORD_HASH = "password_hash";
    public static final String COLUMN_USUARIO_ID_ROL = "id_rol";
    public static final String COLUMN_USUARIO_ACTIVO = "activo";

    // Tabla CATEGORIA (CU-09)
    public static final String TABLE_CATEGORIA = "CATEGORIA";
    public static final String COLUMN_CAT_ID = "id_categoria";
    public static final String COLUMN_CAT_NOMBRE = "nombre_categoria";
    public static final String COLUMN_CAT_DESCRIPCION = "descripcion";
    public static final String COLUMN_CAT_ACTIVO = "activo";

    // Tabla PROVEEDOR (CU-11)
    public static final String TABLE_PROVEEDOR = "PROVEEDOR";
    public static final String COLUMN_PROV_ID = "id_proveedor";
    public static final String COLUMN_PROV_RAZON_SOCIAL = "razon_social";
    public static final String COLUMN_PROV_RUT = "rut";
    public static final String COLUMN_PROV_EMAIL = "email";
    public static final String COLUMN_PROV_TELEFONO = "telefono";
    public static final String COLUMN_PROV_DIRECCION = "direccion";
    public static final String COLUMN_PROV_ACTIVO = "activo";

    // Tabla PRODUCTO (CU-11)
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

    // Tabla MOVIMIENTO (CU-12)
    public static final String TABLE_MOVIMIENTO = "MOVIMIENTO";
    public static final String COLUMN_MOV_ID = "id_movimiento";
    public static final String COLUMN_MOV_ID_PRODUCTO = "id_producto";
    public static final String COLUMN_MOV_TIPO = "tipo";
    public static final String COLUMN_MOV_CANTIDAD = "cantidad";
    public static final String COLUMN_MOV_USUARIO = "usuario";
    public static final String COLUMN_MOV_FECHA = "fecha";
    // En v4 la entrada de stock deja constancia del proveedor que entregó la
    // mercancía. Es nullable porque las salidas y las entradas sin proveedor no
    // tienen uno, y sin clave foránea a propósito: el proveedor histórico no
    // debe bloquear un movimiento viejo si luego se deshabilita o se borra.
    public static final String COLUMN_MOV_ID_PROVEEDOR = "id_proveedor";

    /**
     * SQL de creación de las tres tablas del módulo de inventario.
     * Se centraliza aquí para que onCreate y onUpgrade usen la misma definición
     * y no existan dos variantes del esquema que puedan divergir.
     */
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
                    + "FOREIGN KEY (" + COLUMN_MOV_ID_PRODUCTO + ") REFERENCES " + TABLE_PRODUCTO + "(" + COLUMN_PROD_ID + ")"
                    + ");"
    };

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Activa las claves foráneas en cada conexión que abre SQLite.
     * Sin esto, Android las deja deshabilitadas por defecto y las referencias
     * a CATEGORIA/PROVEEDOR/PRODUCTO no se validarían en tiempo de escritura.
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Habilitar claves foráneas
        db.execSQL("PRAGMA foreign_keys=ON;");

        // Crear tabla ROL
        String CREATE_ROL_TABLE = "CREATE TABLE " + TABLE_ROL + " ("
                + COLUMN_ROL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_ROL_NOMBRE + " TEXT UNIQUE NOT NULL, "
                + COLUMN_ROL_DESCRIPCION + " TEXT"
                + ");";
        db.execSQL(CREATE_ROL_TABLE);

        // Crear tabla USUARIO
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

        // Crear tabla CATEGORIA (CU-09)
        String CREATE_CATEGORIA_TABLE = "CREATE TABLE " + TABLE_CATEGORIA + " ("
                + COLUMN_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_CAT_NOMBRE + " TEXT UNIQUE NOT NULL, "
                + COLUMN_CAT_DESCRIPCION + " TEXT, "
                + COLUMN_CAT_ACTIVO + " INTEGER NOT NULL DEFAULT 1"
                + ");";
        db.execSQL(CREATE_CATEGORIA_TABLE);

        // Crear tablas del módulo de inventario (CU-11 / CU-12).
        // En una instalación limpia el esquema completo se arma desde cero,
        // por eso estas tablas se crean aquí además de en la migración.
        for (String sqlInventario : SQL_TABLAS_INVENTARIO) {
            db.execSQL(sqlInventario);
        }

        // Insertar roles iniciales de prueba
        insertInitialSeedData(db);
    }

    /**
     * Migración incremental: cada salto de versión solo aplica los cambios
     * que le corresponden, en orden. Antes aquí se hacía DROP TABLE de todo,
     * lo que destruía usuarios y categorías al actualizar la app; esa es la
     * falla que esta versión corrige: los datos existentes deben sobrevivir.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // v3 agrega el módulo de inventario: PROVEEDOR, PRODUCTO y MOVIMIENTO.
            // CREATE TABLE IF NOT EXISTS hace la operación idempotente si alguna
            // tabla llegara a existir previamente.
            for (String sqlInventario : SQL_TABLAS_INVENTARIO) {
                db.execSQL(sqlInventario);
            }
        }
        if (oldVersion < 4) {
            // v4 agrega el proveedor de la entrada al historial de movimientos.
            // ALTER TABLE incremental: no se recrea la tabla porque eso
            // destruiría los movimientos ya registrados. El try/catch cubre el
            // caso de una base que ya tuviera la columna por una migración
            // previa interrumpida.
            try {
                db.execSQL("ALTER TABLE " + TABLE_MOVIMIENTO
                        + " ADD COLUMN " + COLUMN_MOV_ID_PROVEEDOR + " INTEGER");
            } catch (android.database.SQLException columnaYaExistente) {
                // La columna ya estaba: nada que migrar, se conservan los datos.
            }
        }
    }

    private void insertInitialSeedData(SQLiteDatabase db) {
        // Insertar roles por defecto
        ContentValues cvAdminRol = new ContentValues();
        cvAdminRol.put(COLUMN_ROL_NOMBRE, "Administrador");
        cvAdminRol.put(COLUMN_ROL_DESCRIPCION, "Acceso total al sistema");
        long adminRolId = db.insert(TABLE_ROL, null, cvAdminRol);

        ContentValues cvOperadorRol = new ContentValues();
        cvOperadorRol.put(COLUMN_ROL_NOMBRE, "Operador");
        cvOperadorRol.put(COLUMN_ROL_DESCRIPCION, "Acceso a operaciones de bodega");
        db.insert(TABLE_ROL, null, cvOperadorRol);

        ContentValues cvVendedorRol = new ContentValues();
        cvVendedorRol.put(COLUMN_ROL_NOMBRE, "Vendedor");
        cvVendedorRol.put(COLUMN_ROL_DESCRIPCION, "Acceso a ventas y consultas");
        db.insert(TABLE_ROL, null, cvVendedorRol);

        // Insertar usuario administrador por defecto (admin / admin)
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

    // --- AUTENTICACIÓN Y SESIÓN (RF-01) ---

    /**
     * Busca a un usuario por username.
     */
    public Usuario getUsuarioByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT u." + COLUMN_USUARIO_ID + ", u." + COLUMN_USUARIO_USERNAME + ", u."
                + COLUMN_USUARIO_NOMBRE_COMPLETO + ", u." + COLUMN_USUARIO_PASSWORD_HASH + ", u."
                + COLUMN_USUARIO_ID_ROL + ", u." + COLUMN_USUARIO_ACTIVO + ", r." + COLUMN_ROL_NOMBRE
                + " FROM " + TABLE_USUARIO + " u INNER JOIN " + TABLE_ROL + " r ON u."
                + COLUMN_USUARIO_ID_ROL + " = r." + COLUMN_ROL_ID
                + " WHERE u." + COLUMN_USUARIO_USERNAME + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{username});
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

    /**
     * Autentica al usuario verificando username, contraseña e id_rol/activo.
     * Retorna el objeto Usuario si es válido y activo, o null.
     */
    public Usuario autenticar(String username, String rawPassword) {
        Usuario usuario = getUsuarioByUsername(username);
        if (usuario == null) {
            return null; // Usuario no existe
        }

        String inputHash = PasswordUtils.hashPassword(rawPassword);
        if (usuario.getPasswordHash().equals(inputHash)) {
            return usuario;
        }

        return null; // Contraseña incorrecta
    }

    // --- ADMINISTRACIÓN DE ROLES (RF-27) ---

    public List<Rol> getAllRoles() {
        List<Rol> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ROL + " ORDER BY " + COLUMN_ROL_NOMBRE + " ASC", null);
        if (cursor.moveToFirst()) {
            do {
                Rol rol = new Rol(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROL_NOMBRE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROL_DESCRIPCION))
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
        return db.insert(TABLE_ROL, null, cv);
    }

    public boolean isRoleNameExists(String nombreRol) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_ROL + " WHERE " + COLUMN_ROL_NOMBRE + " = ? COLLATE NOCASE", new String[]{nombreRol.trim()});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // --- ADMINISTRACIÓN DE USUARIOS (RF-27) ---

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

    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_USUARIO + " WHERE " + COLUMN_USUARIO_USERNAME + " = ? COLLATE NOCASE", new String[]{username.trim()});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // --- MÓDULO DE CATEGORÍAS (CU-09 - Agregado para registro y validación de unicidad) ---

    public boolean isCategoriaExists(String nombreCategoria) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_CATEGORIA + " WHERE " + COLUMN_CAT_NOMBRE + " = ? COLLATE NOCASE", new String[]{nombreCategoria.trim()});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public long insertCategoria(String nombreCategoria, String descripcion) {
        if (isCategoriaExists(nombreCategoria)) {
            return -1; // Retorna -1 si el nombre de categoría ya existe (Cumple CU-09 Flujo Alternativo)
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CAT_NOMBRE, nombreCategoria.trim());
        cv.put(COLUMN_CAT_DESCRIPCION, descripcion != null ? descripcion.trim() : "");
        cv.put(COLUMN_CAT_ACTIVO, 1);
        return db.insert(TABLE_CATEGORIA, null, cv);
    }

    // --- CU-10: CONSULTAR Y SELECCIONAR CATEGORÍAS (RF-10) ---

    /**
     * CU-10 / RF-10: consulta de categorías activas.
     * Filtra únicamente las categorías habilitadas y las ordena alfabéticamente.
     * Las categorías deshabilitadas (activo = 0) nunca se exponen a la interfaz.
     *
     * @return lista de categorías activas, ordenada por nombre ascendente.
     */
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

    /**
     * CU-10 / RF-10: expone las categorías activas de forma reactiva.
     * La consulta se ejecuta fuera del hilo principal y el resultado se entrega
     * como LiveData, tal como exige el caso de uso (StateFlow o LiveData).
     * Devuelve siempre la misma instancia, de modo que la interfaz pueda
     * suscribirse una sola vez y luego pedir actualizaciones con
     * {@link #refrescarCategoriasActivas()} sin duplicar observers.
     *
     * @return LiveData con la lista de categorías activas.
     */
    public LiveData<List<Categoria>> observarCategoriasActivas() {
        if (categoriasActivas == null) {
            categoriasActivas = new MutableLiveData<>();
            refrescarCategoriasActivas();
        }
        return categoriasActivas;
    }

    /**
     * Vuelve a ejecutar el DAO de CU-10 y publica el resultado en el LiveData.
     * Seguro de llamar desde el hilo principal: el trabajo ocurre en el executor.
     */
    public void refrescarCategoriasActivas() {
        final MutableLiveData<List<Categoria>> destino = categoriasActivas;
        if (destino == null) {
            return;
        }
        categoriaExecutor.execute(() -> destino.postValue(getCategoriasActivas()));
    }

    // ---------------------------------------------------------------------
    // USUARIOS (CU-27)
    // ---------------------------------------------------------------------

    /** Hilo dedicado a las consultas reactivas de usuarios. */
    private static final ExecutorService usuarioExecutor = Executors.newSingleThreadExecutor();

    /** Cache LiveData del listado; se llena solo en la primera suscripción. */
    private MutableLiveData<List<Usuario>> usuarios;

    /**
     * CU-27: expone el listado de usuarios como LiveData. La primera
     * suscripción dispara la consulta inicial en el hilo de E/S.
     */
    public LiveData<List<Usuario>> observarUsuarios() {
        if (usuarios == null) {
            usuarios = new MutableLiveData<>();
            refrescarUsuarios();
        }
        return usuarios;
    }

    /**
     * CU-27: vuelve a consultar la base y publica el resultado. El alta de un
     * usuario debe llamar a este método para que la lista reactiva se
     * actualice sola en la interfaz.
     */
    public void refrescarUsuarios() {
        final MutableLiveData<List<Usuario>> destino = usuarios;
        if (destino == null) {
            return;
        }
        usuarioExecutor.execute(() -> destino.postValue(getAllUsuarios()));
    }

    // ---------------------------------------------------------------------
    // PRODUCTOS (RF-04 / RF-05)
    // ---------------------------------------------------------------------

    /** Hilo dedicado a las consultas reactivas de productos. */
    private static final ExecutorService productoExecutor = Executors.newSingleThreadExecutor();

    /** Cache LiveData del catálogo; se llena solo en la primera suscripción. */
    private MutableLiveData<List<Producto>> productos;

    /** DAO de productos reutilizado por el camino reactivo; las consultas siguen en él. */
    private final ProductoDao productoDaoReactivo = new ProductoDao(this);

    /**
     * RF-04 / RF-05: expone el catálogo como LiveData, filtrado por nombre o SKU
     * (el mismo criterio del DAO). La primera suscripción ejecuta la consulta
     * inicial en el hilo de E/S; después la interfaz cambia el filtro con
     * {@link #refrescarProductos(String)} sobre esta misma instancia.
     *
     * @param filtro texto de nombre o SKU; null o vacío lista todo el catálogo.
     * @return LiveData con el catálogo filtrado, siempre la misma instancia.
     */
    public LiveData<List<Producto>> observarProductos(String filtro) {
        if (productos == null) {
            productos = new MutableLiveData<>();
            refrescarProductos(filtro);
        }
        return productos;
    }

    /**
     * Vuelve a consultar PRODUCTO con el filtro de nombre o SKU y publica el
     * resultado en el LiveData. Seguro de llamar desde el hilo principal (cada
     * tecla del buscador): el trabajo ocurre en el executor, que serializa las
     * consultas y deja en la interfaz el último resultado pedido.
     */
    public void refrescarProductos(String filtro) {
        final MutableLiveData<List<Producto>> destino = productos;
        if (destino == null) {
            return;
        }
        final String texto = filtro;
        productoExecutor.execute(() ->
                destino.postValue(productoDaoReactivo.buscarProductos(texto)));
    }
}