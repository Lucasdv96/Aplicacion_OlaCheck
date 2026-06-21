package com.tpoAppInteractivas.olacheck.viewmodel


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpoAppInteractivas.olacheck.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.tpoAppInteractivas.olacheck.repository.BeachDetailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

//Representa un mensaje individual en el chat
data class ChatMessage(
    val text: String,
    val isUser: Boolean // true = mensaje del usuario, false = respuesta de Geimini
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val beachDetailRepository: BeachDetailRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val beachId: String = checkNotNull(savedStateHandle["beachId"])

    // Historial de mensaje del chat obersvado por la UI
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    //true mientras gemini esta procesando una respuesta
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    //Texto actual del campo de entrada del usuario
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    //Modelo gemini inicializando con la API key desde buildconfig
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    // Sesion de chat que mantiene el historial de conversacion para respuesta contextuales
    private var chat = model.startChat()

    // inicia la conversacion enviando las condiciones de la playa como contexto inicial
    fun startChat(){
        android.util.Log.d("AiViewModel", "API Key length: ${BuildConfig.GEMINI_API_KEY.length}")
        viewModelScope.launch {
            _isLoading.value = true

            val conditions = beachDetailRepository.getConditionsForBeach(beachId)
            if(conditions == null){
                _messages.value = listOf(
                    ChatMessage("No hay condiciones disponibles para esta playa.", isUser =  false)
                )
                _isLoading.value = false
                return@launch
            }

            // Prompt inicial con el contexto de la playa - establece el rol del agente
            // para toda la conversacion
            val contextPrompt = """
               Sos un experto en surf y condiciones marítimas para playas argentinas.
                Las condiciones actuales de la playa son:
                - Temperatura del agua: ${conditions.waterTemp}°C
                - Temperatura del aire: ${conditions.airTemp}°C
                - Velocidad del viento: ${conditions.windSpeed} km/h
                - Altura de olas: ${conditions.waveHeight} m
                - Período de olas: ${conditions.wavePeriod} s
                - Humedad: ${conditions.humidity}%
                
                Dá una recomendación inicial sobre el tipo de traje y las condiciones.
                Luego el usuario puede hacerte preguntas de seguimiento.
                Respondé siempre en español, de forma concisa.
                Solo respondé preguntas relacionadas al surf, condiciones del mar, condiciones climaticas, o seguridad en el agua. 
                Si te preguntan algo fuera de ese tema, indicá amablemente que solo podés ayudar con esos temas.
            """.trimIndent()

            try{
                val response = chat.sendMessage(contextPrompt)
                val text = response.text ?: "Sin respuesta"
                _messages.value = listOf(ChatMessage(text, isUser = false))
            } catch (e: Exception) {
                android.util.Log.e("AiViewModel", "Error en startChat", e)
                _messages.value = listOf(ChatMessage("Error al conectar con el asistente", isUser = false))
            }
            _isLoading.value = false
        }
    }
    fun onInputChange(text: String){
        _inputText.value = text
    }

    //Envia un mensaje del usuario y agrega la respuesta de Gemini al historial
    fun sendMessage(){
        val text= _inputText.value.trim()
        if (text.isEmpty() || _isLoading.value) return

        viewModelScope.launch {
            //agrega el mensaje del usuario al historial inmediatamente
            _messages.value = _messages.value + ChatMessage(text, isUser =  true)
            _inputText.value = ""
            _isLoading.value = true

            try {
                // gemini recuerda los mensajes anterior gracias a chatSession
                val response = chat.sendMessage(text)
                val reply = response.text ?: "Sin respuesta"
                _messages.value = _messages.value + ChatMessage(reply, isUser = false)
            } catch (e: Exception) {
            android.util.Log.e("AiViewModel", "Error en sendMessage", e)
            _messages.value = _messages.value + ChatMessage("Error al obtener respuesta.", isUser = false)
        }
            _isLoading.value = false
        }
    }
}