package com.tpoAppInteractivas.olacheck.di

import com.google.firebase.auth.FirebaseAuth
import com.tpoAppInteractivas.olacheck.data.local.SplashRepositoryImpl
import com.tpoAppInteractivas.olacheck.data.remote.AuthRepositoryImpl
import com.tpoAppInteractivas.olacheck.repository.AuthRepository
import com.tpoAppInteractivas.olacheck.repository.SplashRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSplashRepository(
        impl: SplashRepositoryImpl
    ): SplashRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}