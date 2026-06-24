package com.tpoAppInteractivas.olacheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.tpoAppInteractivas.olacheck.repository.AuthRepository
import com.tpoAppInteractivas.olacheck.ui.screens.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    // Mensaje informativo transitorio (ej: "Te enviamos un email de recuperación")
    // Separado del uiState porque NO debe disparar navegación a Home
    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage

    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.sigInWithGoogle(account)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    // Inicia sesión con email y contraseña
    fun signInWithEmail(email: String, password: String) {
        // Validación local: evita un viaje a Firebase para errores obvios
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Completá email y contraseña.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.signInWithEmail(cleanEmail, password)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    // Reporta un error externo (ej: falla del flujo de Google Sign-In) para mostrarlo en la UI
    fun onExternalError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    // Envía un email para restablecer la contraseña al email ingresado en el campo
    fun sendPasswordReset(email: String) {
        val cleanEmail = email.trim()
        // Validación local antes de llamar a Firebase
        if (cleanEmail.isBlank()) {
            _uiState.value = AuthUiState.Error("Ingresá tu email para recuperar la contraseña.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.value = AuthUiState.Error("El email no tiene un formato válido.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.sendPasswordReset(cleanEmail)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Idle
                _infoMessage.value = "Te enviamos un email para restablecer tu contraseña."
            } else {
                _uiState.value = AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "Error desconocido"
                )
            }
        }
    }

    // Limpia el mensaje informativo una vez mostrado
    fun clearInfoMessage() {
        _infoMessage.value = null
    }

    // Resetea el estado para que la UI no quede "colgada" en Error o Success
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
