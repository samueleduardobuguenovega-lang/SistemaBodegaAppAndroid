package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.view.ContextThemeWrapper;

import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Layouts de las pantallas nuevas: el escáner (RF-20) y los informes (RF-19).
 *
 * En el escáner, la pantalla anterior mostraba una retícula dibujada con un View
 * y el texto "enfoque el código", pero no había cámara. Acá se fija que el
 * layout conserve el componente que sí decodifica y el ingreso manual como
 * respaldo. En informes, que los filtros del stock sigan en su propia fila: son
 * los que hay que ocultar al mirar el historial.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EscannerLayoutTest {

    private View inflar(int layout) {
        Context ctx = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
                R.style.Theme_SistemaBodegaAppAndroid);
        return LayoutInflater.from(ctx).inflate(layout, null, false);
    }

    @Test
    public void elEscanerIncluyeLaVistaDeCamaraYElIngresoManual() {
        View escaner = inflar(R.layout.activity_barcode_scanner);

        View camara = escaner.findViewById(R.id.barcodeView);
        assertNotNull("falta la vista de la cámara en el escáner", camara);
        assertTrue("la vista de la cámara debe ser la del decodificador ZXing",
                camara instanceof DecoratedBarcodeView);
        assertNotNull("el ingreso manual debe seguir disponible", escaner.findViewById(R.id.etCodigoManual));
    }

    @Test
    public void losInformesSeparanLosFiltrosDeStock() {
        View informes = inflar(R.layout.activity_informes);

        assertNotNull("el informe de stock necesita su fila de filtros",
                informes.findViewById(R.id.filaFiltrosStock));
        assertNotNull(informes.findViewById(R.id.containerResumenInformes));
        assertNotNull(informes.findViewById(R.id.tabLayoutInformes));
    }
}
