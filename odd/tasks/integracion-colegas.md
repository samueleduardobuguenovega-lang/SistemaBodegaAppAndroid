# Feature: Integración de los aportes de los colegas (CU-04 / CU-05 y baja lógica)

Integración de los dos commits que subieron los compañeros al repo compartido, sin
perder lo que ya había en `master` y sin dejar dos versiones de la misma función.

Origen:
- `5d1f31b` — copiDUKO, "Implementación de baja lógica y mejoras en productos".
- `0045cbe` — FelipeAraya, "lógica completa de entradas y salidas de bodega (CU-04 y CU-05)".

## Specs

- S1: **Un solo diálogo de movimiento.** Registrar entrada o salida (CU-04 / CU-05) desde el
  listado y desde el formulario debe abrir la misma implementación, con las mismas
  validaciones y el mismo registro en MOVIMIENTO.
- S2: **El movimiento conserva su constancia.** Tipo, cantidad, usuario, proveedor (solo en
  entradas), documento y motivo se guardan en la misma transacción que actualiza el stock.
- S3: **La migración no pierde historia.** Pasar de v7 a v8 agrega `documento` y `motivo` con
  ALTER TABLE; los movimientos anteriores sobreviven con NULL en las columnas nuevas.
- S4: **El listado no consulta la base en el hilo principal.** El catálogo activo y el de
  deshabilitados son LiveData servidos por el executor del `DatabaseHelper`.
- S5: **Una cantidad no numérica no cierra la app.** El parseo es tolerante y el diálogo
  informa el error sin descartar lo escrito.
- S6: **Sin formularios paralelos.** No queda una segunda pantalla de edición sin declarar en
  el manifiesto, con menos campos y con Toasts de depuración.

## Desviaciones encontradas en los commits

| # | Qué | Dónde | Efecto |
|---|-----|-------|--------|
| D1 | El listado traía su propia copia del diálogo de movimiento | `ProductosActivity.mostrarDialogoMovimiento` (Felipe) | El proveedor se veía en pantalla, pero se guardaba siempre `id_proveedor = 0` |
| D2 | `Integer.parseInt` directo sobre el texto del usuario | `ProductosActivity` (Felipe) | NumberFormatException y cierre de la app con una cantidad no numérica |
| D3 | Los deshabilitados se consultaban en el hilo principal | `ProductosActivity.cargarListaProductosSegunSwitch` (copiDUKO) | ANR con una bodega real; rompía el diseño reactivo del catálogo |
| D4 | Borrado masivo de comentarios de esquema y de decisiones | `DatabaseHelper`, `ProductoDao`, `Movimiento` (Felipe) | −572 líneas netas; se perdía la documentación que explica migraciones e invariantes |
| D5 | `EditarProductoActivity` sin declarar en el manifiesto y sin nadie que la abriera | `ui/EditarProductoActivity` (copiDUKO) | Formulario paralelo con menos campos, Toasts de depuración y control de acceso por nombre de rol |
| D6 | Las pruebas seguían fijando `DATABASE_VERSION = 7` | `Fase0Test`, `CasosDeUsoFaltantesTest` | La suite fallaba en dos pruebas desde el commit de Felipe |

## Tasks

| ID | Specs | Ruta | Estado |
|----|-------|------|--------|
| T1 | — | Reproducir el estado: `assembleDebug` y `testDebugUnitTest` sobre `master` | Hecho: 85 pruebas, 2 fallos (versión 7 vs 8) |
| T2 | S3 | Reaplicar el cambio funcional de v8 sobre la versión con los comentarios intactos | Hecho |
| T3 | S1, S2, S5 | Extraer `MovimientoStockDialog` y usarlo desde las dos pantallas | Hecho |
| T4 | S4 | LiveData de deshabilitados en el helper y filtro en SQL | Hecho |
| T5 | S6 | Eliminar `EditarProductoActivity` y su layout | Hecho |
| T6 | S3 | Verificar la migración v7 → v8 contra SQLite real | Hecho |
| T7 | — | Pruebas de la integración y suite en verde | Hecho |

## Verificación

- `:app:assembleDebug` — APK compilado.
- `:app:testDebugUnitTest` — suite completa en verde.
- Migración real: base v7 con un movimiento, ALTER de `documento` y `motivo`, fila vieja
  conservada con NULL y movimiento nuevo con ambos datos.
