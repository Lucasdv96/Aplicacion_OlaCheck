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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeachDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: BeachDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Playa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
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
                is UiState.Success -> {
                    val beach = state.data.beach
                    val conditions = state.data.conditions
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun ConditionItem(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
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