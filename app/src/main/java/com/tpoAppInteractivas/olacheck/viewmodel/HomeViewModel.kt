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
import kotlinx.coroutines.flow.onErrorResume
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

    private val _uiState = MutableStateFlow<UiState<List<BeachWithConditions>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<BeachWithConditions>>> = _uiState

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline

    init {
        loadBeaches()
    }
    private fun loadBeaches() {
        viewModelScope.launch {
            _isOnline.value = repository.isOnline()
            if(repository.isOnline()){
                try {
                    repository.refreshBeachData()
                } catch (e: Exception) {
                    _uiState.value = UiState.Error(e.message ?: "Error al cargar datos")
                }
            }
        }
        repository.getBeaches().collect { beaches ->
                if (beaches.isEmpty() && !repository.isOnline()) {
                    _uiState.value = UiState.Offline
                } else {
                    _uiState.value = UiState.Success(
                        beaches.map { beach ->
                            BeachWithConditions(beach = beach, conditions =  null)
                        }
                    )

                }
            }
    }
    fun retry(){
        _uiState.value = UiState.Loading
        loadBeaches()
    }
}
