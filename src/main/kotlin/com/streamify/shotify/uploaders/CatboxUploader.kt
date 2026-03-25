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
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * [MediaUploader] implementation for catbox.moe — free anonymous file hosting.
 *
 * No authentication required. Response is plain text containing the URL.
 * Files are permanent with a 200 MB size limit.
 *
 * @param httpClient Injected OkHttp client (facilitates testing).
 */
class CatboxUploader(
    private val httpClient: OkHttpClient = defaultClient()
) : MediaUploader {

    override val serviceName: String = "catbox.moe"

    override val isConfigured: Boolean = true

    override suspend fun upload(file: File): UploadResult = withContext(Dispatchers.IO) {
        logger.info { "[$serviceName] Starting upload: ${file.name} (${file.length()} bytes)" }

        var lastError: Exception? = null
        for (attempt in 1..MAX_RETRIES) {
            try {
                val url = doUpload(file)
                logger.info { "[$serviceName] Upload succeeded (attempt $attempt): $url" }
                return@withContext UploadResult.Success(url = url, service = serviceName)
            } catch (e: Exception) {
                lastError = e
                logger.warn { "[$serviceName] Attempt $attempt/$MAX_RETRIES failed: ${e.message}" }
                if (attempt < MAX_RETRIES) {
                    val delayMs = RETRY_BASE_DELAY_MS * (1L shl (attempt - 1))
                    delay(delayMs)
                }
            }
        }

        logger.error { "[$serviceName] Failed after $MAX_RETRIES attempts: ${lastError?.message}" }
        UploadResult.Error(
            message = "catbox.moe upload failed after $MAX_RETRIES attempts: ${lastError?.message}",
            cause = lastError
        )
    }

    private fun doUpload(file: File): String {
        val mediaType = "application/octet-stream".toMediaTypeOrNull()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("reqtype", "fileupload")
            .addFormDataPart(
                name = "fileToUpload",
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
                ?: throw IOException("Empty response from catbox.moe")

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $body")
            }

            if (!body.startsWith("http")) {
                throw IOException("Invalid URL returned: $body")
            }

            return body
        }
    }

    companion object {
        private const val UPLOAD_URL = "https://catbox.moe/user/api.php"
        private const val MAX_RETRIES = 3
        private const val RETRY_BASE_DELAY_MS = 1000L

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
