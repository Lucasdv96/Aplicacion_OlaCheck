package com.tpoAppInteractivas.olacheck.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.Flow

interface AuthRepository{
    suspend fun sigInWithGoogle(account: GoogleSignInAccount): Result<Unit>
    suspend fun signOut()
    fun isLoggedIn(): Flow<Boolean>

    // Registro con email y contraseña
    suspend fun registerWithEmail(email: String, password: String): Result<Unit>

    // Login con email y contraseña
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>

    // Envía un email para restablecer la contraseña
    suspend fun sendPasswordReset(email: String): Result<Unit>
}

