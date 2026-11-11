#!/usr/bin/env python3
"""Simula la migración v9 contra SQLite real y comprueba el permiso de informes.

No necesita Android ni el emulador: reproduce los roles que deja la v8 (sin el
token INFORMES, porque el módulo se agregó después de que la columna existiera),
aplica el mismo paso que `DatabaseHelper.agregarPermisoInformesARolesDeInventario`
y mide el resultado.

Responde tres preguntas que una prueba que lee el código como texto no responde:
  1. ¿un rol que ya tenía permisos guardados recibe INFORMES, o queda sin acceso
     al módulo de informes para siempre?
  2. ¿un rol de ventas (sin MOVIMIENTOS) queda igual, sin ganar permisos que no
     le corresponden?
  3. ¿repetir el paso duplica el token o deja la lista intacta?
"""
import sqlite3
import sys

ESQUEMA_V8 = """
CREATE TABLE ROL (
    id_rol INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre_rol TEXT UNIQUE NOT NULL,
    descripcion TEXT,
    permisos TEXT
);
"""

ROLES_V8 = [
    ("Administrador", "Acceso total al sistema",
     "USUARIOS,PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS"),
    ("Operador", "Acceso a operaciones de bodega",
     "PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS"),
    ("Vendedor", "Acceso a ventas y consultas", "PRODUCTOS,CATEGORIAS"),
    ("Sin permisos", "Rol recién creado", None),
]


def tokens(permisos):
    return [t.strip().upper() for t in (permisos or "").split(",") if t.strip()]


def tiene(permisos, permiso):
    """Mismo criterio que Permisos.tiene: token completo, sin distinguir caja."""
    return permiso.strip().upper() in tokens(permisos)


def agregar(permisos, permiso):
    """Mismo criterio que Permisos.agregar: suma el token si falta."""
    if tiene(permisos, permiso):
        return permisos
    base = (permisos or "").strip()
    limpio = permiso.strip().upper()
    return limpio if not base else base + "," + limpio


def aplicar_v9(db):
    """Paso de migración: INFORMES a los roles que ya operan inventario."""
    cambios = []
    for id_rol, permisos in db.execute("SELECT id_rol, permisos FROM ROL").fetchall():
        if tiene(permisos, "MOVIMIENTOS") and not tiene(permisos, "INFORMES"):
            cambios.append((agregar(permisos, "INFORMES"), id_rol))
    db.executemany("UPDATE ROL SET permisos = ? WHERE id_rol = ?", cambios)
    return len(cambios)


def leer_roles(db):
    return {nombre: permisos for nombre, permisos in
            db.execute("SELECT nombre_rol, permisos FROM ROL").fetchall()}


def main():
    db = sqlite3.connect(":memory:")
    db.executescript(ESQUEMA_V8)
    db.executemany(
        "INSERT INTO ROL (nombre_rol, descripcion, permisos) VALUES (?, ?, ?)", ROLES_V8)

    print("Base v8:")
    for nombre, permisos in leer_roles(db).items():
        print(f"  {nombre:14s} {permisos or '(sin permisos)'}")

    primera = aplicar_v9(db)
    print(f"\nPaso v9 aplicado: {primera} rol(es) actualizados")
    roles = leer_roles(db)
    for nombre, permisos in roles.items():
        print(f"  {nombre:14s} {permisos or '(sin permisos)'}")

    errores = []
    if primera != 2:
        errores.append(f"se esperaban 2 roles actualizados, hubo {primera}")
    if not tiene(roles["Administrador"], "INFORMES"):
        errores.append("el Administrador quedó sin INFORMES")
    if not tiene(roles["Operador"], "INFORMES"):
        errores.append("el Operador quedó sin INFORMES")
    if not tiene(roles["Administrador"], "USUARIOS"):
        errores.append("el Administrador perdió USUARIOS")
    if tiene(roles["Vendedor"], "INFORMES"):
        errores.append("el Vendedor ganó INFORMES, que no le corresponde")
    if (roles["Sin permisos"] or "") != "":
        errores.append("un rol sin permisos no debe recibir INFORMES")

    segunda = aplicar_v9(db)
    print(f"\nSegunda ejecución: {segunda} rol(es) actualizados (se espera 0)")
    if segunda != 0:
        errores.append("el paso no es idempotente")
    if tokens(leer_roles(db)["Administrador"]).count("INFORMES") != 1:
        errores.append("INFORMES quedó duplicado")

    if errores:
        print("\nFALLÓ:")
        for error in errores:
            print("  -", error)
        return 1

    print("\nOK: los roles de inventario reciben INFORMES, el de ventas no, y repetir no duplica.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
