# OlaCheck — Plan de Desarrollo TPO 2026

## Stack Técnico
- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose
- **Arquitectura**: MVVM + Clean Architecture + Repository Pattern
- **DI**: Hilt 2.59
- **Base de datos local**: Room
- **Backend**: Firebase Auth + Firestore
- **APIs externas**: Open-Meteo (Weather + Marine), Cloudinary (media), Google Gemini AI
- **Navegación**: Jetpack Navigation con SavedStateHandle
- **Persistencia de sesión**: DataStore Preferences
- **Imágenes**: Glide Compose
- **Versiones clave**: AGP 9.1.1 · Kotlin 2.2.10 · KSP 2.2.10-2.0.2

---

## Estructura de Carpetas
```
com.tpoAppInteractivas.olacheck
├── data/
│   ├── local/          → Room entities, DAOs, AppDatabase, DataStore
│   └── remote/         → Retrofit services, Repository implementations
├── repository/         → Interfaces de repositorio (contratos)
├── viewmodel/          → ViewModels por pantalla
├── ui/
│   ├── screens/        → Composables de cada pantalla
│   ├── theme/          → Color, Typography, Theme (identidad visual)
│   └── navigation/     → NavGraph y Routes
└── di/                 → AppModule (Hilt)
```

---

## Features Completados ✅

### Feature 1 — Splash Screen
- Verifica sesión activa en DataStore al abrir la app
- Redirige a Home si hay sesión, a Login si no hay
- Logo de la app centrado + logo del desarrollador abajo
- `SplashRepository` → `SplashRepositoryImpl` → `SplashViewModel` → `SplashScreen`
- Usa `popUpTo(inclusive = true)` para no volver al Splash con el botón atrás
- Android 12+ usa el launcher icon como splash nativo automáticamente

### Feature 2 — Firebase Auth (Login con Google)
- Login con Google via Firebase Authentication
- Persiste uid, nombre, email, foto en DataStore Preferences
- `AuthRepository` → `AuthRepositoryImpl` → `LoginViewModel` → `LoginScreen`
- Fix: `googleSignInClient.signOut()` al hacer logout para limpiar cuenta cacheada

### Feature 3 — Beach List (Home) con Offline-First
- Descarga playas desde Firestore y condiciones desde Open-Meteo
- Estrategia offline-first: Room como Single Source of Truth
- Snackbar de alerta cuando el refresh falla pero hay datos cacheados
- Detección automática de reconexión con `NetworkCallback` + `callbackFlow`
- Refresh automático de datos al recuperar internet (sin intervención del usuario)
- Banner de modo offline reactivo en tiempo real
- Botón "Reintentar" cuando no hay datos ni conexión
- Dos instancias de Retrofit separadas (Weather API + Marine API tienen URLs base distintas)
- `BeachListRepository` → `BeachListRepositoryImpl` → `HomeViewModel` → `HomeScreen`
- `combine()` en ViewModel para fusionar Flow de playas con Flow de condiciones

### Feature 4 — Búsqueda Reactiva
- Filtrado en tiempo real por nombre de playa dentro de HomeScreen
- Estado local con `remember { mutableStateOf("") }` — no necesita ViewModel
- Botón "X" para limpiar la búsqueda

### Feature 5 — Detalle de Playa
- Muestra condiciones extendidas de una playa específica
- Recibe `beachId` via `SavedStateHandle` (compatible con Hilt)
- Refresca datos de Open-Meteo siempre al entrar para garantizar datos frescos
- Offline-first: muestra datos cacheados si falla el refresh
- `BeachDetailRepository` → `BeachDetailRepositoryImpl` → `BeachDetailViewModel` → `BeachDetailScreen`

### Feature 6 — Agente AI (Gemini)
- Botón en la TopAppBar del detalle de playa
- Abre un ModalBottomSheet con chat multi-turno
- Lee las condiciones actuales de la playa y genera una recomendación de surf/neoprene
- Usa `gemini-1.5-flash` via Google Generative AI SDK
- API Key inyectada con BuildConfig desde `local.properties` (nunca en el repo)
- `AiRepository` → `AiRepositoryImpl` → `AiViewModel` (en `BeachDetailViewModel`)

### Feature 10 — Perfil de Usuario
- Muestra avatar (Glide), nombre y email del usuario logueado
- Header con fondo `primaryContainer`, foto circular con borde
- Cards con íconos para nombre y email
- Lee datos desde DataStore via `ProfileRepository`
- Accesible desde el ícono de persona en la TopAppBar del Home

### Feature 11 — Logout
- Cierra sesión en Firebase Auth + limpia DataStore + limpia cuenta cacheada de Google
- Navega a Login con `popUpTo(0) { inclusive = true }` para limpiar todo el back stack

### Feature 12 — Identidad Visual (Paleta de Colores)
- Paleta personalizada azul oceánica: NavyBlue, BrightBlue, SkyBlue, White, LightGray
- `dynamicColor = false` en el Theme para que se aplique en Android 12+ también
- ColorScheme completo para light y dark mode

### CU-10 — Comunidad: Posts en Tiempo Real
- Lista de posts por playa actualizada automáticamente via SnapshotListener de Firestore
- Colección `community_posts` filtrada por `beachId`
- Cada post: avatar, nombre, timestamp relativo, texto y foto (si tiene)
- Burbujas de chat diferenciadas: mensajes propios a la derecha, ajenos a la izquierda
- SnapshotListener gestionado con `callbackFlow` + `awaitClose` para evitar memory leaks
- `CommunityRepository` → `CommunityRepositoryImpl` → `CommunityViewModel` → `CommunityScreen`

### CU-09 — Subida de Fotos en la Comunidad (Cloudinary)
- Adjuntar foto a los posts desde galería (PhotoPicker, sin permisos) o cámara
- Permiso `CAMERA` + `FileProvider` para captura desde cámara
- Subida a Cloudinary vía Retrofit multipart (endpoint unsigned, sin API secret en la app)
- Compresión automática: redimensionado a máx 1080px + JPEG 80% antes de subir
- Corrección de rotación EXIF para fotos tomadas con la cámara
- Preview de la imagen antes de enviar + spinner durante la subida
- Snackbar de error si falla la subida
- Posts admiten: solo texto, solo foto, o ambos
- URL resultante guardada en campo `mediaUrl` del documento Firestore

### Feature 13 — Unit Testing
- Tests sobre ViewModels con MockK + Turbine + coroutines-test
- `HomeViewModelTest`, `BeachDetailViewModelTest`, `CommunityViewModelTest`

### Feature 14 — Profiler Report
- Análisis de performance con Android Studio Profiler
- Memory, CPU Callstack Sample y Live Telemetry documentados

---

## Firestore — Estructura
```
beaches (colección)
├── {beachId}     → name, latitude, longitude, imageUrl

community_posts (colección)
└── {postId}      → beachId, userId, userName, userPhoto, text, mediaUrl, timestamp
```

### Reglas de seguridad Firestore
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /beaches/{beachId} {
      allow read: if request.auth != null;
    }
    match /community_posts/{postId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## Decisiones Técnicas Clave

| Decisión | Razón |
|----------|-------|
| Hilt 2.59 (no 2.56) | Compatibilidad con AGP 9.x |
| KSP 2.2.10-2.0.2 | El prefijo debe coincidir exactamente con la versión de Kotlin |
| Firebase por string (no catalog) | El BOM de Firebase no resuelve versiones en el catálogo de versiones |
| Dos instancias de Retrofit | Weather y Marine tienen URLs base distintas |
| `class Loading<T>` en lugar de `object` | Evita errores de varianza en Kotlin 2.x con MutableStateFlow |
| DataStore en lugar de Room para sesión | Datos simples clave-valor, no estructuras relacionales |
| Firestore para lista de playas | Escalable: se agregan playas sin tocar el código |
| Cloudinary en lugar de Firebase Storage | Firebase Storage requiere plan Blaze (pago) |
| Cloudinary preset unsigned | Permite subir desde la app sin exponer el API secret |
| `callbackFlow` para NetworkCallback y Firestore | Convierte APIs de callback en Flows reactivos de Kotlin |
| `distinctUntilChanged` + `drop(1)` en conectividad | Evita doble fetch inicial y re-ejecuciones por emisiones repetidas |
| `navigateUp()` en lugar de `popBackStack()` | Evita pantalla en blanco al presionar atrás rápidamente |
| `SavedStateHandle` para beachId en ViewModel | Compatible con Hilt, sobrevive rotación de pantalla |
| `dynamicColor = false` en el Theme | Necesario para que la paleta personalizada se aplique en Android 12+ |
| `inSampleSize` al decodificar imágenes | Evita OOM (Out of Memory) al procesar fotos de cámara de alta resolución |
| ExifInterface para corrección de rotación | La cámara guarda la orientación en metadatos, no rota los píxeles |
| `FileProvider` para URI de cámara | Requerido por Android para compartir archivos entre la app y la cámara |
| Reglas Firestore: `request.auth != null` | Solo usuarios autenticados acceden — más seguro que el modo test (que expira) |

---

## Notas de Seguridad
- `google-services.json` está en `.gitignore` — contiene API key de Firebase, NUNCA commitear
- `GEMINI_API_KEY` solo en `local.properties` → inyectada via BuildConfig
- `CLOUDINARY_CLOUD_NAME` y `CLOUDINARY_UPLOAD_PRESET` en `local.properties` → BuildConfig
- Reglas de Firestore en modo test vencen a los 30 días — configurar reglas de producción
- Restricción de API key de Firebase en Google Cloud Console al package name de la app
