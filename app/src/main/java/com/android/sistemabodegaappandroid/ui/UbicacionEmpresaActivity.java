package com.android.sistemabodegaappandroid.ui;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.sistemabodegaappandroid.R;
import com.android.sistemabodegaappandroid.session.SessionManager;
import com.android.sistemabodegaappandroid.util.EdgeToEdgeInsets;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

/**
 * Visualización de la Ubicación de la Empresa mediante Google Maps (CU-08 / RF-24).
 */
public class UbicacionEmpresaActivity extends AppCompatActivity {

    // Coordenadas fijas configurables de la empresa
    private static final double LATITUD_EMPRESA = -30.605015194619963;
    private static final double LONGITUD_EMPRESA = -71.20709579140474;
    private static final String NOMBRE_EMPRESA = "Mercadito P&P";

    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    abrirRutaEnGoogleMaps();
                } else {
                    // Flujo alternativo: permiso de ubicación denegado
                    Toast.makeText(this, R.string.msg_permiso_ubicacion_denegado, Toast.LENGTH_LONG).show();
                    abrirUbicacionFijaEnMaps();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Precondición CU-08: usuario autenticado
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_ubicacion_empresa);
        EdgeToEdgeInsets.apply(this, findViewById(R.id.rootUbicacionEmpresa));

        MaterialToolbar toolbar = findViewById(R.id.toolbarUbicacionEmpresa);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        MaterialButton btnAbrirMaps = findViewById(R.id.btnAbrirGoogleMaps);
        MaterialButton btnComoLlegar = findViewById(R.id.btnComoLlegar);

        // Mapa real del local (OpenStreetMap embebido, sin API key).
        WebView webMapa = findViewById(R.id.webMapa);
        if (webMapa != null) {
            WebSettings ajustes = webMapa.getSettings();
            ajustes.setJavaScriptEnabled(true);
            ajustes.setDomStorageEnabled(true);
            webMapa.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_container));
            webMapa.setWebViewClient(new WebViewClient());
            webMapa.loadDataWithBaseURL("https://www.openstreetmap.org/", htmlMapa(), "text/html", "utf-8", null);
        }

        btnAbrirMaps.setOnClickListener(v -> abrirUbicacionFijaEnMaps());
        btnComoLlegar.setOnClickListener(v -> verificarPermisoYCalcularRuta());
    }

    /**
     * HTML del mapa. Leaflet en vez del embed público de openstreetmap.org: el embed
     * pinta el cartel de colaboración/donaciones encima del mapa y en una tarjeta de
     * 280 dp se come la mitad útil. Acá la atribución es una línea chica y los
     * controles van abajo a la derecha, fuera del marcador.
     */
    private String htmlMapa() {
        return "<!doctype html><html><head>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"
                + "<style>"
                + "html,body,#mapa{margin:0;padding:0;height:100%;width:100%;background:#B3DEFF}"
                + ".leaflet-control-attribution{font-size:9px;line-height:1.2;padding:1px 4px;"
                + "background:rgba(255,255,255,.8);color:#5F6368}"
                + ".leaflet-control-attribution a{color:#1565C0}"
                + ".leaflet-control-zoom a{width:26px;height:26px;line-height:26px;font-size:15px}"
                + "</style></head><body><div id='mapa'></div>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<script>"
                + "var lat=" + LATITUD_EMPRESA + ",lng=" + LONGITUD_EMPRESA + ";"
                + "var m=L.map('mapa',{zoomControl:false,attributionControl:true});"
                + "m.attributionControl.setPrefix(false);"
                + "m.setView([lat,lng],15);"
                + "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,"
                + "attribution:'\u00a9 OpenStreetMap contributors'}).addTo(m);"
                + "L.control.zoom({position:'bottomright'}).addTo(m);"
                + "L.marker([lat,lng]).addTo(m).bindTooltip('Bodega Central Express',{permanent:false});"
                + "</script></body></html>";
    }

    private void abrirUbicacionFijaEnMaps() {
        String uriGeo = String.format(java.util.Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                LATITUD_EMPRESA, LONGITUD_EMPRESA, LATITUD_EMPRESA, LONGITUD_EMPRESA, Uri.encode(NOMBRE_EMPRESA));
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriGeo));
        intent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Flujo alternativo: Google Maps app no instalada, abre URL web compatible
            String urlWeb = String.format(java.util.Locale.US, "https://www.google.com/maps/search/?api=1&query=%f,%f",
                    LATITUD_EMPRESA, LONGITUD_EMPRESA);
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlWeb));
            try {
                startActivity(webIntent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, R.string.msg_maps_no_disponible, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void verificarPermisoYCalcularRuta() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            abrirRutaEnGoogleMaps();
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void abrirRutaEnGoogleMaps() {
        String urlRuta = String.format(java.util.Locale.US, "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                LATITUD_EMPRESA, LONGITUD_EMPRESA);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlRuta));
        intent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlRuta));
            try {
                startActivity(webIntent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, R.string.msg_maps_no_disponible, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
