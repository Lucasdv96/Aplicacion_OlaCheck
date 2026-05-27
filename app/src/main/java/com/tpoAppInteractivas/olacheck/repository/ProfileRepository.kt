package com.tpoAppInteractivas.olacheck.repository

import com.tpoAppInteractivas.olacheck.data.local.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun signOut()
}