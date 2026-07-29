# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

**brujula-de-lezo** es una app Android nativa (Kotlin + Jetpack Compose) que funciona como brújula: en vez de señalar al norte, la aguja apunta siempre hacia Londres (51,5074°N, -0,1278°O), aplicando la declinación magnética real de la posición del usuario. Vibra cuando el usuario apunta a Inglaterra (±5°) y no depende de red ni de Google Play Services.

`applicationId` / namespace raíz: `com.brujuladelezo`. `minSdk` 26, `compileSdk`/`targetSdk` 36, JVM target 21, Kotlin 2.2.21, AGP 8.13.2, Gradle 9.0.0.

## Comandos de desarrollo

```bash
# Compilar (variante debug)
./gradlew assembleDebug

# Ejecutar todos los tests unitarios
./gradlew test

# Tests de un módulo concreto (los módulos con lógica de negocio/tests son domain y presentation)
./gradlew :domain:test                        # BearingCalculatorTest, ObserveLondonDirectionUseCaseTest
./gradlew :presentation:testDebugUnitTest      # CompassViewModelTest

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

El CI (`.github/workflows/android.yml`) ejecuta, en este orden, `test` → `lint` → `assembleDebug` en cada push/PR a `master`/`develop`. El release (`.github/workflows/release.yml`) se dispara con tags `v*`, ejecuta `assembleRelease bundleRelease` y firma con un keystore decodificado desde el secret `KEYSTORE_BASE64` (variables de entorno `KEYSTORE_PASSWORD`, `KEY_PASSWORD`, `KEY_ALIAS`; ver `app/build.gradle.kts` → `signingConfigs`).

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
| `:domain` | Modelos, contratos de repositorio, lógica de negocio pura | `GeoPoint`, `Landmarks.LONDON`, `LondonDirection`, `CompassAccuracy`, `RawOrientation`, `BearingCalculator`, `ObserveLondonDirectionUseCase`, interfaces `LocationRepository`/`OrientationRepository`/`GeomagneticRepository` |
| `:data` | Implementaciones de repositorio sobre APIs de Android | `LocationRepositoryImpl` (`LocationManager`, sin Play Services), `OrientationRepositoryImpl` (`SensorManager` + rotación del display inyectada), `GeomagneticRepositoryImpl` (`GeomagneticField`) |
| `:presentation` | ViewModel y pantalla Compose | `CompassViewModel` (`StateFlow<CompassUiState>`), `CompassUiState`, `CompassScreen` |
| `:design-system` | Componentes Compose reutilizables, tema | `CompassRose`, `LondonNeedle`, `CruzDeBorgona`, tema Material 3 |
| `:app` | Entry point, DI manual | `MainActivity`, `BrujulaApplication`, `AppContainer` |

### Inyección de dependencias

**No hay Koin ni Hilt.** El grafo de dependencias se construye a mano en `app/src/main/kotlin/com/brujuladelezo/di/AppContainer.kt`: instancia repositorios y el caso de uso, y expone un `ViewModelProvider.Factory` para `CompassViewModel`. Si se añade un nuevo repositorio o caso de uso, se cablea aquí.

### La matemática del rumbo (lógica central, repartida entre módulos)

1. **Azimut del dispositivo** (`OrientationRepositoryImpl`): sensor `TYPE_ROTATION_VECTOR` → `getRotationMatrix` → `getOrientation` → azimut magnético en `[0, 360)`.
2. **Norte verdadero**: `azimutVerdadero = azimutMagnético + GeomagneticField.declination` (`GeomagneticRepositoryImpl`).
3. **Rumbo a Londres** (`BearingCalculator.initialBearing`): fórmula de círculo máximo, `atan2(sinΔλ·cosφ₂, cosφ₁·sinφ₂ − sinφ₁·cosφ₂·cosΔλ)`.
4. **Ángulo de la aguja**: `BearingCalculator.normalizeDegrees(rumbo − azimutVerdadero)`.
5. **"Apuntando a Londres"**: `|BearingCalculator.relativeAngle(rumbo, azimut)| < 5°` (constante `POINTING_THRESHOLD_DEGREES` en `ObserveLondonDirectionUseCase`) → dispara vibración háptica.

Todo esto se combina en `ObserveLondonDirectionUseCase.invoke()`, que hace `combine()` de los flows de ubicación y orientación y emite un `LondonDirection` por cada actualización.

### Patrones clave

- **UseCase**: `operator fun invoke()` devolviendo `Flow<T>` para lógica reactiva (ver `ObserveLondonDirectionUseCase`)
- **Repository**: interfaz en `domain`, implementación en `data`, basada en `callbackFlow` para APIs de callback de Android (sensores, ubicación)
- **UiState**: data class inmutable (`CompassUiState`) expuesta como `StateFlow` desde el ViewModel; se actualiza con `MutableStateFlow.update {}`
- **Errores**: `AppError` sealed interface en `core` (`SinSensorBrujula`, `SinPermisoUbicacion`, `UbicacionDesactivada`)

### Anti-patrones críticos

- No importar clases del framework Android en `domain` ni en `core` — deben ser Kotlin puro
- No poner lógica de negocio en ViewModels — extraer a UseCases en `domain`
- No crear dependencias circulares entre módulos
- No nombrar recursos XML con nombres reservados de Android (`background`, `foreground`, `icon`, `logo`, `text`, `button`) — añadir prefijo descriptivo (p. ej. `app_background`, `ic_home`)
- No añadir dependencias de red (Ktor, Retrofit) ni de persistencia (Room, SQLDelight, DataStore): la app es intencionadamente 100% offline y sin telemetría — ver "Privacidad" más abajo

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

- **Edge-to-edge obligatorio, sin opt-out** (`windowOptOutEdgeToEdgeEnforcement` ya no sirve). `MainActivity` llama a `enableEdgeToEdge()` sin parametrizar —su default decide el color de los iconos por `Configuration.uiMode`, la misma señal que usa `BrujulaDeLezoTheme`— y `CompassScreen` separa el fondo (pinta hasta los bordes) del contenido (`windowInsetsPadding(WindowInsets.safeDrawing)`). No se usa `Scaffold`: sin TopAppBar ni FAB, solo añadiría `PaddingValues` que propagar.
- **Restricciones de orientación ignoradas en pantallas ≥600dp.** No hay `android:screenOrientation` en el manifest; el bloqueo es programático y gateado por `R.bool.lock_portrait_orientation` (`true` en `values/`, `false` en `values-sw600dp/` y en `src/debug/` para poder probar el apaisado en móvil). No se declara `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY`, el opt-out disponible hasta API 37.

**El layout no puede asumir vertical.** `CompassContent` usa `BoxWithConstraints` (no `LocalConfiguration.screenWidthDp`, lint `ConfigurationScreenWidthHeight`) y exige ancho ≥600dp **y** ventana apaisada para las dos columnas: con el ancho solo, una tablet en vertical deja media pantalla vacía. La rosa se dimensiona a partir del espacio disponible; `compass_face.png` es 1024×1024, así que hasta ~420dp no hay reescalado.

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

Stack: **JUnit 4 + MockK 1.14.9 + Turbine 1.2.1 + `kotlinx-coroutines-test`**, con `MainDispatcherRule` (en `presentation/src/test`) usando `UnconfinedTestDispatcher`.

`:data` solo tiene JUnit: su único test (`AxisRemapTest`) cubre lógica pura. Las constantes `Surface.ROTATION_*` y `SensorManager.AXIS_*` son `static final int` y kotlinc las inlinea, así que no hace falta Robolectric ni `unitTests.isReturnDefaultValues`.

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
