package com.streamify.shotify.ui

import javax.swing.JPanel
import javax.swing.JProgressBar
import java.awt.BorderLayout

/**
 * Barre de progression indéterminée affichée pendant un upload.
 *
 * Se cache automatiquement quand [hide] est appelé.
 * Usage : intégrer [panel] dans le layout principal, puis appeler [show]/[hide].
 */
class UploadProgressIndicator {

    private val progressBar = JProgressBar().apply {
        isIndeterminate = true
        toolTipText = "Upload en cours..."
        string = "Upload en cours..."
        isStringPainted = true
    }

    /** Panel à insérer dans le layout parent. */
    val panel: JPanel = JPanel(BorderLayout()).apply {
        add(progressBar, BorderLayout.CENTER)
        isVisible = false
    }

    /** Affiche la barre de progression. Doit être appelé sur l'EDT. */
    fun show() {
        panel.isVisible = true
        progressBar.isIndeterminate = true
    }

    /** Cache la barre de progression. Doit être appelé sur l'EDT. */
    fun hide() {
        panel.isVisible = false
        progressBar.isIndeterminate = false
    }
}
