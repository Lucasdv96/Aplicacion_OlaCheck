package com.tpoAppInteractivas.olacheck.data.local

import com.tpoAppInteractivas.olacheck.repository.SplashRepository
import javax.inject.Inject

class SplashRepositoryImpl @Inject constructor() : SplashRepository {
    override suspend fun isUserLoggedIn(): Boolean = false
}

//intento de commit?