package com.android.sistemabodegaappandroid.util;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.android.sistemabodegaappandroid.MainActivity;
import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.session.SessionManager;

/**
 * Guarda de entrada de los módulos del panel.
 *
 * Ocultar el botón no alcanza: la actividad puede abrirse con un intent directo
 * o quedar en una tarea restaurada, así que cada módulo vuelve a validar su
 * permiso al crearse. La autoridad sigue siendo la fila de ROL en la base, no
 * la copia que la sesión guarda en las preferencias.
 */
public final class AccesoModulo {

    private AccesoModulo() {
    }

    /**
     * @param actividad actividad que está abriéndose.
     * @param permiso   permiso exigido, de {@link Permisos}.
     * @return true si la sesión lo tiene. Si no, avisa, vuelve al panel y cierra
     *         la actividad, de modo que el llamador solo tiene que salir.
     */
    public static boolean permitido(Activity actividad, String permiso) {
        SessionManager session = new SessionManager(actividad);
        if (session.isLoggedIn() && session.tienePermiso(permiso)) {
            return true;
        }

        Toast.makeText(actividad, R.string.msg_acceso_modulo_denegado, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(actividad, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        actividad.startActivity(intent);
        actividad.finish();
        return false;
    }
}
