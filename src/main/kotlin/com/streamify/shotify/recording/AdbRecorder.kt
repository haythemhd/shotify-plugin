package com.streamify.shotify.recording

import com.android.ddmlib.IDevice
import com.android.ddmlib.NullOutputReceiver
import com.streamify.shotify.utils.TempFileManager
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/** Chemin temporaire sur le device Android pour l'enregistrement. */
private const val REMOTE_FILE_PATH = "/sdcard/rc_record.mp4"

/**
 * Gère l'enregistrement vidéo d'écran via `adb screenrecord`.
 *
 * Cycle de vie typique :
 * 1. [startRecording] → lance `screenrecord` en arrière-plan
 * 2. [stopAndPull] → envoie SIGINT, attend finalisation, pull le fichier
 *
 * @param device Device Android connecté via ddmlib.
 */
class AdbRecorder(private val device: IDevice) {

    private var recordingJob: Job? = null
    private var isRecording = false

    /**
     * Lance l'enregistrement écran sur le device.
     * La commande tourne indéfiniment jusqu'à [stopAndPull].
     *
     * @param scope CoroutineScope dans lequel la commande ADB tourne.
     */
    fun startRecording(scope: CoroutineScope) {
        check(!isRecording) { "Un enregistrement est déjà en cours" }
        isRecording = true

        logger.info { "[AdbRecorder] Démarrage enregistrement sur ${device.serialNumber}" }

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                // Timeout 0L = illimité, bloque jusqu'à SIGINT
                device.executeShellCommand(
                    "screenrecord --bit-rate 4000000 $REMOTE_FILE_PATH",
                    NullOutputReceiver.getReceiver(),
                    0L,
                    TimeUnit.SECONDS
                )
            } catch (e: Exception) {
                // L'exception est normale lors du SIGINT — screenrecord se termine
                logger.debug { "[AdbRecorder] screenrecord terminé: ${e.message}" }
            }
        }
    }

    /**
     * Arrête l'enregistrement, attend la finalisation du fichier MP4,
     * puis pull le fichier vers le stockage local.
     *
     * @return Fichier MP4 local prêt à être uploadé.
     * @throws IllegalStateException Si aucun enregistrement n'est en cours.
     */
    suspend fun stopAndPull(): File = withContext(Dispatchers.IO) {
        check(isRecording) { "Aucun enregistrement en cours" }

        logger.info { "[AdbRecorder] Arrêt de l'enregistrement..." }

        // Envoie SIGINT pour arrêter proprement screenrecord et finaliser le MP4
        try {
            device.executeShellCommand(
                "killall -SIGINT screenrecord",
                NullOutputReceiver.getReceiver(),
                5L,
                TimeUnit.SECONDS
            )
        } catch (e: Exception) {
            logger.warn { "[AdbRecorder] Erreur lors du SIGINT: ${e.message}" }
        }

        // Attendre que screenrecord finalise l'écriture du fichier
        delay(1500)

        // Annuler la coroutine d'enregistrement
        recordingJob?.cancel()
        recordingJob = null
        isRecording = false

        // Pull le fichier depuis le device
        val localFile = TempFileManager.createVideoFile()
        logger.info { "[AdbRecorder] Pull vers: ${localFile.absolutePath}" }

        try {
            device.pullFile(REMOTE_FILE_PATH, localFile.absolutePath)
        } catch (e: Exception) {
            throw RuntimeException("Impossible de récupérer la vidéo: ${e.message}", e)
        } finally {
            // Nettoyer le fichier sur le device
            cleanupRemoteFile()
        }

        logger.info { "[AdbRecorder] Fichier récupéré: ${localFile.length()} bytes" }
        localFile
    }

    /** Supprime le fichier temporaire sur le device Android. */
    private fun cleanupRemoteFile() {
        try {
            device.executeShellCommand(
                "rm -f $REMOTE_FILE_PATH",
                NullOutputReceiver.getReceiver(),
                5L,
                TimeUnit.SECONDS
            )
            logger.debug { "[AdbRecorder] Fichier distant supprimé: $REMOTE_FILE_PATH" }
        } catch (e: Exception) {
            logger.warn { "[AdbRecorder] Impossible de supprimer $REMOTE_FILE_PATH: ${e.message}" }
        }
    }

    /** Retourne true si un enregistrement est actuellement actif. */
    fun isCurrentlyRecording(): Boolean = isRecording
}
