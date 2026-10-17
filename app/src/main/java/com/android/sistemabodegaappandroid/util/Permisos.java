package com.android.sistemabodegaappandroid.util;

import java.util.Locale;

/**
 * Tokens de permiso de ROL.permisos y su evaluación.
 *
 * La columna guarda los módulos habilitados como lista separada por comas
 * ("USUARIOS,PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS"). La comparación es
 * por token completo: una búsqueda por subcadena daría por habilitado
 * "PRODUCTOS" cuando la lista dice "PRODUCTOS_ANTIGUO" y arrastraría el mismo
 * defecto que tenía el panel al comparar el nombre del rol.
 */
public final class Permisos {

    public static final String USUARIOS = "USUARIOS";
    public static final String PRODUCTOS = "PRODUCTOS";
    public static final String CATEGORIAS = "CATEGORIAS";
    public static final String PROVEEDORES = "PROVEEDORES";
    public static final String MOVIMIENTOS = "MOVIMIENTOS";

    private Permisos() {
    }

    /**
     * @param permisosCsv valor crudo de ROL.permisos; puede ser null.
     * @param permiso     token buscado.
     * @return true solo si la lista contiene ese token completo, sin distinguir
     *         mayúsculas de minúsculas ni el espaciado alrededor de cada coma.
     */
    public static boolean tiene(String permisosCsv, String permiso) {
        if (permisosCsv == null || permiso == null) {
            return false;
        }
        String buscado = permiso.trim().toUpperCase(Locale.ROOT);
        if (buscado.isEmpty()) {
            return false;
        }
        for (String token : permisosCsv.split(",")) {
            if (token.trim().toUpperCase(Locale.ROOT).equals(buscado)) {
                return true;
            }
        }
        return false;
    }
}
