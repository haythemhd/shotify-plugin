package com.streamify.shotify.utils

import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

/**
 * Utilitaire pour copier du texte dans le presse-papier système via l'API IntelliJ.
 */
object ClipboardHelper {

    /**
     * Copie le texte fourni dans le presse-papier système.
     * Utilise [CopyPasteManager] pour s'intégrer correctement avec l'IDE.
     *
     * @param text Texte à copier (typiquement une URL).
     */
    fun copyToClipboard(text: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }
}
