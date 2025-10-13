package com.github.okafke.poryscriptidea.lang

import com.github.okafke.poryscriptidea.Icons
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.textmate.TextMateBackedFileType
import org.jetbrains.plugins.textmate.TextMateFileType
import org.jetbrains.plugins.textmate.TextMateLanguage
import javax.swing.Icon


object PsFileType : LanguageFileType(TextMateLanguage.LANGUAGE), TextMateBackedFileType {
    override fun getName(): String {
        return "Poryscript"
    }

    override fun getDisplayName(): @Nls String {
        return name
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