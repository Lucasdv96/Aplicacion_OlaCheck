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

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userDataStore: UserDataStore
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
        firebaseAuth.signOut()
        userDataStore.clearUser()
    }

    override fun isLoggedIn(): Flow<Boolean> = userDataStore.isLoggedIn
}