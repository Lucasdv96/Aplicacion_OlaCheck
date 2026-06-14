package com.tpoAppInteractivas.olacheck.data.remote

import com.google.ai.client.generativeai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.tpoAppInteractivas.olacheck.repository.AiRepository
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AiRepositoryImpl @Inject constructor(): AiRepository{
    // Inicializa el modelo Gemini con la API Key desde BuildConfig
    // gemini-s1.5-flash es el modelo mas rapido y economigo para la respuesta de texto
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    override suspend fun getBeachRecommendation(
        waterTemp: Float,
        airTemp: Float,
        windSpeed: Float,
        waveHeight: Float,
        wavePeriod: Float,
        humidity: Float
    ): Result<String> {
        return try {
            // Construye el prompt con los datos actuales de la playa
            // El contexto de "experto en surf" guia al modelo para dar respuesta especificas
            val prompt = """
            Sos un experto en surf y condiciones marítimas.
            Analizá estas condiciones actuales de una playa argentina y dá una recomendación concisa:
            
             - Temperatura del agua: ${'$'}{waterTemp}°C
             - Temperatura del aire: ${'$'}{airTemp}°C
             - Velocidad del viento: ${'$'}{windSpeed} km/h
             - Altura de olas: ${'$'}{waveHeight} m
             - Período de olas: ${'$'}{wavePeriod} s
             - Humedad: ${'$'}{humidity}%
                
              Respondé en español con:
                1. Tipo de traje recomendado (shortie, 3/2mm, 4/3mm, 5/4mm, traje seco, o sin traje)
                2. Breve justificación basada en las condiciones
                3. Advertencias si el mar está agitado o el viento es fuerte
                
                Sé conciso, máximo 4 oraciones.   
                
              """.trimIndent()

            val response = model.generateContent(prompt)
            val text = response.text ?: return Result.failure(Exception("Respuesta vacia del modelo"))
            Result.success(text)
        } catch (e: Exception){
            Result.failure(e)
        }
    }
}