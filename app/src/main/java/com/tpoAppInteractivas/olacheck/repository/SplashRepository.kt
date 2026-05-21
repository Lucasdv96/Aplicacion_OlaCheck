package com.tpoAppInteractivas.olacheck.repository

interface SplashRepository{
    suspend fun isUserLoggedIn(): Boolean


}