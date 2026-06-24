package com.tpoAppInteractivas.olacheck.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.tpoAppInteractivas.olacheck.data.local.Beach
import com.tpoAppInteractivas.olacheck.data.local.BeachConditions
import com.tpoAppInteractivas.olacheck.data.local.BeachConditionsDao
import com.tpoAppInteractivas.olacheck.data.local.BeachDao
import com.tpoAppInteractivas.olacheck.repository.BeachListRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeachListRepositoryImpl @Inject constructor(
    private val beachDao: BeachDao,
    private val beachConditionsDao: BeachConditionsDao,
    private val weatherService: WeatherService,
    private val marineService: MarineService,
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
                val marine = marineService.getMarineConditions(beach.latitude, beach.longitude)
                val weather = weatherService.getWeatherConditions(beach.latitude, beach.longitude)
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
    // Escucha en tiempo real los cambios de conexión usando un NetworkCallback.
    // callbackFlow convierte ese callback en un Flow que el ViewModel puede observar.
    override fun observeConnectivity(): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Avisa cuando se gana o se pierde la conexión
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)   // volvió internet
            }
            override fun onLost(network: Network) {
                trySend(false)  // se perdió internet
            }
        }

        // Solo nos interesan redes con acceso real a internet
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, callback)

        // Emitimos el estado actual apenas empezamos a escuchar
        trySend(isOnline())

        // Cuando el Flow se cancela, des-registramos el callback para no tener fugas de memoria
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
}