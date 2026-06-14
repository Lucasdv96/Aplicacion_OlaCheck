package com.tpoAppInteractivas.olacheck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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

@OptIn(ExperimentalMaterial3Api::class)
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

    // Hace scroll automático al último mensaje cuando llega uno nuevo
    LaunchedEffect(posts.size) {
        if (posts.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
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
            // Barra de entrada de texto fija en la parte inferior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.onInputChange(it) },
                    placeholder = { Text("Enviar mensaje...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.sendPost() }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
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