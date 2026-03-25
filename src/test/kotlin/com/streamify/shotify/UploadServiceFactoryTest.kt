package com.streamify.shotify

import com.streamify.shotify.models.CloudinaryConfig
import com.streamify.shotify.models.ServiceType
import com.streamify.shotify.models.UploadServiceConfig
import com.streamify.shotify.uploaders.CatboxUploader
import com.streamify.shotify.uploaders.CloudinaryUploader
import com.streamify.shotify.uploaders.UploadServiceFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UploadServiceFactoryTest {

    // ─── create() ────────────────────────────────────────────────────────────

    @Test
    fun `create returns CloudinaryUploader for CLOUDINARY service type`() {
        val config = UploadServiceConfig(
            serviceType = ServiceType.CLOUDINARY,
            cloudinaryConfig = CloudinaryConfig("mycloud", "ml_default")
        )
        val uploader = UploadServiceFactory.create(config)
        assertInstanceOf(CloudinaryUploader::class.java, uploader)
        assertEquals("Cloudinary", uploader.serviceName)
    }

    @Test
    fun `create returns CatboxUploader for CATBOX service type`() {
        val config = UploadServiceConfig(serviceType = ServiceType.CATBOX)
        val uploader = UploadServiceFactory.create(config)
        assertInstanceOf(CatboxUploader::class.java, uploader)
        assertEquals("catbox.moe", uploader.serviceName)
    }

    @Test
    fun `create throws IllegalArgumentException when CLOUDINARY config is null`() {
        val config = UploadServiceConfig(
            serviceType = ServiceType.CLOUDINARY,
            cloudinaryConfig = null
        )
        assertThrows<IllegalArgumentException> {
            UploadServiceFactory.create(config)
        }
    }

    // ─── createOrNull() ───────────────────────────────────────────────────────

    @Test
    fun `createOrNull returns uploader for valid CLOUDINARY config`() {
        val config = UploadServiceConfig(
            serviceType = ServiceType.CLOUDINARY,
            cloudinaryConfig = CloudinaryConfig("cloud", "preset")
        )
        val uploader = UploadServiceFactory.createOrNull(config)
        assertNotNull(uploader)
        assertInstanceOf(CloudinaryUploader::class.java, uploader)
    }

    @Test
    fun `createOrNull returns null when CLOUDINARY cloudName is blank`() {
        val config = UploadServiceConfig(
            serviceType = ServiceType.CLOUDINARY,
            cloudinaryConfig = CloudinaryConfig(cloudName = "", uploadPreset = "preset")
        )
        assertNull(UploadServiceFactory.createOrNull(config))
    }

    @Test
    fun `createOrNull returns null when CLOUDINARY uploadPreset is blank`() {
        val config = UploadServiceConfig(
            serviceType = ServiceType.CLOUDINARY,
            cloudinaryConfig = CloudinaryConfig(cloudName = "cloud", uploadPreset = "")
        )
        assertNull(UploadServiceFactory.createOrNull(config))
    }

    @Test
    fun `createOrNull returns CatboxUploader without config`() {
        val config = UploadServiceConfig(serviceType = ServiceType.CATBOX)
        val uploader = UploadServiceFactory.createOrNull(config)
        assertNotNull(uploader)
        assertInstanceOf(CatboxUploader::class.java, uploader)
    }

    @Test
    fun `createOrNull returns null when CLOUDINARY config is null`() {
        val config = UploadServiceConfig(
            serviceType = ServiceType.CLOUDINARY,
            cloudinaryConfig = null
        )
        assertNull(UploadServiceFactory.createOrNull(config))
    }

    // ─── UploadServiceConfig.isValid() ───────────────────────────────────────

    @Test
    fun `isValid returns true for CATBOX without cloudinary config`() {
        val config = UploadServiceConfig(serviceType = ServiceType.CATBOX)
        assert(config.isValid())
    }

    @Test
    fun `isValid returns false for CLOUDINARY with null config`() {
        val config = UploadServiceConfig(
            serviceType = ServiceType.CLOUDINARY,
            cloudinaryConfig = null
        )
        assert(!config.isValid())
    }

    @Test
    fun `isValid returns true for CLOUDINARY with full config`() {
        val config = UploadServiceConfig(
            serviceType = ServiceType.CLOUDINARY,
            cloudinaryConfig = CloudinaryConfig("cloud", "preset")
        )
        assert(config.isValid())
    }
}
