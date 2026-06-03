package com.tpoAppInteractivas.olacheck.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tpoAppInteractivas.olacheck.data.local.CommunityPost
import com.tpoAppInteractivas.olacheck.repository.CommunityRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CommunityRepository {

    // Referencia a la colección de posts en Firestore
    private val postsCollection = firestore.collection("community_posts")

    // Convierte el SnapshotListener de Firestore en un Flow de Kotlin
    // callbackFlow permite transformar APIs de callback en Flows reactivos
    // El listener se cancela automáticamente cuando el Flow deja de ser observado
    override fun getPostsForBeach(beachId: String): Flow<List<CommunityPost>> = callbackFlow {

        // Filtra por playa y trae los últimos 50 ordenados por timestamp descendente
        val listener = postsCollection
            .whereEqualTo("beachId", beachId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // cierra el Flow con error si Firestore falla
                    return@addSnapshotListener
                }
                // Convierte cada documento de Firestore a un CommunityPost
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CommunityPost::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(posts) // emite la lista actualizada al Flow
            }

        // awaitClose cancela el SnapshotListener cuando el Flow se destruye
        // evita memory leaks cuando el usuario sale de la pantalla
        awaitClose { listener.remove() }
    }

    // Guarda un nuevo post en Firestore con timestamp actual
    override suspend fun sendPost(post: CommunityPost) {
        postsCollection.add(
            post.copy(timestamp = System.currentTimeMillis())
        ).await()
    }
}