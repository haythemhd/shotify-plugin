package com.streamify.shotify.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * Centralise la création et l'affichage des notifications balloon dans l'IDE.
 *
 * Utilise le groupe "Shotify" déclaré dans plugin.xml.
 */
object NotificationHelper {

    private const val GROUP_ID = "Shotify"

    /**
     * Affiche une notification de succès avec l'URL générée.
     * Inclut un bouton "Copier l'URL" intégré dans la notification.
     *
     * @param project Projet courant.
     * @param title Titre de la notification.
     * @param url URL à afficher et copier.
     */
    fun notifySuccess(project: Project, title: String, url: String) {
        val content = "<a href='$url'>$url</a>"
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, NotificationType.INFORMATION)
            .addAction(object : com.intellij.openapi.actionSystem.AnAction("Copier l'URL") {
                override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                    ClipboardHelper.copyToClipboard(url)
                }
            })
            .addAction(object : com.intellij.openapi.actionSystem.AnAction("Ouvrir dans le navigateur") {
                override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
                    runCatching {
                        java.awt.Desktop.getDesktop().browse(java.net.URI(url))
                    }
                }
            })
            .notify(project)
    }

    /**
     * Affiche une notification d'erreur.
     *
     * @param project Projet courant.
     * @param title Titre de la notification.
     * @param message Message d'erreur détaillé.
     */
    fun notifyError(project: Project, title: String, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, message, NotificationType.ERROR)
            .notify(project)
    }

    /**
     * Affiche une notification informative simple.
     *
     * @param project Projet courant.
     * @param message Message à afficher.
     */
    fun notifyInfo(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }
}
