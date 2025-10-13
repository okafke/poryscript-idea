package com.github.okafke.poryscriptidea

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(name = "PsSettingsState", storages = [Storage("poryscript.xml")])
class PsSettingsState : PersistentStateComponent<PsSettingsState> {
    var languageServerPath: String = ""

    override fun getState() = this

    override fun loadState(state: PsSettingsState) {
        this.languageServerPath = state.languageServerPath
    }

    companion object {
        fun getInstance(project: Project): PsSettingsState = project.getService(PsSettingsState::class.java)
    }

}
