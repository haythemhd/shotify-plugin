package com.streamify.shotify.uploaders

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
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Implémentation [MediaUploader] pour 0x0.st — service d'hébergement anonyme gratuit.
 *
 * Aucune authentification requise. La réponse est un plain-text contenant l'URL.
 * Attention : les fichiers sur 0x0.st sont publics et peuvent expirer.
 *
 * @param httpClient Client OkHttp injecté (facilite les tests).
 */
class ZeroExZeroUploader(
    private val httpClient: OkHttpClient = defaultClient()
) : MediaUploader {

    override val serviceName: String = "0x0.st"

    // Aucune configuration requise pour ce service
    override val isConfigured: Boolean = true

    override suspend fun upload(file: File): UploadResult = withContext(Dispatchers.IO) {
        logger.info { "[$serviceName] Début upload: ${file.name} (${file.length()} bytes)" }

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
                    val delayMs = RETRY_BASE_DELAY_MS * (1L shl (attempt - 1))
                    delay(delayMs)
                }
            }
        }

        logger.error { "[$serviceName] Échec après $MAX_RETRIES tentatives: ${lastError?.message}" }
        UploadResult.Error(
            message = "Upload 0x0.st échoué après $MAX_RETRIES tentatives: ${lastError?.message}",
            cause = lastError
        )
    }

    /** Effectue l'appel HTTP multipart vers 0x0.st. */
    private fun doUpload(file: File): String {
        val mediaType = "application/octet-stream".toMediaTypeOrNull()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "file",
                filename = file.name,
                body = file.asRequestBody(mediaType)
            )
            .build()

        val request = Request.Builder()
            .url(UPLOAD_URL)
            .post(requestBody)
            .header("User-Agent", "Shotify-Plugin/1.0")
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()?.trim()
                ?: throw IOException("Réponse vide de 0x0.st")

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $body")
            }

            // 0x0.st retourne directement l'URL en plain text
            if (!body.startsWith("http")) {
                throw IOException("URL invalide retournée: $body")
            }

            return body
        }
    }

    companion object {
        private const val UPLOAD_URL = "https://0x0.st/"
        private const val MAX_RETRIES = 3
        private const val RETRY_BASE_DELAY_MS = 1000L

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
