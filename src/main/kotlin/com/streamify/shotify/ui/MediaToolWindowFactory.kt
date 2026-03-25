package com.streamify.shotify.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory qui crée le contenu du Tool Window "Shotify".
 *
 * Déclaré dans plugin.xml avec :
 * ```xml
 * <toolWindow id="Shotify" anchor="bottom"
 *             factoryClass="...MediaToolWindowFactory"/>
 * ```
 */
class MediaToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mediaPanel = MediaPanel(project)

        val content = ContentFactory.getInstance()
            .createContent(mediaPanel.rootPanel, "", false)

        toolWindow.contentManager.addContent(content)

        // Libérer les ressources quand le tool window est détruit
        toolWindow.contentManager.addContentManagerListener(
            object : com.intellij.ui.content.ContentManagerListener {
                override fun contentRemoved(event: com.intellij.ui.content.ContentManagerEvent) {
                    mediaPanel.dispose()
                }
            }
        )
    }

    /** Le Tool Window est disponible pour tous les types de projets. */
    override fun shouldBeAvailable(project: Project): Boolean = true
}
