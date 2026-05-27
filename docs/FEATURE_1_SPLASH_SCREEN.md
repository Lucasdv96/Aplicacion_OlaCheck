# Feature 1: Splash Screen

## Objetivo
Pantalla inicial de la app que verifica si hay una sesión activa guardada y redirige al usuario a Home o Login según corresponda.

## Archivos

### `SplashRepository.kt` — `repository/`
Interfaz que define el contrato para verificar si hay sesión activa.
Existe como interfaz (y no como implementación directa) para seguir Clean Architecture: el ViewModel no necesita saber cómo se verifica la sesión, solo que puede preguntarlo.

```kotlin
interface SplashRepository {
    suspend fun isUserLoggedIn(): Boolean
}
```

---

### `SplashRepositoryImpl.kt` — `data/local/`
Implementación del `SplashRepository`. Consulta el `UserDataStore` para saber si hay un `uid` guardado localmente.
Usa `.first()` sobre el Flow del DataStore para obtener el valor actual de forma puntual (sin suscribirse continuamente).

```kotlin
override suspend fun isUserLoggedIn(): Boolean {
    return userDataStore.isLoggedIn.first()
}
```

---

### `SplashViewModel.kt` — `viewmodel/`
Contiene la lógica de la pantalla. Al iniciarse llama al repositorio y expone el resultado como `StateFlow<Boolean?>`.
- `null` → todavía verificando
- `true` → hay sesión activa
- `false` → no hay sesión

La UI observa este StateFlow y navega cuando el valor cambia de `null`.

---

### `SplashScreen.kt` — `ui/screens/`
Composable visual del Splash. Muestra el nombre de la app mientras el ViewModel verifica la sesión.
Usa `LaunchedEffect` para reaccionar al cambio de estado y navegar a Home o Login sin que el Composable sepa nada de rutas.

```kotlin
LaunchedEffect(isUserLoggedIn) {
    when (isUserLoggedIn) {
        true -> onNavigateToHome()
        false -> onNavigateToLogin()
        null -> Unit
    }
}
```

---

## Flujo
```
App abre → SplashScreen muestra "OlaCheck"
               ↓
    SplashViewModel verifica sesión (DataStore)
               ↓
    ¿Sesión activa? → true  → navega a Home
                    → false → navega a Login
```

## Decisiones técnicas
- Se usa `popUpTo(Routes.SPLASH) { inclusive = true }` al navegar para que el usuario no pueda volver al Splash con el botón de atrás.
- La implementación del repositorio devuelve `false` por defecto hasta que Firebase Auth esté implementado (Feature 2).
