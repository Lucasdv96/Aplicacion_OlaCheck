# Feature 5: Detalle de Playa

## Objetivo
Pantalla que muestra las condiciones climáticas extendidas de una playa específica al navegar desde el Home. Refresca los datos de la API al entrar para mostrar información actualizada.

## Archivos

### `BeachDetailRepository.kt` — `repository/`
Interfaz que define el contrato para obtener los datos de una playa específica.

```kotlin
interface BeachDetailRepository {
    suspend fun getBeachById(beachId: String): Beach?
    suspend fun getConditionsForBeach(beachId: String): BeachConditions?
    suspend fun refreshConditions(beachId: String)
}
```

A diferencia del `BeachListRepository`, este repositorio trabaja con funciones `suspend` en lugar de `Flow` porque el detalle es una consulta puntual, no una observación continua.

---

### `BeachDetailRepositoryImpl.kt` — `data/remote/`
Implementación del repositorio de detalle.

- `getBeachById()` → lee la playa de Room por su id
- `getConditionsForBeach()` → obtiene las condiciones de Room usando `.first()` para una lectura puntual
- `refreshConditions()` → llama a Open-Meteo (Weather + Marine) y actualiza las condiciones en Room antes de mostrarlas

El refresh siempre ocurre al entrar al detalle para garantizar datos frescos, a diferencia del Home que solo refresca si hay conexión.

---

### `BeachDetailViewModel.kt` — `viewmodel/`
Lógica de la pantalla de detalle. Recibe el `beachId` a través de `SavedStateHandle`.

```kotlin
private val beachId: String = checkNotNull(savedStateHandle["beachId"])
```

`SavedStateHandle` es la forma correcta de recibir argumentos de navegación en un ViewModel con Hilt — lee el `beachId` directamente de la ruta de navegación, sobrevive a cambios de configuración (rotación de pantalla) y es la alternativa recomendada a pasar argumentos por el constructor.

`BeachDetailUiState` agrupa la playa y sus condiciones en un solo objeto:
```kotlin
data class BeachDetailUiState(
    val beach: Beach? = null,
    val conditions: BeachConditions? = null
)
```

---

### `BeachDetailScreen.kt` — `ui/screens/`
Pantalla de detalle con `TopAppBar` que incluye botón de volver y muestra cada condición en una card individual.

- `ConditionItem()` es un Composable auxiliar que muestra cada dato con su etiqueta a la izquierda y el valor a la derecha
- El `when` maneja los estados Loading, Error y Success
- Si las condiciones son `null` muestra un `CircularProgressIndicator` mientras carga

---

### Actualización de `NavGraph.kt`
Se agregó la ruta de detalle con argumento:

```kotlin
composable(
    route = "detail/{beachId}",
    arguments = listOf(navArgument("beachId") { type = NavType.StringType })
) {
    BeachDetailScreen(onNavigateBack = { navController.popBackStack() })
}
```

`navArgument` define el tipo del parámetro para que Navigation pueda parsearlo correctamente. `popBackStack()` vuelve a la pantalla anterior (Home) sin crear una nueva entrada en el back stack.

---

## Flujo
```
Usuario toca una playa en Home
    → NavController navega a "detail/{beachId}"
    → BeachDetailViewModel recibe el beachId via SavedStateHandle
    → refreshConditions() llama a Open-Meteo y actualiza Room
    → getBeachById() y getConditionsForBeach() leen de Room
    → BeachDetailScreen muestra los datos
    → Usuario presiona ← → popBackStack() vuelve a Home
```

## Condiciones mostradas
| Dato | Unidad |
|------|--------|
| Temperatura del agua | °C |
| Temperatura del aire | °C |
| Velocidad del viento | km/h |
| Dirección del viento | ° |
| Altura de olas | m |
| Período de olas | s |
| Humedad | % |

## Decisiones técnicas
- Se usa `SavedStateHandle` en lugar de pasar el `beachId` al constructor del ViewModel para compatibilidad con Hilt y supervivencia a cambios de configuración.
- El refresh se hace siempre al entrar (no solo si hay conexión) para priorizar datos frescos en la pantalla de detalle.
- `popBackStack()` en lugar de `navigate(Routes.HOME)` para no crear una nueva instancia de Home en el back stack.
