package com.tpoAppInteractivas.olacheck.data.local

// Representa un post de la comunidad asociado a una playa específica
data class CommunityPost(
    val id: String = "",
    val beachId: String = "",       // referencia a la playa donde se publicó
    val userId: String = "",        // uid del autor
    val userName: String = "",      // nombre para mostrar en la lista
    val userPhoto: String = "",     // URL del avatar del autor
    val text: String = "",          // contenido del comentario
    val mediaUrl: String = "",      // URL de foto/video (vacío si no tiene)
    val timestamp: Long = 0L        // milisegundos epoch para ordenar y mostrar tiempo relativo
)