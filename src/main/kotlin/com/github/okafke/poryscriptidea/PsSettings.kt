package com.github.okafke.poryscriptidea

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
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
        var commandConfigFilepath: String = "tools/poryscript/command_config.json",
        var traceServer: String = "off",
        var poryscriptPlsJson: String = "{}",
        var poryscriptPlsPath: String? = null
    )

    private var state = State()

    override fun getState(): State = state
    override fun loadState(newState: State) { state = newState }

    companion object {
        fun getInstance(project: Project): PsSettings = project.service<PsSettings>()

        fun defaultSymbolIncludesJson(): String = """
            [
                {"expression": "^\\s*def_special\\s+(\\w+)", "type": "special", "file": "data/specials.inc"},
                {"expression": "^\\s*#define\\s+(FLAG_\\w+)\\s+(.+)", "type": "define", "file": "include/constants/flags.h"},
                {"expression": "^\\s*#define\\s+(VAR_\\w+)\\s+(.+)", "type": "define", "file": "include/constants/vars.h"},
                {"expression": "^\\s*#define\\s+(ITEM_\\w+)\\s+(.+)", "type": "define", "file": "include/constants/items.h"},
                {"expression": "^\\s*#define\\s+(SE_\\w+)\\s+(.+)", "type": "define", "file": "include/constants/songs.h"},
                {"expression": "^\\s*#define\\s+(MUS_\\w+)\\s+(.+)", "type": "define", "file": "include/constants/songs.h"},
                {"expression": "^\\s*#define\\s+(MAP_SCRIPT_\\w+)\\s+(.+)", "type": "define", "file": "include/constants/map_scripts.h"}
            ]
        """.trimIndent()
    }

}