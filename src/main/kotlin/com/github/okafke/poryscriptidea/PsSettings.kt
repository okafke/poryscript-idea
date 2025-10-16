package com.github.okafke.poryscriptidea

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "PsSettings",
    storages = [Storage("poryscriptLanguageServer.xml")]
)
class PsSettings : PersistentStateComponent<PsSettings.State> {
    data class State(
        var commandIncludes: List<String> = listOf(
            "asm/macros/event.inc",
            "asm/macros/movement.inc"
        ),
        var symbolIncludesJson: String = defaultSymbolIncludesJson(),
        var commandConfigFilepath: String = "/tools/poryscript/command_config.json",
        var poryscriptPlsPath: String? = null
    )

    private var state = State()

    override fun getState(): State = state
    override fun loadState(newState: State) { state = newState }

    companion object {
        fun getInstance(project: Project): PsSettings = project.service<PsSettings>()

        fun defaultSymbolIncludesJson(): String {
            val inputStream = this::class.java.classLoader.getResourceAsStream("symbol-includes.json")
                ?: throw IllegalStateException("Cannot find symbol-includes.json in resources!")

            return inputStream.bufferedReader().readText()
        }
    }

}