package com.android.sistemabodegaappandroid.util;

import android.app.Activity;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Utilidad central para el manejo de areas seguras (safe areas).
 *
 * Con targetSdk 35 o superior Android fuerza edge-to-edge: la app dibuja detras
 * del notch y de la barra de navegacion. Sin aplicar los insets, el contenido
 * queda tapado. Esta clase aplica el padding una sola vez por pantalla.
 *
 * Se aplica sobre la vista RAIZ del layout: el fondo sigue cubriendo toda la
 * pantalla, pero el contenido deja de quedar debajo del notch y de las barras.
 */
public final class EdgeToEdgeInsets {

    private EdgeToEdgeInsets() {
    }

    /**
     * Aplica el padding de las barras del sistema a la vista raiz.
     *
     * @param activity actividad actual
     * @param root     vista raiz del layout
     */
    public static void apply(Activity activity, View root) {
        if (activity == null || root == null) {
            return;
        }

        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Guardar el padding original para no acumularlo en cada rotacion.
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            v.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + bars.bottom);
            return insets;
        });

        ViewCompat.requestApplyInsets(root);
    }

    /** Atajo para activities con findViewById del id dado. */
    public static void apply(Activity activity, int viewId) {
        if (activity == null) {
            return;
        }
        apply(activity, activity.findViewById(viewId));
    }
}