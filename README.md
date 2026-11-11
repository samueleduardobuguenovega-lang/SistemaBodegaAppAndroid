# SistemaBodegaAppAndroid

Aplicación Android de gestión de bodega sobre SQLite: usuarios y roles con permisos, categorías,
productos con SKU único, proveedores, movimientos de entrada y salida con constancia documental,
informe de stock, historial de movimientos, escaneo por código de barras y ubicación de la empresa.
Proyecto académico de Programación Android (IEI-093).

## Descargar el APK e instalar

La entrega final está publicada como *release* del repositorio:

**https://github.com/samueleduardobuguenovega-lang/SistemaBodegaAppAndroid/releases/latest**

1. Descargar `SistemaBodegaAppAndroid-1.1.apk` en el teléfono.
2. Abrirlo y aceptar la instalación de origen desconocido cuando Android la pida.
3. Iniciar sesión con **usuario `admin` y contraseña `admin`** (el usuario se crea en
   la primera apertura de la app, sobre la base local del teléfono).

No hace falta compilar nada: el APK ya está firmado e incluye todo lo necesario.
La versión de código de esa entrega es la etiqueta `v1.1` de este repositorio.

## Requisitos para compilar

| Componente | Versión | Nota |
| --- | --- | --- |
| JDK | 17 | AGP 9 compila con un toolchain 17. Un JDK 21 puede ejecutar Gradle, pero el toolchain de `javac` tiene que ser 17 |
| Gradle | 9.6.0 | Incluido en el wrapper (`./gradlew`): no hay que instalarlo |
| Android SDK Platform | `platforms;android-37.0` | `compileSdk = 37` |
| Android SDK Build-Tools | `build-tools;37.0.0` | |
| Platform-Tools | cualquiera reciente | Solo para `adb install` o `sqlite3` |
| Android en el dispositivo | API 30 o superior | `minSdk 30`, `targetSdk 37` |

`local.properties` con la ruta del SDK **no se versiona** (está en `.gitignore`):

```bash
printf 'sdk.dir=/ruta/a/tu/Android/Sdk\n' > local.properties
```

Si el proyecto se abre en Android Studio, el IDE genera ese archivo solo. Para que Gradle
encuentre el JDK 17 sin depender del que tenga el sistema, se puede fijar en
`~/.gradle/gradle.properties`:

```properties
org.gradle.java.installations.paths=/ruta/al/jdk17
```

## Compilar y probar

```bash
./gradlew :app:assembleDebug        # APK de depuración
./gradlew :app:testDebugUnitTest    # pruebas unitarias (JVM, sin dispositivo)
./gradlew :app:assembleDebug :app:testDebugUnitTest --rerun-tasks   # desde cero
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk` y se instala con
`adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Para instalar sobre una versión anterior, tener en cuenta que la app aplica migraciones
incrementales al abrir (`DatabaseHelper.onUpgrade`): hacer copia del archivo
`sistema_bodega.db` del dispositivo antes de probar un esquema nuevo.

## Dependencias declaradas

Todas viven en el *version catalog* [`gradle/libs.versions.toml`](gradle/libs.versions.toml), que es
la única fuente de versiones. No hay dependencias declaradas en `app/build.gradle.kts` por fuera de
ese archivo.

| Dependencia | Versión | Para qué |
| --- | --- | --- |
| `com.android.application` (AGP) | 9.3.0 | Plugin de aplicación Android |
| `androidx.appcompat` | 1.8.0 | `AppCompatActivity`, compatibilidad de temas |
| `com.google.android.material` | 1.14.0 | Material 3: toolbar, `TextInputLayout`, `MaterialSwitch`, FAB, radios |
| `androidx.activity` (`activity-ktx`) | 1.13.0 | API de Activity usada por los componentes |
| `androidx.constraintlayout` | 2.2.2 | Layouts de las pantallas |
| `androidx.lifecycle` (`lifecycle-livedata`) | 2.6.2 | Catálogo, categorías, proveedores y deshabilitados como `LiveData` |
| `androidx.security` (`security-crypto`) | 1.1.0 | Sesión del usuario cifrada (`SessionManager`) |
| `com.journeyapps` (`zxing-android-embedded`) | 4.3.0 | Cámara y decodificación del código de barras (RF-20) |
| `junit` | 4.13.2 | Pruebas unitarias |
| `org.robolectric` (`robolectric`) | 4.17 | Pruebas de SQLite y de layouts sobre la JVM, sin dispositivo |
| `androidx.test.ext:junit` | 1.3.0 | Pruebas instrumentadas |
| `androidx.test.espresso:espresso-core` | 3.7.0 | Pruebas instrumentadas |

Decisiones que conviene saber antes de agregar dependencias:

- **No hay Room.** El acceso a datos es SQLite directo con `SQLiteOpenHelper` y `Cursor`; el
  esquema se versiona a mano en `DatabaseHelper` y cada salto tiene su paso en `onUpgrade`.
- **No hay Kotlin.** El módulo es Java puro; por eso las tareas `compileDebugKotlin` salen
  `NO-SOURCE` y no hay `kotlin-stdlib` en el catálogo.
- **Los repositorios se resuelven solo desde `settings.gradle.kts`** (`FAIL_ON_PROJECT_REPOS`):
  un `repositories {}` dentro de un módulo rompe la configuración.

## Estructura

```
app/src/main/java/com/android/sistemabodegaappandroid/
├── LoginActivity, MainActivity              sesión y panel con accesos por permiso
├── ProductosActivity                        catálogo, búsqueda, ver deshabilitados, movimiento
├── ProductoFormActivity                     alta, edición, historial y baja lógica del producto
├── CategoriasActivity, ProveedoresActivity  módulos de apoyo
├── db/DatabaseHelper                        esquema, migraciones y consultas reactivas
├── db/*Dao                                  consultas de productos y proveedores
├── model/                                   POJOs leídos con Cursor (sin ORM)
├── session/SessionManager                   sesión cifrada
├── ui/InformesActivity                      informe de stock e historial general (RF-19 / RF-21 / RF-22)
├── ui/BarcodeScannerActivity                escáner con cámara y búsqueda por código (RF-20)
├── ui/UbicacionEmpresaActivity              ubicación de la empresa en Maps (RF-24)
├── ui/MovimientoStockDialog                 diálogo único de entrada/salida (CU-04 / CU-05)
├── ui/adapter, ui/admin                     listas y administración de usuarios y roles
└── util/                                    permisos, guard de módulo, insets
```

## Base de datos

Esquema actual: **versión 9**, en `sistema_bodega.db`. Las migraciones son incrementales e
idempotentes: cada `ALTER TABLE` consulta antes `PRAGMA table_info`, de modo que repetir un paso
no rompe y un fallo a medias se reintenta en el arranque siguiente.

| Versión | Cambio |
| --- | --- |
| v4 | `MOVIMIENTO.id_proveedor`: proveedor de la entrada |
| v5 | `ROL.permisos`, con relleno de los roles existentes |
| v6 | `PRODUCTO.codigo_barras` (único cuando se informa) y `unidad_medida` |
| v7 | Índices de SKU, código de barras y movimiento; fechas ordenables |
| v8 | `MOVIMIENTO.documento` y `MOVIMIENTO.motivo` (CU-04 / CU-05) |
| v9 | `INFORMES` agregado a `ROL.permisos` de los roles que ya operan inventario |

Las migraciones que tocan datos se pueden reproducir sin dispositivo, contra SQLite real:

```bash
python3 tools/simular_migracion_v5.py          # ROL.permisos y su relleno
python3 tools/simular_reparacion_permisos.py   # base marcada como migrada sin la columna
python3 tools/simular_migracion_v8.py          # documento y motivo sin perder movimientos
python3 tools/simular_migracion_v9.py          # permiso de informes en los roles existentes
```

## Requisito → archivo

| Requisito | Dónde se implementa |
| --- | --- |
| CU-01 / RF-01 Iniciar sesión | `LoginActivity`, `DatabaseHelper.autenticar` (PBKDF2), `SessionManager.createLoginSession` |
| CU-02 / RF-02 Cerrar sesión | `MainActivity.mostrarDialogoCerrarSesion`, `SessionManager.logoutUser` |
| CU-03 / RF-03 Registrar producto | `ProductoFormActivity.guardar`, `ProductoDao.insertarProductoConStockInicial` |
| RF-04 / RF-05 Consultar y buscar | `ProductosActivity` + `DatabaseHelper.observarProductos` (LiveData, executor) |
| RF-07 Modificar producto | `ProductoFormActivity.guardarEdicion`, `ProductoDao.updateProducto` |
| RF-08 Deshabilitar producto | `ProductoDao.updateProductoEstado`, `ProductosActivity` (interruptor de deshabilitados) |
| CU-04 / CU-05 Entradas y salidas | `MovimientoStockDialog`, `ProductoDao.actualizarStock`, `MOVIMIENTO.documento` y `.motivo` |
| RF-19 / RF-22 Informe de stock y filtros | `InformesActivity` + `ProductoDao.getInformeProductos` |
| RF-20 Escaneo por código de barras | `BarcodeScannerActivity` + `DecoratedBarcodeView` (ZXing) + `ProductoDao.getProductoByCodigoBarrasIncluyendoInactivos` |
| RF-21 Historial de movimientos | `InformesActivity` (pestaña Historial) + `ProductoDao.getHistorialMovimientosGeneral` |
| RF-24 Ubicación de la empresa | `UbicacionEmpresaActivity` (intent `geo:` y ruta en Maps) |
| CU-09 Categorías | `CategoriasActivity` + `DatabaseHelper.observarCategoriasActivas` |
| CU-11 / CU-12 Proveedores | `ProveedoresActivity`, `ProveedorFormActivity`, `ProveedorDao` |
| CU-27 / RF-27 Usuarios y roles | `ui/admin/UsuariosFragment`, `RolesFragment`, `UserRoleAdminActivity` |
| RF-27 Permisos por rol | `DatabaseHelper.getPermisosDeRol`, `Permisos.tiene`, `AccesoModulo.permitido` |

## Pruebas

```bash
./gradlew :app:testDebugUnitTest
```

**116 pruebas, 20 clases.** La mayoría son pruebas JVM de estructura y contratos (presencia de
métodos, columnas del esquema, cadenas en recursos, uso del executor) y de lógica pura —hash de
contraseñas, parseo de cantidades, reglas de permisos—.

Las que sí ejercitan Android corren con **Robolectric**, contra SQLite real: el historial con el
nombre del producto, la búsqueda del informe, el rechazo de una salida mayor al stock y la migración
v8 → v9 (`HistorialMovimientosTest`, `MigracionV9Test`, `EscannerLayoutTest`). Lo que Robolectric no
cubre es abrir una `Activity`: `SessionManager` usa `EncryptedSharedPreferences` y necesita el
keystore del teléfono, así que el recorrido de pantallas se prueba en el dispositivo, y las
migraciones que tocan datos también se pueden reproducir con los scripts de `tools/`.

Las pruebas que ejercitan Android corren contra SQLite real; lo que no se puede probar sin
dispositivo está listado en los límites conocidos del proyecto.
