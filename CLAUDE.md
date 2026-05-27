# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

**brujula-de-lezo** es una aplicación Android nativa en desarrollo. El proyecto está en su fase inicial: aún no hay código fuente de aplicación ni estructura Gradle.

## Comandos de desarrollo

Una vez que exista la estructura Gradle del proyecto (fichero `gradlew`):

```bash
# Compilar (variante debug)
./gradlew assembleDebug

# Ejecutar todos los tests unitarios
./gradlew test

# Tests unitarios de un módulo concreto
./gradlew :app:testDebugUnitTest

# Ejecutar una clase de test concreta
./gradlew :app:testDebugUnitTest --tests "com.example.myapp.SomeViewModelTest"

# Tests de instrumentación (requiere dispositivo o emulador)
./gradlew connectedDebugAndroidTest

# Lint con Detekt
./gradlew detekt

# Lint de estilo con ktlint
./gradlew ktlintCheck

# Limpiar y recompilar
./gradlew clean assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug

# Diagnóstico de errores de build
./gradlew assembleDebug --stacktrace
./gradlew :app:dependencies
./gradlew --refresh-dependencies
```

## Arquitectura

El proyecto usa **Clean Architecture** en módulos Gradle separados con la siguiente jerarquía de dependencias:

```
app → presentation, domain, data, core
presentation → domain, design-system, core
data → domain, core
domain → core
core → (sin dependencias)
```

**Regla crítica**: `domain` es Kotlin puro — no puede importar clases del framework Android ni depender de `data` o `presentation`.

### Capas

- **`domain/`** — modelos de dominio (data classes puras), interfaces de Repository, UseCases con `operator fun invoke`
- **`data/`** — implementaciones de Repository, DataSources (Room/SQLDelight para local, Ktor para red), mappers entre entidades y modelos de dominio
- **`presentation/`** — ViewModels con `StateFlow<UiState>`, pantallas Compose, navegación
- **`core/`** — utilidades compartidas, tipos de error base
- **`design-system/`** — componentes Compose reutilizables, tema Material 3
- **`app/`** — punto de entrada Android, Application class, configuración de DI (Koin o Hilt)

### Patrones clave

- **UseCase**: una operación de negocio por clase, `operator fun invoke` para llamadas limpias
- **Repository**: interfaz en `domain`, implementación en `data`; coordina DataSources local y remoto
- **Mappers**: funciones de extensión junto a los modelos de datos (`ItemEntity.toDomain()`, `ItemDto.toEntity()`)
- **UiState**: sealed class o data class con `isLoading`, `error`, y el dato; expuesta como `StateFlow` inmutable
- **Errores**: `Result<T>` de Kotlin o sealed interface propio en domain; se mapean a mensajes UI en el ViewModel

### Anti-patrones críticos

- No importar clases del framework Android en `domain` — debe ser Kotlin puro
- No exponer entidades de BD ni DTOs a la capa UI — siempre mapear a modelos de dominio
- No poner lógica de negocio en ViewModels — extraer a UseCases
- No crear dependencias circulares entre módulos
- No nombrar recursos XML con nombres reservados de Android (`background`, `foreground`, `icon`, `logo`, `text`, `button`) — añadir prefijo descriptivo (e.g., `app_background`, `ic_home`)

### Corrutinas y threading

| Operación | Dispatcher |
|-----------|-----------|
| UI / actualizar state | `Dispatchers.Main` (por defecto en `viewModelScope`) |
| Red, ficheros, BD | `Dispatchers.IO` |
| Cálculo intensivo | `Dispatchers.Default` |

Los repositorios deben ser *main-safe* usando `withContext(Dispatchers.IO)` internamente. No usar `GlobalScope`; usar `viewModelScope` o `lifecycleScope`.

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

## Convenciones

- Inyección de dependencias: **Koin** (preferido para KMP) o **Hilt** (Android-only)
- Base de datos local: **Room** (Android) o **SQLDelight** (KMP)
- Red: **Ktor** con `ContentNegotiation` + `kotlinx.serialization`
- Tests de ViewModel: **MockK** + **Turbine** + `MainDispatcherRule` con `UnconfinedTestDispatcher`
- Todos los campos de DTOs de red deben declararse como `nullable`
- No usar `runBlocking` en el hilo principal
- No exponer `MutableStateFlow` fuera del ViewModel
