package com.tpoAppInteractivas.olacheck.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.tpoAppInteractivas.olacheck.data.local.UserDataStore
import com.tpoAppInteractivas.olacheck.data.local.UserProfile
import com.tpoAppInteractivas.olacheck.repository.AuthRepository
import com.tpoAppInteractivas.olacheck.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userDataStore: UserDataStore,
    private val authRepository: AuthRepository
) : ProfileRepository {
    override fun getUserProfile(): Flow<UserProfile?> {
        return userDataStore.userData.map { data ->
            data?.let {
                UserProfile(
                    uid = it.uid,
                    displayName = it.displayName,
                    email = it.email,
                    photoUrl = it.photoUrl
                )
            }
        }
    }

    override suspend fun signOut() {
        authRepository.signOut()
    }
}
