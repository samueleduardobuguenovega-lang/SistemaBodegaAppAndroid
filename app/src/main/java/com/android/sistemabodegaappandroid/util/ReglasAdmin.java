package com.android.sistemabodegaappandroid.util;

/**
 * Reglas de administración, a prueba de errores humanos.
 *
 * Vive aparte de la UI para poder probarse sin Android y para que la misma
 * regla valga en el diálogo de usuarios, en el de roles y en la base. La idea
 * es simple: el sistema nunca puede quedarse sin un administrador, y nadie
 * puede quitarse a sí mismo el acceso que está usando.
 */
public final class ReglasAdmin {

    private ReglasAdmin() {
    }

    /**
     * ¿Se puede cambiar el rol del usuario objetivo?
     *
     * @param idObjetivo          usuario que se está editando
     * @param idActual            usuario de la sesión
     * @param objetivoEsUltimoAdmin true si el objetivo es el único admin activo
     * @param nuevoRolEsAdmin     true si el rol elegido conserva la administración
     */
    public static boolean puedeCambiarRol(int idObjetivo, int idActual,
                                          boolean objetivoEsUltimoAdmin, boolean nuevoRolEsAdmin) {
        // Nadie se quita su propio acceso de administración: es el error que
        // deja el sistema sin nadie que pueda arreglarlo.
        if (idObjetivo == idActual && !nuevoRolEsAdmin) {
            return false;
        }
        // Aunque sea otro usuario, el sistema no puede quedar sin admins.
        if (objetivoEsUltimoAdmin && !nuevoRolEsAdmin) {
            return false;
        }
        return true;
    }

    /**
     * ¿Se puede eliminar (o desactivar) la cuenta objetivo? Nunca la propia y
     * nunca la del último administrador activo.
     */
    public static boolean puedeEliminar(int idObjetivo, int idActual, boolean objetivoEsUltimoAdmin) {
        return idObjetivo != idActual && !objetivoEsUltimoAdmin;
    }

    /**
     * ¿Se puede borrar el rol objetivo? No si es el rol de la sesión: borrarlo
     * deja al usuario sin rol y sin forma de volver.
     */
    public static boolean puedeEliminarRol(int idRolObjetivo, int idRolActual) {
        return idRolObjetivo != idRolActual;
    }

    /**
     * ¿Se puede guardar un rol sin el permiso de usuarios?
     *
     * Quitarle USUARIOS al rol propio cierra la puerta desde adentro: la app
     * sigue abierta, pero el módulo de administración deja de estar permitido.
     * Solo se admite si queda otro administrador activo que pueda revertirlo.
     */
    public static boolean puedeQuitarPermisoUsuarios(int idRolObjetivo, int idRolActual,
                                                     boolean conservaPermisoUsuarios,
                                                     boolean quedanOtrosAdmins) {
        if (idRolObjetivo != idRolActual) {
            return true;
        }
        if (conservaPermisoUsuarios) {
            return true;
        }
        return quedanOtrosAdmins;
    }
}
