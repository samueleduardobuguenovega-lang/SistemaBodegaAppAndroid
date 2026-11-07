# Feature: Create + Read de productos (parte individual integrante)

Parte individual de la Evaluación Final Unidad II (Programación Android, IEI-093): función
Create + Read sobre el producto compartido `SistemaBodegaAppAndroid`, presentada en un
PowerPoint individual de máximo 10 minutos con demostración.

## Specs

Los requerimientos del producto compartido provienen de `Casos de Usos - 2.docx`
(CU-03 parte 1; RF-03, RF-04, RF-05; prioridad MUST).

- S1: **RF-03 Registrar productos** — "Registrar productos con código, nombre, descripción, categoría, unidad de medida y stock inicial." Alta con SKU único; código de barras único cuando se informa (CU-03 criterios de aceptación: "El SKU debe ser único.", "El código de barras debe ser único cuando se informa.").
- S2: **RF-04 Consultar productos** — "Mostrar el catálogo de productos registrados." Listado del catálogo editable disponible para Operador/Administrador.
- S3: **RF-05 Buscar productos** — "Buscar productos por nombre o código SKU."
- S4: **Validaciones de alta (CU-03 flujos alternativos)** — "Campos obligatorios vacíos: el sistema destaca los campos pendientes." "Código SKU duplicado: el sistema informa el error y no guarda." "El stock inicial debe ser mayor o igual a cero."
- S5: **Contexto de presentación** — La app completa (login, movimientos, informes, admin) es compartida; cada integrante solo demuestra su función. Evaluación: 7 indicadores × 7 pts, corte 34; PPTX ≤10 min; producto compartido en repo Git demostrando conexión SQLite.

## Tasks

| ID | Specs | Route | Commit |
|----|-------|-------|--------|
| T1 | — | Verificación de build (ensamblar APK en esta máquina) | — |
| T2 | S1-S4 | Auditoría de Create (RF-03): formulario, SKU único, campos obligatorios, stock inicial, baja lógica no toca CREATE | — |
| T3 | S2-S3 | Auditoría de Read (RF-04, RF-05): catálogo y búsqueda por nombre/SKU | — |
| T4 | S1-S4 | Corrección de desviaciones detectadas (si corresponde) | — |
| T5 | S5 | Guion individual de presentación mapeado a los 7 indicadores (docs/presentacion/create-read.md) | — |

## Log
- L4: Rediseño visual del módulo (solo archivos de la parte Create+Read):
  item_producto.xml rediseñado (pildora de stock con semáforo verde/ámbar/rojo,
  precio destacado, ripple al tocar); ProductoAdapter migrado a ListAdapter+DiffUtil
  (refrescos animados solo en lo que cambió, píldora con unidad de medida);
  ProductosActivity con animación de entrada escalonada (item_fall.xml);
  activity_producto_form.xml con encabezados de sección e ícono de código de barras.
  Datos demo insertados en el emulador (3 productos) para verificar; build verde y
  screenshots confirman listado y formulario.
- L5: Variante Expressive aplicada por worker con superficies limitadas al módulo:
  tarjeta hero con conteo en vivo ("Productos en catálogo: N"), transiciones slide
  listado↔formulario, entrada escalonada de secciones del formulario y rebote táctil
  en tarjetas (switch con flechas sustituido por if/else; módulo es Java 11 de
  sourceCompatibility). Verificado en emulador: hero, listado y formulario OK;
  build exit 0. Pendiente decisión de commit/push (propia del usuario).
- L6 (petición textual): "y al iniciar la app, y el panel principal? faltan iconos, es poco intuitiva la app, no hay animaciones, guias, paso a paso algo que enseñe al usuario no experimentado con la tecnologia que le diga que hace cada cosa de forma indirecta". Se extiende el alcance a LoginActivity + MainActivity (base compartida del grupo): iconos por módulo, textos-guía (qué hace cada cosa), animaciones de entrada y feedback táctil. Tareas nuevas T6.
- L7: T6 verificada en emulador: login con logo/título/subtítulo/helpers/tarjeta de credenciales
  (admin / admin es la única cuenta del seed real, corregido según spec) y panel principal
  con 4 tarjetas de menú icono + descripción. Suite JVM 78/78 verde tras actualizar el pin
  de versión de BD (esperaba 5, contrato nuevo es 6 con migración v6). Build e installDebug OK.
- L8: Segunda vuelta (auditoría read-only por subagente): 13 hallazgos de propósito/estructura. Los 4 de la parte Create+Read (transacción del alta sin single-transaction y retorno ignorado, guard de permiso faltante en ProductoForm, LIMIT de búsqueda, fecha ISO) + limpieza de recursos sin uso → FASE 0 en docs/presentacion/plan_segunda_vuelta.md; restos de otros módulos y decisiones de grupo en FASE 1/2. Ejecución Fase 0 pendiente de autorización del usuario.
- L9: FASE 0 ejecutada (worker, superficies del módulo, ajuste de alcance tomado en cuenta: strings de login intactos). Puan Red 18 tests/6 fallos observados, GREEN 84/84. Verificación parental: insertarProductoConStockInicial con rollback correcto (sin setTransactionSuccessful en falla), única llamada restante a actualizarStock en el formulario es el diálogo de movimientos (compañero 3), guard en ProductoFormActivity:112.
## Log anterior
- L3: T1 con resultado: build assembleDebug exit 0 (antes y después de los cambios). T2/T3: Create cumple RF-03 (SKU único por índice UNIQUE + reintentos, stock inicial auditado); Read cumple RF-04/RF-05 (catálogo con LiveData, búsqueda en vivo). Desviaciones detectadas: el formulario no tenía código de barras ni unidad de medida, exigidos por el CU-03. T4 corrigió ambas (versión de BD 6, migración idempotente, validación de código de barras único cuando se informa, selector de unidades fija), build verde de nuevo. T5: docs/presentacion/create-read.md (guion mapeado a los 7 indicadores).

- L1 (petición original, textual): "mira los 4 vamos a presentar de la misma app-programa, pero esto, lo de la funcion es lo que sera individual, asi que tenemos presentaciones indivuduales de 10 minutos maximos, cada uno, entra uno presenta toda la app y su funcion en especifico con demostracion y se va, pasa otro, presenta nuevamente toda la app y presente su funciones, me doy a entender ?  asi que necesito que me crees un github con esta actividad y haz mi parte que sera la de create y read, haz primero un odd y una entrevista de como hacer la aplicacion"
- L2: En entrevista se decidió: solo la función individual; login con password; categorías fijas; repo local listo para push. Luego se descubrió que el producto ya existe (github.com/Benjaclownstar/SistemaBodegaAppAndroid) y el usuario eligió "Usar la app existente": verificar la función, completar desviaciones, preparar demo + guion de presentación.
