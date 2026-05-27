# Feature 2: Firebase Auth

## Objetivo
Implementar el login con Google mediante Firebase Authentication, persistir la sesión del usuario con DataStore y redirigir al Home tras una autenticación exitosa.

## Archivos

### `AuthUiState.kt` — `ui/screens/`
Define los posibles estados de la pantalla de login.
Usa `sealed class` para garantizar que solo existan estos 4 estados posibles, lo que permite un `when` exhaustivo en la UI sin necesidad de `else`.

```kotlin
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
```

---

### `UserDataStore.kt` — `data/local/`
Maneja la persistencia de sesión usando DataStore Preferences.
Guarda los datos del usuario (uid, nombre, email, foto) en el almacenamiento local del dispositivo.

- `isLoggedIn` → Flow que emite `true` si existe un `uid` guardado
- `saveUser()` → guarda los 4 datos del usuario tras un login exitoso
- `clearUser()` → borra todos los datos al hacer logout

Se usa DataStore en lugar de SharedPreferences porque es más seguro, soporta coroutines nativamente y es la solución recomendada por Google.

---

### `AuthRepository.kt` — `repository/`
Interfaz que define el contrato de autenticación.
El ViewModel solo conoce esta interfaz, no sabe si usa Firebase u otro proveedor. Esto permite cambiar la implementación sin tocar la lógica de negocio.

```kotlin
interface AuthRepository {
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<Unit>
    suspend fun signOut()
    fun isLoggedIn(): Flow<Boolean>
}
```

---

### `AuthRepositoryImpl.kt` — `data/remote/`
Implementación real del `AuthRepository`. Convierte la cuenta de Google en una credencial de Firebase, autentica y guarda los datos en DataStore.

- `GoogleAuthProvider.getCredential()` convierte la cuenta de Google en una credencial que entiende Firebase
- `.await()` convierte la tarea asíncrona de Firebase en una coroutine suspendible
- Si algo falla, el `try/catch` devuelve `Result.failure()` en lugar de crashear

---

### `LoginViewModel.kt` — `viewmodel/`
Maneja la lógica de la pantalla de login. Recibe el resultado del Google Sign-In, lo pasa al repositorio y actualiza el `AuthUiState` según el resultado.

- Empieza en `Idle`
- Al presionar el botón → `Loading`
- Si el login es exitoso → `Success`
- Si falla → `Error` con el mensaje de error

---

### `LoginScreen.kt` — `ui/screens/`
Pantalla de login con botón "Iniciar sesión con Google".

- `rememberLauncherForActivityResult` lanza el flujo de Google Sign-In y recibe el resultado
- `LaunchedEffect(uiState)` navega a Home si el estado es `Success` o muestra un `Snackbar` si es `Error`
- Mientras carga muestra un `CircularProgressIndicator` en lugar del botón
- Requiere el `default_web_client_id` en `strings.xml` (generado desde `google-services.json`)

---

## Flujo
```
Usuario presiona "Iniciar sesión con Google"
    → Launcher abre el selector de cuentas de Google
    → Usuario selecciona su cuenta
    → LoginViewModel recibe la cuenta
    → AuthRepositoryImpl autentica con Firebase
    → Guarda uid, nombre, email, foto en DataStore
    → LoginViewModel emite Success
    → LoginScreen navega a Home
```

## Decisiones técnicas
- Se usa `Result<Unit>` como tipo de retorno para manejar éxito/error sin excepciones no controladas.
- La sesión se persiste en DataStore para que el Splash Screen pueda verificarla sin necesidad de conectarse a Firebase en cada inicio.
- Se necesita agregar el SHA-1 del certificado de debug en Firebase Console para que el Google Sign-In funcione en el dispositivo físico.
