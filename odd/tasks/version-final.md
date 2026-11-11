# Feature: versión final integrada y entrega descargable (v1.1)

Cierre del proyecto para la evaluación de la Unidad II: una sola versión con los
aportes de todo el equipo, un APK que se instala y funciona sin compilar nada, y
la evidencia de lo que quedó probado.

## Punto de partida

Los commits de los cuatro integrantes ya estaban en `master` (nada quedó en ramas
sueltas), pero el resultado integrado tenía tres defectos que se notaban al usar
la app y un requerimiento sin implementar:

| # | Qué | Dónde |
|---|-----|-------|
| D1 | El escaneo por código de barras nunca abría la cámara: solo había una retícula dibujada y un campo manual | `BarcodeScannerActivity.iniciarCamara` |
| D2 | El filtro de estado del stock vaciaba el historial, porque viajaba como tipo de movimiento | `InformesActivity.cargarHistorialMovimientos` |
| D3 | Editar un rol borraba el permiso `INFORMES`; una base actualizada nunca lo recibía | `RolesFragment`, `DatabaseHelper` (v8) |
| D4 | El historial mostraba «Producto ID #7» en vez del nombre | `MovimientoGeneralAdapter`, `ProductoDao` |

## Specs

- S1: **Escaneo real.** La pantalla del escáner abre la cámara, decodifica un
  código de barras y lo resuelve con el mismo flujo que el ingreso manual. Si el
  permiso se niega o no hay cámara, el campo manual sigue disponible.
- S2: **El historial no se vacía por un filtro que no le corresponde.** En la
  pestaña Historial se ocultan la categoría y el estado del stock; la consulta
  recibe solo el texto de búsqueda.
- S3: **El historial identifica el producto** por nombre y SKU, resueltos con un
  JOIN a PRODUCTO.
- S4: **El permiso `INFORMES` sobrevive.** El editor de roles lo expone como una
  casilla y la migración v9 lo agrega a los roles que ya existían, sin duplicar y
  sin dárselo a los roles de ventas.
- S5: **Una sola versión instalable.** APK firmado, publicado como release, con
  código y credenciales de arranque documentados.
- S6: **Sin regresiones.** Las pruebas existentes siguen en verde y las nuevas
  cubren cada arreglo contra SQLite real.

## Registro de rutas

| Tarea | Ruta | Motivo |
| --- | --- | --- |
| S1–S4 | inline | El proyecto se entiende completo con unas pocas lecturas y ya tenía el entorno de build resuelto |
| Verificación | inline | `assembleDebug`, `testDebugUnitTest` (116 pruebas) y `simular_migracion_v9.py` |

## Verificación

- `:app:assembleDebug` y `:app:testDebugUnitTest`: 116 pruebas, 0 fallos.
- `python3 tools/simular_migracion_v9.py`: roles de inventario reciben INFORMES,
  el rol de ventas no, y repetir el paso no duplica.
- `:app:assembleRelease` + `apksigner verify`: APK firmado, `versionCode 2`,
  `versionName 1.1`, no debuggable.
- Límite conocido: no hay emulador ni dispositivo en el entorno de desarrollo, así
  que la cámara, el intent a Maps y el recorrido de pantallas se prueban en el
  teléfono; lo que sí se verifica acá es compilación, pruebas y contenido del APK
  (clases de ZXing presentes, permisos declarados).
