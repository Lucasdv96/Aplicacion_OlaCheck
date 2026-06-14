package com.tpoAppInteractivas.olacheck.viewmodel


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpoAppInteractivas.olacheck.data.local.CommunityPost
import com.tpoAppInteractivas.olacheck.data.local.UserDataStore
import com.tpoAppInteractivas.olacheck.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val repository: CommunityRepository,
    private val userDataStore: UserDataStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Obtiene el beachId desde la ruta de navegación via SavedStateHandle
    private val beachId: String = checkNotNull(savedStateHandle["beachId"])

    // Lista de posts observada por la UI — se actualiza en tiempo real desde Firestore
    private val _posts = MutableStateFlow<List<CommunityPost>>(emptyList())
    val posts: StateFlow<List<CommunityPost>> = _posts

    // Texto actual del campo de entrada del chat
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    init {
        // Inicia la escucha de posts al crear el ViewModel
        loadPosts()
    }

    // Suscribe el Flow de Firestore y actualiza _posts con cada cambio en tiempo real
    private fun loadPosts() {
        viewModelScope.launch {
            repository.getPostsForBeach(beachId).collect { posts ->
                _posts.value = posts
            }
        }
    }

    // Actualiza el texto del campo de entrada mientras el usuario escribe
    fun onInputChange(text: String) {
        _inputText.value = text
    }

    // Publica un nuevo post en Firestore con los datos del usuario logueado
    fun sendPost() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return // no envía si el campo está vacío

        viewModelScope.launch {
            // Lee los datos del usuario desde DataStore de forma puntual
            val userData = userDataStore.userData.first() ?: return@launch

            val post = CommunityPost(
                beachId = beachId,
                userId = userData.uid,
                userName = userData.displayName,
                userPhoto = userData.photoUrl,
                text = text
            )
            repository.sendPost(post)
            _inputText.value = "" // limpia el campo después de enviar
        }
    }
    // UID del usuario logueado, usado en la UI para diferenciar mensajes propios
    val currentUserId: StateFlow<String> = MutableStateFlow("").also { flow ->
        viewModelScope.launch {
            val uid = userDataStore.userData.first()?.uid ?: ""
            (flow as MutableStateFlow).value = uid
        }
    }
}