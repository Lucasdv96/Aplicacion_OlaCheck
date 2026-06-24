# OlaCheck — Resumen Completo para Defensa TPO 2026

---

## ¿Qué es OlaCheck?

OlaCheck es una aplicación Android para consultar condiciones de playas argentinas en tiempo real. El usuario puede ver temperatura del agua, del aire, viento, olas y humedad de cada playa, obtener recomendaciones de un agente de IA, y participar en una comunidad por playa donde se comparten mensajes y fotos.

---

## Arquitectura General

La app sigue **MVVM + Clean Architecture + Repository Pattern**:

```
UI (Compose)
    ↕ observa StateFlow
ViewModel
    ↕ llama métodos
Repository (interfaz)
    ↕ implementación
Data Sources (Room / Firestore / Retrofit / Cloudinary)
```

- **La UI nunca accede directamente a datos** — siempre pasa por el ViewModel
- **El ViewModel nunca conoce de dónde vienen los datos** — solo habla con la interfaz del repositorio
- **Los repositorios abstraen la fuente** — pueden cambiar la implementación sin tocar el ViewModel ni la UI

---

## Flujo de Arranque (Splash → Home)

1. **`MainActivity`** crea el `NavGraph` y arranca en la ruta `splash`
2. **`SplashViewModel`** lee `UserDataStore.isLoggedIn` (Flow<Boolean>)
3. Si hay sesión guardada → navega a `home` con `popUpTo(splash, inclusive=true)`
4. Si no hay sesión → navega a `login`
5. El `popUpTo` es crítico: evita que el usuario vuelva al splash con el botón atrás

---

## Autenticación (Firebase Auth + DataStore)

### Flujo de login
1. Usuario toca "Iniciar sesión con Google"
2. `LoginViewModel` llama a `AuthRepository.signInWithGoogle(idToken)`
3. `AuthRepositoryImpl` autentica con Firebase (`signInWithCredential`)
4. Guarda uid, nombre, email y foto en `UserDataStore` (DataStore Preferences)
5. Navega a Home

### Flujo de logout
1. Usuario toca "Cerrar sesión" en ProfileScreen
2. `ProfileViewModel.signOut()` → `ProfileRepository.signOut()`
3. `AuthRepositoryImpl.signOut()` hace:
   - `firebaseAuth.signOut()` → cierra sesión en Firebase
   - `userDataStore.clearUser()` → borra los datos locales
   - `googleSignInClient.signOut()` → limpia la cuenta cacheada de Google (sin esto, el próximo login saltea el selector de cuenta)
4. Navega a Login con `popUpTo(0, inclusive=true)` → limpia todo el back stack

### ¿Por qué DataStore y no Room para la sesión?
DataStore es ideal para datos simples clave-valor (uid, nombre, email, foto). Room es una base de datos relacional — sería sobrediseño para guardar 4 strings.

---

## Home Screen — Playas con Offline-First

### ¿Cómo se cargan las playas?

```
HomeViewModel.init
    → loadBeaches()
        → isOnline()? → refreshBeachData() (Firestore + Open-Meteo → Room)
        → getBeaches() + getAllConditions()  (Room, siempre)
        → combine() → lista de BeachWithConditions → UiState.Success
    → observeConnectivity()
        → cuando vuelve internet → refreshBeachData() automático
```

### Offline-First en detalle

**Single Source of Truth = Room.** La UI siempre lee de Room, nunca de la red directamente.

1. Al arrancar: si hay internet, refresca Firestore → Open-Meteo → Room
2. Si el refresh falla: muestra datos viejos de Room + Snackbar de aviso
3. Si no hay datos y no hay internet: `UiState.Offline` con botón Reintentar
4. Cuando vuelve la red: `NetworkCallback` detecta la reconexión → refresh automático sin que el usuario haga nada

### ¿Qué es `combine()`?

```kotlin
getBeaches().combine(getAllConditions()) { beaches, conditions ->
    beaches.map { beach ->
        BeachWithConditions(beach, conditions.find { it.beachId == beach.id })
    }
}
```

Fusiona dos Flows de Room en uno solo. Cada vez que cambia alguno de los dos (playas o condiciones), re-emite la lista combinada. La UI solo necesita observar un Flow.

### Detección automática de reconexión

```kotlin
// En BeachListRepositoryImpl
override fun observeConnectivity(): Flow<Boolean> = callbackFlow {
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(true) }
        override fun onLost(network: Network) { trySend(false) }
    }
    cm.registerNetworkCallback(request, callback)
    trySend(isOnline()) // emite el estado actual al arrancar
    awaitClose { cm.unregisterNetworkCallback(callback) } // evita memory leak
}

// En HomeViewModel
repository.observeConnectivity()
    .distinctUntilChanged() // ignora si emite el mismo estado dos veces seguidas
    .drop(1)                // saltea la primera emisión (ya la maneja loadBeaches)
    .collect { online ->
        if (online) repository.refreshBeachData()
    }
```

`callbackFlow` convierte el `NetworkCallback` (API de callback) en un Flow de Kotlin reactivo. `awaitClose` es esencial para des-registrar el callback cuando el Flow se destruye (sin esto habría un memory leak).

---

## Detalle de Playa

- Recibe `beachId` via ruta de navegación → accedido en el ViewModel con `SavedStateHandle`
- `SavedStateHandle` es necesario con Hilt: no se puede usar `arguments` directamente
- Al entrar siempre refresca las condiciones de esa playa desde Open-Meteo
- Si el refresh falla pero hay datos en Room → muestra los datos viejos (offline-first)
- Si no hay datos y falla el refresh → `UiState.Offline`

### APIs usadas (Open-Meteo, gratuitas)
- **WeatherService**: `https://api.open-meteo.com/` → temperatura del aire, viento, humedad
- **MarineService**: `https://marine-api.open-meteo.com/` → temperatura del agua, altura y período de olas
- Son dos instancias de Retrofit separadas porque tienen **URLs base distintas**

---

## Agente AI (Gemini)

- Botón en la TopAppBar del detalle de playa → abre un `ModalBottomSheet` con chat
- El sistema lee las condiciones actuales (temp agua, aire, viento, olas, período, humedad)
- Construye un prompt en español solicitando: tipo de neoprene recomendado, justificación y advertencias
- Usa `gemini-1.5-flash` (modelo rápido y económico para respuestas de texto)
- La API Key nunca está en el código: vive en `local.properties` → inyectada en `BuildConfig` en tiempo de compilación

```kotlin
buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
```

---

## Comunidad

### ¿Cómo funciona el tiempo real?

Firestore tiene `addSnapshotListener`, que es un listener que se llama cada vez que cambia la colección. Lo envolvemos en `callbackFlow` para convertirlo en un Flow:

```kotlin
override fun getPostsForBeach(beachId: String): Flow<List<CommunityPost>> = callbackFlow {
    val listener = postsCollection
        .whereEqualTo("beachId", beachId)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(50)
        .addSnapshotListener { snapshot, error ->
            trySend(snapshot?.documents?.mapNotNull { ... } ?: emptyList())
        }
    awaitClose { listener.remove() } // cancela el listener al destruirse el Flow
}
```

El `awaitClose` es crítico: cuando el usuario sale de la pantalla y el ViewModel se destruye, el Flow se cancela y esto ejecuta `listener.remove()`. Sin esto, el listener seguiría activo y habría un memory leak.

### Estructura de un post en Firestore
```
community_posts/{postId}
├── beachId: "mardelplata"
├── userId: "abc123"
├── userName: "Lucas Del Valle"
├── userPhoto: "https://..."
├── text: "Las olas están buenas hoy"
├── mediaUrl: "https://res.cloudinary.com/..."  (vacío si no tiene foto)
└── timestamp: 1719180000000
```

---

## Subida de Fotos (Cloudinary)

### ¿Por qué Cloudinary y no Firebase Storage?
Firebase Storage requiere el **plan Blaze** (pago con tarjeta). Cloudinary tiene un plan gratuito con 25 créditos/mes, más que suficiente para un TPO.

### Flujo completo de subida

```
Usuario elige foto (galería o cámara)
    → URI local de la imagen
    → compressImage(uri):
        1. Lee dimensiones sin cargar la imagen completa (inJustDecodeBounds)
        2. Calcula inSampleSize para reducir uso de memoria
        3. Decodifica el bitmap reducido
        4. Escala a máx 1080px manteniendo proporción
        5. Lee metadatos EXIF y corrige la rotación
        6. Comprime a JPEG 80%
    → Arma request multipart (Retrofit)
    → POST https://api.cloudinary.com/v1_1/{cloudName}/image/upload
        con "file" (bytes) y "upload_preset" (texto)
    → Respuesta: { "secure_url": "https://res.cloudinary.com/..." }
    → Guarda la URL en el post de Firestore
```

### ¿Por qué el preset unsigned?
Cloudinary tiene dos modos de subida: signed (requiere firmar la request con el API secret) y unsigned (sin firma). El API secret **nunca puede ir en una app Android** porque cualquiera puede descompilar el APK y extraerlo. El preset unsigned está diseñado para apps cliente.

### ¿Por qué `inSampleSize`?
Una foto de cámara puede pesar 8-12 MB y ser de 4000x3000px. Cargarla entera en memoria como Bitmap provocaría un `OutOfMemoryError`. `inSampleSize = 4` carga la imagen a 1/4 de resolución en memoria antes de escalarla, sin perder calidad visible.

### Corrección de rotación EXIF
La cámara de Android guarda la orientación de la foto en los metadatos EXIF, pero los píxeles del archivo están siempre en la misma orientación (landscape). Si no se corrige, la foto aparece de costado.

```kotlin
val exif = ExifInterface(contentResolver.openInputStream(uri)!!)
val rotation = when (exif.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL)) {
    ORIENTATION_ROTATE_90  -> 90f
    ORIENTATION_ROTATE_180 -> 180f
    ORIENTATION_ROTATE_270 -> 270f
    else -> 0f
}
if (rotation != 0f) {
    val matrix = Matrix().apply { postRotate(rotation) }
    Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
}
```

---

## Identidad Visual

Paleta azul oceánica, coherente con la temática de playas:

| Color | Hex | Uso |
|-------|-----|-----|
| NavyBlue | `#0A1F44` | Primary en dark, surface en dark |
| BrightBlue | `#0072FF` | Primary en light (botones, acentos) |
| SkyBlue | `#00C6FF` | Primary en dark |
| White | `#FFFFFF` | Background en light |
| LightGray | `#F5F5F5` | Surface en light |
| NavyBlueDark | `#061530` | Background en dark |
| CardNavy | `#0D2B5E` | SurfaceVariant en dark (cards) |

**`dynamicColor = false`** es obligatorio: sin esto, Android 12+ ignora la paleta personalizada y usa los colores del wallpaper del sistema.

---

## Navegación

```
Splash ──→ Home ──→ Detail ──→ Community
              │         │
              │         └──→ (volver)
              │
              └──→ Profile ──→ (logout → Login)
```

Rutas:
- `splash`, `login`, `home`, `profile`
- `detail/{beachId}` — el beachId viaja como argumento de navegación
- `community/{beachId}` — idem

`navigateUp()` en lugar de `popBackStack()`: evita que la pantalla quede en blanco cuando el usuario toca atrás muy rápido.

---

## Seguridad

| Secreto | Dónde vive | Dónde NO va |
|---------|-----------|-------------|
| `google-services.json` | Solo local, en `.gitignore` | Nunca en el repo |
| `GEMINI_API_KEY` | `local.properties` → `BuildConfig` | Nunca en el código ni en el repo |
| `CLOUDINARY_CLOUD_NAME` | `local.properties` → `BuildConfig` | No es secreto pero se mantiene fuera del repo |
| `CLOUDINARY_UPLOAD_PRESET` | `local.properties` → `BuildConfig` | Idem |
| Cloudinary API secret | No se usa en la app | Nunca en la app (usar preset unsigned) |

### Reglas de Firestore — Por qué son críticas
Las reglas en modo test tienen una **fecha de expiración de 30 días**. Cuando vencen, Firestore devuelve `PERMISSION_DENIED` para todas las operaciones. La app siguió funcionando con los datos cacheados en Room (offline-first), pero no pudo actualizar. La solución: configurar reglas de producción que requieren `request.auth != null`.

---

## UiState — Manejo de estados

```kotlin
sealed class UiState<out T> {
    class Loading<T> : UiState<T>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error<T>(val message: String) : UiState<T>()
    class Offline<T> : UiState<T>()
}
```

- `Loading`: mientras se cargan los datos (muestra un spinner)
- `Success`: datos listos para mostrar
- `Error`: falló algo y no hay datos cacheados
- `Offline`: sin internet y sin datos cacheados

**¿Por qué `class` y no `object` para `Loading` y `Offline`?** En Kotlin 2.x con `MutableStateFlow`, usar `object` para sealed classes sin datos genera errores de varianza en tiempo de compilación. Usar `class` lo resuelve.

---

## Inyección de Dependencias (Hilt)

Hilt provee todas las dependencias automáticamente. Las más importantes:

```kotlin
// AppModule provee:
AppDatabase      → Room database
BeachDao         → DAO de playas
BeachConditionsDao → DAO de condiciones
FirebaseFirestore → instancia singleton de Firestore
WeatherService   → Retrofit para Open-Meteo Weather
MarineService    → Retrofit para Open-Meteo Marine
CloudinaryService → Retrofit para Cloudinary

// Repositorios bindeados:
BeachListRepository    ← BeachListRepositoryImpl
BeachDetailRepository  ← BeachDetailRepositoryImpl
CommunityRepository    ← CommunityRepositoryImpl
AuthRepository         ← AuthRepositoryImpl
ProfileRepository      ← ProfileRepositoryImpl
SplashRepository       ← SplashRepositoryImpl
```

`@Singleton` garantiza que haya una sola instancia de cada dependencia en toda la app. Room y Firestore son particularmente importantes que sean singleton.

---

## Testing

Tests unitarios con **MockK** (mocks en Kotlin) + **Turbine** (testing de Flows) + **coroutines-test** (control del tiempo en coroutinas):

- `HomeViewModelTest`: carga de playas, estado offline, reintento
- `BeachDetailViewModelTest`: carga de detalle, offline-first, error
- `CommunityViewModelTest`: carga de posts, envío de mensaje, validación de texto vacío

Los tests mockean el repositorio para no depender de red ni base de datos real.

---

## Puntos Clave para la Defensa

1. **¿Por qué MVVM?** Separación de responsabilidades: la UI no conoce la lógica de negocio, el ViewModel no conoce de dónde vienen los datos. Facilita el testing (podés mockear el repositorio).

2. **¿Por qué offline-first?** Las condiciones de playa son datos que no cambian cada segundo. Tiene sentido cachearlos en Room y mostrarlos aunque no haya conexión. El usuario ve datos un poco viejos en lugar de una pantalla de error.

3. **¿Por qué Cloudinary?** Firebase Storage requiere plan Blaze (pago). Cloudinary tiene plan gratuito suficiente para desarrollo y un TPO. El preset unsigned permite subir desde la app sin exponer el API secret.

4. **¿Por qué Hilt?** Inyección de dependencias declarativa. Sin Hilt, habría que instanciar manualmente Room, Retrofit, Firestore y pasarlos a cada repositorio y ViewModel. Con Hilt, se declara una vez y Hilt lo provee en todos lados.

5. **¿Por qué DataStore y no SharedPreferences?** DataStore es la alternativa moderna y recomendada por Google. Es asíncrono (no bloquea el hilo principal) y usa Flows para observar cambios reactivamente.

6. **¿Cómo se protegen las API Keys?** Viven en `local.properties` (excluido del repo con `.gitignore`). En tiempo de compilación, Gradle las lee e inyecta en la clase `BuildConfig`. El código accede a `BuildConfig.GEMINI_API_KEY` en lugar de tener el string hardcodeado.

7. **¿Qué es `callbackFlow`?** Un builder de Flow que permite integrar APIs basadas en callbacks (como los listeners de Firestore y el NetworkCallback de Android) en el ecosistema reactivo de Kotlin Coroutines.

8. **¿Qué problema resuelve `awaitClose`?** Cuando un Flow con `callbackFlow` se cancela (por ejemplo, el usuario sale de la pantalla y el ViewModel se destruye), `awaitClose` ejecuta el código de limpieza: des-registra el listener. Sin esto habría memory leaks.
