package com.streamify.shotify.actions

import com.android.ddmlib.AndroidDebugBridge
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.streamify.shotify.utils.NotificationHelper

/**
 * Action clavier pour arrêter l'enregistrement et lancer l'upload.
 * Raccourci : Ctrl+Shift+T
 */
class StopRecordingAction : AnAction(
    "Stop Recording & Upload",
    "Arrêter l'enregistrement et uploader la vidéo",
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val device = AndroidDebugBridge.getBridge()
            ?.devices?.firstOrNull { it.isOnline }

        if (device == null) {
            NotificationHelper.notifyError(
                project,
                "Shotify",
                "Aucun device Android connecté."
            )
            return
        }

        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow("Shotify")
        toolWindow?.show()

        NotificationHelper.notifyInfo(
            project,
            "Utilisez le bouton ⏹ Stop & Upload dans le panneau Shotify."
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
