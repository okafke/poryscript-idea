package com.github.okafke.poryscriptidea.lang

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState

// check Poryscript specification to understand if this works
class PsCompletionConfidence : CompletionConfidence() {
    override fun shouldSkipAutopopup(
        editor: Editor,
        contextElement: PsiElement,
        psiFile: PsiFile,
        offset: Int
    ): ThreeState {
        if (psiFile.virtualFile.extension.equals("pory", ignoreCase = true)
            && isInsideCommentOrString(editor, offset)) {
            return ThreeState.YES
        }

        return ThreeState.UNSURE
    }

    private fun isInsideCommentOrString(editor: Editor, offset: Int): Boolean {
        val document = editor.document
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val textBeforeCaret = document.getText(TextRange(lineStart, offset))

        val commentIndex = textBeforeCaret.indexOf("//")
        if (commentIndex != -1) return true

        var quoteCount = 0
        for (c in textBeforeCaret) if (c == '"') quoteCount++
        return quoteCount % 2 != 0
    }

}
