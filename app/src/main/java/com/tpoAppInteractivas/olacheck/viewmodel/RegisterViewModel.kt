package com.tpoAppInteractivas.olacheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpoAppInteractivas.olacheck.repository.AuthRepository
import com.tpoAppInteractivas.olacheck.ui.screens.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    // Registra un nuevo usuario con email y contraseña
    // Valida los campos antes de llamar al repositorio
    fun register(email: String, password: String, confirmPassword: String) {
        // Validaciones locales antes de ir a Firebase
        val validationError = validate(email, password, confirmPassword)
        if (validationError != null) {
            _uiState.value = AuthUiState.Error(validationError)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.registerWithEmail(email, password)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    // Resetea el estado para que la UI no quede colgada en Error
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    // Validaciones locales: evitan un round-trip a Firebase para errores obvios
    private fun validate(email: String, password: String, confirmPassword: String): String? {
        if (email.isBlank()) return "El email no puede estar vacío."
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return "El email no tiene un formato válido."
        if (password.length < 6) return "La contraseña debe tener al menos 6 caracteres."
        if (password != confirmPassword) return "Las contraseñas no coinciden."
        return null
    }
}
