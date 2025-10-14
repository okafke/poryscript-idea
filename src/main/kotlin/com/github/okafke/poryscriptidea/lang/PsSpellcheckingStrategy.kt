package com.github.okafke.poryscriptidea.lang

import com.intellij.psi.PsiElement
import com.intellij.spellchecker.inspections.SpellCheckingInspection
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer

class PsSpellcheckingStrategy : SpellcheckingStrategy() {
    override fun getTokenizer(
        element: PsiElement?,
        scope: Set<SpellCheckingInspection.SpellCheckingScope?>?
    ): Tokenizer<*>? {
        print("Hu?!")
        return EMPTY_TOKENIZER
    }

    override fun getTokenizer(element: PsiElement): Tokenizer<*> {
        print("HI?!")
        val i = 0
        return super.getTokenizer(element)
    }

    override fun elementFitsScope(
        element: PsiElement,
        scope: Set<SpellCheckingInspection.SpellCheckingScope?>?
    ): Boolean {
        print("Ho!")
        return super.elementFitsScope(element, scope)
    }

}