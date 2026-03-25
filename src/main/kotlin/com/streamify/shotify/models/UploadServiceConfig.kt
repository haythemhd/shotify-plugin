package com.streamify.shotify.models

/** Supported upload service types. */
enum class ServiceType {
    CLOUDINARY,
    CATBOX
}

/**
 * Configuration spécifique à Cloudinary.
 *
 * @param cloudName Nom du cloud Cloudinary (ex: "myapp").
 * @param uploadPreset Preset d'upload non signé configuré dans la console Cloudinary.
 */
data class CloudinaryConfig(
    val cloudName: String,
    val uploadPreset: String
)

/**
 * Configuration globale du service d'upload sélectionné.
 *
 * @param serviceType Service actif.
 * @param cloudinaryConfig Configuration Cloudinary, obligatoire si [serviceType] = CLOUDINARY.
 */
data class UploadServiceConfig(
    val serviceType: ServiceType,
    val cloudinaryConfig: CloudinaryConfig? = null
) {
    /** Vérifie que la configuration est valide pour le service sélectionné. */
    fun isValid(): Boolean = when (serviceType) {
        ServiceType.CLOUDINARY ->
            cloudinaryConfig != null &&
            cloudinaryConfig.cloudName.isNotBlank() &&
            cloudinaryConfig.uploadPreset.isNotBlank()
        ServiceType.CATBOX -> true // no config required
    }
}
