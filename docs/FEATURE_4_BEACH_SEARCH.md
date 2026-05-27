# Feature 4: Búsqueda y Filtrado Reactivo

## Objetivo
Permitir al usuario buscar playas por nombre en tiempo real dentro de la pantalla Home, con opción de limpiar la búsqueda.

## Implementación
Esta feature se implementó directamente dentro de `HomeScreen.kt` ya que no requiere lógica adicional en el ViewModel ni en el repositorio — el filtrado se hace en la UI sobre la lista ya cargada.

## Cómo funciona

### Estado local de búsqueda
```kotlin
var searchQuery by remember { mutableStateOf("") }
```
`remember` mantiene el valor del texto entre recomposiciones. Es estado local de la UI, no necesita estar en el ViewModel porque no afecta la lógica de negocio.

### Campo de búsqueda
```kotlin
OutlinedTextField(
    value = searchQuery,
    onValueChange = { searchQuery = it },
    placeholder = { Text("Buscar playa...") },
    singleLine = true,
    trailingIcon = {
        if (searchQuery.isNotEmpty()) {
            TextButton(onClick = { searchQuery = "" }) { Text("X") }
        }
    }
)
```
- `onValueChange` actualiza el estado en cada tecla presionada
- El botón "X" solo aparece cuando hay texto escrito y limpia la búsqueda al presionarlo

### Filtrado reactivo
```kotlin
val filtered = state.data.filter {
    it.beach.name.contains(searchQuery, ignoreCase = true)
}
```
- Se filtra sobre la lista cargada en memoria (no hace nuevas llamadas a la API ni a Room)
- `ignoreCase = true` permite buscar "mar del plata", "Mar Del Plata" o "MAR DEL PLATA" indistintamente
- Al estar dentro del `when (is UiState.Success)`, el filtrado solo ocurre cuando hay datos disponibles

## Decisiones técnicas
- El filtrado se hace en la UI y no en el ViewModel porque es una operación de presentación pura, sin lógica de negocio.
- No se necesita debounce porque la lista es pequeña (4 playas) y el filtrado es instantáneo.
- Si en el futuro la lista crece significativamente, se puede mover el filtrado al ViewModel con un `StateFlow<String>` para el query y `combine()` con el Flow de playas.
