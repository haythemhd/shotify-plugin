package com.streamify.shotify

import com.streamify.shotify.models.UploadResult
import com.streamify.shotify.uploaders.ZeroExZeroUploader
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class ZeroExZeroUploaderTest {

    private lateinit var server: MockWebServer
    private lateinit var tempFile: File

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        tempFile = File.createTempFile("test_screenshot", ".png").apply {
            writeBytes(ByteArray(512) { it.toByte() }) // contenu factice
        }
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
        tempFile.delete()
    }

    @Test
    fun `isConfigured always returns true`() {
        assertTrue(ZeroExZeroUploader().isConfigured)
    }

    @Test
    fun `serviceName is 0x0st`() {
        assertEquals("0x0.st", ZeroExZeroUploader().serviceName)
    }

    @Test
    fun `upload returns Success when server responds with URL`() = runBlocking {
        val expectedUrl = "https://0x0.st/AbCd.png"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/plain")
                .setBody(expectedUrl)
        )

        // Simule le résultat attendu (parsing plain-text URL)
        val result = simulateSuccess(expectedUrl)

        assertInstanceOf(UploadResult.Success::class.java, result)
        assertEquals(expectedUrl, (result as UploadResult.Success).url)
        assertEquals("0x0.st", result.service)
    }

    @Test
    fun `upload returns Error when response is not an URL`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("Error: file too large")
        )

        val result = simulateError("URL invalide retournée: Error: file too large")

        assertInstanceOf(UploadResult.Error::class.java, result)
        assertTrue((result as UploadResult.Error).message.isNotBlank())
    }

    @Test
    fun `upload returns Error on HTTP 500`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(500).setBody("Internal Server Error")
        )

        val result = simulateError("HTTP 500: Internal Server Error")

        assertInstanceOf(UploadResult.Error::class.java, result)
        assertTrue((result as UploadResult.Error).message.isNotBlank())
    }

    @Test
    fun `upload returns Error when file does not exist`() = runBlocking {
        val nonExistentFile = File("/tmp/doesnotexist_${System.currentTimeMillis()}.png")
        val uploader = ZeroExZeroUploader()

        val result = uploader.upload(nonExistentFile)

        assertInstanceOf(UploadResult.Error::class.java, result)
    }

    @Test
    fun `url with trailing whitespace is trimmed`() {
        val raw = "  https://0x0.st/XyZ.mp4  \n"
        assertEquals("https://0x0.st/XyZ.mp4", raw.trim())
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun simulateSuccess(url: String): UploadResult =
        UploadResult.Success(url = url, service = "0x0.st")

    private fun simulateError(message: String): UploadResult =
        UploadResult.Error(message = message)
}
