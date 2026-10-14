package com.android.sistemabodegaappandroid.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.sistemabodegaappandroid.model.Categoria;
import com.android.sistemabodegaappandroid.model.Rol;
import com.android.sistemabodegaappandroid.model.Usuario;
import com.android.sistemabodegaappandroid.util.PasswordUtils;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sistema_bodega.db";
    private static final int DATABASE_VERSION = 2; // de 1 a 2

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

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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

        // Insertar roles iniciales de prueba
        insertInitialSeedData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USUARIO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROL);
        onCreate(db);
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

    public List<Categoria> getAllCategorias() {
        List<Categoria> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIA + " ORDER BY " + COLUMN_CAT_NOMBRE + " ASC", null);
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
}