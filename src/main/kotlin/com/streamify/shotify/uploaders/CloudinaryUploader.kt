package com.streamify.shotify.uploaders

import com.streamify.shotify.models.CloudinaryConfig
import com.streamify.shotify.models.MediaUploader
import com.streamify.shotify.models.UploadResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Implémentation [MediaUploader] pour Cloudinary.
 *
 * Utilise un upload preset non signé — aucune clé secrète n'est requise côté client.
 * Retry automatique 3 fois avec backoff exponentiel (1s, 2s, 4s).
 *
 * @param config Configuration Cloudinary (cloudName + uploadPreset).
 * @param httpClient Client OkHttp injecté (facilite les tests).
 */
class CloudinaryUploader(
    private val config: CloudinaryConfig,
    private val httpClient: OkHttpClient = defaultClient()
) : MediaUploader {

    override val serviceName: String = "Cloudinary"

    override val isConfigured: Boolean
        get() = config.cloudName.isNotBlank() && config.uploadPreset.isNotBlank()

    override suspend fun upload(file: File): UploadResult = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext UploadResult.Error(
                "Cloudinary non configuré. Vérifiez Settings > Tools > Shotify."
            )
        }

        logger.info { "[$serviceName] Début upload: ${file.name} (${file.length()} bytes)" }

        // Retry avec backoff exponentiel
        var lastError: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                val url = doUpload(file)
                logger.info { "[$serviceName] Upload réussi (tentative $attempt): $url" }
                return@withContext UploadResult.Success(url = url, service = serviceName)
            } catch (e: Exception) {
                lastError = e
                logger.warn { "[$serviceName] Tentative $attempt/$MAX_RETRIES échouée: ${e.message}" }
                if (attempt < MAX_RETRIES) {
                    val delayMs = RETRY_BASE_DELAY_MS * (1L shl (attempt - 1)) // 1s, 2s, 4s
                    delay(delayMs)
                }
            }
        }

        logger.error { "[$serviceName] Échec après $MAX_RETRIES tentatives: ${lastError?.message}" }
        UploadResult.Error(
            message = "Upload Cloudinary échoué après $MAX_RETRIES tentatives: ${lastError?.message}",
            cause = lastError
        )
    }

    /** Effectue l'appel HTTP multipart vers l'API Cloudinary. */
    private fun doUpload(file: File): String {
        val mediaType = when (file.extension.lowercase()) {
            "mp4", "mov" -> "video/mp4"
            "png"        -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else         -> "application/octet-stream"
        }.toMediaTypeOrNull()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "file",
                filename = file.name,
                body = file.asRequestBody(mediaType)
            )
            .addFormDataPart("upload_preset", config.uploadPreset)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/${config.cloudName}/auto/upload")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
                ?: throw IOException("Réponse vide de Cloudinary")

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $body")
            }

            val json = JSONObject(body)

            // Cloudinary retourne un objet error si le preset est invalide même avec 200
            if (json.has("error")) {
                throw IOException("Cloudinary error: ${json.getJSONObject("error").getString("message")}")
            }

            return json.getString("secure_url")
        }
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_BASE_DELAY_MS = 1000L

        /** Crée un OkHttpClient avec timeout de 30 secondes. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
