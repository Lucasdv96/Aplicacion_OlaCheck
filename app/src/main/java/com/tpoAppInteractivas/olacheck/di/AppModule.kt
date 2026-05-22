package com.tpoAppInteractivas.olacheck.di

import com.tpoAppInteractivas.olacheck.data.local.SplashRepositoryImpl
import com.tpoAppInteractivas.olacheck.repository.SplashRepository
import dagger.Binds
import dagger.Module
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
}