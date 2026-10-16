package com.android.sistemabodegaappandroid.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contraseñas.
 *
 * Formato nuevo (autodescriptivo, 4 campos separados por `$`):
 * {@code pbkdf2$<iteraciones>$<sal base64>$<hash base64>},
 * por ejemplo {@code pbkdf2$120000$AAAA...$BBBB...}.
 *
 * Los hashes heredados (SHA-256 en hexadecimal, sin sal) siguen siendo
 * verificables mediante {@link #verificar(String, String)} para que las bases
 * ya instaladas no pierdan el login; {@link #necesitaRehash(String)} indica
 * cuándo conviene reescribirlos con el formato nuevo.
 */
public final class PasswordUtils {

    /** Prefijo del formato nuevo; también marca el corte entre formatos. */
    private static final String PREFIJO_NUEVO = "pbkdf2";

    private static final String ALGORITMO_NUEVO = "PBKDF2WithHmacSHA256";
    private static final String ALGORITMO_LEGADO = "SHA-256";

    private static final int ITERACIONES = 120000;
    private static final int TAMANO_SAL_BYTES = 16;
    private static final int LARGO_CLAVE_BITS = 256;

    /** Tope defensivo: una iteración absurda en un registro dañado no debe colgar la app. */
    private static final int ITERACIONES_MAXIMAS = 10000000;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private PasswordUtils() {
        // Utilidad puramente estática.
    }

    /**
     * Genera un hash PBKDF2 con sal aleatoria de 16 bytes, 120000 iteraciones
     * y clave de 256 bits.
     *
     * Nunca devuelve la contraseña en claro: si el algoritmo no existe se
     * lanza {@link IllegalStateException} en lugar de degradar a un valor
     * inseguro.
     *
     * @param password contraseña en texto plano; {@code null} produce "".
     * @return cadena {@code pbkdf2$iteraciones$sal$hash}.
     */
    public static String hashPassword(String password) {
        if (password == null) {
            return "";
        }
        byte[] sal = new byte[TAMANO_SAL_BYTES];
        ALEATORIO.nextBytes(sal);
        byte[] derivado = derivarPbkdf2(password.toCharArray(), sal, ITERACIONES);
        Base64.Encoder encoder = Base64.getEncoder();
        return PREFIJO_NUEVO + "$" + ITERACIONES + "$"
                + encoder.encodeToString(sal) + "$"
                + encoder.encodeToString(derivado);
    }

    /**
     * Verifica una contraseña contra el hash almacenado, sea del formato nuevo
     * (PBKDF2) o heredado (SHA-256 hexadecimal).
     *
     * La comparación es de tiempo constante ({@link MessageDigest#isEqual}).
     *
     * @param passwordPlano contraseña que escribe el usuario.
     * @param hashAlmacenado hash guardado en la base.
     * @return true si la contraseña coincide.
     */
    public static boolean verificar(String passwordPlano, String hashAlmacenado) {
        if (passwordPlano == null || hashAlmacenado == null || hashAlmacenado.isEmpty()) {
            return false;
        }
        if (hashAlmacenado.startsWith(PREFIJO_NUEVO)) {
            return verificarPbkdf2(passwordPlano, hashAlmacenado);
        }
        // Hash heredado: SHA-256 en hexadecimal sobre UTF-8 (como lo escribía
        // la versión anterior), para que las bases instaladas sigan entrando.
        String esperado = sha256Hex(passwordPlano);
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                hashAlmacenado.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Indica si el hash guardado debe reescribirse con el formato nuevo.
     *
     * @param hashAlmacenado hash guardado en la base.
     * @return true cuando el hash no es del formato pbkdf2 (incluye nulos).
     */
    public static boolean necesitaRehash(String hashAlmacenado) {
        if (hashAlmacenado == null || hashAlmacenado.isEmpty()) {
            return true;
        }
        String[] campos = hashAlmacenado.split("\\$", -1);
        return campos.length != 4 || !PREFIJO_NUEVO.equals(campos[0]);
    }

    /** Recalcula PBKDF2 con la sal y las iteraciones guardadas y compara en tiempo constante. */
    private static boolean verificarPbkdf2(String passwordPlano, String hashAlmacenado) {
        String[] campos = hashAlmacenado.split("\\$", -1);
        if (campos.length != 4 || !PREFIJO_NUEVO.equals(campos[0])) {
            return false;
        }
        int iteraciones;
        try {
            iteraciones = Integer.parseInt(campos[1]);
        } catch (NumberFormatException numeroInvalido) {
            return false;
        }
        if (iteraciones < 1 || iteraciones > ITERACIONES_MAXIMAS) {
            return false;
        }
        try {
            byte[] sal = Base64.getDecoder().decode(campos[2]);
            byte[] guardado = Base64.getDecoder().decode(campos[3]);
            if (sal.length == 0 || guardado.length == 0) {
                return false;
            }
            byte[] calculado = derivarPbkdf2(passwordPlano.toCharArray(), sal, iteraciones);
            return MessageDigest.isEqual(guardado, calculado);
        } catch (IllegalArgumentException base64Invalido) {
            return false;
        }
    }

    /** Deriva la clave; si el algoritmo no existe, falla explícito y jamás devuelve el dato en claro. */
    private static byte[] derivarPbkdf2(char[] password, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(password, sal, iteraciones, LARGO_CLAVE_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITMO_NUEVO);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException error) {
            throw new IllegalStateException(
                    "No se pudo calcular el hash de la contraseña: falta el algoritmo "
                            + ALGORITMO_NUEVO, error);
        } finally {
            spec.clearPassword();
        }
    }

    /** SHA-256 en hexadecimal minúsculas (formato heredado). */
    private static String sha256Hex(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_LEGADO);
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(
                    "No se pudo verificar la contraseña: falta el algoritmo "
                            + ALGORITMO_LEGADO, error);
        }
    }
}
