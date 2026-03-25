package com.streamify.shotify.actions

import com.android.ddmlib.AndroidDebugBridge
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.streamify.shotify.utils.NotificationHelper

/**
 * Action clavier pour démarrer l'enregistrement écran.
 * Raccourci : Ctrl+Shift+R
 *
 * Ouvre le Tool Window Shotify et déclenche le recording
 * si un device est connecté.
 */
class StartRecordingAction : AnAction(
    "Start Recording",
    "Démarrer l'enregistrement écran Android",
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
                "Aucun device Android connecté. Vérifiez la connexion ADB."
            )
            return
        }

        // Ouvrir et afficher le Tool Window
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow("Shotify")
        toolWindow?.show()

        NotificationHelper.notifyInfo(
            project,
            "Utilisez le bouton ▶ Start Recording dans le panneau Shotify."
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
