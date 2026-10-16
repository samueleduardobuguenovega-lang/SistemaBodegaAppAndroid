package com.android.sistemabodegaappandroid;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.android.sistemabodegaappandroid.util.PasswordUtils;

import java.util.Base64;

/**
 * Pruebas de comportamiento de PasswordUtils sobre JVM pura (sin android.*).
 *
 * Cubren el formato pbkdf2, el efecto de la sal, la verificación correcta e
 * incorrecta, la compatibilidad con el hash heredado SHA-256 y la detección
 * de rehash.
 */
public class PasswordUtilsTest {

    /** SHA-256 en hexadecimal de "admin", tal como lo escribía el código heredado. */
    private static final String LEGACY_ADMIN =
            "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918";

    @Test
    public void hashNuevoUsaFormatoPbkdf2SinContrasena() {
        String hash = PasswordUtils.hashPassword("admin");

        assertTrue("debe empezar con el prefijo pbkdf2: " + hash, hash.startsWith("pbkdf2$"));
        String[] campos = hash.split("\\$", -1);
        assertEquals("el formato tiene cuatro campos separados por $", 4, campos.length);
        assertEquals("pbkdf2", campos[0]);
        assertEquals(120000, Integer.parseInt(campos[1]));
        assertEquals("la sal mide 16 bytes", 16, Base64.getDecoder().decode(campos[2]).length);
        assertEquals("la clave deriva a 256 bits", 32, Base64.getDecoder().decode(campos[3]).length);
        assertFalse("no debe contener la contraseña en claro", hash.contains("admin"));
    }

    @Test
    public void dosHashesDeLaMismaContrasenaSonDistintos() {
        String primero = PasswordUtils.hashPassword("admin");
        String segundo = PasswordUtils.hashPassword("admin");
        String otro = PasswordUtils.hashPassword("otra");

        assertFalse("la sal hace que dos hashes de la misma contraseña difieran",
                primero.equals(segundo));
        assertFalse(primero.equals(otro));
    }

    @Test
    public void verificarAceptaLaCorrectaYRechazaLaIncorrecta() {
        String hash = PasswordUtils.hashPassword("admin");

        assertTrue(PasswordUtils.verificar("admin", hash));
        assertFalse(PasswordUtils.verificar("Admin", hash));
        assertFalse(PasswordUtils.verificar("admi", hash));
        assertFalse(PasswordUtils.verificar("", hash));
        assertFalse(PasswordUtils.verificar("admin", null));
        assertFalse(PasswordUtils.verificar(null, hash));
    }

    @Test
    public void hashHeredadoSha256SigueVerificando() {
        assertTrue(PasswordUtils.verificar("admin", LEGACY_ADMIN));
        assertFalse(PasswordUtils.verificar("no-es-esta", LEGACY_ADMIN));
        // Un hash heredado distinto de la contraseña nunca debe aceptarse.
        assertFalse(PasswordUtils.verificar("admin",
                "0000000000000000000000000000000000000000000000000000000000000000"));
    }

    @Test
    public void necesitaRehashDetectaElFormatoHeredado() {
        assertTrue(PasswordUtils.necesitaRehash(LEGACY_ADMIN));
        assertTrue(PasswordUtils.necesitaRehash(null));
        assertTrue(PasswordUtils.necesitaRehash(""));
        assertFalse(PasswordUtils.necesitaRehash(PasswordUtils.hashPassword("admin")));
    }

    @Test
    public void verificacionRechazaUnHashNuevoAlterado() {
        String hash = PasswordUtils.hashPassword("admin");
        String[] campos = hash.split("\\$", -1);

        // Se cambia un byte de la parte final: la contraseña ya no debe pasar.
        byte[] derivado = Base64.getDecoder().decode(campos[3]);
        derivado[0] ^= (byte) 0x01;
        String alterado = campos[0] + "$" + campos[1] + "$" + campos[2] + "$"
                + Base64.getEncoder().encodeToString(derivado);

        assertFalse(PasswordUtils.verificar("admin", alterado));
        assertFalse(PasswordUtils.verificar("admin", "pbkdf2$abc$sal$hash"));
        assertTrue(PasswordUtils.necesitaRehash(alterado) == false);
    }
}
