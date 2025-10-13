package com.github.okafke.poryscriptidea.lang

import com.intellij.lang.Language
import org.jetbrains.plugins.textmate.TextMateLanguage

object PsLanguage : Language(TextMateLanguage.LANGUAGE, "poryscript") {
    @Suppress("unused") // for serialization
    private fun readResolve(): Any = PsLanguage

}