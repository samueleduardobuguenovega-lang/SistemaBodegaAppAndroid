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
 * El escáner (RF-20) tiene que traer la vista de la cámara en el layout.
 *
 * Antes esta pantalla mostraba una retícula dibujada con un View y un texto
 * "enfoque el código", pero no había cámara: nada leía el código. Esta prueba
 * fija que el layout conserve el componente que sí decodifica, y que el ingreso
 * manual siga disponible como respaldo.
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
}
