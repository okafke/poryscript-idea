package com.github.okafke.poryscriptidea.lang

import com.intellij.ide.actions.CreateFileAction

class PsCreateFileAction: CreateFileAction() {
    override fun getDefaultExtension(): String {
        return "pory"
    }

}