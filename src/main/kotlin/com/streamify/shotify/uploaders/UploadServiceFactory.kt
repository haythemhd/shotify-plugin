package com.streamify.shotify.uploaders

import com.streamify.shotify.models.MediaUploader
import com.streamify.shotify.models.ServiceType
import com.streamify.shotify.models.UploadServiceConfig

object UploadServiceFactory {

    fun create(config: UploadServiceConfig): MediaUploader = when (config.serviceType) {
        ServiceType.CLOUDINARY -> {
            val cloudinaryConfig = requireNotNull(config.cloudinaryConfig) {
                "CloudinaryConfig is required for ServiceType.CLOUDINARY"
            }
            CloudinaryUploader(config = cloudinaryConfig)
        }
        ServiceType.CATBOX -> CatboxUploader()
    }

    fun createOrNull(config: UploadServiceConfig): MediaUploader? {
        if (!config.isValid()) return null
        return create(config)
    }
}
