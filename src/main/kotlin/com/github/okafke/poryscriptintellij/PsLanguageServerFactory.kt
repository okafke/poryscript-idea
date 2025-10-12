package com.github.okafke.poryscriptintellij

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider


class PsLanguageServerFactory : LanguageServerFactory {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return PsLanguageServer(project)
    }

    override fun createLanguageClient(project: Project): LanguageClientImpl {
        return PsLanguageClient(project)
    }

}