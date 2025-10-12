package com.github.okafke.poryscriptintellij

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider

class PsLanguageServer(project: Project): OSProcessStreamConnectionProvider() {
    init {
        val settings = PsSettingsState.getInstance(project)
        val executablePath = settings.languageServerPath
        if (executablePath.isBlank()) {
            throw IllegalArgumentException("Please specify language server path")
        }

        val commandLine = GeneralCommandLine(executablePath)
        super.setCommandLine(commandLine)
    }

}