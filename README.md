# Brújula de Lezo

> «Todo buen español deberá mear siempre mirando a Inglaterra»
> — *atribuido a Blas de Lezo*

Una brújula para Android que no apunta al norte. Apunta siempre a Londres.

[![Android CI](https://github.com/parrazam/brujula-de-lezo/actions/workflows/android.yml/badge.svg)](https://github.com/parrazam/brujula-de-lezo/actions/workflows/android.yml)
[![Release](https://img.shields.io/github/v/release/parrazam/brujula-de-lezo?label=release)](https://github.com/parrazam/brujula-de-lezo/releases/latest)

🌐 [brujuladelezo.cuzo.dev](https://brujuladelezo.cuzo.dev)

---

## Qué hace

Gires como gires el móvil, la aguja señala la dirección real hacia Londres (51,51 °N, 0,13 °O). Aplica la declinación magnética de tu posición para que el rumbo sea geográficamente exacto, vibra cuando apuntas a Inglaterra (±5°) y avisa si el sensor de la brújula necesita calibración.

## Tecnología

| | |
|---|---|
| **Lenguaje** | Kotlin 2.1.0 |
| **UI** | Jetpack Compose + Material 3 (Compose BOM 2024.12.01) |
| **Arquitectura** | Clean Architecture multi-módulo |
| **DI** | Manual (`AppContainer`) |
| **Sensores** | `TYPE_ROTATION_VECTOR` + `GeomagneticField` |
| **Localización** | `LocationManager` (sin Google Play Services) |
| **SDK mínimo** | API 26 (Android 8.0) |
| **SDK objetivo** | API 35 (Android 15) |

## Arquitectura

Seis módulos Gradle con la siguiente jerarquía de dependencias:

```
app → presentation, data, domain, core, design-system
presentation → domain, design-system, core
data → domain, core
domain → core   ← Kotlin puro, sin Android
core            ← Kotlin puro, sin Android
design-system   ← Compose
```

| Módulo | Responsabilidad |
|---|---|
| `:core` | `DispatcherProvider`, tipos base |
| `:domain` | Modelos, interfaces de repositorio, `ObserveLondonDirectionUseCase`, `BearingCalculator` |
| `:data` | `OrientationRepositoryImpl`, `LocationRepositoryImpl`, `GeomagneticRepositoryImpl` |
| `:presentation` | `CompassViewModel`, `CompassScreen` |
| `:design-system` | Tema Material 3, paleta del Siglo de Oro, `CompassRose`, `LondonNeedle` |
| `:app` | `MainActivity`, `BrujulaApplication`, `AppContainer` |

## Cómo funciona la matemática

1. **Azimut del dispositivo**: sensor `TYPE_ROTATION_VECTOR` → `getRotationMatrix` → `getOrientation` → azimut magnético en `[0, 360)`.
2. **Norte verdadero**: `azimutVerdadero = azimutMagnético + GeomagneticField.declination`.
3. **Rumbo a Londres**: fórmula de círculo máximo → `atan2(sinΔλ·cosφ₂, cosφ₁·sinφ₂ − sinφ₁·cosφ₂·cosΔλ)`.
4. **Ángulo de la aguja**: `normalizeDegrees(rumboALondres − azimutVerdadero)`.
5. **Apuntando**: cuando `|relativeAngle(rumbo, azimut)| < 5°` → vibración háptica.

## Desarrollo local

Requiere **JDK 17** y **Android SDK con API 35**.

```bash
# Compilar (variante debug)
./gradlew assembleDebug

# Ejecutar todos los tests unitarios
./gradlew test

# Lint
./gradlew lint

# Instalar en dispositivo conectado
./gradlew installDebug
```

## Tests

```bash
./gradlew :domain:test          # BearingCalculatorTest, ObserveLondonDirectionUseCaseTest
./gradlew :presentation:testDebugUnitTest   # CompassViewModelTest
```

Stack de test: **JUnit 4 + MockK 1.13.13 + Turbine 1.2.0 + `MainDispatcherRule`** con `UnconfinedTestDispatcher`.

## CI/CD

| Disparador | Pipeline |
|---|---|
| Push a `master`/`develop`, PR a `master` | `android.yml` → test + lint + assembleDebug |
| Tag `v*` | `release.yml` → assembleRelease + bundleRelease + firma APK + GitHub Release |

Los artefactos de release se firman con un keystore codificado en el secret `KEYSTORE_BASE64`. Ver [`.github/workflows/`](.github/workflows/).

## Privacidad

- Sin acceso a internet: la app no pide permiso de red.
- Sin telemetría, sin analítica, sin anuncios.
- Permiso mínimo: `ACCESS_COARSE_LOCATION` — solo para calcular el rumbo, en memoria, sin almacenarlo.
- Las coordenadas de Londres están escritas en el código; no se descargan datos.

## Licencia

MIT © 2026 Víctor Parra
