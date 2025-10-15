package com.github.okafke.poryscriptidea.lsp

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.features.semanticTokens.SemanticTokensColorsProvider

/**
 * The Highlighting from LSP4IJ Semantic Tokens from the Language Server
 * and the highlighting from TextMate conflict.
 * Semantic Token Highlighting together with the highlighting of TextMate leads to garbled text when typing.
 * Also Semantic Token Highlighting will override the highlighting of TextMate for
 * function declarations, so that all functions names are just plain white, which looks ugly.
 * For now the solution is to just turn off the Highlighting with this SemanticTokensColorsProvider.
 */
class PsSemanticTokenColorsProvider : SemanticTokensColorsProvider {
    // private val defaultSemanticTokenColorsProvider = DefaultSemanticTokensColorsProvider()

    override fun getTextAttributesKey(
        tokenType: String,
        tokenModifiers: List<String>,
        file: PsiFile
    ): TextAttributesKey? {
        // val key = defaultSemanticTokenColorsProvider.getTextAttributesKey(tokenType, tokenModifiers, file)
        return null
    }

}