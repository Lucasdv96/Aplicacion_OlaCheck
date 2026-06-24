package com.tpoAppInteractivas.olacheck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tpoAppInteractivas.olacheck.data.local.BeachConditions
import com.tpoAppInteractivas.olacheck.viewmodel.BeachDetailViewModel
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Color
import com.tpoAppInteractivas.olacheck.viewmodel.AiViewModel
import com.tpoAppInteractivas.olacheck.viewmodel.ChatMessage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun BeachDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    viewModel: BeachDetailViewModel = hiltViewModel()
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // controla si el BottomSheet del chat está visible
    var showAiChat by remember { mutableStateOf(false) }
    val aiViewModel: AiViewModel = hiltViewModel()
    val aiMessages by aiViewModel.messages.collectAsStateWithLifecycle()
    val aiIsLoading by aiViewModel.isLoading.collectAsStateWithLifecycle()
    val aiInputText by aiViewModel.inputText.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

// hace scroll al último mensaje cuando llega uno nuevo
    LaunchedEffect(aiMessages.size) {
        if (aiMessages.isNotEmpty()) listState.animateScrollToItem(aiMessages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Playa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                // muestra el botón de IA en la barra superior solo cuando los datos están disponibles
                actions = {
                    if (uiState is UiState.Success) {
                        IconButton(onClick = {
                            showAiChat = true
                            aiViewModel.startChat()
                        }) {
                            Icon(Icons.Default.Psychology, contentDescription = "Consultar IA")
                        }
                    }
                }

            )
        },


    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text("Reintentar")
                        }
                    }
                }
                is UiState.Offline -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No hay conexión a internet", color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text("Reintentar")
                        }
                    }
                }
                is UiState.Success -> {
                    val beach = state.data.beach
                    val conditions = state.data.conditions
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        beach?.imageUrl?.let { url ->
                            GlideImage(
                                model = url,
                                contentDescription = beach.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            text = beach?.name ?: "",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        conditions?.let {
                            ConditionItem(label = "Temperatura del agua", value = "${it.waterTemp}°C")
                            ConditionItem(label = "Temperatura del aire", value = "${it.airTemp}°C")
                            ConditionItem(label = "Velocidad del viento", value = "${it.windSpeed} km/h")
                            ConditionItem(label = "Dirección del viento", value = "${it.windDirection}°")
                            ConditionItem(label = "Altura de olas", value = "${it.waveHeight} m")
                            ConditionItem(label = "Período de olas", value = "${it.wavePeriod} s")
                            ConditionItem(label = "Humedad", value = "${it.humidity}%")
                        } ?: CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateToCommunity,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Comunidad")
                        }
                    }
                }
                else -> Unit
            }
        }
    }
    if (showAiChat) {
        ModalBottomSheet(
            onDismissRequest = { showAiChat = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .imePadding() //Sube el contenido cuando aparece el telcado
            ) {
                Text(
                    text = "Agente de Neoprene",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider()

                // lista de mensajes del chat
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(aiMessages) { message ->
                        AiChatBubble(message = message)
                    }
                    if (aiIsLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                HorizontalDivider()
                // campo de entrada para preguntas de seguimiento
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = aiInputText,
                        onValueChange = { aiViewModel.onInputChange(it) },
                        placeholder = { Text("Preguntá algo...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { aiViewModel.sendMessage() },
                        enabled = !aiIsLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    }
}
// Burbuja de mensaje del chat de IA
// Los mensajes del usuario van a la derecha, las respuestas de Gemini a la izquierda
@Composable
fun AiChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (message.isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
@Composable
fun ConditionItem(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontWeight = FontWeight.Medium)
            Text(text = value, fontWeight = FontWeight.Bold)
        }
    }
}