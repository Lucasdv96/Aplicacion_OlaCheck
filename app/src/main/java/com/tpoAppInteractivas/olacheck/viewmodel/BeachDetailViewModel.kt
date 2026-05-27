package com.tpoAppInteractivas.olacheck.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpoAppInteractivas.olacheck.data.local.Beach
import com.tpoAppInteractivas.olacheck.data.local.BeachConditions
import com.tpoAppInteractivas.olacheck.repository.BeachDetailRepository
import com.tpoAppInteractivas.olacheck.ui.screens.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BeachDetailUiState(
    val beach: Beach? = null,
    val conditions: BeachConditions? = null
)

@HiltViewModel
class BeachDetailViewModel @Inject constructor(
    private val repository: BeachDetailRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val beachId: String = checkNotNull(savedStateHandle["beachId"])

    private val _uiState = MutableStateFlow<UiState<BeachDetailUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<BeachDetailUiState>> = _uiState

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            try {
                repository.refreshConditions(beachId)
                val beach = repository.getBeachById(beachId)
                val conditions = repository.getConditionsForBeach(beachId)
                _uiState.value = UiState.Success(
                    BeachDetailUiState(beach = beach, conditions = conditions)
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error al cargar detalle")
            }
        }
    }
    fun retry() {
        _uiState.value = UiState.Loading
        loadDetail()
    }
}
