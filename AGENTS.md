# AGENT.md — NextPlayer

Guía de reglas y restricciones para agentes de IA que modifiquen este repositorio.
Lee este archivo **completo antes de tocar cualquier código**.

---

## 1. Qué es este proyecto

NextPlayer es un **reproductor de video nativo para Android** escrito en Kotlin.
Usa arquitectura multi-módulo con Clean Architecture, Jetpack Compose (Material 3), Hilt para DI, Room para base de datos y ExoPlayer/Media3 para reproducción.

El objetivo del proyecto es ser **simple, eficiente y libre de ads**. No es una plataforma general. No sobre-extiendas su alcance.

---

## 2. Mapa de módulos

```
:app                      ← Punto de entrada. Solo routing, DI wiring, MainActivity
:core:common              ← Utilidades puras: extensions, Result wrapper, constants
:core:model               ← Data classes puras. SIN dependencias de Android/framework
:core:database            ← Room: DAOs, entidades, migrations
:core:datastore           ← DataStore: preferencias de usuario
:core:data                ← Repositorios. Une database + datastore + fuentes externas
:core:domain              ← Use cases. Solo lógica de negocio pura
:core:media               ← Wrapper de Media3/ExoPlayer. Player logic
:core:ui                  ← Componentes Compose compartidos, temas, tokens de diseño
:feature:player           ← Pantalla del reproductor de video
:feature:settings         ← Pantalla de configuración
:feature:videopicker      ← Pantalla de exploración y selección de archivos
```

### Reglas de dependencia entre módulos (NO romper)

```
feature:*   → puede depender de → core:ui, core:domain, core:model, core:common
feature:*   → NUNCA depende de → otro feature:*
feature:*   → NUNCA depende de → core:database, core:datastore directamente

core:domain → puede depender de → core:model, core:common
core:domain → NUNCA depende de → core:database, core:datastore, core:data, core:media

core:data   → puede depender de → core:database, core:datastore, core:model, core:common
core:data   → NUNCA depende de → core:domain, core:ui, feature:*

core:model  → NUNCA depende de → ningún otro módulo del proyecto

:app        → puede depender de → todos los módulos
:app        → NO debe contener lógica de negocio
```

Si necesitas compartir algo entre dos features, el lugar correcto es `core:common`, `core:model` o `core:ui`.

---

## 3. Stack tecnológico — versiones y patrones esperados

| Tecnología | Uso | Notas |
|---|---|---|
| Kotlin | Lenguaje principal | Usar idioms Kotlin: `data class`, `sealed class`, `when`, etc. |
| Jetpack Compose | UI | Solo Compose. No añadir Views/XML sin justificación fuerte |
| Material 3 | Design system | Usar tokens de `MaterialTheme`. No hardcodear colores |
| Hilt | Inyección de dependencias | `@HiltViewModel`, `@Inject`, `@Module`. Sin Service Locators manuales |
| Room | Persistencia local | Migraciones obligatorias si cambias el esquema |
| DataStore (Proto/Prefs) | Preferencias usuario | No usar SharedPreferences |
| Media3 / ExoPlayer | Reproducción multimedia | Cambios en `:core:media` afectan toda la reproducción. Extremar cuidado |
| KSP | Procesador de anotaciones | No mezclar con KAPT |
| ktlint | Linting | El build falla si hay errores de estilo. Ejecutar antes de entregar cambios |
| Gradle KTS | Scripts de build | No usar Groovy |

---

## 4. Patrones de arquitectura obligatorios

### 4.1 MVVM en features

Cada pantalla sigue este flujo sin excepción:

```
UI (Composable)
  ↓ eventos del usuario
ViewModel (HiltViewModel)
  ↓ llama a
UseCase (:core:domain)
  ↓ llama a
Repository (:core:data)
  ↓ lee/escribe
Database / DataStore / Media
```

- El ViewModel expone **`StateFlow<UiState>`** y acepta **eventos** mediante funciones o `Channel`.
- Los Composables son **stateless** donde sea posible: reciben estado y emiten eventos.
- **NUNCA** inyectes `Context` en un ViewModel. Usa `AndroidViewModel` solo si es absolutamente necesario, y justifícalo.

### 4.2 Use Cases

- Un Use Case = una sola responsabilidad.
- Son clases `operator fun invoke(...)`: `GetVideoListUseCase`, `UpdatePlayerPreferenceUseCase`.
- No mezcles lógica de UI dentro de un Use Case.
- Devuelven `Flow<T>` o `Result<T>` según corresponda.

### 4.3 Repositorios

- La interfaz vive en `:core:domain` o `:core:data` (no en features).
- La implementación concreta en `:core:data`.
- Hilt hace el binding de interfaz → implementación.

### 4.4 Modelos

- Los modelos de dominio viven en `:core:model` como `data class` sin anotaciones de Room ni de red.
- Los modelos de BD (`*Entity`) viven en `:core:database` y tienen mappers a modelos de dominio.
- **NUNCA** uses entidades de Room directamente en la UI o en use cases.

---

## 5. Reglas de código Kotlin

```kotlin
// ✅ CORRECTO: función de extensión en lugar de util class
fun Long.toReadableDuration(): String { ... }

// ❌ INCORRECTO: clase utilitaria estática
object DurationUtils {
    fun toReadable(ms: Long): String { ... }
}

// ✅ CORRECTO: sealed class para estados de UI
sealed interface PlayerUiState {
    object Loading : PlayerUiState
    data class Ready(val video: Video) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

// ❌ INCORRECTO: booleanos y nullable para representar estado
data class PlayerUiState(
    val isLoading: Boolean = false,
    val video: Video? = null,
    val error: String? = null
)

// ✅ CORRECTO: StateFlow inmutable expuesto desde ViewModel
private val _uiState = MutableStateFlow<PlayerUiState>(Loading)
val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

// ❌ INCORRECTO: exponer MutableStateFlow
val uiState = MutableStateFlow<PlayerUiState>(Loading)
```

- Preferir `val` sobre `var`.
- Usar `data class` para modelos, `sealed class`/`sealed interface` para estados y eventos.
- No uses `!!` (null assertion). Si el compilador no puede garantizar non-null, maneja el caso.
- Coroutines: lanzar desde `viewModelScope` en ViewModels. No crear `CoroutineScope` manualmente fuera de DI.
- Flows: preferir `StateFlow` sobre `LiveData`. No mezclar ambos.

---

## 6. Reglas de UI / Compose

- Cada pantalla tiene su propio `Route` composable (stateful, recibe el ViewModel) y sub-composables stateless.
- Usa `LaunchedEffect` para efectos de una sola vez (navegación, snackbars).
- No pongas lógica de negocio dentro de composables.
- Los colores, tipografía y formas vienen de `MaterialTheme`. Si necesitas un color nuevo, agrégalo al tema en `:core:ui`, no lo hardcodees.
- Para iconos: usar `Icons.Rounded.*` o los assets existentes en `:core:ui`. No importes librerías de iconos nuevas sin discutirlo.
- Previews: cada composable nuevo debe tener al menos un `@Preview`.

---

## 7. Base de datos (Room)

- **Si modificas el esquema** (`@Entity`), DEBES:
  1. Incrementar `version` en el `@Database`.
  2. Agregar una `Migration` o usar `fallbackToDestructiveMigration()` solo si los datos no importan.
  3. Nunca elimines una migración existente.
- Los DAOs devuelven `Flow<T>` para queries reactivas y `suspend fun` para escrituras.
- No ejecutes queries de Room en el hilo principal.

---

## 8. Reproducción multimedia (:core:media)

Este módulo es el más crítico. Un error aquí rompe la función principal de la app.

- **No refactorices este módulo sin tests**.
- Cualquier cambio en cómo se inicializa o se libera el `Player` debe verificarse manualmente en:
  - Pausa/reanuda de la app
  - Picture-in-Picture
  - Rotación de pantalla
  - Llamada telefónica entrante
- El ciclo de vida del player debe estar correctamente ligado al `LifecycleOwner`.
- No añadas lógica de UI en este módulo.

---

## 9. Inyección de dependencias (Hilt)

- Los módulos Hilt van en el paquete `di/` de cada módulo core.
- Usa `@Singleton` solo para dependencias realmente globales (repositorios, player).
- Usa `@ViewModelScoped` para cosas que viven con el ViewModel.
- No uses `@ActivityScoped` en módulos core (no deben conocer Activities).
- No inyectes `Application` o `Context` donde no sea necesario.

---

## 10. Lo que NO debes hacer (anti-patrones prohibidos)

| Anti-patrón | Por qué está prohibido |
|---|---|
| Agregar lógica de negocio en un Composable | Hace el código inestable y no testeable |
| Importar un feature desde otro feature | Rompe la separación de módulos |
| Usar `GlobalScope` para coroutines | Causa memory leaks |
| Añadir dependencias directas de Room en features | Los features no saben de persistencia |
| Crear una clase `God Object` o `Manager` con 500+ líneas | Monolítico, difícil de mantener |
| Hardcodear strings en la UI | Deben ir en `strings.xml` |
| Hardcodear colores en Composables | Deben venir de `MaterialTheme` |
| Hacer network requests en el hilo principal | Crash garantizado |
| Modificar `gradle.properties` sin justificación | Afecta el build de todos |
| Añadir una nueva dependencia sin revisar si ya existe una equivalente | Infla el APK |

---

## 11. Cómo añadir una nueva feature

1. Crear el módulo en `feature/nombre_feature/`.
2. Configurar su `build.gradle.kts` siguiendo el patrón de `feature:player` o `feature:settings`.
3. Registrarlo en `settings.gradle.kts` con `include(":feature:nombre_feature")`.
4. Crear el Use Case correspondiente en `:core:domain`.
5. Si necesita datos persistentes: agregar Entity + DAO en `:core:database`, Repository en `:core:data`.
6. Wiring de Hilt en `:app` si es necesario.
7. Registrar la ruta de navegación en `:app`.

**No crear features directamente en `:app`.** El módulo `:app` solo orquesta.

---

## 12. Verificación antes de entregar cambios

Ejecutar en orden. Si algo falla, **no entregar hasta corregirlo**.

```bash
# 1. Verificar estilo de código
./gradlew ktlintCheck

# 2. Compilar todos los módulos
./gradlew assembleDebug

# 3. Correr tests unitarios
./gradlew test

# 4. Si modificaste :core:database, verificar migraciones
./gradlew :core:database:test

# 5. Si modificaste :core:media o :feature:player, prueba manual obligatoria:
#    - Reproducción normal
#    - Pausa y reanuda
#    - Picture-in-Picture
#    - Rotación de pantalla
```

Para corregir errores de estilo automáticamente:
```bash
./gradlew ktlintFormat
```

---

## 13. Gestión de dependencias

Las versiones de todas las dependencias están en `gradle/libs.versions.toml` (Version Catalog).

- **Siempre** añade nuevas dependencias al catalog, nunca directamente en un `build.gradle.kts`.
- Antes de añadir una librería nueva, verifica si ya hay una que cubra esa necesidad.
- No subas versiones de dependencias sin verificar el changelog por breaking changes.
- Las dependencias de Compose y Media3 son especialmente sensibles a versiones incompatibles.

---

## 14. Scope del agente — qué modificar y qué NO tocar

### Puedes modificar libremente:
- Archivos dentro de `feature:*` si el cambio es acotado a esa feature.
- Use Cases en `core:domain`.
- Strings, drawables, estilos en `core:ui`.
- Tests unitarios.

### Modificar con cuidado (revisa impacto):
- `core:data` y `core:database` — cambios aquí afectan datos persistidos del usuario.
- `core:model` — cambios en data classes pueden romper serialización o Room.
- `core:media` — riesgo alto de romper reproducción.

### NO modificar sin instrucción explícita del desarrollador:
- `gradle/libs.versions.toml` (subir versiones de dependencias)
- `gradle.properties` (configuración de build)
- `settings.gradle.kts` (registro de módulos)
- Migraciones de Room existentes
- El módulo `:app` más allá de wiring de DI y navegación

---

## 15. Resumen rápido de reglas críticas

1. **Un módulo, una responsabilidad.** No metas todo en `:app` ni en `:core:common`.
2. **Las features no se conocen entre sí.** Comunicación solo a través de core.
3. **Los modelos de dominio no tienen anotaciones de framework.** Room y Gson van en sus propias capas.
4. **Cada cambio en Room necesita una migración.** Sin excepciones en producción.
5. **ktlint debe pasar.** El build del CI falla si no.
6. **No uses `!!`.** Maneja nullabilidad explícitamente.
7. **No toques `:core:media` sin tests.** Es la columna vertebral del reproductor.
8. **Pregunta antes de añadir dependencias externas.** Menos es más.
