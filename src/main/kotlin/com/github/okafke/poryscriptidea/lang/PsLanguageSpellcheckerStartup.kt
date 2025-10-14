package com.github.okafke.poryscriptidea.lang

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.spellchecker.tokenizer.LanguageSpellchecking
import org.jetbrains.plugins.textmate.TextMateLanguage

class PsLanguageSpellcheckerStartup : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Add explicitly so it comes before XML-registered ones:
        LanguageSpellchecking.INSTANCE.addExplicitExtension(
            TextMateLanguage.LANGUAGE,      // Or TextMateLanguage.INSTANCE if needed
            PsSpellcheckingStrategy()
        )
    }

}