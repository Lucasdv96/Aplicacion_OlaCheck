package com.tpoAppInteractivas.olacheck.viewmodel

import androidx.compose.foundation.pager.PagerState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpoAppInteractivas.olacheck.repository.AiRepository
import com.tpoAppInteractivas.olacheck.repository.BeachDetailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

//Estados posibles de la pantalla del agente IA
sealed class AiUiState{
    object Idle : AiUiState()      //Estado inicial, antes de consultar
    object Loading : AiUiState()   // GEMINI esta procesando el prompt
    data class Success(val recommendation: String) : AiUiState() //respuesta recibida
    data class Error(val message: String) : AiUiState()          //fallo la llamada
}

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val beachDetailRepository: BeachDetailRepository,
    savedStateHandle: SavedStateHandle
): ViewModel(){

    //obtiene el beachID desde la ruta de navegacion via SavedStateHandle
    private val beachId: String = checkNotNull(savedStateHandle["beachId"])

    // estado de la UI observado por la pantalla
    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)

    // consulta las condiciones actuales de la playa y las envia a Gemini
    fun getRecommendation(){
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading

            // Lee las condiciones acutales de la playa desde Room de forma puntual
            val conditions = beachDetailRepository.getConditionsForBeach(beachId)
            if(conditions == null){
                _uiState.value = AiUiState.Error("No hay condiciones disponibles para esta playa")
                return@launch
            }

            // envia las condiciones a Gemini y actualiza el esado con la respuesta
            val result = aiRepository.getBeachRecommendation(
                waterTemp = conditions.waterTemp,
                airTemp = conditions.airTemp,
                windSpeed = conditions.windSpeed,
                waveHeight = conditions.waveHeight,
                wavePeriod =  conditions.wavePeriod,
                humidity = conditions.humidity
            )

            _uiState.value = result.fold(
                onSuccess = { AiUiState.Success(it) },
                onFailure = { AiUiState.Error(it.message ?: "Error desconocido")}
            )
        }
    }
}