package com.streamify.shotify.utils

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Gère l'accès au bridge ADB.
 *
 * Stratégie :
 * 1. Android Studio gère déjà son propre bridge → on le réutilise directement.
 * 2. Si aucun bridge n'existe (plugin lancé hors Android Studio) → on en crée un.
 *
 * Ne jamais appeler createBridge() si un bridge existe déjà : cela déconnecte
 * les devices détectés par Android Studio.
 */
object AdbManager {

    fun init() {
        // Android Studio a déjà initialisé le bridge → rien à faire
        if (AndroidDebugBridge.getBridge() != null) {
            logger.info { "[AdbManager] Bridge ADB existant détecté (Android Studio)" }
            return
        }

        // Initialisation standalone (ex: IntelliJ Community sans plugin Android)
        try {
            AndroidDebugBridge.init(false)
        } catch (_: IllegalStateException) {
            // Déjà initialisé — pas de problème
        }

        val adbPath = findAdbPath()
        if (adbPath == null) {
            logger.warn { "[AdbManager] Binaire adb introuvable. Définissez ANDROID_HOME." }
            return
        }

        logger.info { "[AdbManager] Création bridge ADB: $adbPath" }
        // forceNewBridge=false : réutilise le bridge s'il a été créé entre-temps
        AndroidDebugBridge.createBridge(adbPath, false, 30L, TimeUnit.SECONDS)
    }

    /**
     * Liste tous les devices détectés par ADB (en ligne ou hors ligne).
     * On n'exclut pas les devices OFFLINE pour que l'utilisateur voie ce qui est branché.
     */
    fun getDevices(): List<IDevice> {
        val bridge = AndroidDebugBridge.getBridge()
        if (bridge == null) {
            logger.warn { "[AdbManager] getBridge() = null" }
            return emptyList()
        }
        val devices = bridge.devices.toList()
        logger.info { "[AdbManager] ${devices.size} device(s): ${devices.map { "${it.serialNumber}/${it.state}" }}" }
        return devices
    }

    /**
     * Cherche le binaire `adb` dans :
     * 1. $ANDROID_HOME/platform-tools/adb
     * 2. $ANDROID_SDK_ROOT/platform-tools/adb
     * 3. ~/Library/Android/sdk/platform-tools/adb  (macOS par défaut)
     * 4. `which adb`
     */
    private fun findAdbPath(): String? {
        val candidates = mutableListOf<String>()

        listOf("ANDROID_HOME", "ANDROID_SDK_ROOT").forEach { env ->
            System.getenv(env)?.let { candidates.add("$it/platform-tools/adb") }
        }

        val home = System.getProperty("user.home")
        candidates.add("$home/Library/Android/sdk/platform-tools/adb")

        candidates.forEach { path ->
            if (File(path).exists()) {
                logger.info { "[AdbManager] ADB trouvé: $path" }
                return path
            }
        }

        return try {
            val proc = ProcessBuilder("which", "adb").start()
            proc.waitFor(3, TimeUnit.SECONDS)
            proc.inputStream.bufferedReader().readLine()?.trim()
                ?.takeIf { it.isNotBlank() && File(it).exists() }
                ?.also { logger.info { "[AdbManager] ADB via PATH: $it" } }
        } catch (e: Exception) {
            logger.warn { "[AdbManager] `which adb` échoué: ${e.message}" }
            null
        }
    }
}
