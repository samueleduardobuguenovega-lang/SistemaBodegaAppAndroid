package com.android.sistemabodegaappandroid;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifica que TODAS las activities manejen las areas seguras del sistema.
 *
 * Con targetSdk 35+ Android fuerza edge-to-edge: si una pantalla no aplica los
 * insets, el notch y la barra de navegacion tapan el contenido. Ese defecto no
 * se ve leyendo el codigo, aparece solo en un dispositivo con notch.
 */
public class SafeAreasTest {

    /** Solo las etiquetas <activity>, no las <category> ni <action>. */
    private static final Pattern ACTIVITY_TAG =
            Pattern.compile("<activity\\b[^>]*>", Pattern.DOTALL);

    private static final Pattern NAME_ATTR =
            Pattern.compile("android:name=\"(\\.?[A-Za-z0-9_.]+)\"");

    /** Activities que no muestran UI y por lo tanto no necesitan insets. */
    private static final List<String> EXENTAS = new ArrayList<>();

    /** El manifest declara categorias y acciones ademas de activities. */
    private static final List<String> NO_ACTIVITY = new ArrayList<>();

    static {
        NO_ACTIVITY.add("MAIN");
        NO_ACTIVITY.add("LAUNCHER");
        NO_ACTIVITY.add("DEFAULT");
    }

    private static Path resDir() {
        // Los tests unitarios corren con el modulo :app como directorio de trabajo.
        Path relativo = Paths.get("src", "main", "java");
        Path modulo = Paths.get("app", "src", "main", "java");
        if (Files.isDirectory(relativo)) {
            return relativo;
        }
        if (Files.isDirectory(modulo)) {
            return modulo;
        }
        throw new AssertionError("No se encontro el modulo app desde "
                + Paths.get("").toAbsolutePath());
    }

    private static Path manifest() {
        Path relativo = Paths.get("src", "main", "AndroidManifest.xml");
        if (Files.isRegularFile(relativo)) {
            return relativo;
        }
        return Paths.get("app", "src", "main", "AndroidManifest.xml");
    }

    private static List<String> activitiesDelManifest() {
        List<String> names = new ArrayList<>();
        try {
            String xml = new String(Files.readAllBytes(manifest()), StandardCharsets.UTF_8);
            Matcher tag = ACTIVITY_TAG.matcher(xml);
            while (tag.find()) {
                Matcher attr = NAME_ATTR.matcher(tag.group(0));
                if (attr.find()) {
                    String n = attr.group(1);
                    if (n.startsWith(".")) {
                        n = "com.android.sistemabodegaappandroid" + n;
                    }
                    names.add(n);
                }
            }
        } catch (Exception e) {
            throw new AssertionError("No se pudo leer el manifest: " + e.getMessage(), e);
        }
        return names;
    }

    private static String fuenteDe(String activitySimple) {
        // Buscar recursivamente el archivo por nombre, sin asumir la estructura
        // de paquetes: el directorio de trabajo del test es el modulo :app.
        try {
            java.util.stream.Stream<Path> st = Files.walk(resDir());
            try {
                java.util.Optional<Path> hit = st
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().equals(activitySimple + ".java"))
                        .findFirst();
                if (hit.isPresent()) {
                    return new String(Files.readAllBytes(hit.get()), StandardCharsets.UTF_8);
                }
            } finally {
                st.close();
            }
        } catch (Exception e) {
            throw new AssertionError("No se pudo buscar " + activitySimple + ": " + e.getMessage(), e);
        }
        return "";
    }

    @Test
    public void todaActivityAplicaAreasSeguras() {
        List<String> sinInsets = new ArrayList<>();
        for (String fqcn : activitiesDelManifest()) {
            if (EXENTAS.contains(fqcn)) {
                continue;
            }
            String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);
            String fuente = fuenteDe(simple);
            assertFalse("No se encontro el fuente de " + simple, fuente.isEmpty());
            boolean aplica = fuente.contains("EdgeToEdgeInsets.apply")
                    || fuente.contains("setOnApplyWindowInsetsListener")
                    || fuente.contains("WindowInsets");
            if (!aplica) {
                sinInsets.add(simple);
            }
        }
        assertTrue("Activities sin manejo de areas seguras (el notch las tapa): " + sinInsets,
                sinInsets.isEmpty());
    }

    @Test
    public void elHelperUsaSystemBarsYDisplayCutout() {
        String src = fuenteDe("EdgeToEdgeInsets");
        assertFalse("Falta el helper EdgeToEdgeInsets", src.isEmpty());
        // Debe considerar las barras Y el recorte del notch
        assertTrue("El helper debe aplicar systemBars",
                src.contains("WindowInsetsCompat.Type.systemBars()"));
        assertTrue("El helper debe aplicar displayCutout (notch)",
                src.contains("WindowInsetsCompat.Type.displayCutout()"));
    }

    @Test
    public void elToolbarDeAdminNoUsaSupportActionBar() {
        // setSupportActionBar descarta el titleTextColor del layout y deja el
        // titulo con el color por defecto del tema (ilegible sobre fondo azul).
        String fuente = fuenteDe("UserRoleAdminActivity");
        // Ignorar comentarios: el codigo puede documenting por que NO usar esa API.
        String codigo = fuente.replaceAll("(?s)/\\*.*?\\*/", "")
                             .replaceAll("(?m)//.*$", "");
        assertFalse("UserRoleAdminActivity usa setSupportActionBar, "
                        + "eso descarta el color blanco del titulo",
                codigo.contains("setSupportActionBar"));
        assertTrue("Debe fijar el color del titulo explicitamente",
                codigo.contains("setTitleTextColor"));
    }
}