package com.github.okafke.poryscriptidea.lsp

import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile


class PostVisitorSyntaxRehighlighterPass(
    project: Project,
    private val file: PsiFile,
    private val editor: Editor
) : TextEditorHighlightingPass(project, editor.document, false) {
    override fun doCollectInformation(indicator: ProgressIndicator) {}

    override fun doApplyInformationToEditor() {
        val project = file.project
        val vFile = file.virtualFile ?: return

        val language = file.language
        val syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, vFile) ?: return

        val markupModel = editor.markupModel
        markupModel.removeAllHighlighters()

        val text = editor.document.charsSequence
        val lexer = syntaxHighlighter.highlightingLexer
        lexer.start(text)

        while (lexer.tokenType != null) {
            var attributes: TextAttributes? = null
            val keys = syntaxHighlighter.getTokenHighlights(lexer.tokenType)
            if (keys.size > 0 && keys[0] != null) {
                attributes = editor.colorsScheme.getAttributes(keys[0])
            }

            if (attributes != null) {
                markupModel.addRangeHighlighter(
                    lexer.tokenStart,
                    lexer.tokenEnd,
                    Pass.UPDATE_ALL,
                    attributes,
                    HighlighterTargetArea.EXACT_RANGE
                )
            }

            lexer.advance()
        }
    }

}