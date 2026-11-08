#!/usr/bin/env python3
"""Simula la base rota del reporte y comprueba que onOpen la repara.

Reproduce el estado exacto que produce «no such column: permisos»: un archivo
SQLite marcado en la version 7 cuya tabla ROL no tiene la columna. Es lo que
queda cuando el numero de version sube en una copia del helper y el ALTER no lo
acompana, o cuando un merge deja el helper viejo encima del nuevo: la app se
instala, la base se marca como migrada y la columna no existe.

onUpgrade no puede arreglarla (solo corre si la version guardada es menor), asi
que la reparacion vive en onOpen. Este script la mide contra SQLite real:

  1. la base rota reproduce el error tal cual,
  2. la reparacion la deja usable y con el acceso que tenia antes,
  3. una segunda apertura no pisa los permisos que el usuario ya definio.
"""
import sqlite3
import sys

VERSION_ROTA = 7
TODOS = "USUARIOS,PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS"
INVENTARIO = "PRODUCTOS,CATEGORIAS,PROVEEDORES,MOVIMIENTOS"

# Esquema de ROL tal como quedo en una base marcada como v7 sin la columna.
ESQUEMA_ROTO = """
CREATE TABLE ROL (
    id_rol INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre_rol TEXT UNIQUE NOT NULL,
    descripcion TEXT
);
INSERT INTO ROL (nombre_rol, descripcion) VALUES
    ('Administrador', 'Acceso total al sistema'),
    ('Operador', 'Acceso a operaciones de bodega'),
    ('Jefe de turno', 'Rol propio del negocio');
"""

fallos = []


def comprobar(condicion, mensaje):
    print(("  ok   " if condicion else "  FALLA ") + mensaje)
    if not condicion:
        fallos.append(mensaje)


def columna_existe(con, tabla, columna):
    """Mismo criterio que DatabaseHelper.columnaExisteEnTabla."""
    return columna in [fila[1] for fila in con.execute("PRAGMA table_info(%s)" % tabla)]


def reparar_permisos(con):
    """Copia de DatabaseHelper.repararPermisosDeRol + su relleno."""
    if columna_existe(con, "ROL", "permisos"):
        return False
    con.execute("ALTER TABLE ROL ADD COLUMN permisos TEXT")
    sin_permisos = " AND (permisos IS NULL OR TRIM(permisos) = '')"
    con.execute("UPDATE ROL SET permisos = ? WHERE UPPER(TRIM(nombre_rol)) = 'ADMINISTRADOR'"
                + sin_permisos, (TODOS,))
    con.execute("UPDATE ROL SET permisos = ? WHERE UPPER(TRIM(nombre_rol)) <> 'ADMINISTRADOR'"
                + sin_permisos, (INVENTARIO,))
    return True


def permisos_de(con, nombre):
    return con.execute("SELECT permisos FROM ROL WHERE nombre_rol = ?", (nombre,)).fetchone()[0]


def main():
    con = sqlite3.connect(":memory:")
    con.executescript(ESQUEMA_ROTO)
    con.execute("PRAGMA user_version = %d" % VERSION_ROTA)

    print("1) La base marcada como v%d sin la columna reproduce el error" % VERSION_ROTA)
    try:
        con.execute("SELECT permisos FROM ROL WHERE id_rol = 1").fetchone()
        comprobar(False, "se esperaba «no such column: permisos» y la consulta paso")
    except sqlite3.OperationalError as error:
        comprobar("no such column: permisos" in str(error),
                  "la consulta falla con el mensaje del reporte: " + str(error))

    print("2) onUpgrade no corre sobre una base ya marcada; onOpen repara")
    comprobar(con.execute("PRAGMA user_version").fetchone()[0] == VERSION_ROTA,
              "la version guardada ya es la del helper, asi que no hay migracion pendiente")
    comprobar(reparar_permisos(con), "la reparacion agrega la columna")
    comprobar(permisos_de(con, "Administrador") == TODOS,
              "Administrador recupera los cinco modulos")
    comprobar(permisos_de(con, "Operador") == INVENTARIO,
              "Operador recupera los modulos de inventario")
    comprobar(permisos_de(con, "Jefe de turno") == INVENTARIO,
              "el rol propio del negocio tambien queda con acceso")

    print("3) Una segunda apertura no vuelve a tocar la base")
    con.execute("UPDATE ROL SET permisos = 'PRODUCTOS' WHERE nombre_rol = 'Operador'")
    comprobar(not reparar_permisos(con), "con la columna presente la reparacion no hace nada")
    comprobar(permisos_de(con, "Operador") == "PRODUCTOS",
              "los permisos que el usuario definio se conservan")

    con.close()

    if fallos:
        print("\n%d comprobacion(es) fallaron" % len(fallos))
        return 1
    print("\nLas 3 preguntas quedan respondidas contra SQLite real")
    return 0


if __name__ == "__main__":
    sys.exit(main())
