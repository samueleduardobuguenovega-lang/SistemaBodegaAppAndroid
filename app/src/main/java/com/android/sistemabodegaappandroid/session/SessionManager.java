package com.android.sistemabodegaappandroid.session;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.android.sistemabodegaappandroid.LoginActivity;
import com.android.sistemabodegaappandroid.model.Usuario;

import java.io.IOException;
import java.security.GeneralSecurityException;
import com.android.sistemabodegaappandroid.db.DatabaseHelper;
import com.android.sistemabodegaappandroid.util.Permisos;

public class SessionManager {
    private static final String PREF_NAME = "SistemaBodegaSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_NOMBRE_COMPLETO = "nombreCompleto";
    private static final String KEY_ROLE_ID = "roleId";
    private static final String KEY_ROLE_NAME = "roleName";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = abrirPreferenciasCifradas(context);
        editor = pref.edit();
    }

    /**
     * Abre las preferencias de sesión cifradas con una MasterKey del keystore.
     *
     * El CU-01 paso 7 exige que la sesión no quede en disco en claro. Como las
     * preferencias cambian de nombre/almacenamiento al cifrarse, una sesión que
     * existiera en las preferencias planas anteriores no se migra: ese usuario
     * vuelve a la pantalla de login una sola vez, que es lo esperable.
     *
     * minSdk 30, así que no hace falta ninguna guarda por versión de Android.
     */
    private static SharedPreferences abrirPreferenciasCifradas(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException error) {
            // Sin keystore no hay sesión segura: se falla explícito en lugar de
            // degradar a preferencias en claro, que es justamente lo que se evita.
            throw new IllegalStateException(
                    "No se pudieron abrir las preferencias de sesión cifradas", error);
        }
    }

    public void createLoginSession(Usuario usuario) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, usuario.getIdUsuario());
        editor.putString(KEY_USERNAME, usuario.getUsername());
        editor.putString(KEY_NOMBRE_COMPLETO, usuario.getNombreCompleto());
        editor.putInt(KEY_ROLE_ID, usuario.getIdRol());
        editor.putString(KEY_ROLE_NAME, usuario.getNombreRol() != null ? usuario.getNombreRol() : "");
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return pref.getString(KEY_USERNAME, "");
    }

    public String getNombreCompleto() {
        return pref.getString(KEY_NOMBRE_COMPLETO, "");
    }

    public int getRoleId() {
        return pref.getInt(KEY_ROLE_ID, -1);
    }

    public String getRoleName() {
        return pref.getString(KEY_ROLE_NAME, "");
    }

    /**
     * Indica si el usuario en sesion tiene privilegios de Administrador.
     *
     * Los permisos se leen de ROL por el id del rol guardado en la sesion, no
     * por su nombre: renombrar «Administrador» no debe cambiar lo que el rol
     * puede hacer, y un rol nuevo con el permiso marcado tiene que entrar.
     *
     * @return true solo si el rol de la sesion declara el permiso USUARIOS.
     */
    public boolean esAdministrador() {
        return tienePermiso(Permisos.USUARIOS);
    }

    /**
     * Indica si el rol del usuario en sesión tiene habilitado un módulo.
     *
     * Se consulta ROL.permisos en la base y no una copia en las preferencias:
     * el administrador puede cambiar los permisos de un rol y el efecto debe
     * notarse al abrir el módulo, sin volver a iniciar sesión. El id del rol sí
     * se guarda en la sesión porque no cambia.
     *
     * @param permiso token de {@link Permisos}.
     * @return true solo si hay sesión, el rol existe y declara ese permiso.
     */
    public boolean tienePermiso(String permiso) {
        int idRol = pref.getInt(KEY_ROLE_ID, -1);
        if (idRol <= 0) {
            return false;
        }
        return Permisos.tiene(new DatabaseHelper(context).getPermisosDeRol(idRol), permiso);
    }

    public void logoutUser() {
        editor.clear();
        editor.apply();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
