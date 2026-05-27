package com.tpoAppInteractivas.olacheck.data.remote

import com.tpoAppInteractivas.olacheck.data.local.Beach
import com.tpoAppInteractivas.olacheck.data.local.BeachConditions
import com.tpoAppInteractivas.olacheck.data.local.BeachConditionsDao
import com.tpoAppInteractivas.olacheck.data.local.BeachDao
import com.tpoAppInteractivas.olacheck.repository.BeachDetailRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeachDetailRepositoryImpl @Inject constructor(
    private val beachDao: BeachDao,
    private val beachConditionsDao: BeachConditionsDao,
    private val weatherService: WeatherService,
    private val marineService: MarineService
) : BeachDetailRepository {
    override suspend fun getBeachById(beachId: String): Beach? {
        return beachDao.getBeachById(beachId)
    }

    override suspend fun getConditionsForBeach(beachId: String): BeachConditions? {
        return beachConditionsDao.getConditionsByBeachId(beachId).first()
    }

    override suspend fun refreshConditions(beachId: String) {
        val beach = beachDao.getBeachById(beachId) ?: return
        val marine = marineService.getMarineConditions(beach.latitude, beach.longitude)
        val weather = weatherService.getWeatherConditions(beach.latitude, beach.longitude)
        val conditions = BeachConditions(
            beachId = beachId,
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
    }


}
