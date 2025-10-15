package com.github.okafke.poryscriptidea

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.*
import javax.swing.DefaultComboBoxModel

class PsConfigurable(
    private val project: Project
) : BoundSearchableConfigurable(
    "Poryscript",
    "Poryscript",
    "Settings.Poryscript"
) {
    private val settings
        get() = PsSettings.getInstance(project).state
    private var commandIncludesString = settings.commandIncludes.joinToString("\n")
    private var symbolIncludesJson = settings.symbolIncludesJson
    private var commandConfigFilepath = settings.commandConfigFilepath
    private var traceServer = settings.traceServer
    private var poryscriptPlsJson = settings.poryscriptPlsJson
    private var poryscriptPlsPath = settings.poryscriptPlsPath

    private var symbolIncludesEditor: EditorEx? = null
    private var poryscriptPlsEditor: EditorEx? = null

    override fun createPanel(): DialogPanel =
        panel {
            group("Poryscript Settings") {
                row("Command Includes") {
                    textArea()
                        .bindText(
                            { commandIncludesString },
                            { commandIncludesString = it }
                        )
                        .comment("Macro Files that should be read and handled by the IntelliSense of the language server. One path per line.")
                        .rows(4)
                        .align(AlignX.FILL)
                }

                row("Symbol Includes (JSON array)") {
                    cell(createJsonEditor(symbolIncludesJson) { text -> symbolIncludesJson = text }.withScrollPane())
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .comment("Files that are read as specified by `expression` to read additional symbol definitions.")
                }

                row("Command Config") {
                    textField()
                        .bindText(
                            { commandConfigFilepath },
                            { commandConfigFilepath = it }
                        )
                        .comment("The filepath for Poryscript's command config file (command_config.json).")
                        .align(AlignX.FILL)
                }

                row("Trace Server Output") {
                    comboBox(
                        DefaultComboBoxModel(arrayOf("off", "messages", "verbose"))
                    ).bindItem(
                        { traceServer },
                        { traceServer = it ?: "off" }
                    )
                        .comment("Traces the communication between the IDE and the language server.")
                }

                row("poryscript-pls Settings (JSON object)") {
                    cell(createJsonEditor(poryscriptPlsJson) { text -> poryscriptPlsJson = text }.withScrollPane())
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .comment("Settings passed down to poryscript-pls server (edit as JSON).")
                }

                row("poryscript-pls binary path (optional)") {
                    textField()
                        .bindText(
                            { poryscriptPlsPath.orEmpty() },
                            { poryscriptPlsPath = it.ifBlank { null } }
                        )
                        .comment("When specified, uses the poryscript-pls binary at a given path")
                        .align(AlignX.FILL)
                }
            }
        }

    override fun isModified(): Boolean {
        val s = settings
        return commandIncludesString.split('\n').map { it.trim() }.filter { it.isNotEmpty() } != s.commandIncludes ||
                symbolIncludesJson.trim() != s.symbolIncludesJson.trim() ||
                commandConfigFilepath != s.commandConfigFilepath ||
                traceServer != s.traceServer ||
                poryscriptPlsJson.trim() != s.poryscriptPlsJson.trim() ||
                (poryscriptPlsPath?.trim() ?: "") != (s.poryscriptPlsPath?.trim() ?: "")
    }

    override fun apply() {
        val s = settings
        s.commandIncludes = commandIncludesString.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        s.symbolIncludesJson = symbolIncludesEditor?.document?.text ?: symbolIncludesJson
        s.commandConfigFilepath = commandConfigFilepath
        s.traceServer = traceServer
        s.poryscriptPlsJson = poryscriptPlsEditor?.document?.text ?: poryscriptPlsJson
        s.poryscriptPlsPath = poryscriptPlsPath?.trim().takeIf { !it.isNullOrBlank() }
    }

    override fun reset() {
        val s = settings
        commandIncludesString = s.commandIncludes.joinToString("\n")
        symbolIncludesJson = s.symbolIncludesJson
        commandConfigFilepath = s.commandConfigFilepath
        traceServer = s.traceServer
        poryscriptPlsJson = s.poryscriptPlsJson
        poryscriptPlsPath = s.poryscriptPlsPath
        symbolIncludesEditor?.let { runWriteAction { it.document.setText(symbolIncludesJson) } }
        poryscriptPlsEditor?.let { runWriteAction { it.document.setText(poryscriptPlsJson) } }
    }

    override fun disposeUIResources() {
        symbolIncludesEditor?.let(EditorFactory.getInstance()::releaseEditor)
        symbolIncludesEditor = null
        poryscriptPlsEditor?.let(EditorFactory.getInstance()::releaseEditor)
        poryscriptPlsEditor = null
        super.disposeUIResources()
    }

    private fun createJsonEditor(initialText: String, onTextChanged: (String) -> Unit): EditorEx {
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension("json")
        val vFile = LightVirtualFile("PsConfigurable", fileType, initialText)

        val document = FileDocumentManager.getInstance().getDocument(vFile) ?: EditorFactory.getInstance().createDocument(initialText)
        val editor = EditorFactory.getInstance().createEditor(document, project, fileType, false) as EditorEx

        val jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json")
        editor.highlighter = com.intellij.ide.highlighter.HighlighterFactory.createHighlighter(
            project, jsonFileType
        )

        with(editor.settings) {
            isWhitespacesShown = false
            isLineNumbersShown = true
            isLineMarkerAreaShown = false
            isIndentGuidesShown = false
            isFoldingOutlineShown = false
            additionalColumnsCount = 1
            additionalLinesCount = 3
            isUseSoftWraps = true
        }
        editor.isViewer = false

        val documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                onTextChanged(document.text)
            }
        }

        val disposable = this.disposable
        if (disposable == null) {
            document.addDocumentListener(documentListener)
        } else {
            document.addDocumentListener(documentListener, disposable)
        }

        if (initialText === symbolIncludesJson) symbolIncludesEditor = editor
        if (initialText === poryscriptPlsJson) poryscriptPlsEditor = editor

        return editor
    }

    fun EditorEx.withScrollPane(heightPx: Int = 120): JBScrollPane {
        val pane = JBScrollPane(this.component)
        pane.preferredSize = java.awt.Dimension(0, heightPx)
        pane.minimumSize = java.awt.Dimension(0, 80)
        return pane
    }

}