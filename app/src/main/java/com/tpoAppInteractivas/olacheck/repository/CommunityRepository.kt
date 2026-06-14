package com.tpoAppInteractivas.olacheck.repository

import com.tpoAppInteractivas.olacheck.data.local.CommunityPost
import kotlinx.coroutines.flow.Flow

// Contrato del repositorio de comunidad
// Define las operaciones disponibles sin exponer el origen de datos (Firestore)
interface CommunityRepository {

    // Devuelve un Flow con los últimos 50 posts de una playa específica
    // Se actualiza en tiempo real via SnapshotListener de Firestore
    fun getPostsForBeach(beachId: String): Flow<List<CommunityPost>>

    // Publica un nuevo post en Firestore para la playa indicada
    suspend fun sendPost(post: CommunityPost)
}