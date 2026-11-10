#!/usr/bin/env python3
"""Simula la migración v8 contra SQLite real y comprueba que no se pierde historia.

No necesita Android ni el emulador: reproduce el esquema que deja la versión 7
con un movimiento ya registrado, aplica el paso que lleva `DatabaseHelper`
(ALTER TABLE de `documento` y `motivo`, previa consulta a PRAGMA table_info) y
mide el resultado.

Responde tres preguntas que una prueba que lee el código como texto no responde:
  1. ¿el movimiento anterior a la v8 sobrevive a la migración, o el ALTER se
     lleva la tabla por delante?
  2. ¿las filas viejas quedan en NULL y no en un valor inventado, de modo que
     "sin documento" siga significando lo mismo antes y después?
  3. ¿repetir el paso es seguro, o el segundo ALTER revienta con
     "duplicate column name" y deja la base a medio migrar?
"""
import sqlite3
import sys

# Esquema tal como queda después del paso v7 (índices incluidos), con datos.
ESQUEMA_V7 = """
CREATE TABLE MOVIMIENTO (
    id_movimiento INTEGER PRIMARY KEY AUTOINCREMENT,
    id_producto INTEGER NOT NULL,
    tipo TEXT NOT NULL,
    cantidad INTEGER NOT NULL,
    usuario TEXT,
    fecha TEXT NOT NULL,
    id_proveedor INTEGER
);
CREATE INDEX idx_movimiento_producto ON MOVIMIENTO(id_producto);
INSERT INTO MOVIMIENTO (id_producto, tipo, cantidad, usuario, fecha, id_proveedor)
VALUES (1, 'ENTRADA', 10, 'admin', '2026-11-01 10:00:00', 7);
PRAGMA user_version = 7;
"""

COLUMNAS_V8 = ("documento", "motivo")


def columnas_de_movimiento(con):
    return [fila[1] for fila in con.execute("PRAGMA table_info(MOVIMIENTO)")]


def migrar(con):
    """Replica agregarColumnaSiFalta: consulta PRAGMA y solo entonces altera."""
    columnas = columnas_de_movimiento(con)
    for columna in COLUMNAS_V8:
        if columna not in columnas:
            con.execute("ALTER TABLE MOVIMIENTO ADD COLUMN %s TEXT" % columna)
    con.execute("PRAGMA user_version = 8")
    con.commit()


def correr():
    con = sqlite3.connect(":memory:")
    con.executescript(ESQUEMA_V7)
    return con


def main():
    fallos = 0

    # 1. La fila anterior a la v8 sobrevive, con NULL en las columnas nuevas.
    con = correr()
    filas_antes = con.execute("SELECT COUNT(*) FROM MOVIMIENTO").fetchone()[0]
    migrar(con)
    filas_despues = con.execute("SELECT COUNT(*) FROM MOVIMIENTO").fetchone()[0]
    print("filas antes=%d después=%d" % (filas_antes, filas_despues))
    if filas_despues != filas_antes:
        print("   FALLO: la migración perdió movimientos")
        fallos += 1

    fila = con.execute(
        "SELECT tipo, cantidad, documento, motivo FROM MOVIMIENTO WHERE id_movimiento = 1"
    ).fetchone()
    print("fila v7 tras migrar: %s" % (fila,))
    if fila[0] != "ENTRADA" or fila[1] != 10:
        print("   FALLO: el movimiento viejo cambió de contenido")
        fallos += 1
    if fila[2] is not None or fila[3] is not None:
        print("   FALLO: una fila que no tenía documento no puede aparecer con uno")
        fallos += 1

    # 2. Un movimiento nuevo con la v8 guarda la constancia documental.
    con.execute(
        "INSERT INTO MOVIMIENTO (id_producto, tipo, cantidad, usuario, fecha,"
        " id_proveedor, documento, motivo) VALUES (1, 'SALIDA', 3, 'admin',"
        " '2026-11-10 09:00:00', NULL, 'GUIA-9921', 'Venta mostrador')"
    )
    con.commit()
    nueva = con.execute(
        "SELECT documento, motivo FROM MOVIMIENTO WHERE tipo = 'SALIDA'"
    ).fetchone()
    print("movimiento v8: %s" % (nueva,))
    if nueva != ("GUIA-9921", "Venta mostrador"):
        print("   FALLO: el movimiento nuevo no guardó documento y motivo")
        fallos += 1

    # 3. Idempotencia: repetir el paso no rompe ni duplica columnas.
    antes = columnas_de_movimiento(con)
    try:
        migrar(con)
    except sqlite3.OperationalError as error:
        print("   FALLO: la segunda migración reventó: %s" % error)
        fallos += 1
    despues = columnas_de_movimiento(con)
    if antes != despues:
        print("   FALLO: el esquema cambió al repetir la migración")
        fallos += 1
    else:
        print("IDEMPOTENCIA -> columnas: %s  OK" % ", ".join(despues))

    # 4. EL ALTER repetido sí falla en SQLite: es la razón del PRAGMA previo.
    try:
        con.execute("ALTER TABLE MOVIMIENTO ADD COLUMN documento TEXT")
        print("   AVISO: SQLite aceptó una columna duplicada (no se esperaba)")
    except sqlite3.OperationalError as error:
        print("ALTER repetido sin PRAGMA -> %s (por eso se consulta antes)" % error)

    print("\nRESULTADO: %s" % ("TODO OK" if fallos == 0 else "%d FALLOS" % fallos))
    return 1 if fallos else 0


if __name__ == "__main__":
    sys.exit(main())
