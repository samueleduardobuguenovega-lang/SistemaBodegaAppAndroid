#!/usr/bin/env python3
"""Simula la migración v5 contra SQLite real y comprueba el relleno de permisos.

No necesita Android: reproduce el esquema de la versión 4, aplica el ALTER TABLE
y los dos UPDATE que lleva el código de DatabaseHelper, y mide el resultado.

Responde dos preguntas que una prueba que lee el código como texto no responde:
  1. ¿el defecto era real? (sin relleno, los roles preexistentes quedan sin
     permisos y todos los módulos quedan negados, administrador incluido)
  2. ¿el relleno deja el acceso como lo tenía la versión anterior y no pisa los
     permisos que el usuario ya había definido?
"""
import sqlite3
import sys

TODOS = "USUARIOS,PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS"
INVENTARIO = "PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS"

ESQUEMA_V4 = """
CREATE TABLE ROL (
    id_rol INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre_rol TEXT UNIQUE NOT NULL,
    descripcion TEXT
);
CREATE TABLE USUARIO (
    id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    id_rol INTEGER NOT NULL REFERENCES ROL(id_rol)
);
INSERT INTO ROL (nombre_rol, descripcion) VALUES
    ('Administrador', 'Acceso total al sistema'),
    ('Operador', 'Acceso a operaciones de bodega'),
    ('Vendedor', 'Acceso a ventas y consultas'),
    ('Jefe de turno', 'Rol propio del negocio');
INSERT INTO USUARIO (username, id_rol) VALUES ('admin', 1), ('samuel', 3);
"""


def permisos_de_rol(con, nombre):
    fila = con.execute("SELECT permisos FROM ROL WHERE nombre_rol = ?", (nombre,)).fetchone()
    valor = fila[0] if fila else None
    return valor if valor is not None else ""


def modulos_habilitados(csv, modulos=TODOS.split(",")):
    tokens = [t.strip().upper() for t in (csv or "").split(",")]
    return [m for m in modulos if m in tokens]


def migrar(con, con_relleno):
    # Igual que agregarColumnaSiFalta: la columna se agrega una sola vez, y la
    # guarda es lo que hace que la migracion se pueda repetir sin romper.
    columnas = [fila[1] for fila in con.execute("PRAGMA table_info(ROL)")]
    if "permisos" not in columnas:
        con.execute("ALTER TABLE ROL ADD COLUMN permisos TEXT")
    if not con_relleno:
        return
    sin_permisos = " AND (permisos IS NULL OR TRIM(permisos) = '')"
    con.execute("UPDATE ROL SET permisos = ? WHERE UPPER(TRIM(nombre_rol)) = 'ADMINISTRADOR'" + sin_permisos, (TODOS,))
    con.execute("UPDATE ROL SET permisos = ? WHERE UPPER(TRIM(nombre_rol)) <> 'ADMINISTRADOR'" + sin_permisos, (INVENTARIO,))
    con.commit()


def correr(con_relleno):
    con = sqlite3.connect(":memory:")
    con.executescript(ESQUEMA_V4)
    migrar(con, con_relleno)
    return con


def main():
    fallos = 0

    # 1. El defecto: sin relleno nadie entra.
    con = correr(con_relleno=False)
    negados = [r[0] for r in con.execute("SELECT nombre_rol FROM ROL")
               if not modulos_habilitados(permisos_de_rol(con, r[0]))]
    print("SIN relleno  -> roles sin ningun modulo: %d/%d %s"
          % (len(negados), len(negados), negados))
    if len(negados) != 4:
        print("   FALLO: se esperaba que los 4 roles quedaran sin acceso")
        fallos += 1

    # 2. Con relleno: se conserva el acceso de la version anterior.
    con = correr(con_relleno=True)
    esperado = {
        "Administrador": TODOS,
        "Operador": INVENTARIO,
        "Vendedor": INVENTARIO,
        "Jefe de turno": INVENTARIO,
    }
    print("CON relleno:")
    for rol, esperada in esperado.items():
        obtenida = permisos_de_rol(con, rol)
        ok = obtenida == esperada
        if not ok:
            fallos += 1
        print("   %-14s %-47s %s" % (rol, obtenida or "(vacio)", "OK" if ok else "FALLO"))
    # el administrador no pierde la administracion y el vendedor no la gana
    if "USUARIOS" not in permisos_de_rol(con, "Administrador"):
        print("   FALLO: el administrador perdio USUARIOS"); fallos += 1
    if "USUARIOS" in permisos_de_rol(con, "Vendedor"):
        print("   FALLO: un vendedor gano la administracion"); fallos += 1

    # 3. Idempotencia: una segunda corrida no pisa lo definido por el usuario.
    con.execute("UPDATE ROL SET permisos = 'PRODUCTOS' WHERE nombre_rol = 'Operador'")
    con.commit()
    migrar(con, True)
    if permisos_de_rol(con, "Operador") != "PRODUCTOS":
        print("   FALLO: la migracion piso permisos ya definidos"); fallos += 1
    else:
        print("IDEMPOTENCIA -> 'Operador' conserva 'PRODUCTOS' tras repetir la migracion  OK")

    print("\nRESULTADO: %s" % ("TODO OK" if fallos == 0 else "%d FALLOS" % fallos))
    return 1 if fallos else 0


if __name__ == "__main__":
    sys.exit(main())
