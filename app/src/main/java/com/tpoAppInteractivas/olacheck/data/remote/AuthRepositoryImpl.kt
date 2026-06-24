package com.tpoAppInteractivas.olacheck.data.remote

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.tpoAppInteractivas.olacheck.data.local.UserDataStore
import com.tpoAppInteractivas.olacheck.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userDataStore: UserDataStore,
    @ApplicationContext private val context: Context
) : AuthRepository{
    override suspend fun sigInWithGoogle(account: GoogleSignInAccount): Result<Unit> {
        return try{
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user ?: return Result.failure(Exception("Usuario nulo"))
            userDataStore.saveUser(
                uid = user.uid,
                displayName = user.displayName ?: "",
                email =  user.email ?: "",
                photoUrl = user.photoUrl.toString() ?: ""
            )
            Result.success(Unit)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut().await()
        firebaseAuth.signOut()
        userDataStore.clearUser()
    }

    override fun isLoggedIn(): Flow<Boolean> = userDataStore.isLoggedIn

    // Crea una cuenta nueva con email y contraseña en Firebase Auth
    override suspend fun registerWithEmail(email: String, password: String): Result<Unit> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Usuario nulo"))
            // Guardamos los datos en DataStore — sin foto de perfil por defecto
            userDataStore.saveUser(
                uid = user.uid,
                displayName = user.displayName ?: email.substringBefore("@"),
                email = user.email ?: "",
                photoUrl = ""
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseError(e))
        }
    }

    // Inicia sesión con email y contraseña en Firebase Auth
    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Usuario nulo"))
            userDataStore.saveUser(
                uid = user.uid,
                displayName = user.displayName ?: email.substringBefore("@"),
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString() ?: ""
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirebaseError(e))
        }
    }

    // Traduce los códigos de error de Firebase a mensajes legibles para el usuario
    private fun mapFirebaseError(e: Exception): Exception {
        val message = when {
            e.message?.contains("email address is already in use") == true ->
                "Ese email ya está registrado. Intentá iniciar sesión."
            e.message?.contains("password is invalid") == true ||
            e.message?.contains("INVALID_PASSWORD") == true ->
                "Contraseña incorrecta."
            e.message?.contains("no user record") == true ||
            e.message?.contains("USER_NOT_FOUND") == true ->
                "No existe una cuenta con ese email."
            e.message?.contains("badly formatted") == true ||
            e.message?.contains("INVALID_EMAIL") == true ->
                "El email no tiene un formato válido."
            e.message?.contains("Password should be at least") == true ||
            e.message?.contains("WEAK_PASSWORD") == true ->
                "La contraseña debe tener al menos 6 caracteres."
            e.message?.contains("network error", ignoreCase = true) == true ||
            e.message?.contains("Unable to resolve host") == true ||
            e.message?.contains("NETWORK_ERROR") == true ->
                "Sin conexión a internet. Verificá tu red e intentá de nuevo."
            e.message?.contains("too many requests", ignoreCase = true) == true ||
            e.message?.contains("TOO_MANY_ATTEMPTS") == true ->
                "Demasiados intentos fallidos. Esperá unos minutos e intentá de nuevo."
            else -> e.message ?: "Error desconocido. Intentá de nuevo."
        }
        return Exception(message)
    }
}