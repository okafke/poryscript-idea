package com.github.okafke.poryscriptidea.lsp

import com.github.okafke.poryscriptidea.PsSettings
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider

// TODO: Language server installer
class PsLanguageServer(project: Project) : OSProcessStreamConnectionProvider() {
    init {
        val settings = PsSettings.getInstance(project)
        val executablePath = settings.state.poryscriptPlsPath
        if (executablePath == null || executablePath.isBlank()) {
            throw IllegalArgumentException("Please specify language server path")
        }

        val commandLine = GeneralCommandLine(executablePath)
        super.setCommandLine(commandLine)
    }

}