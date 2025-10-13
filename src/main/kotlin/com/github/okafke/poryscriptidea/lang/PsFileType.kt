package com.github.okafke.poryscriptidea.lang

import com.github.okafke.poryscriptidea.Icons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon


object PsFileType : LanguageFileType(PsLanguage) {
    override fun getName(): String {
        return "Poryscript"
    }

    override fun getDescription(): String {
        return "High-level scripting language for gen 3 pokemon decompilation projects"
    }

    override fun getDefaultExtension(): String {
        return "pory"
    }

    override fun getIcon(): Icon {
        return Icons.Icon
    }

}