package com.streamify.shotify.models

/**
 * Sealed class représentant les états possibles d'un upload.
 *
 * Utilisation typique :
 * ```kotlin
 * when (result) {
 *     is UploadResult.Success -> copyToClipboard(result.url)
 *     is UploadResult.Error   -> showError(result.message)
 *     UploadResult.Loading    -> showProgressBar()
 * }
 * ```
 */
sealed class UploadResult {

    /**
     * Upload terminé avec succès.
     * @param url URL publique du fichier uploadé.
     * @param service Nom du service utilisé (ex: "Cloudinary", "0x0.st").
     */
    data class Success(val url: String, val service: String) : UploadResult()

    /**
     * Upload échoué.
     * @param message Message d'erreur lisible par l'utilisateur.
     * @param cause Exception originale, nullable.
     */
    data class Error(val message: String, val cause: Exception? = null) : UploadResult()

    /** Upload en cours. */
    object Loading : UploadResult()
}
