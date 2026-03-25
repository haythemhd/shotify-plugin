package com.streamify.shotify.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.streamify.shotify.models.CloudinaryConfig
import com.streamify.shotify.models.ServiceType
import com.streamify.shotify.models.UploadServiceConfig

/**
 * Settings persistés au niveau application (partagés entre tous les projets).
 *
 * Stockés dans `shotify.xml` dans le répertoire de config de l'IDE.
 * Accessibles via [UploadServiceSettings.getInstance].
 */
@Service(Service.Level.APP)
@State(
    name = "ShotifySettings",
    storages = [Storage("shotify.xml")]
)
class UploadServiceSettings : PersistentStateComponent<UploadServiceSettings.State> {

    /**
     * Classe de données sérialisée par l'IDE.
     * Tous les champs doivent être var avec valeur par défaut.
     */
    data class State(
        var selectedService: String = ServiceType.CATBOX.name,
        var cloudName: String = "",
        var uploadPreset: String = ""
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    // ─── Accesseurs pratiques ─────────────────────────────────────────────────

    var selectedService: ServiceType
        get() = runCatching { ServiceType.valueOf(myState.selectedService) }
            .getOrDefault(ServiceType.CATBOX)
        set(value) { myState.selectedService = value.name }

    var cloudName: String
        get() = myState.cloudName
        set(value) { myState.cloudName = value }

    var uploadPreset: String
        get() = myState.uploadPreset
        set(value) { myState.uploadPreset = value }

    /**
     * Construit un [UploadServiceConfig] depuis l'état courant.
     */
    fun toConfig(): UploadServiceConfig = UploadServiceConfig(
        serviceType = selectedService,
        cloudinaryConfig = if (selectedService == ServiceType.CLOUDINARY) {
            CloudinaryConfig(cloudName = cloudName, uploadPreset = uploadPreset)
        } else null
    )

    companion object {
        /** Accès global depuis n'importe où dans le plugin. */
        fun getInstance(): UploadServiceSettings =
            ApplicationManager.getApplication().getService(UploadServiceSettings::class.java)
    }
}
