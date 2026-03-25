package com.streamify.shotify.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.streamify.shotify.models.ServiceType
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.SwingUtilities

/**
 * Panneau Swing pour configurer le service d'upload dans les Settings IDE.
 *
 * Affiche :
 * - 2 radio buttons (Cloudinary / 0x0.st)
 * - Champs cloudName + uploadPreset (visibles uniquement si Cloudinary sélectionné)
 * - Bouton "Test Connection" pour valider la config Cloudinary
 */
class UploadServicePanel {

    // ─── Composants UI ────────────────────────────────────────────────────────

    private val radioCloudinary = JRadioButton("Cloudinary (recommandé)").apply {
        toolTipText = "Service gratuit jusqu'à 25 GB/mois. Nécessite un compte."
    }
    private val radioCatbox = JRadioButton("catbox.moe (anonyme, sans compte)").apply {
        toolTipText = "Aucun compte requis. Fichiers permanents, limite 200 MB."
    }

    val cloudNameField = JBTextField(20).apply {
        emptyText.text = "ex: myapp-dev"
    }
    val uploadPresetField = JBTextField(20).apply {
        emptyText.text = "ex: ml_default"
    }

    private val cloudinaryFieldsPanel = JPanel(GridBagLayout())
    private val testButton = JButton("Tester la connexion")
    private val testResultLabel = JBLabel("")

    /** Panneau racine à retourner dans [UploadServiceConfigurable.createComponent]. */
    val rootPanel: JPanel = JPanel(GridBagLayout())

    init {
        // Groupe radio boutons (un seul sélectionnable à la fois)
        ButtonGroup().apply {
            add(radioCloudinary)
            add(radioCatbox)
        }

        buildCloudinaryFieldsPanel()
        buildRootPanel()
        setupListeners()

        // État initial
        radioCatbox.isSelected = true
        updateCloudinaryVisibility()
    }

    // ─── Construction UI ──────────────────────────────────────────────────────

    private fun buildCloudinaryFieldsPanel() {
        cloudinaryFieldsPanel.border = BorderFactory.createEmptyBorder(4, 16, 4, 0)
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(4, 4, 4, 4)
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        cloudinaryFieldsPanel.add(JBLabel("Cloud name :"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        cloudinaryFieldsPanel.add(cloudNameField, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0
        cloudinaryFieldsPanel.add(JBLabel("Upload preset :"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        cloudinaryFieldsPanel.add(uploadPresetField, gbc)

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.0
        cloudinaryFieldsPanel.add(testButton, gbc)
        gbc.gridx = 1; gbc.gridy = 3
        cloudinaryFieldsPanel.add(testResultLabel, gbc)
    }

    private fun buildRootPanel() {
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.NORTHWEST
            insets = Insets(4, 4, 4, 4)
            gridx = 0; weightx = 1.0
        }

        gbc.gridy = 0
        rootPanel.add(JBLabel("Service d'upload :"), gbc)
        gbc.gridy = 1
        rootPanel.add(radioCloudinary, gbc)
        gbc.gridy = 2
        rootPanel.add(cloudinaryFieldsPanel, gbc)
        gbc.gridy = 3
        rootPanel.add(radioCatbox, gbc)

        // Pousse tout vers le haut
        gbc.gridy = 4; gbc.weighty = 1.0
        rootPanel.add(JPanel(), gbc)
    }

    private fun setupListeners() {
        radioCloudinary.addActionListener { updateCloudinaryVisibility() }
        radioCatbox.addActionListener { updateCloudinaryVisibility() }

        testButton.addActionListener {
            testResultLabel.text = "Test en cours..."
            testButton.isEnabled = false
            // Test dans un thread non-EDT pour ne pas bloquer l'UI
            Thread {
                val result = testCloudinaryConnection()
                SwingUtilities.invokeLater {
                    testResultLabel.text = result
                    testButton.isEnabled = true
                }
            }.start()
        }
    }

    private fun updateCloudinaryVisibility() {
        cloudinaryFieldsPanel.isVisible = radioCloudinary.isSelected
        rootPanel.revalidate()
        rootPanel.repaint()
    }

    /** Teste la connectivité Cloudinary avec les credentials saisis. */
    private fun testCloudinaryConnection(): String {
        val cloudName = cloudNameField.text.trim()
        val preset = uploadPresetField.text.trim()

        if (cloudName.isBlank() || preset.isBlank()) {
            return "⚠ Remplissez Cloud name et Upload preset."
        }

        return try {
            val url = java.net.URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.connect()
            val code = connection.responseCode
            connection.disconnect()
            if (code < 500) "✓ Connexion OK (HTTP $code)" else "✗ Erreur serveur (HTTP $code)"
        } catch (e: Exception) {
            "✗ Impossible de joindre Cloudinary: ${e.message}"
        }
    }

    // ─── Accesseurs pour Configurable ─────────────────────────────────────────

    /** Service actuellement sélectionné dans le panneau. */
    val selectedService: ServiceType
        get() = if (radioCloudinary.isSelected) ServiceType.CLOUDINARY else ServiceType.CATBOX

    /** Applique un [ServiceType] aux radio buttons. */
    fun setSelectedService(type: ServiceType) {
        when (type) {
            ServiceType.CLOUDINARY -> radioCloudinary.isSelected = true
            ServiceType.CATBOX     -> radioCatbox.isSelected = true
        }
        updateCloudinaryVisibility()
    }
}
