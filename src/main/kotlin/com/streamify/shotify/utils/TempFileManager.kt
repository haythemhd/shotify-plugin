package com.streamify.shotify.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Gère la création et le nettoyage des fichiers temporaires du plugin.
 *
 * Tous les fichiers sont créés dans le répertoire temp système avec le préfixe "shotify_".
 * [cleanup] supprime les fichiers de plus d'une heure pour éviter l'accumulation.
 */
object TempFileManager {

    private const val PREFIX = "shotify_"
    private val tempDir = File(System.getProperty("java.io.tmpdir"))

    /**
     * Crée un fichier temporaire MP4 pour stocker l'enregistrement vidéo.
     *
     * @return Fichier vide prêt à recevoir les données vidéo.
     */
    fun createVideoFile(): File {
        val file = File(tempDir, "${PREFIX}video_${System.currentTimeMillis()}.mp4")
        logger.debug { "[TempFileManager] Fichier vidéo créé: ${file.absolutePath}" }
        return file
    }

    /**
     * Crée un fichier temporaire PNG pour stocker un screenshot.
     *
     * @return Fichier vide prêt à recevoir les données PNG.
     */
    fun createScreenshotFile(): File {
        val file = File(tempDir, "${PREFIX}screenshot_${System.currentTimeMillis()}.png")
        logger.debug { "[TempFileManager] Fichier screenshot créé: ${file.absolutePath}" }
        return file
    }

    /**
     * Supprime tous les fichiers temporaires Shotify de plus d'une heure.
     * À appeler au démarrage du plugin ou périodiquement.
     */
    fun cleanup() {
        val oneHourAgo = Instant.now().minusSeconds(3600).toEpochMilli()
        val deleted = tempDir.listFiles { file ->
            file.name.startsWith(PREFIX) && file.lastModified() < oneHourAgo
        }?.onEach { file ->
            if (file.delete()) {
                logger.debug { "[TempFileManager] Supprimé: ${file.name}" }
            }
        }?.size ?: 0

        if (deleted > 0) {
            logger.info { "[TempFileManager] $deleted fichier(s) temporaire(s) supprimé(s)" }
        }
    }
}
