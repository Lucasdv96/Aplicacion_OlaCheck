# OlaCheck — Plan de Desarrollo TPO 2026

## Stack Técnico
- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose
- **Arquitectura**: MVVM + Clean Architecture + Repository Pattern
- **DI**: Hilt 2.59
- **Base de datos local**: Room
- **Backend**: Firebase Auth + Firestore
- **APIs externas**: Open-Meteo (Weather + Marine), Cloudinary (media)
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
│   └── navigation/     → NavGraph y Routes
└── di/                 → AppModule (Hilt)
```

---

## Features Completados ✅

### Feature 1 — Splash Screen
- Verifica sesión activa en DataStore al abrir la app
- Redirige a Home si hay sesión, a Login si no hay
- `SplashRepository` → `SplashRepositoryImpl` → `SplashViewModel` → `SplashScreen`
- Usa `popUpTo(inclusive = true)` para no volver al Splash con el botón atrás

### Feature 2 — Firebase Auth (Login con Google)
- Login con Google via Firebase Authentication
- Persiste uid, nombre, email, foto en DataStore
- `AuthRepository` → `AuthRepositoryImpl` → `LoginViewModel` → `LoginScreen`
- Fix aplicado: `googleSignInClient.signOut()` al hacer logout para limpiar cuenta cacheada

### Feature 3 — Beach List (Home)
- Descarga playas desde Firestore y condiciones desde Open-Meteo
- Estrategia offline-first: Room como Single Source of Truth
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
- `BeachDetailRepository` → `BeachDetailRepositoryImpl` → `BeachDetailViewModel` → `BeachDetailScreen`

### Feature 10 — Perfil de Usuario
- Muestra avatar (Glide), nombre y email del usuario logueado
- Lee datos desde DataStore via `ProfileRepository`
- `ProfileRepository` → `ProfileRepositoryImpl` → `ProfileViewModel` → `ProfileScreen`
- Accesible desde el ícono de persona en la TopAppBar del Home

### Feature 11 — Logout
- Cierra sesión en Firebase Auth + limpia DataStore + limpia cuenta cacheada de Google
- Navega a Login con `popUpTo(0) { inclusive = true }` para limpiar todo el back stack

### Imágenes de Playas
- URLs almacenadas en Firestore campo `imageUrl` de cada documento
- Cargadas con Glide Compose + `ContentScale.Crop`
- Thumbnail 80dp en cards del Home, imagen 200dp de alto en el Detalle

---

## Features Pendientes 🔲

### CU-10 — Comunidad: Comentarios en Tiempo Real
- Lista de posts por playa actualizada automáticamente via SnapshotListener de Firestore
- Colección `community_posts` filtrada por `beachId`
- Cada post: avatar, nombre, timestamp relativo, texto, miniatura (si tiene)
- Offline: caché automático del SDK de Firestore
- SnapshotListener gestionado con ciclo de vida para evitar memory leaks

### Feature 12 — Room Offline-First Completo
- Verificar y completar la estrategia offline en todos los repositorios
- Banner de modo offline ya implementado en HomeScreen

### Feature 6 — AI Agent
- A definir con el profesor
- Se implementa después del Feature 12

### CU-09 — Subir Foto/Video a la Comunidad
- Permisos: READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, CAMERA
- Selección via ActivityResultContracts (PickVisualMedia / TakePicture / TakeVideo)
- Subida a **Cloudinary** (gratuito — reemplaza Firebase Storage que requiere plan Blaze)
- Ruta en Storage: `media/{beachId}/{userId}/{timestamp}.{ext}`
- Barra de progreso durante la subida
- URL resultante se guarda en el documento Firestore del post
- **Se implementa al final** (requiere configurar cuenta Cloudinary)

### Feature 13 — Unit Testing
- Tests sobre ViewModels y Repositories

### Feature 14 — Profiler Report
- Análisis de performance con Android Studio Profiler

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
| `navigateUp()` en lugar de `popBackStack()` | Evita pantalla en blanco al presionar atrás rápidamente |
| `SavedStateHandle` para beachId en ViewModel | Compatible con Hilt, sobrevive rotación de pantalla |

---

## Firestore — Estructura
```
beaches (colección)
├── pinamar       → name, latitude, longitude, imageUrl
├── mardelplata   → name, latitude, longitude, imageUrl
├── villagesell   → name, latitude, longitude, imageUrl
└── mardeajo      → name, latitude, longitude, imageUrl

community_posts (colección) — pendiente
└── {postId}      → beachId, userId, userName, userPhoto, text, mediaUrl, timestamp
```

---

## Notas de Seguridad
- `google-services.json` está en `.gitignore` — contiene API key de Firebase, NO commitear
- Debe copiarse manualmente entre computadoras
- Restringir la API key en Google Cloud Console al package name de la app
