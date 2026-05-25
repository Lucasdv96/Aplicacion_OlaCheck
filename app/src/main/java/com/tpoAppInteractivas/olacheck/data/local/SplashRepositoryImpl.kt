package com.tpoAppInteractivas.olacheck.data.local

import com.tpoAppInteractivas.olacheck.repository.SplashRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SplashRepositoryImpl @Inject constructor(
    private val userDataStore: UserDataStore
) : SplashRepository {

    override suspend fun isUserLoggedIn(): Boolean {
        return userDataStore.isLoggedIn.first()
    }
}