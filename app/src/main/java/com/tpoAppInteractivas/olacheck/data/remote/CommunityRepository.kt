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
import android.content.Context
import android.net.Uri
import com.tpoAppInteractivas.olacheck.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryService: CloudinaryService,
    @ApplicationContext private val context: Context
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
    // Sube una imagen a Cloudinary a partir de su URI local y devuelve la URL pública.
// Lee los bytes de la imagen, arma el cuerpo multipart y llama al endpoint unsigned.
    override suspend fun uploadImage(uri: Uri): String {
        // Comprimimos y redimensionamos antes de subir
        val bytes = compressImage(uri)

        // Convertimos los bytes en el archivo del formulario multipart
        val requestFile = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", "upload.jpg", requestFile)

        // El preset unsigned va como campo de texto del formulario
        val presetPart = BuildConfig.CLOUDINARY_UPLOAD_PRESET
            .toRequestBody("text/plain".toMediaTypeOrNull())

        // Llamamos a Cloudinary y devolvemos la URL segura de la imagen subida
        val response = cloudinaryService.uploadImage(
            cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME,
            file = filePart,
            uploadPreset = presetPart
        )
        return response.secure_url
    }

    // Comprime y redimensiona la imagen antes de subirla, para que pese menos
// y no consuma tanto de la cuota de Cloudinary. Devuelve los bytes en JPEG.
    private fun compressImage(uri: Uri): ByteArray {
        val resolver = context.contentResolver

        // 1) Leemos solo las dimensiones de la imagen, sin cargarla entera en memoria
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        // 2) Calculamos un factor de reducción para no cargar una imagen gigante (evita OOM)
        val maxDimension = 1080
        var sampleSize = 1
        val largerSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (largerSide / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }

        // 3) Decodificamos la imagen ya reducida
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw Exception("No se pudo leer la imagen")

        // 4) Escalamos para que el lado más largo no supere maxDimension (mantiene proporción)
        val ratio = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaled = if (ratio < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true
            )
        } else {
            bitmap
        }

        // 4.5) Corregimos la rotación según los metadatos EXIF de la foto
//      La cámara guarda la orientación en los metadatos pero no rota los píxeles
        val exif = androidx.exifinterface.media.ExifInterface(
            context.contentResolver.openInputStream(uri)!!
        )
        val rotation = when (
            exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val rotated = if (rotation != 0f) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
        } else {
            scaled
        }

        // 5) Comprimimos a JPEG al 80% de calidad
        // 5) Comprimimos a JPEG al 80% de calidad
        val output = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, 80, output)
        return output.toByteArray()
    }
}