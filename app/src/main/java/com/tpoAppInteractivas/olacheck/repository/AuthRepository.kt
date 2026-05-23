package com.tpoAppInteractivas.olacheck.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.Flow

interface AuthRepository{
    suspend fun sigInWithGoogle(account: GoogleSignInAccount): Result<Unit>
    suspend fun signOut()
    fun isLoggedIn(): Flow<Boolean>
}