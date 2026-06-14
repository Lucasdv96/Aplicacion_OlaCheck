package com.tpoAppInteractivas.olacheck.repository

// Contrato del repositorio de IA
// El ViewModel no sabe si usa Gemini u otro modelo — solo llama a este método
interface AiRepository {

    //Recibe las condiciones de la playa y devuelve una recomendacion en lenguaje natural
    //Retorna Result para manejar exito/error sin excepniones no controladas

    suspend fun getBeachRecommendation(
        waterTemp: Float,
        airTemp: Float,
        windSpeed: Float,
        waveHeight: Float,
        wavePeriod: Float,
        humidity: Float
    ): Result<String>
}