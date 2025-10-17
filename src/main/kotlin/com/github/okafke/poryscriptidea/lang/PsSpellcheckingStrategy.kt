package com.github.okafke.poryscriptidea.lang

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.TokenConsumer
import com.intellij.spellchecker.tokenizer.Tokenizer
import java.util.regex.Pattern

/**
 * Since our language is TextMate based and we have not written any parsers
 * to parse poryscript into PsiElements all we get is the raw text of poryscript files.
 * Intellij's Spellchecker marks keywords such as trainerbattle_single as spelling mistakes.
 * But we only want Spellchecking inside Strings and comments,
 * not inside code, so we need this custom [SpellcheckingStrategy].
 */
class PsSpellcheckingStrategy : SpellcheckingStrategy() {
    // TODO: we actually would like to have spell checking for function names too?
    override fun getTokenizer(element: PsiElement): Tokenizer<*> {
        if (element !is PsiFile) {
            thisLogger().warn("Unexpected element type: ${element.javaClass}")
            return super.getTokenizer(element)
        }

        return object : Tokenizer<PsiElement>() {
            override fun tokenize(
                element: PsiElement,
                consumer: TokenConsumer
            ) {
                if (element is PsiFile) {
                    val text = element.text
                    val matcher = COMMENT_OR_STRING_PATTERN.matcher(text)
                    while (matcher.find()) {
                        val start: Int = matcher.start()
                        val end: Int = matcher.end()
                        val range = TextRange(start, end)
                        consumer.consumeToken(element, element.text, false, 0, range, PlainTextSplitter.getInstance())
                    }
                } else {
                    thisLogger().warn("Unexpected element type in Tokenizer: ${element.javaClass}")
                }
            }
        }
    }

    override fun isMyContext(element: PsiElement): Boolean {
        // because we have to register this for the textmate language we need to perform this check.
        return element is PsiFile
                && element.virtualFile.extension.equals("pory", ignoreCase = true)
    }

    @Suppress("CompanionObjectInExtension", "RegExpUnnecessaryNonCapturingGroup")
    companion object {
        // Strings in poryscript are in quotes ""
        // Comments in poryscript are single-line and start with # or //
        @JvmStatic
        private val COMMENT_OR_STRING_PATTERN: Pattern =
            Pattern.compile("(?://[^\\n]*|#[^\\n]*|\"([^\"]*)\")")
    }

}