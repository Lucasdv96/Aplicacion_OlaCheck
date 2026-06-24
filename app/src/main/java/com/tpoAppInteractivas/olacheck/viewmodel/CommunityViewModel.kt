package com.tpoAppInteractivas.olacheck.viewmodel


import android.net.Uri
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

    // URI de la foto seleccionada/capturada, antes de subirla. Null = sin foto.
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    // Indica si se está subiendo la imagen (para mostrar un spinner y bloquear el botón)
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    // Mensaje de error transitorio (para mostrar en un Snackbar si falla la subida)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

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
    // Publica un nuevo post. Si hay una imagen seleccionada, primero la sube a
// Cloudinary y guarda la URL resultante en el campo mediaUrl del post.
    fun sendPost() {
        val text = _inputText.value.trim()
        val imageUri = _selectedImageUri.value

        // No enviamos si no hay ni texto ni imagen
        if (text.isEmpty() && imageUri == null) return

        viewModelScope.launch {
            val userData = userDataStore.userData.first() ?: return@launch

            _isUploading.value = true
            try {
                // Si hay imagen, la subimos a Cloudinary y obtenemos su URL pública
                val mediaUrl = if (imageUri != null) {
                    repository.uploadImage(imageUri)
                } else {
                    ""
                }

                val post = CommunityPost(
                    beachId = beachId,
                    userId = userData.uid,
                    userName = userData.displayName,
                    userPhoto = userData.photoUrl,
                    text = text,
                    mediaUrl = mediaUrl
                )
                repository.sendPost(post)

                // Limpiamos el campo de texto y la imagen seleccionada
                _inputText.value = ""
                _selectedImageUri.value = null
            } catch (e: Exception) {
                _errorMessage.value = "No se pudo subir la imagen. Intentá de nuevo."
            } finally {
                _isUploading.value = false
            }
        }
    }
    // UID del usuario logueado, usado en la UI para diferenciar mensajes propios
    val currentUserId: StateFlow<String> = MutableStateFlow("").also { flow ->
        viewModelScope.launch {
            val uid = userDataStore.userData.first()?.uid ?: ""
            (flow as MutableStateFlow).value = uid
        }
    }
    // Guarda la foto elegida (desde galería o cámara) para mostrarla en preview
    fun onImageSelected(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    // Limpia el mensaje de error una vez mostrado
    fun clearError() {
        _errorMessage.value = null
    }
}