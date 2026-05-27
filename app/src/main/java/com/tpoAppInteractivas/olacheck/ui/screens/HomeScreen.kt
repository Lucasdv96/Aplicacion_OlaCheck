package com.tpoAppInteractivas.olacheck.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tpoAppInteractivas.olacheck.viewmodel.BeachWithConditions
import com.tpoAppInteractivas.olacheck.viewmodel.HomeViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OlaCheck") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isOnline) {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = "Modo Offline — mostrando datos guardados",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar playa...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        TextButton(onClick = { searchQuery = "" }) {
                            Text("X")
                        }
                    }
                }
            )
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.retry() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                is UiState.Offline -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sin conexión y sin datos guardados")
                    }
                }

                is UiState.Success -> {
                    val filtered = state.data.filter {
                        it.beach.name.contains(searchQuery, ignoreCase = true)
                    }
                    LazyColumn {
                        items(filtered) { item ->
                            BeachCard(
                                item = item,
                                onClick = { onNavigateToDetail(item.beach.id) })
                        }
                    }
                }
            }
        }
    }
}




@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BeachCard(item: BeachWithConditions, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            item.beach.imageUrl?.let { url ->
                GlideImage(
                    model = url,
                    contentDescription = item.beach.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.beach.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                item.conditions?.let { conditions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "💧 ${conditions.waterTemp}°C")
                        Text(text = "🌡 ${conditions.airTemp}°C")
                        Text(text = "💨 ${conditions.windSpeed} km/h")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "🌊 ${conditions.waveHeight}m")
                        Text(text = "💦 ${conditions.humidity}%")
                    }
                } ?: Text(text = "Cargando condiciones...", color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
