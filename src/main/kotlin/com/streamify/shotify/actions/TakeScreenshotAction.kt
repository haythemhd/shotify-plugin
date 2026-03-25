package com.streamify.shotify.actions

import com.android.ddmlib.AndroidDebugBridge
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.streamify.shotify.models.UploadResult
import com.streamify.shotify.recording.AdbScreenshot
import com.streamify.shotify.settings.UploadServiceSettings
import com.streamify.shotify.uploaders.UploadServiceFactory
import com.streamify.shotify.utils.ClipboardHelper
import com.streamify.shotify.utils.NotificationHelper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

/**
 * Action clavier pour capturer l'écran et uploader directement.
 * Raccourci : Ctrl+Shift+S
 *
 * Contrairement aux actions Record, celle-ci est self-contained :
 * elle capture, uploade et copie l'URL sans passer par le Tool Window.
 */
class TakeScreenshotAction : AnAction(
    "Take Screenshot & Upload",
    "Capturer l'écran Android et uploader",
    null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val device = AndroidDebugBridge.getBridge()
            ?.devices?.firstOrNull { it.isOnline }

        if (device == null) {
            NotificationHelper.notifyError(
                project,
                "Shotify",
                "Aucun device Android connecté. Vérifiez la connexion ADB."
            )
            return
        }

        val config = UploadServiceSettings.getInstance().toConfig()
        val uploader = UploadServiceFactory.createOrNull(config)

        if (uploader == null) {
            NotificationHelper.notifyError(
                project,
                "Shotify",
                "Service d'upload non configuré. Allez dans Settings > Tools > Shotify."
            )
            return
        }

        NotificationHelper.notifyInfo(project, "Capture en cours...")

        scope.launch {
            try {
                val file = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    AdbScreenshot(device).capture()
                }
                logger.info { "[TakeScreenshotAction] Fichier capturé: ${file.name}" }

                when (val result = uploader.upload(file)) {
                    is UploadResult.Success -> {
                        ClipboardHelper.copyToClipboard(result.url)
                        NotificationHelper.notifySuccess(
                            project,
                            "Screenshot prêt — URL copiée",
                            result.url
                        )
                        logger.info { "[TakeScreenshotAction] Upload réussi: ${result.url}" }
                    }
                    is UploadResult.Error -> {
                        NotificationHelper.notifyError(project, "Échec upload screenshot", result.message)
                        logger.error { "[TakeScreenshotAction] Échec: ${result.message}" }
                    }
                    UploadResult.Loading -> Unit
                }
            } catch (ex: Exception) {
                logger.error(ex) { "[TakeScreenshotAction] Exception inattendue" }
                NotificationHelper.notifyError(project, "Shotify", "Erreur: ${ex.message}")
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
