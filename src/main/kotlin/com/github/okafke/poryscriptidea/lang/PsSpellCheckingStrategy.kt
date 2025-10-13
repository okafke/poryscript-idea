package com.github.okafke.poryscriptidea.lang

import com.intellij.psi.PsiElement
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer

class PsSpellCheckingStrategy : SpellcheckingStrategy() {
    override fun getTokenizer(element: PsiElement): Tokenizer<*> {
        print("HI?!")
        val i = 0
        return super.getTokenizer(element)
    }

}