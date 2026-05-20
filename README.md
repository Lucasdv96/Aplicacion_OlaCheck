# OlaCheck 🌊

Aplicación móvil nativa para Android que centraliza información de condiciones de playas en tiempo real, con inteligencia artificial y comunidad activa.

## 📱 Sobre la Aplicación

OlaCheck es una app diseñada para bañistas, surfistas y turistas que visitan las playas del partido de Pinamar. Combina datos meteorológicos y marinos en tiempo real, recomendaciones personalizadas mediante IA y una comunidad donde los usuarios comparten experiencias del estado actual de cada playa.

## 🎯 Características Principales

- **Autenticación con Google**: Login seguro mediante Firebase Authentication
- **Condiciones en Tiempo Real**: Temperatura del agua y aire, velocidad del viento, estado de mareas y altura de olas
- **Funcionamiento Offline**: Últimos datos cacheados en Room (SQLite local)
- **Agente IA**: Recomendaciones personalizadas de neoprene según condiciones actuales (Google Gemini)
- **Comunidad**: Comentarios, fotos y videos sincronizados en tiempo real con Firebase Firestore
- **Búsqueda Reactiva**: Filtrado de playas por nombre en tiempo real
- **Perfil de Usuario**: Visualización de datos personales e historial de publicaciones

## 🏖️ Playas Cubiertas

- Pinamar
- Ostende
- Valeria del Mar
- Cariló

## 🛠️ Stack Tecnológico

### Frontend
- **Lenguaje**: Kotlin
- **Arquitectura**: MVVM + Clean Architecture
- **UI**: Jetpack Compose
- **Navegación**: Jetpack Navigation

### Backend & Servicios
- **Autenticación**: Firebase Authentication (Google Sign-In)
- **Base de Datos Local**: Room (SQLite)
- **Base de Datos Remota**: Firebase Firestore
- **Almacenamiento**: Firebase Storage
- **APIs Externas**:
  - Open-Meteo Marine API (condiciones marinas)
  - Open-Meteo Weather API (meteorología)
  - Google Gemini API (recomendaciones IA)

### Patrones & Librerías
- **State Management**: StateFlow + Flow
- **Inyección de Dependencias**: Hilt
- **Carga de Imágenes**: Glide
- **Persistencia**: DataStore Preferences
- **Conectividad**: ConnectivityManager

## 📋 Estructura de Datos

### Room (Local)
- `beach`: Información de playas
- `beach_conditions`: Condiciones climáticas y marinas cacheadas

### Firestore (Remoto)
- `community_posts`: Comentarios, fotos y videos de usuarios

## 🔄 Flujos Principales

1. **Splash Screen**: Verifica sesión activa y redirige a Home o Login
2. **Login**: Autenticación con Google, persistencia de sesión en DataStore
3. **Home**: Listado de playas con datos cacheados (offline-first)
4. **Detalle**: Condiciones extendidas, recomendación IA y comunidad
5. **Comunidad**: Publicación y visualización en tiempo real de posts
6. **Perfil**: Datos del usuario e historial de publicaciones

## 📱 Requisitos Minimos

- Android 8.0+ (API 26)
- Conexión a internet para sincronización de datos

## 📚 Documentación

- **Prototipo Figma**: [Ver Diseño](https://www.figma.com/design/yKNWx8GE3nTwWOUFjmv5IG/OlaCheck---TPO?node-id=1-2)
- **Especificación Técnica**: Consultar documentación del TPO para detalles completos

## 👨‍💻 Autor

Desarrollado como Trabajo Práctico Obligatorio para la materia **Desarrollo de Aplicaciones I** - UADE

## 📄 Licencia

[Especificar licencia según corresponda]

---

**Nota**: Aplicación en desarrollo. Consultar issues y pull requests para estado actual.
