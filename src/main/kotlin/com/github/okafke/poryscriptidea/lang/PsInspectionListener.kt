package com.github.okafke.poryscriptidea.lang

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DaemonListener
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.internal.statistic.InspectionUsageFUSCollector
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import javax.swing.text.Document


@Service(Service.Level.PROJECT)
class PsInspectionListener : DaemonListener {
    override fun daemonFinished(fileEditors: Collection<FileEditor>) {

        for (editor in fileEditors) {
            if (editor !is TextEditor) continue
            val document = editor.editor.document
            val project: Project = editor.editor.project ?: continue
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: continue

            val highlights: MutableList<HighlightInfo> = DaemonCodeAnalyzerImpl.getHighlights(document, null, project)
            if (highlights.isEmpty()) continue

            println("==== " + psiFile.getName() + " ====")
            for (info in highlights) {
                System.out.printf(
                    "[%s] %s (%d-%d)%n",
                    info.getSeverity(),
                    info.getDescription(),
                    info.getStartOffset(),
                    info.getEndOffset()
                )
            }
        }
    }

}