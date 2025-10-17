package com.github.okafke.poryscriptidea.lsp

import com.github.okafke.poryscriptidea.PsSettings
import com.github.okafke.poryscriptidea.lsp.util.getPlsBinaryPath
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider
import java.nio.file.Files

class PsLanguageServer(project: Project) : OSProcessStreamConnectionProvider() {
    init {
        val settings = PsSettings.getInstance(project)
        var executablePath = settings.state.poryscriptPlsPath
        if (executablePath == null || executablePath.isBlank()) {
            val plsBinary = getPlsBinaryPath()
            if (plsBinary == null || !Files.exists(plsBinary) || Files.isDirectory(plsBinary)) {
                throw IllegalStateException("Failed to find poryscript-pls binary at path: $plsBinary")
            }

            executablePath = plsBinary.toAbsolutePath().toString()
        }

        val commandLine = GeneralCommandLine(executablePath)
        super.setCommandLine(commandLine)
    }

}