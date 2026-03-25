package com.streamify.shotify.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

/**
 * Entrée dans Settings > Tools > Shotify.
 *
 * Connecte [UploadServicePanel] (UI) à [UploadServiceSettings] (persistance).
 */
class UploadServiceConfigurable : Configurable {

    private var panel: UploadServicePanel? = null

    override fun getDisplayName(): String = "Shotify"

    override fun createComponent(): JComponent {
        val p = UploadServicePanel()
        panel = p
        reset() // initialise avec les valeurs persistées
        return p.rootPanel
    }

    /** Retourne true si l'utilisateur a modifié quelque chose depuis le dernier apply/reset. */
    override fun isModified(): Boolean {
        val settings = UploadServiceSettings.getInstance()
        val p = panel ?: return false
        return p.selectedService != settings.selectedService ||
               p.cloudNameField.text.trim() != settings.cloudName ||
               p.uploadPresetField.text.trim() != settings.uploadPreset
    }

    /** Sauvegarde les modifications dans [UploadServiceSettings]. */
    override fun apply() {
        val settings = UploadServiceSettings.getInstance()
        val p = panel ?: return
        settings.selectedService = p.selectedService
        settings.cloudName = p.cloudNameField.text.trim()
        settings.uploadPreset = p.uploadPresetField.text.trim()
    }

    /** Recharge les valeurs persistées dans le panneau. */
    override fun reset() {
        val settings = UploadServiceSettings.getInstance()
        val p = panel ?: return
        p.setSelectedService(settings.selectedService)
        p.cloudNameField.text = settings.cloudName
        p.uploadPresetField.text = settings.uploadPreset
    }

    override fun disposeUIResources() {
        panel = null
    }
}
