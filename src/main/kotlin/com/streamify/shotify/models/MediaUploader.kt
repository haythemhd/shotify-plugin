package com.streamify.shotify.models

import java.io.File

/**
 * Interface Strategy pour les services d'upload.
 *
 * Chaque implémentation gère un service spécifique (Cloudinary, 0x0.st, etc.).
 * L'upload doit toujours s'exécuter sur [kotlinx.coroutines.Dispatchers.IO].
 */
interface MediaUploader {

    /** Nom affiché du service, ex: "Cloudinary" ou "0x0.st". */
    val serviceName: String

    /**
     * Indique si le service est correctement configuré et prêt à être utilisé.
     * Retourne false si des credentials obligatoires sont manquants.
     */
    val isConfigured: Boolean

    /**
     * Upload un fichier vers le service distant.
     *
     * @param file Fichier local à uploader (MP4 ou PNG).
     * @return [UploadResult.Success] avec l'URL publique, ou [UploadResult.Error].
     */
    suspend fun upload(file: File): UploadResult
}
