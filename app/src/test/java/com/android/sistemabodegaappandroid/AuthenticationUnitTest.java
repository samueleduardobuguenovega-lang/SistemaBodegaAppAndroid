package com.android.sistemabodegaappandroid;

import org.junit.Test;

import static org.junit.Assert.*;

import com.android.sistemabodegaappandroid.model.Rol;
import com.android.sistemabodegaappandroid.model.Usuario;
import com.android.sistemabodegaappandroid.util.PasswordUtils;

public class AuthenticationUnitTest {

    @Test
    public void testPasswordHashing() {
        String rawPassword = "admin";
        String hash1 = PasswordUtils.hashPassword(rawPassword);
        String hash2 = PasswordUtils.hashPassword("admin");
        String hashDifferent = PasswordUtils.hashPassword("different");

        assertNotNull(hash1);
        assertFalse(hash1.isEmpty());
        // La sal aleatoria hace que dos hashes de la misma contraseña difieran.
        assertNotEquals(hash1, hash2);
        assertNotEquals(hash1, hashDifferent);
        assertTrue(hash1.startsWith("pbkdf2$"));
        // Ambos siguen validando la misma contraseña.
        assertTrue(PasswordUtils.verificar(rawPassword, hash1));
        assertTrue(PasswordUtils.verificar(rawPassword, hash2));
    }

    @Test
    public void testRolModel() {
        Rol rol = new Rol(1, "Administrador", "Acceso total");
        assertEquals(1, rol.getIdRol());
        assertEquals("Administrador", rol.getNombreRol());
        assertEquals("Acceso total", rol.getDescripcion());
        assertEquals("Administrador", rol.toString());
    }

    @Test
    public void testUsuarioModel() {
        Usuario usuario = new Usuario(1, "admin", "Administrador del Sistema", "hash", 1, "Administrador", 1);
        assertEquals(1, usuario.getIdUsuario());
        assertEquals("admin", usuario.getUsername());
        assertEquals("Administrador del Sistema", usuario.getNombreCompleto());
        assertEquals(1, usuario.getActivo());
        assertTrue(usuario.isActivo());

        usuario.setActivo(0);
        assertFalse(usuario.isActivo());
    }
}
