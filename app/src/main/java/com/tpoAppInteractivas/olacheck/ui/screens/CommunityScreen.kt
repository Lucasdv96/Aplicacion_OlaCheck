package com.tpoAppInteractivas.olacheck.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.tpoAppInteractivas.olacheck.data.local.CommunityPost
import com.tpoAppInteractivas.olacheck.viewmodel.CommunityViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun CommunityScreen(
    onNavigateBack: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Obtiene el uid del usuario logueado para saber qué mensajes son propios
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

// Launcher del selector de fotos nativo de Android (no requiere permisos)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImageSelected(uri)
    }

    // Muestra el Snackbar cuando hay un error de subida
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }


    // Hace scroll automático al último mensaje cuando llega uno nuevo
    LaunchedEffect(posts.size) {
        if (posts.isNotEmpty()) listState.animateScrollToItem(0)
    }

    // URI donde se guarda la foto que saca la cámara (archivo temporal en caché)
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

// Launcher de la cámara: recibe true si el usuario sacó la foto, false si canceló
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onImageSelected(cameraImageUri)
        }
    }

// Launcher del permiso de cámara: si lo concede, abre la cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraImageUri?.let { cameraLauncher.launch(it) }
        }
    }


    val context = LocalContext.current

    // Crea un archivo temporal en caché y devuelve su URI via FileProvider
    fun createCameraUri(): Uri {
        val file = java.io.File(context.cacheDir, "images/camera_${System.currentTimeMillis()}.jpg")
            .also { it.parentFile?.mkdirs() }
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Comunidad") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Preview de la imagen seleccionada (si hay una)
                selectedImageUri?.let { uri ->
                    Box(modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                        GlideImage(
                            model = uri,
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        // Botón para quitar la imagen elegida
                        IconButton(
                            onClick = { viewModel.onImageSelected(null) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar imagen")
                        }
                        // Botón cámara
                        IconButton(onClick = {
                            // Creamos el archivo temporal y pedimos permiso (o abrimos directo si ya lo tiene)
                            cameraImageUri = createCameraUri()
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Cámara")
                        }
                    }
                }

                // Barra de entrada: galería + texto + enviar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón galería
                    IconButton(onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(Icons.Default.Image, contentDescription = "Galería")
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.onInputChange(it) },
                        placeholder = { Text("Enviar mensaje...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Mientras sube, mostramos un spinner; si no, el botón de enviar
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = { viewModel.sendPost() }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                        }
                    }
                }
            }
        }
    ) { padding ->
        // Lista de mensajes en orden inverso (el más nuevo abajo)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            reverseLayout = true, // muestra los mensajes de abajo hacia arriba como un chat
            contentPadding = PaddingValues(8.dp)
        ) {
            items(posts, key = { it.id }) { post ->
                ChatBubble(
                    post = post,
                    isOwn = post.userId == currentUserId
                )
            }
        }
    }
}

// Burbuja de mensaje individual
// isOwn determina si el mensaje va a la derecha (propio) o izquierda (otros)
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ChatBubble(post: CommunityPost, isOwn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar del autor — solo se muestra en mensajes de otros usuarios
        if (!isOwn) {
            GlideImage(
                model = post.userPhoto,
                contentDescription = post.userName,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
        ) {
            // Nombre del autor y timestamp relativo
            if (!isOwn) {
                Text(
                    text = "${post.userName} · ${formatTimestamp(post.timestamp)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                Text(
                    text = formatTimestamp(post.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            if(post.text.isNotEmpty()) {
                // Burbuja de texto con color diferente según si es propio o ajeno
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwn) 16.dp else 4.dp,
                        bottomEnd = if (isOwn) 4.dp else 16.dp
                    ),
                    color = if (isOwn)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = post.text,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (isOwn)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Foto del post (si tiene)
            if (post.mediaUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                GlideImage(
                    model = post.mediaUrl,
                    contentDescription = "Foto del post",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

// Convierte un timestamp en milisegundos a formato legible
// Muestra hora si es del día de hoy, fecha si es anterior
fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "ahora"
        diff < 3_600_000 -> "hace ${diff / 60_000} min"
        diff < 86_400_000 -> "hace ${diff / 3_600_000} h"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}