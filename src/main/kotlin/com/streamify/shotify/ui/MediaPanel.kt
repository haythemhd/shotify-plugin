package com.streamify.shotify.ui

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.streamify.shotify.models.ServiceType
import com.streamify.shotify.models.UploadResult
import com.streamify.shotify.recording.AdbRecorder
import com.streamify.shotify.recording.AdbScreenshot
import com.streamify.shotify.settings.UploadServiceSettings
import com.streamify.shotify.uploaders.UploadServiceFactory
import com.streamify.shotify.utils.AdbManager
import com.streamify.shotify.utils.ClipboardHelper
import com.streamify.shotify.utils.NotificationHelper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JRadioButton
import javax.swing.JTabbedPane
import javax.swing.Timer
import javax.swing.UIManager

private val logger = KotlinLogging.logger {}

class MediaPanel(private val project: Project) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recorder: AdbRecorder? = null

    // ─── Onglet Capture ───────────────────────────────────────────────────────

    private val deviceCombo = JComboBox<IDevice>().apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int,
                isSelected: Boolean, hasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, hasFocus)
                text = if (value is IDevice) {
                    val name = value.avdName?.takeIf { it.isNotBlank() } ?: value.serialNumber
                    val stateIcon = if (value.isOnline) "🟢" else "🔴"
                    "$stateIcon  $name  (${value.serialNumber})"
                } else "— aucun device —"
                return this
            }
        }
        preferredSize = Dimension(260, 28)
    }

    private val refreshBtn = JButton("⟳").apply {
        toolTipText = "Actualiser la liste des devices"
        preferredSize = Dimension(36, 28)
    }

    private val startRecordBtn = JButton("▶  Start Recording").apply {
        toolTipText = "Démarrer l'enregistrement écran (Ctrl+Shift+R)"
    }
    private val stopRecordBtn = JButton("⏹  Stop & Upload").apply {
        toolTipText = "Arrêter et uploader la vidéo (Ctrl+Shift+T)"
        isEnabled = false
    }
    private val screenshotBtn = JButton("📷  Screenshot").apply {
        toolTipText = "Capturer et uploader (Ctrl+Shift+S)"
    }

    private val progressBar = JProgressBar(0, 100).apply {
        isStringPainted = true
        string = "Prêt"
        preferredSize = Dimension(0, 24)
    }

    private val urlField = JBTextField().apply {
        isEditable = false
        emptyText.text = "L'URL apparaîtra ici après l'upload"
    }
    private val copyBtn = JButton("📋 Copier").apply {
        isEnabled = false
        preferredSize = Dimension(100, 28)
    }

    // ─── Onglet Config ────────────────────────────────────────────────────────

    private val radioZero = JRadioButton("catbox.moe  (anonyme, sans compte)")
    private val radioCloudinary = JRadioButton("Cloudinary  (cloud privé)")
    private val cloudNameField = JBTextField(20).apply { emptyText.text = "ex: myapp-dev" }
    private val presetField = JBTextField(20).apply { emptyText.text = "ex: ml_default" }
    private val cloudinaryFields = JPanel(GridBagLayout())
    private val saveBtn = JButton("💾  Sauvegarder")
    private val testConnectionBtn = JButton("🔌  Tester la connexion")
    private val saveStatusLabel = JBLabel("")

    // ─── Root ─────────────────────────────────────────────────────────────────

    val rootPanel: JPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
    }

    init {
        AdbManager.init()
        buildLayout()
        setupListeners()
        loadConfig()
        refreshDevices()
    }

    // ─── Construction UI ──────────────────────────────────────────────────────

    private fun buildLayout() {
        val tabs = JTabbedPane()
        tabs.addTab("📷  Capture", buildCaptureTab())
        tabs.addTab("⚙️  Config", buildConfigTab())
        rootPanel.add(tabs, BorderLayout.CENTER)
    }

    private fun buildCaptureTab(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            gridx = 0
            insets = Insets(4, 4, 4, 4)
        }

        // Ligne device
        val deviceRow = JPanel(BorderLayout(6, 0))
        deviceRow.add(JBLabel("Device :"), BorderLayout.WEST)
        deviceRow.add(deviceCombo, BorderLayout.CENTER)
        deviceRow.add(refreshBtn, BorderLayout.EAST)
        gbc.gridy = 0
        panel.add(deviceRow, gbc)

        // Ligne boutons
        val btnRow = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        btnRow.add(startRecordBtn)
        btnRow.add(stopRecordBtn)
        btnRow.add(screenshotBtn)
        gbc.gridy = 1
        panel.add(btnRow, gbc)

        // Barre de progression
        gbc.gridy = 2
        panel.add(progressBar, gbc)

        // Ligne URL
        val urlRow = JPanel(BorderLayout(6, 0))
        urlRow.add(JBLabel("URL :"), BorderLayout.WEST)
        urlRow.add(urlField, BorderLayout.CENTER)
        urlRow.add(copyBtn, BorderLayout.EAST)
        gbc.gridy = 3
        panel.add(urlRow, gbc)

        // Spacer
        gbc.gridy = 4
        gbc.weighty = 1.0
        panel.add(JPanel(), gbc)

        return panel
    }

    private fun buildConfigTab(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            gridx = 0
            insets = Insets(4, 4, 4, 4)
        }

        ButtonGroup().apply { add(radioZero); add(radioCloudinary) }

        // Champs Cloudinary
        cloudinaryFields.border = BorderFactory.createEmptyBorder(0, 24, 0, 0)
        val cgbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(3, 3, 3, 3)
        }
        cgbc.gridx = 0; cgbc.gridy = 0; cgbc.weightx = 0.0
        cloudinaryFields.add(JBLabel("Cloud name :"), cgbc)
        cgbc.gridx = 1; cgbc.weightx = 1.0
        cloudinaryFields.add(cloudNameField, cgbc)
        cgbc.gridx = 0; cgbc.gridy = 1; cgbc.weightx = 0.0
        cloudinaryFields.add(JBLabel("Upload preset :"), cgbc)
        cgbc.gridx = 1; cgbc.weightx = 1.0
        cloudinaryFields.add(presetField, cgbc)

        gbc.gridy = 0; panel.add(JBLabel("Service d'upload :"), gbc)
        gbc.gridy = 1; panel.add(radioZero, gbc)
        gbc.gridy = 2; panel.add(radioCloudinary, gbc)
        gbc.gridy = 3; panel.add(cloudinaryFields, gbc)
        gbc.gridy = 4; panel.add(saveBtn, gbc)
        gbc.gridy = 5; panel.add(testConnectionBtn, gbc)
        gbc.gridy = 6; panel.add(saveStatusLabel, gbc)

        // Spacer
        gbc.gridy = 7; gbc.weighty = 1.0; panel.add(JPanel(), gbc)

        return panel
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    private fun setupListeners() {
        refreshBtn.addActionListener { refreshDevices() }
        startRecordBtn.addActionListener { onStartRecording() }
        stopRecordBtn.addActionListener { onStopRecording() }
        screenshotBtn.addActionListener { onScreenshot() }

        copyBtn.addActionListener {
            val url = urlField.text.takeIf { it.isNotBlank() } ?: return@addActionListener
            ClipboardHelper.copyToClipboard(url)
            showSaveStatus(saveStatusLabel, "✓ URL copiée !")
        }

        radioCloudinary.addActionListener {
            cloudinaryFields.isVisible = true
            rootPanel.revalidate()
        }
        radioZero.addActionListener {
            cloudinaryFields.isVisible = false
            rootPanel.revalidate()
        }

        saveBtn.addActionListener { saveConfig() }
        testConnectionBtn.addActionListener { onTestConnection() }
    }

    // ─── Devices ──────────────────────────────────────────────────────────────

    private fun refreshDevices() {
        scope.launch {
            val devices = withContext(Dispatchers.IO) {
                AdbManager.init()   // réessaie si le bridge n'était pas encore prêt
                AdbManager.getDevices()
            }
            deviceCombo.removeAllItems()
            if (devices.isEmpty()) {
                setStatus("Aucun device détecté — vérifiez USB/ADB Debug activé", StatusType.IDLE)
            } else {
                devices.forEach { deviceCombo.addItem(it) }
                val online = devices.count { it.isOnline }
                setStatus("$online device(s) en ligne sur ${devices.size} détecté(s)", StatusType.IDLE)
            }
        }
    }

    private fun selectedDevice(): IDevice? = deviceCombo.selectedItem as? IDevice

    // ─── Actions ──────────────────────────────────────────────────────────────

    private fun onStartRecording() {
        val device = selectedDevice() ?: run {
            setStatus("Sélectionnez un device d'abord", StatusType.ERROR)
            return
        }
        recorder = AdbRecorder(device)
        recorder!!.startRecording(scope)
        setStatus("⏺  Enregistrement en cours...", StatusType.BUSY)
        startRecordBtn.isEnabled = false
        stopRecordBtn.isEnabled = true
        screenshotBtn.isEnabled = false
        logger.info { "[MediaPanel] Enregistrement démarré sur ${device.serialNumber}" }
    }

    private fun onStopRecording() {
        val rec = recorder ?: return
        val uploader = resolveUploader() ?: return
        stopRecordBtn.isEnabled = false
        setStatus("Arrêt + upload en cours...", StatusType.BUSY)

        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { rec.stopAndPull() }
                val result = uploader.upload(file)
                handleUploadResult(result)
            } catch (e: Exception) {
                logger.error(e) { "[MediaPanel] Erreur stop/upload" }
                setStatus("Erreur : ${e.message}", StatusType.ERROR)
                NotificationHelper.notifyError(project, "Shotify", "Erreur : ${e.message}")
            } finally {
                recorder = null
                startRecordBtn.isEnabled = true
                stopRecordBtn.isEnabled = false
                screenshotBtn.isEnabled = true
            }
        }
    }

    private fun onScreenshot() {
        val device = selectedDevice() ?: run {
            setStatus("Sélectionnez un device d'abord", StatusType.ERROR)
            return
        }
        val uploader = resolveUploader() ?: return
        screenshotBtn.isEnabled = false
        startRecordBtn.isEnabled = false
        setStatus("Capture en cours...", StatusType.BUSY)

        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { AdbScreenshot(device).capture() }
                setStatus("Upload en cours...", StatusType.BUSY)
                val result = uploader.upload(file)
                handleUploadResult(result)
            } catch (e: Exception) {
                logger.error(e) { "[MediaPanel] Erreur screenshot/upload" }
                setStatus("Erreur : ${e.message}", StatusType.ERROR)
                NotificationHelper.notifyError(project, "Shotify", "Erreur : ${e.message}")
            } finally {
                screenshotBtn.isEnabled = true
                startRecordBtn.isEnabled = true
            }
        }
    }

    private fun onTestConnection() {
        val cloudName = cloudNameField.text.trim()
        val preset = presetField.text.trim()
        if (cloudName.isBlank() || preset.isBlank()) {
            showSaveStatus(saveStatusLabel, "⚠ Remplis Cloud name et Upload preset d'abord")
            return
        }
        testConnectionBtn.isEnabled = false
        showSaveStatus(saveStatusLabel, "Test en cours...")

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    // Crée un PNG 1x1 valide via ImageIO
                    val testFile = java.io.File.createTempFile("rc_test_", ".png").apply {
                        deleteOnExit()
                        val img = java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                        javax.imageio.ImageIO.write(img, "png", this)
                    }
                    val config = com.streamify.shotify.models.CloudinaryConfig(
                        cloudName = cloudName,
                        uploadPreset = preset
                    )
                    com.streamify.shotify.uploaders.CloudinaryUploader(config).upload(testFile)
                        .also { testFile.delete() }
                } catch (e: Exception) {
                    com.streamify.shotify.models.UploadResult.Error(e.message ?: "Erreur inconnue")
                }
            }
            when (result) {
                is com.streamify.shotify.models.UploadResult.Success ->
                    showSaveStatus(saveStatusLabel, "✅ Connexion OK — preset valide !")
                is com.streamify.shotify.models.UploadResult.Error ->
                    showSaveStatus(saveStatusLabel, "❌ ${result.message}")
                else -> {}
            }
            testConnectionBtn.isEnabled = true
        }
    }

    // ─── Upload result ────────────────────────────────────────────────────────

    private fun handleUploadResult(result: UploadResult) {
        when (result) {
            is UploadResult.Success -> {
                setStatus("✓  Upload terminé via ${result.service} !", StatusType.SUCCESS)
                urlField.text = result.url
                copyBtn.isEnabled = true
                ClipboardHelper.copyToClipboard(result.url)
                NotificationHelper.notifySuccess(project, "Shotify — prêt", result.url)
                logger.info { "[MediaPanel] Upload réussi: ${result.url}" }
            }
            is UploadResult.Error -> {
                setStatus("✗  Erreur upload : ${result.message}", StatusType.ERROR)
                NotificationHelper.notifyError(project, "Échec upload", result.message)
                logger.error { "[MediaPanel] Échec upload: ${result.message}" }
            }
            UploadResult.Loading -> {}
        }
    }

    // ─── Barre de statut colorée ──────────────────────────────────────────────

    private enum class StatusType { IDLE, BUSY, SUCCESS, ERROR }

    private fun setStatus(text: String, type: StatusType) {
        progressBar.string = text
        when (type) {
            StatusType.BUSY -> {
                progressBar.isIndeterminate = true
                progressBar.foreground = UIManager.getColor("ProgressBar.foreground") ?: Color(0x1565C0)
            }
            StatusType.SUCCESS -> {
                progressBar.isIndeterminate = false
                progressBar.value = 100
                progressBar.foreground = Color(0x2E7D32)   // vert
            }
            StatusType.ERROR -> {
                progressBar.isIndeterminate = false
                progressBar.value = 100
                progressBar.foreground = Color(0xC62828)   // rouge
            }
            StatusType.IDLE -> {
                progressBar.isIndeterminate = false
                progressBar.value = 0
                progressBar.foreground = UIManager.getColor("ProgressBar.foreground") ?: Color.GRAY
            }
        }
    }

    // ─── Config ───────────────────────────────────────────────────────────────

    private fun loadConfig() {
        val s = UploadServiceSettings.getInstance()
        when (s.selectedService) {
            ServiceType.CLOUDINARY -> {
                radioCloudinary.isSelected = true
                cloudinaryFields.isVisible = true
                cloudNameField.text = s.cloudName
                presetField.text = s.uploadPreset
            }
            ServiceType.CATBOX -> {
                radioZero.isSelected = true
                cloudinaryFields.isVisible = false
            }
        }
    }

    private fun saveConfig() {
        val s = UploadServiceSettings.getInstance()
        s.selectedService = if (radioCloudinary.isSelected) ServiceType.CLOUDINARY else ServiceType.CATBOX
        s.cloudName = cloudNameField.text.trim()
        s.uploadPreset = presetField.text.trim()
        showSaveStatus(saveStatusLabel, "✓  Sauvegardé")
        logger.info { "[MediaPanel] Config sauvegardée: service=${s.selectedService}" }
    }

    private fun showSaveStatus(label: JBLabel, message: String) {
        label.text = message
        Timer(2500) { label.text = "" }.also { it.isRepeats = false; it.start() }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun resolveUploader(): com.streamify.shotify.models.MediaUploader? {
        val config = UploadServiceSettings.getInstance().toConfig()
        return UploadServiceFactory.createOrNull(config) ?: run {
            setStatus("Service non configuré — allez dans l'onglet Config", StatusType.ERROR)
            null
        }
    }

    fun dispose() = scope.cancel()
}
