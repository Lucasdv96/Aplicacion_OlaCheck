package com.tpoAppInteractivas.olacheck.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

// Servicio Retrofit para subir imágenes a Cloudinary.
// Usa el endpoint de subida "unsigned", que no requiere firmar con el API secret.
interface CloudinaryService {

    @Multipart
    @POST("v1_1/{cloudName}/image/upload")
    suspend fun uploadImage(
        @Path("cloudName") cloudName: String,             // nuestro cloud_name
        @Part file: MultipartBody.Part,                   // el archivo de imagen
        @Part("upload_preset") uploadPreset: RequestBody  // el preset unsigned
    ): CloudinaryResponse
}

// Respuesta de Cloudinary. Solo nos interesa la URL segura (https) de la imagen subida.
data class CloudinaryResponse(
    val secure_url: String
)