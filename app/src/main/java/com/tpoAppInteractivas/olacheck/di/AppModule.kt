package com.tpoAppInteractivas.olacheck.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tpoAppInteractivas.olacheck.data.local.AppDatabase
import com.tpoAppInteractivas.olacheck.data.local.BeachConditionsDao
import com.tpoAppInteractivas.olacheck.data.local.BeachDao
import com.tpoAppInteractivas.olacheck.data.local.SplashRepositoryImpl
import com.tpoAppInteractivas.olacheck.data.remote.AuthRepositoryImpl
import com.tpoAppInteractivas.olacheck.data.remote.BeachListRepositoryImpl
import com.tpoAppInteractivas.olacheck.data.remote.OpenMeteoService
import com.tpoAppInteractivas.olacheck.repository.AuthRepository
import com.tpoAppInteractivas.olacheck.repository.BeachListRepository
import com.tpoAppInteractivas.olacheck.repository.SplashRepository
import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSplashRepository(impl: SplashRepositoryImpl): SplashRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindBeachListRepository(impl: BeachListRepositoryImpl): BeachListRepository

    companion object {

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "olacheck_db").build()

        @Provides
        @Singleton
        fun provideBeachDao(db: AppDatabase): BeachDao = db.beachDao()

        @Provides
        @Singleton
        fun provideBeachConditionsDao(db: AppDatabase): BeachConditionsDao = db.beachConditionsDao()

        @Provides
        @Singleton
        fun provideOpenMeteoService(): OpenMeteoService = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoService::class.java)
    }
}