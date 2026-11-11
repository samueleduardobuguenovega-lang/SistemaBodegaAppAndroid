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
    public static final String INFORMES = "INFORMES";

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

    /**
     * Arma el valor de ROL.permisos a partir de las casillas del editor de roles.
     *
     * Vive acá y no dentro del diálogo porque ahí se armaba a mano sumando los
     * tokens de las casillas: cuando se agregó INFORMES (RF-19) nadie actualizó
     * esa suma y editar un rol borraba el permiso del módulo de informes.
     *
     * @return los tokens marcados, en el orden del editor, unidos por coma.
     */
    public static String unir(boolean usuarios, boolean productos, boolean categorias,
                              boolean proveedores, boolean movimientos, boolean informes) {
        StringBuilder csv = new StringBuilder();
        agregarToken(csv, usuarios, USUARIOS);
        agregarToken(csv, productos, PRODUCTOS);
        agregarToken(csv, categorias, CATEGORIAS);
        agregarToken(csv, proveedores, PROVEEDORES);
        agregarToken(csv, movimientos, MOVIMIENTOS);
        agregarToken(csv, informes, INFORMES);
        return csv.toString();
    }

    private static void agregarToken(StringBuilder csv, boolean marcado, String token) {
        if (!marcado) {
            return;
        }
        if (csv.length() > 0) {
            csv.append(',');
        }
        csv.append(token);
    }

    /**
     * Devuelve el CSV con {@code permiso} agregado si no estaba.
     *
     * Lo usa la migración a la v9: un rol que ya tenía permisos guardados no
     * recibía los tokens nuevos, así que el módulo quedaba inalcanzable.
     *
     * @param permisosCsv valor actual de ROL.permisos; puede ser null o vacío.
     * @param permiso     token a asegurar.
     * @return la lista resultante, sin duplicados.
     */
    public static String agregar(String permisosCsv, String permiso) {
        if (permiso == null || permiso.trim().isEmpty()) {
            return permisosCsv != null ? permisosCsv : "";
        }
        if (tiene(permisosCsv, permiso)) {
            return permisosCsv;
        }
        String base = permisosCsv != null ? permisosCsv.trim() : "";
        String limpio = permiso.trim().toUpperCase(Locale.ROOT);
        return base.isEmpty() ? limpio : base + "," + limpio;
    }
}
