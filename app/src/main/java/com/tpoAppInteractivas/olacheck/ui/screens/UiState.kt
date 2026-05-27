package com.tpoAppInteractivas.olacheck.ui.screens


sealed class UiState<out T>{
    class Loading<T> : UiState<T>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error<T>(val message: String) : UiState<T>()
    class Offline<T> : UiState<T>()
}