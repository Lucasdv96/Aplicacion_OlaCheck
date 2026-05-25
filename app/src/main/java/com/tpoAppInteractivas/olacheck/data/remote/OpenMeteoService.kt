package com.tpoAppInteractivas.olacheck.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoService {
    @GET("v1/marine")
    suspend fun getMarineConditions(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "wave_height,wave_period,sea_surface_temperature",
        @Query("timezone") timezone: String = "America/Argentina/Buenos_Aires"
    ): MarineResponse

    @GET("v1/forecast")
    suspend fun getWeatherConditions(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,wind_speed_10m,wind_direction_10m,relative_humidity_2m",
        @Query("timezone") timezone: String = "America/Argentina/Buenos_Aires"
    ): WeatherResponse

}

data class MarineResponse(
    val current: MarineCurrent?
)

data class MarineCurrent(
    val wave_height: Float?,
    val wave_period: Float?,
    val sea_surface_temperature: Float?
)

data class WeatherResponse(
    val current: WeatherCurrent?
)

data class WeatherCurrent(
    val temperature_2m: Float?,
    val wind_speed_10m: Float?,
    val wind_direction_10m: Float?,
    val relative_humidity_2m: Float?
)