# Feature 3: Beach List (Home)

## Objetivo
Pantalla principal que muestra la lista de playas con sus condiciones climáticas en tiempo real. Implementa estrategia offline-first: Room como Single Source of Truth, con datos frescos de Open-Meteo y la lista de playas descargada de Firestore.

## Archivos

### `Beach.kt` — `data/local/`
Entidad de Room que representa una playa. Room la convierte en una tabla `beach` en la base de datos SQLite local.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | String (PK) | Identificador único |
| name | String | Nombre de la playa |
| latitude | Double | Coordenada geográfica |
| longitude | Double | Coordenada geográfica |
| imageUrl | String? | URL de imagen (nullable) |
| lastUpdated | Long | Timestamp de última actualización |

---

### `BeachConditions.kt` — `data/local/`
Entidad de Room que guarda las condiciones climáticas de cada playa. Tiene una `ForeignKey` que la vincula a la tabla `beach` con `CASCADE` — si se borra una playa, se borran sus condiciones automáticamente.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| beachId | String (FK) | Referencia a beach.id |
| waterTemp | Float | Temperatura del agua |
| airTemp | Float | Temperatura del aire |
| windSpeed | Float | Velocidad del viento |
| windDirection | Float | Dirección del viento |
| waveHeight | Float | Altura de olas |
| wavePeriod | Float | Período de olas |
| tideStatus | String | Estado de marea |
| humidity | Float | Humedad relativa |
| fetchedAt | Long | Timestamp de última consulta |

---

### `BeachDao.kt` — `data/local/`
Interfaz DAO de Room para operaciones sobre la tabla `beach`.
Room genera automáticamente el código SQL a partir de las anotaciones.

- `getAllBeaches()` → Flow reactivo, la UI se actualiza automáticamente
- `insertBeaches()` → con `REPLACE` para actualizar sin errores
- `getBeachById()` → consulta puntual para el detalle

---

### `BeachConditionsDao.kt` — `data/local/`
Interfaz DAO de Room para operaciones sobre la tabla `beach_conditions`.

- `getConditionsByBeachId()` → Flow reactivo por playa
- `insertConditions()` → inserta o reemplaza condiciones de una playa
- `getAllConditions()` → Flow con todas las condiciones (usado para combinar con la lista de playas)

---

### `AppDatabase.kt` — `data/local/`
Clase principal de Room que agrupa todas las entidades y DAOs. Room usa esta clase para crear y gestionar la base de datos SQLite.

- `version = 1` → si se cambia la estructura de alguna entidad, hay que incrementar este número y crear una migración
- `exportSchema = false` → evita que Room genere archivos de esquema

---

### `WeatherService.kt` / `MarineService.kt` — `data/remote/`
Interfaces de Retrofit que definen las llamadas a Open-Meteo.

Se separaron en dos interfaces porque tienen **URLs base distintas**:
- Weather: `https://api.open-meteo.com/`
- Marine: `https://marine-api.open-meteo.com/`

Los campos de la respuesta usan los nombres exactos de la API (ej: `temperature_2m`, `wave_height`) para que Gson pueda mapear el JSON automáticamente.

---

### `BeachListRepository.kt` — `repository/`
Interfaz que define el contrato del repositorio de playas.

```kotlin
interface BeachListRepository {
    fun getBeaches(): Flow<List<Beach>>
    fun getAllConditions(): Flow<List<BeachConditions>>
    fun getConditionsForBeach(beachId: String): Flow<BeachConditions?>
    suspend fun refreshBeachData()
    fun isOnline(): Boolean
}
```

---

### `BeachListRepositoryImpl.kt` — `data/remote/`
Implementación del repositorio. Aplica la estrategia **offline-first**:

1. Descarga la lista de playas desde Firestore
2. Guarda las playas en Room
3. Para cada playa llama a Open-Meteo (Weather + Marine) y guarda las condiciones en Room
4. La UI siempre lee desde Room (nunca directamente de la API)

Las playas se almacenan en Firestore para que sean escalables — se pueden agregar nuevas playas sin tocar el código, solo agregando documentos en la colección `beaches`.

`isOnline()` usa `ConnectivityManager` para verificar si hay internet real disponible.

---

### `UiState.kt` — `ui/screens/`
Estados posibles de la pantalla Home. Genérico para poder reutilizarse en otras pantallas.

```kotlin
sealed class UiState<out T> {
    class Loading<T> : UiState<T>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error<T>(val message: String) : UiState<T>()
    class Offline<T> : UiState<T>()
}
```

---

### `HomeViewModel.kt` — `viewmodel/`
Lógica de la pantalla Home. Combina el Flow de playas con el Flow de condiciones usando `combine`.

- `init { loadBeaches() }` arranca la carga automáticamente
- Si hay conexión → refresca datos de API antes de mostrar
- Si no hay conexión → muestra datos cacheados en Room
- `retry()` permite reintentar desde la UI

---

### `HomeScreen.kt` — `ui/screens/`
UI de la pantalla Home con:
- Barra de búsqueda reactiva que filtra por nombre de playa en tiempo real
- `LazyColumn` con cards de playas
- Banner de "Modo Offline" cuando no hay conexión
- Estados de Loading, Error y Offline manejados visualmente

---

## Flujo
```
Home abre → HomeViewModel verifica conexión
                ↓
    ¿Hay conexión? → Sí → descarga playas de Firestore → guarda en Room
                         → llama a Open-Meteo por cada playa → guarda condiciones en Room
                  → No → usa datos cacheados en Room
                ↓
    UI observa Room via Flow → se actualiza automáticamente
```

## Estructura Firestore
```
beaches (colección)
├── pinamar     → name, latitude, longitude, imageUrl
├── mardelplata → name, latitude, longitude, imageUrl
├── villagesell → name, latitude, longitude, imageUrl
└── mardeajo    → name, latitude, longitude, imageUrl
```

## Decisiones técnicas
- Las playas se guardan en Firestore (no hardcodeadas) para que la app sea escalable.
- Se usan dos instancias de Retrofit separadas porque Weather y Marine tienen URLs base distintas.
- `combine()` en el ViewModel permite combinar dos Flows reactivos (playas + condiciones) en uno solo.
