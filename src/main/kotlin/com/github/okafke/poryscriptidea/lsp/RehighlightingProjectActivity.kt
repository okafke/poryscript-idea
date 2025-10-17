package com.github.okafke.poryscriptidea.lsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class RehighlightProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.getService(RehighlightService::class.java).start()
    }

}