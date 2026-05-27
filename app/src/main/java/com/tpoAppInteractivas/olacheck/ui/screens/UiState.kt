package com.tpoAppInteractivas.olacheck.ui.screens

import perfetto.protos.UiState

sealed class UiState<out T>{
    object Loading: UiState<Nothing>()
    data class Success<T> (val data: T) : UiState<T>()
    data class Error (val message: String) : UiState<Nothing>()
    object Offline : UiState<Nothing>()
}