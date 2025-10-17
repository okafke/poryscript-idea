package com.github.okafke.poryscriptidea.lsp

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.util.concurrent.Future
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class RehighlightService(private val project: Project) {

    private var future: Future<*>? = null

    fun start() {
        // Run every 2 seconds
        future = ApplicationManager.getApplication().executeOnPooledThread {
            while (!Thread.currentThread().isInterrupted) {
                rehighlightOpenEditors()
                Thread.sleep(2000)
            }
        }
    }

    fun stop() {
        future?.cancel(true)
    }

    private fun rehighlightOpenEditors() {
        if (project.isDisposed) return

        val fileEditorManager = FileEditorManager.getInstance(project)
        for (virtualFile in fileEditorManager.openFiles) {
            val psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(virtualFile) ?: continue

            // Request re-highlighting for this file:
            DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
        }
    }

}