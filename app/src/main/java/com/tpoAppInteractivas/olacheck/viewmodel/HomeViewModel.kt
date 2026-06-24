package com.tpoAppInteractivas.olacheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpoAppInteractivas.olacheck.data.local.Beach
import com.tpoAppInteractivas.olacheck.data.local.BeachConditions
import com.tpoAppInteractivas.olacheck.repository.BeachListRepository
import com.tpoAppInteractivas.olacheck.ui.screens.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BeachWithConditions(
    val beach: Beach,
    val conditions: BeachConditions?
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BeachListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<BeachWithConditions>>>(UiState.Loading())
    val uiState: StateFlow<UiState<List<BeachWithConditions>>> = _uiState

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline
    // Mensaje de alerta transitorio (para el Snackbar). Null = sin alerta.
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadBeaches()
        observeConnectivity()
    }
    private fun loadBeaches() {
        viewModelScope.launch {
            _isOnline.value = repository.isOnline()
            if (repository.isOnline()) {
                try {
                    repository.refreshBeachData()
                }catch (e: Exception) {
                    // No pisamos el uiState: si hay datos en Room igual se muestran.
                    // Solo disparamos una alerta para avisar que no se pudo actualizar.
                    _errorMessage.value = "No se pudieron actualizar los datos. Mostrando información guardada."

                }
            }
            repository.getBeaches()
                .combine(repository.getAllConditions()) { beaches, conditions ->
                    beaches.map { beach ->
                        BeachWithConditions(
                            beach = beach,
                            conditions = conditions.find { it.beachId == beach.id }
                        )

                    }
                }
                .collect { beachesWithConditions ->
                    if ( beachesWithConditions.isEmpty() && !repository.isOnline()){
                        _uiState.value = UiState.Offline()
                    }else {
                        _uiState.value = UiState.Success(beachesWithConditions)
                    }
                }
        }
    }
    // Escucha los cambios de conexión. Cuando vuelve internet, refresca los datos solo.
    private fun observeConnectivity() {
        viewModelScope.launch {
            repository.observeConnectivity()
                .distinctUntilChanged()   // ignora emisiones repetidas del mismo estado
                .drop(1)                  // saltea el estado inicial (de eso ya se encarga loadBeaches)
                .collect { online ->
                    _isOnline.value = online
                    if (online) {
                        // Volvió la conexión: actualizamos los datos
                        try {
                            repository.refreshBeachData()
                        } catch (e: Exception) {
                            _errorMessage.value = "No se pudieron actualizar los datos. Mostrando información guardada."
                        }
                    }
                }
        }
    }


    // Limpia la alerta después de mostrarla, para que no vuelva a aparecer.
    fun clearError() {
        _errorMessage.value = null
    }

    fun retry(){
        _uiState.value = UiState.Loading()
        loadBeaches()
    }
}

