# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

**brujula-de-lezo** es una app Android nativa (Kotlin + Jetpack Compose) que funciona como brújula: en vez de señalar al norte, la aguja apunta siempre hacia Londres (51,5074°N, -0,1278°O), aplicando la declinación magnética real de la posición del usuario. Vibra cuando el usuario apunta a Inglaterra (±5°) y no depende de red ni de Google Play Services.

`applicationId` / namespace raíz: `com.brujuladelezo`. `minSdk` 26, `compileSdk`/`targetSdk` 36, JVM target 21, Kotlin 2.4.21, AGP 9.4.1, Gradle 9.8.2.

AGP 9 usa **Kotlin integrado**: los módulos Android (`app`, `data`, `presentation`, `design-system`) no aplican `org.jetbrains.kotlin.android` (AGP falla si se aplica) y su `jvmTarget` sale de `compileOptions.targetCompatibility`. AGP arrastra su propia versión de KGP, así que el `build.gradle.kts` raíz fija `libs.kotlin.gradle.plugin` en el classpath del buildscript; por eso `core` y `domain` aplican `kotlin("jvm")` **sin versión** (con versión, Gradle rechaza la petición porque el plugin ya está en el classpath). Actualizar AGP y el wrapper de Gradle a la vez (AGP 9.4 necesita Gradle ≥ 9.6). Las versiones de AndroidX/Compose están en las últimas compatibles con `compileSdk` 36: Compose BOM ≥ 2026.09, lifecycle ≥ 2.11 y core-ktx ≥ 1.19 exigen `compileSdk` 37.

`versionCode`/`versionName` no se escriben a mano: salen de `git describe --tags --long` (`major*1_000_000 + minor*10_000 + patch*100 + commits desde la etiqueta`, ver `versionCodeFromDescribe` en `app/build.gradle.kts`), nunca de `rev-list --count`, que puede bajar tras un rebase. Las versiones 1.0.0 y 1.1.0 se publicaron con `versionCode` 1 y 2; cualquier valor derivado de un tag es mayor.

## Comandos de desarrollo

```bash
# Compilar (variante debug)
./gradlew assembleDebug

# Ejecutar todos los tests unitarios
./gradlew test

# Tests de un módulo concreto
./gradlew :domain:test                        # rumbo, distancia, histéresis, rumbo cardinal, ajustes, caso de uso
./gradlew :presentation:testDebugUnitTest      # CompassViewModelTest, SettingsViewModelTest, DirectionFormatTest
./gradlew :data:testDebugUnitTest              # AxisRemapTest, LocationProvidersTest

# Ejecutar una clase de test concreta
./gradlew :domain:test --tests "com.brujuladelezo.domain.math.BearingCalculatorTest"

# Lint (Android Lint; no hay detekt ni ktlint configurados en este proyecto)
./gradlew lint

# Instalar en dispositivo conectado
./gradlew installDebug

# Limpiar y recompilar
./gradlew clean assembleDebug

# Diagnóstico de errores de build
./gradlew assembleDebug --stacktrace
./gradlew :app:dependencies
```

El CI (`.github/workflows/android.yml`) ejecuta, en este orden, `test` → `lint` → `assembleDebug` en cada push a `master`/`develop` y en cada PR a `master` o a `release/**` (la rama común donde se integran las fases de una versión). El release (`.github/workflows/release.yml`) se dispara con tags `v*`, ejecuta `assembleRelease bundleRelease` y firma con un keystore decodificado desde el secret `KEYSTORE_BASE64` (variables de entorno `KEYSTORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS`; ver `app/build.gradle.kts` → `signingConfigs`).

## Releases

No hay números de versión en el código (salen del tag, ver arriba), así que una release es un **tag anotado** `vX.Y.Z` empujado sobre el commit de `master` ya mergeado. Minor para funcionalidades nuevas, patch para arreglos.

Preparación: una rama `release/vX.Y.Z` desde `master`. Si la versión tiene varias partes, cada una va en su propia rama con PR contra `release/vX.Y.Z` (el CI corre también en esas PRs) y se mergea al estar en verde; la rama común se valida en un dispositivo físico antes de su PR a `master`. Esa PR actualiza las listas de funcionalidades de cara al usuario: `README.md` y las tarjetas de `docs/index.html`.

El cuerpo de la release es el **mensaje del tag**: un paso de `release.yml` lo lee por la API de GitHub (no por git, porque `actions/checkout` puede dejar el tag como ligero) y lo pasa como `body_path`; `generate_release_notes: true` añade debajo la lista de PRs. Escribirlo para usuarios, en español: la primera línea es el título (se renderiza `##`), un párrafo corto y luego secciones como línea suelta seguida de items `- ` (se renderizan `###`; p. ej. Novedades, Cambios de comportamiento, Interno). Crearlo con `git tag -a vX.Y.Z -F <fichero>` y `git push origin vX.Y.Z`. Un tag ligero da una release con solo la lista de PRs.

`release.yml` también comprueba que las `.so` de 64 bits del APK estén alineadas a 16 KB (`readelf -lW`, todos los segmentos `LOAD` ≥ `0x4000`) y falla si no.

## Arquitectura

Seis módulos Gradle, Clean Architecture, con esta jerarquía real de dependencias (ver `settings.gradle.kts` y los `build.gradle.kts` de cada módulo):

```
app            → core, domain, data, presentation, design-system
presentation   → core, domain, design-system
data           → core, domain
domain         → core                       (Kotlin puro, plugin kotlin.jvm, sin Android)
core           → (sin dependencias)          (Kotlin puro, plugin kotlin.jvm)
design-system  → (sin dependencias internas) (Compose)
```

**Regla crítica**: `domain` y `core` usan el plugin `kotlin.jvm`, no `android.library` — no pueden importar clases del framework Android.

| Módulo | Responsabilidad | Clases clave |
|---|---|---|
| `:core` | Tipos base compartidos | `DispatcherProvider`/`DefaultDispatcherProvider`, `AppError` (sealed interface) |
| `:domain` | Modelos, contratos de repositorio, lógica de negocio pura | `GeoPoint`, `Landmarks.LONDON`, `LondonDirection`, `CardinalDirection`, `AppSettings`, `CompassAccuracy`, `RawOrientation`, `BearingCalculator`, `PointingHysteresis`, `ObserveLondonDirectionUseCase`, `ObserveLocationEnabledUseCase`, `Observe/UpdateSettingsUseCase`, interfaces `LocationRepository`/`OrientationRepository`/`GeomagneticRepository`/`SettingsRepository` |
| `:data` | Implementaciones de repositorio sobre APIs de Android | `LocationRepositoryImpl` (`LocationManager` con los proveedores fusionado de AOSP en API 31+, red y pasivo; sin Play Services), `OrientationRepositoryImpl` (`SensorManager` + rotación del display inyectada), `GeomagneticRepositoryImpl` (`GeomagneticField`), `SettingsRepositoryImpl` (`SharedPreferences`) |
| `:presentation` | ViewModels y pantalla Compose | `CompassViewModel` (`StateFlow<CompassUiState>`), `SettingsViewModel`, `CompassUiState`, `CompassScreen`, `SettingsSheet` |
| `:design-system` | Componentes Compose reutilizables, tema | `CompassDial` (rosa + aguja), `StatusPill`, `CompassRose`, `LondonNeedle`, `BrujulaDeLezoTheme`, `BrujulaTheme.colors` (`ExtendedColors`), tipografía |
| `:app` | Entry point, DI manual | `MainActivity`, `BrujulaApplication`, `AppContainer` |

### Inyección de dependencias

**No hay Koin ni Hilt.** El grafo de dependencias se construye a mano en `app/src/main/kotlin/com/brujuladelezo/di/AppContainer.kt`: instancia repositorios y casos de uso, y expone un `ViewModelProvider.Factory` para `CompassViewModel` y otro para `SettingsViewModel`. Si se añade un nuevo repositorio o caso de uso, se cablea aquí.

### La matemática del rumbo (lógica central, repartida entre módulos)

1. **Azimut del dispositivo** (`OrientationRepositoryImpl`): sensor `TYPE_ROTATION_VECTOR` → `getRotationMatrix` → `getOrientation` → azimut magnético en `[0, 360)`.
2. **Norte verdadero**: `azimutVerdadero = azimutMagnético + GeomagneticField.declination` (`GeomagneticRepositoryImpl`).
3. **Rumbo a Londres** (`BearingCalculator.initialBearing`): fórmula de círculo máximo, `atan2(sinΔλ·cosφ₂, cosφ₁·sinφ₂ − sinφ₁·cosφ₂·cosΔλ)`.
4. **Ángulo de la aguja**: `BearingCalculator.normalizeDegrees(rumbo − azimutVerdadero)`.
5. **"Apuntando a Londres"**: con histéresis (`PointingHysteresis`): se entra cuando `|BearingCalculator.relativeAngle(rumbo, azimut)| < 5°` y no se sale hasta superar 8°. Con un solo umbral, el ruido del sensor cruzaba los 5° varias veces por segundo y cada cruce volvía a vibrar. El estado de la histéresis vive dentro del `flow {}` del caso de uso, uno por colector.

Todo esto se combina en `ObserveLondonDirectionUseCase.invoke()`, que hace `combine()` de los flows de ubicación y orientación y emite un `LondonDirection` por cada actualización. Además del ángulo de la aguja lleva el rumbo verdadero (y su `CardinalDirection` de 16 puntos, con O de oeste), la distancia de círculo máximo (`BearingCalculator.distanceKm`, haversine) y `turnDegrees`, el giro con signo (positivo = a la derecha) que muestra la tarjeta. La declinación se aplica pero no se enseña al usuario: no ayuda a apuntar.

### Patrones clave

- **UseCase**: `operator fun invoke()` devolviendo `Flow<T>` para lógica reactiva (ver `ObserveLondonDirectionUseCase`)
- **Repository**: interfaz en `domain`, implementación en `data`, basada en `callbackFlow` para APIs de callback de Android (sensores, ubicación)
- **UiState**: data class inmutable (`CompassUiState`) expuesta como `StateFlow`. `CompassViewModel` la construye con `flatMapLatest` sobre la petición (permiso + reintento) y `stateIn(WhileSubscribed(5_000))`: los sensores y la ubicación solo están registrados mientras la UI observa. Antes se recogía en `viewModelScope` sin más y seguían activos con la app en segundo plano. No volver a lanzar colecciones largas en `viewModelScope`.
- **Permiso**: `CompassScreen` lo comprueba en cada `ON_RESUME` (`LifecycleResumeEffect`) para enterarse si se concedió desde Ajustes, y solo lo da por denegado para siempre cuando `shouldShowRequestPermissionRationale` devuelve `false` tras una negativa.
- **Errores**: `AppError` sealed interface en `core` (`SinSensorBrujula`, `SinPermisoUbicacion`, `UbicacionDesactivada`). En la UI: sin ubicación del sistema y sin un primer rumbo se pide activarla (`CompassUiState.showLocationDisabled`); si el flujo de sensores falla se muestra un mensaje con «Reintentar», nunca una aguja a 0°.
- **Textos**: en `res/values/strings.xml` de cada módulo, con `tools:locale="es"` (sin él, el lint `Typos` corrige el español como si fuera inglés).

### Anti-patrones críticos

- No importar clases del framework Android en `domain` ni en `core` — deben ser Kotlin puro
- No poner lógica de negocio en ViewModels — extraer a UseCases en `domain`
- No crear dependencias circulares entre módulos
- No nombrar recursos XML con nombres reservados de Android (`background`, `foreground`, `icon`, `logo`, `text`, `button`) — añadir prefijo descriptivo (p. ej. `app_background`, `ic_home`)
- No añadir dependencias de red (Ktor, Retrofit) ni de persistencia (Room, SQLDelight, DataStore): la app es intencionadamente 100% offline y sin telemetría — ver "Privacidad" más abajo. Los ajustes (tema, vibración, unidad de distancia) van en `SharedPreferences` del framework (`SettingsRepositoryImpl`, fichero `settings`); nada más se escribe en disco, y la ubicación nunca

### Corrutinas y threading

| Operación | Dispatcher |
|-----------|-----------|
| UI / actualizar state | `Dispatchers.Main` (`viewModelScope`, vía `DispatcherProvider.main`) |
| Sensores, ubicación | `DispatcherProvider.io` |
| Cálculo de rumbo/combine | `DispatcherProvider.default` |

Los repositorios son *main-safe* vía `.flowOn(dispatchers.io)` internamente. No usar `GlobalScope`; usar `viewModelScope`. La abstracción `DispatcherProvider` (en `:core`) es la que se sustituye por un fake en tests.

Ojo con las APIs de `LocationManager` que exigen `Looper` en el hilo llamante: los `callbackFlow` corren bajo `flowOn(dispatchers.io)`, que no lo tiene. Usar siempre las sobrecargas con `Executor` (`LocationManagerCompat`), y no envolver el registro en un `runCatching` mudo — así fue como pasó desapercibido durante meses que las actualizaciones de ubicación nunca llegaban a registrarse.

## targetSdk 36 (Android 16)

Play exige API 36 para publicar actualizaciones. Los dos cambios de comportamiento que afectan a esta app:

- **Edge-to-edge obligatorio, sin opt-out** (`windowOptOutEdgeToEdgeEnforcement` ya no sirve). `MainActivity` llama a `enableEdgeToEdge()` en `onCreate` y lo repite con `SystemBarStyle.auto { darkTheme }` cada vez que cambia el tema resuelto: con el tema forzado desde Ajustes, el default (que mira `Configuration.uiMode`) pintaría iconos oscuros sobre fondo oscuro. Hasta la primera lectura de ajustes `MainActivity` no compone nada (se ve el `windowBackground`) para no mostrar un fotograma en el tema equivocado. `CompassScreen` separa el fondo (pinta hasta los bordes) del contenido (`windowInsetsPadding(WindowInsets.safeDrawing)`). No se usa `Scaffold`: sin TopAppBar ni FAB, solo añadiría `PaddingValues` que propagar.
- **Restricciones de orientación ignoradas en pantallas ≥600dp.** No hay `android:screenOrientation` en el manifest; el bloqueo es programático y gateado por `R.bool.lock_portrait_orientation` (`true` en `values/`, `false` en `values-sw600dp/` y en `src/debug/` para poder probar el apaisado en móvil). No se declara `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY`, el opt-out disponible hasta API 37.

**El layout no puede asumir vertical.** `CompassContent` usa `BoxWithConstraints` (no `LocalConfiguration.screenWidthDp`, lint `ConfigurationScreenWidthHeight`) y exige ancho ≥600dp **y** ventana apaisada para las dos columnas: con el ancho solo, una tablet en vertical deja media pantalla vacía. `compass_face.png` es 1024×1024, así que hasta ~420dp no hay reescalado (`MaxDialSize` = 420dp). Su disco tiene que ser redondo y estar centrado en (512, 512), que es donde pivota `compass_needle.png`: el original venía un 4 % más estrecho (949×988 px) y desplazado unos píxeles, y se corrigió en la 1.2.0 (también la copia de `docs/`). Si se cambia la imagen, medir el disco por el canal alfa antes de commitearla.

**Nada puede solapar la rosa.** En vertical, `TallLayout` es un `Layout` propio: mide la fila superior (`TopBar`: hueco de estado centrado y botón de ajustes; no hay título, el nombre ya va grabado en la rosa), la tarjeta y la cita, y da a la rosa el alto restante (mínimo `MinDialSize`, 200dp); si no cabe, apila y deja desplazar. El hueco de estado (`StatusSlot`) compone siempre las cuatro pastillas superpuestas y muestra solo la activa (apuntando > calibrar > precisión), así que su alto no cambia al empezar o dejar de apuntar. Al apuntar, la rosa no cambia: lo avisan la pastilla y la vibración (hubo un halo burdeos alrededor y se quitó en la 1.2.0 por sobrar). Cualquier elemento nuevo en la pantalla de la brújula entra en ese `Layout`, no encima de la rosa. Las previews de `CompassScreen.kt` cubren móvil, móvil pequeño, apaisado, tablet en ambas orientaciones, letra x2 y oscuro.

**Tipografía empaquetada.** Fraunces (títulos, cita, distancia) e IBM Plex Sans (resto), las mismas que la web, como TTF estáticos con subconjunto latino en `design-system/src/main/res/font/`; licencias OFL en `licenses/`. Plex tiene cifras tabulares por defecto; Fraunces no, así que no se usa para números que cambian a menudo. Los TTF de Google Fonts vienen sin la característica `tnum`.

**Icono.** El adaptativo lleva capa `monochrome` (`drawable/ic_launcher_monochrome.xml`) para los iconos temáticos de Android 13+. Se queda en `mipmap-anydpi-v26` aunque lint (`ObsoleteSdkInt`) proponga quitar el `-v26`: aapt2 versiona igualmente los `adaptive-icon` a v26 y, sin los PNG de `mipmap-*dpi` como base, el manifest no encuentra `@mipmap/ic_launcher`.

**La rotación del display no se lee del `WindowManager`.** `WindowManager.getDefaultDisplay()` está deprecado y el `WindowManager` de `AppContainer` sale del Application context; la migración obvia (`Context.getDisplay()`) lanza `UnsupportedOperationException` sobre un contexto no visual. Se usa `DisplayManager.getDisplay(DEFAULT_DISPLAY)` (`app/di/DisplayRotation.kt`), inyectado en `OrientationRepositoryImpl` como `displayRotation: () -> Int` para que `:data` no dependa de nada de ventanas. El remapeo de ejes vive en `axesForRotation` con test propio: esas ramas eran código muerto mientras la app estaba bloqueada en retrato.

**16 KB page size.** La app no trae `jniLibs` propias, pero el AAB incluye `libandroidx.graphics.path.so` (transitiva de Compose), ya alineada (`readelf -lW` reporta `align 0x4000` en todos los segmentos `LOAD`). Re-verificar tras subir Compose:
`find app/build/intermediates/stripped_native_libs/release -name '*.so' -exec readelf -lW {} \;`

`bundleRelease`/`assembleRelease` en local fallan al firmar: el keystore (`keystore/release.jks`) solo existe en CI, decodificado desde el secret. Los intermedios sí se generan y sirven para verificar las nativas.

## Privacidad

- Sin acceso a internet: la app no pide permiso de red y no existe capa de red en el proyecto.
- Sin telemetría, sin analítica, sin anuncios.
- Permiso mínimo: `ACCESS_COARSE_LOCATION` (y `VIBRATE`) — la ubicación solo se usa en memoria para calcular el rumbo, nunca se almacena ni se envía.
- Las coordenadas de Londres están hardcodeadas en `domain/model/Landmarks.kt`; no se descargan datos externos.

## Tests

Stack: **JUnit 4 + MockK 1.14.11 + Turbine 1.2.1 + `kotlinx-coroutines-test`**, con `MainDispatcherRule` (en `presentation/src/test`) usando `UnconfinedTestDispatcher`. Los tests que comprueban `WhileSubscribed` corren con `runTest(mainDispatcherRule.dispatcher)` para compartir el reloj virtual y poder avanzar `STOP_TIMEOUT_MILLIS`. Los flows de prueba que emiten varios valores en un `combine` los espacian con `delay`: `combine` puede saltarse valores intermedios.

`:data` solo tiene JUnit: sus tests (`AxisRemapTest`, `LocationProvidersTest`) cubren lógica pura. Las constantes `Surface.ROTATION_*` y `SensorManager.AXIS_*` son `static final int` y kotlinc las inlinea, así que no hace falta Robolectric ni `unitTests.isReturnDefaultValues`.

**Capturas sin emulador.** Para revisar el layout en varios tamaños sin dispositivo (p. ej. en un contenedor sin KVM) se puede renderizar `CompassContent` (es `internal` para esto) con Robolectric + Roborazzi en un arnés temporal: `testImplementation` de `robolectric` y `roborazzi-compose`, `unitTests.isIncludeAndroidResources = true`, la propiedad de sistema `roborazzi.test.record=true` y `RuntimeEnvironment.setQualifiers("w320dp-h540dp-xhdpi")`/`setFontScale(2f)` por caso. No está commiteado: ni Robolectric ni Roborazzi son dependencias del proyecto.

## Skills instalados

Este proyecto tiene los siguientes skills activos (en `.agents/skills/`). Se activan automáticamente según el contexto:

| Skill | Activación |
|-------|-----------|
| `android-clean-architecture` | Al estructurar módulos, UseCases, Repositories, DI |
| `android-kotlin` | Al trabajar con ficheros `.kt` / `.kts` |
| `android-native-dev` | Guía de Material Design 3, configuración de proyecto, diagnóstico de errores de build |
| `mobile-android-design` | Al diseñar UI con Jetpack Compose y Material 3 |
| `clerk-android` | Al implementar autenticación con Clerk |
| `android-device-automation` | Automatización visual de dispositivos Android mediante Midscene |

## Notas de skills

### Clerk (`clerk-android`)

Antes de editar cualquier fichero, **es obligatorio obtener ambos valores**:
1. Tipo de flujo: `prebuilt` (AuthView/UserButton) o `custom` (API-driven)
2. Publishable key de Clerk del desarrollador

No continuar sin ambos. El artefacto correcto depende del flujo: `clerk-android-ui` para prebuilt, `clerk-android-api` para custom.

### Automatización de dispositivo (`android-device-automation`)

Requiere fichero `.env` en el directorio de trabajo con las variables del modelo visual:

```
MIDSCENE_MODEL_API_KEY=...
MIDSCENE_MODEL_NAME=...
MIDSCENE_MODEL_BASE_URL=...
MIDSCENE_MODEL_FAMILY=...
```

Nunca ejecutar comandos `npx @midscene/android` en segundo plano — cada comando debe completarse de forma síncrona para poder leer la captura de pantalla antes de decidir la siguiente acción.
