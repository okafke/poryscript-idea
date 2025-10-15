package com.github.okafke.poryscriptidea.lang

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.spellchecker.tokenizer.LanguageSpellchecking
import org.jetbrains.plugins.textmate.TextMateLanguage

/**
 * There is a TextMate SpellcheckingStrategy that is installed before ours.
 * That leads to spellchecking marking everything inside poryscript files,
 * even keywords and code.
 * But we only want Spellchecking inside Strings and comments.
 */
class PsLanguageSpellcheckerStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        LanguageSpellchecking.INSTANCE.addExplicitExtension(
            TextMateLanguage.LANGUAGE,
            PsSpellcheckingStrategy()
        )
    }

}