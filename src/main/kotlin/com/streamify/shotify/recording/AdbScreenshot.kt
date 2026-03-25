package com.streamify.shotify.recording

import com.android.ddmlib.IDevice
import com.android.ddmlib.RawImage
import com.streamify.shotify.utils.TempFileManager
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

/**
 * Capture l'écran d'un device Android via `adb screencap` (ddmlib).
 *
 * Utilise [IDevice.getScreenshot] pour obtenir un [RawImage] brut,
 * puis le convertit en PNG via [BufferedImage].
 *
 * @param device Device Android connecté via ddmlib.
 */
class AdbScreenshot(private val device: IDevice) {

    /**
     * Capture une image PNG de l'écran du device.
     *
     * @return Fichier PNG local prêt à être uploadé.
     * @throws RuntimeException Si la capture ou la conversion échoue.
     */
    suspend fun capture(): File = withContext(Dispatchers.IO) {
        logger.info { "[AdbScreenshot] Capture sur ${device.serialNumber}" }

        val rawImage: RawImage = try {
            device.screenshot
        } catch (e: Exception) {
            throw RuntimeException("Impossible de capturer l'écran: ${e.message}", e)
        }

        logger.debug { "[AdbScreenshot] Image brute: ${rawImage.width}x${rawImage.height}, bpp=${rawImage.bpp}" }

        val bufferedImage = convertRawImage(rawImage)
        val file = TempFileManager.createScreenshotFile()

        try {
            ImageIO.write(bufferedImage, "PNG", file)
        } catch (e: Exception) {
            throw RuntimeException("Impossible de sauvegarder le PNG: ${e.message}", e)
        }

        logger.info { "[AdbScreenshot] Screenshot sauvegardé: ${file.absolutePath} (${file.length()} bytes)" }
        file
    }

    /**
     * Convertit un [RawImage] ddmlib en [BufferedImage] standard Java.
     *
     * Le format raw d'Android encode chaque pixel selon [RawImage.bpp] bits.
     * [RawImage.getARGB] gère la conversion des composantes RGBA/BGRA.
     */
    private fun convertRawImage(raw: RawImage): BufferedImage {
        val image = BufferedImage(raw.width, raw.height, BufferedImage.TYPE_INT_ARGB)
        var index = 0
        val bytesPerPixel = raw.bpp shr 3 // bpp / 8

        for (y in 0 until raw.height) {
            for (x in 0 until raw.width) {
                image.setRGB(x, y, raw.getARGB(index))
                index += bytesPerPixel
            }
        }

        return image
    }
}
