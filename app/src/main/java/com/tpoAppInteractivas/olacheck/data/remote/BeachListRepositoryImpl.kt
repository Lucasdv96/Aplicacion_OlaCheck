package com.tpoAppInteractivas.olacheck.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestore
import com.tpoAppInteractivas.olacheck.data.local.Beach
import com.tpoAppInteractivas.olacheck.data.local.BeachConditions
import com.tpoAppInteractivas.olacheck.data.local.BeachConditionsDao
import com.tpoAppInteractivas.olacheck.data.local.BeachDao
import com.tpoAppInteractivas.olacheck.repository.BeachListRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeachListRepositoryImpl @Inject constructor(
    private val beachDao: BeachDao,
    private val beachConditionsDao: BeachConditionsDao,
    private val openMeteoService: OpenMeteoService,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : BeachListRepository {
    override fun getBeaches(): Flow<List<Beach>> = beachDao.getAllBeaches()

    override fun getAllConditions(): Flow<List<BeachConditions>> =
        beachConditionsDao.getAllConditions()

    override fun getConditionsForBeach(beachId: String): Flow<BeachConditions?> =
        beachConditionsDao.getConditionsByBeachId(beachId)

    override suspend fun refreshBeachData() {
        val snapshot = firestore.collection("beaches").get().await()
        val beaches = snapshot.documents.mapNotNull { doc ->
            Beach(
                id = doc.id,
                name = doc.getString("name") ?: return@mapNotNull null,
                latitude = doc.getDouble("latitude") ?: return@mapNotNull null,
                longitude = doc.getDouble("longitude") ?: return@mapNotNull null,
                imageUrl = doc.getString("imageUrl"),
                lastUpdated = System.currentTimeMillis()
            )
        }
        beachDao.insertBeaches(beaches)
        beaches.forEach { beach ->
            try {
                val marine = openMeteoService.getMarineConditions(beach.latitude, beach.longitude)
                val weather = openMeteoService.getWeatherConditions(beach.latitude, beach.longitude)
                val conditions = BeachConditions(
                    beachId = beach.id,
                    waterTemp = marine.current?.sea_surface_temperature ?: 0f,
                    airTemp = weather.current?.temperature_2m ?: 0f,
                    windSpeed = weather.current?.wind_speed_10m ?: 0f,
                    windDirection = weather.current?.wind_direction_10m ?: 0f,
                    waveHeight = marine.current?.wave_height ?: 0f,
                    wavePeriod = marine.current?.wave_period ?: 0f,
                    humidity = weather.current?.relative_humidity_2m ?: 0f,
                    fetchedAt = System.currentTimeMillis()
                )
                beachConditionsDao.insertConditions(conditions)
            } catch (e: Exception) {
                // Si falla una playa, continúa con las demás
            }
        }
    }
    override fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}