package com.github.okafke.poryscriptidea

import com.github.okafke.poryscriptidea.lsp.util.SymbolInclude
import com.github.okafke.poryscriptidea.lsp.util.findRelativeFile
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.intellij.execution.wsl.WslPath
import com.intellij.ide.highlighter.HighlighterFactory
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.jetbrains.rd.generator.nova.Lang
import com.redhat.devtools.lsp4ij.LanguageServerManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Supplier
import javax.swing.BorderFactory
import javax.swing.text.JTextComponent
import kotlin.io.path.extension
import javax.swing.event.DocumentEvent as SwingDocumentEvent
import javax.swing.event.DocumentListener as SwingDocumentListener

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
    private var poryscriptPlsPath = settings.poryscriptPlsPath

    private var symbolIncludesEditor: EditorEx? = null
    private var poryscriptPlsEditor: EditorEx? = null

    private lateinit var dialogPanel: DialogPanel

    override fun createPanel(): DialogPanel {
        dialogPanel = panel {
            group("Poryscript Settings") {
                row("Server Binary") {
                    val serverBinaryDescriptor = FileChooserDescriptorFactory.singleFile()
                        .withTitle("Select Poryscript Server Binary")
                        .withDescription("Select the optional poryscript-pls binary. This field may be left empty.")
                        .withFileFilter { true }

                    val tfwb = textFieldWithBrowseButton(serverBinaryDescriptor, project) { file: VirtualFile -> file.path }
                        .align(AlignX.FILL)
                        .comment("Optional. Uses the poryscript-pls binary at the specified path if provided.")
                        .component

                    tfwb.text = poryscriptPlsPath.orEmpty()
                    tfwb.textField.onSwingTextChanged {
                        val value = it.ifBlank { null }
                        if (poryscriptPlsPath != value) {
                            poryscriptPlsPath = value
                            dialogPanel.apply()
                        }
                    }
                }

                // ugh this is pretty ugly
                // what we want to achieve is a text field with a browse files button
                // that makes the text relative to the project base dir,
                // because it seems like the poryscript-pls takes the file relative from the project baseDir.
                row("Command Config") {
                    val commandConfigDescriptor = FileChooserDescriptorFactory.singleFile()
                        .withRoots(project.getBaseDirectories().stream().toList())
                        .withHideIgnored(true)
                        .withShowFileSystemRoots(true)
                        .withTitle("Select Command Config File")
                        .withDescription("Select or enter the path to command_config.json within your project.")

                    fun relativizePath(path: Path?): Path? {
                        for (baseDir in project.getBaseDirectories()) {
                            val baseDirPath = baseDir.fileSystem.getNioPath(baseDir)
                            if (baseDirPath != null && path?.startsWith(baseDirPath) == true) {
                                return baseDirPath.relativize(path)
                            }
                        }

                        return null
                    }

                    val tfwb = textFieldWithBrowseButton(commandConfigDescriptor, project) { file ->
                        val filePath = file.fileSystem.getNioPath(file)?.toAbsolutePath()
                        val path = relativizePath(filePath)
                        if (path != null) {
                            // if the file is in WSL then it might use windows file separators which is a problem
                            // when the LanguageServer tries to read the file relative to the project root
                            if (WslPath.isWslUncPath(filePath?.toAbsolutePath().toString())) {
                                return@textFieldWithBrowseButton path.toString().replace("\\", "/")
                            }

                            return@textFieldWithBrowseButton path.toString()
                        }

                        thisLogger().error("Failed to relativize $file base paths: ${project.getBaseDirectories()}")
                        return@textFieldWithBrowseButton file.path
                    }.align(AlignX.FILL)
                        .comment("The filepath for Poryscript's command config file (command_config.json). This is the file that defines the available autovar commands.")
                        .component

                    tfwb.text = commandConfigFilepath
                    tfwb.textField.onSwingTextChanged {
                        if (commandConfigFilepath != it) {
                            commandConfigFilepath = it
                            dialogPanel.apply()
                        }
                    }

                    val validator = ComponentValidator(this@PsConfigurable.disposable
                            ?: Disposer.newDisposable("PsConfigurable-CommandConfig-ComponentValidator"))
                        .installOn(tfwb.textField)
                        .andRegisterOnDocumentListener(tfwb.textField)
                        .withValidator {
                            val text = tfwb.text.trim()
                            if (text.isBlank()) {
                                return@withValidator ValidationInfo("Please provide a command_config.json file.", tfwb.textField)
                            }

                            val file = findRelativeFile(project, text) ?: return@withValidator ValidationInfo(
                                "The selected file does not exist or is not inside the project directory.",
                                tfwb.textField
                            )

                            if (!file.extension.equals("json", ignoreCase = true)) {
                                return@withValidator ValidationInfo("The file is not a json file.", tfwb.textField)
                            }

                            return@withValidator null
                        }

                    invokeLater {
                        validator.revalidate()
                    }
                }

                row("Command Includes") {
                    val area = textArea()
                        .rows(4).align(AlignX.FILL)
                        .comment("Macro files read by IntelliSense. One path per line.")
                        .component

                    area.text = commandIncludesString
                    area.onSwingTextChanged {
                        if (commandIncludesString != it) {
                            commandIncludesString = it
                            dialogPanel.apply()
                        }
                    }
                }

                row("Symbol Includes") {
                    val editor = createJsonEditor(symbolIncludesJson) { text ->
                        symbolIncludesJson = text
                        dialogPanel.apply()
                    }

                    val scroll = editor.withScrollPane()
                    val cell = cell(scroll)
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .comment("JSON expressions defining additional symbol sources.")

                    val validator = object : Supplier<ValidationInfo?> {
                        override fun get(): ValidationInfo? {
                            val text = editor.document.text.trim()
                            val symbolIncludeListType = object : TypeToken<List<SymbolInclude>>() {}.type
                            try {
                                Gson().fromJson<List<SymbolInclude>>(text, symbolIncludeListType)
                                return null
                            } catch (ex: JsonParseException) {
                                return ValidationInfo(ex.localizedMessage, cell.component)
                            }
                        }
                    }

                    cell.component.border = BorderFactory.createEmptyBorder()
                    val componentValidator = ComponentValidator(this@PsConfigurable.disposable
                            ?: Disposer.newDisposable("PsConfigurable-SymbolIncludes-ComponentValidator"))
                        .installOn(cell.component)
                        .withValidator(validator)

                    editor.document.addDocumentListener(object : DocumentListener {
                        override fun documentChanged(event: DocumentEvent) {
                            componentValidator.revalidate()
                            if (validator.get() != null) {
                                cell.component.border = BorderFactory.createLineBorder(JBColor.RED, 2)
                            } else {
                                cell.component.border = BorderFactory.createEmptyBorder()
                            }
                        }
                    }, this@PsConfigurable.disposable
                        ?: Disposer.newDisposable("PsConfigurable-SymbolIncludes-DocumentListener"))

                    invokeLater {
                        componentValidator.revalidate()
                    }
                }
            }
        }

        // this ensures that the json syntax highlighting happens when the panel is displayed
        invokeLater {
            symbolIncludesEditor?.contentComponent?.requestFocusInWindow()
        }

        return dialogPanel
    }

    override fun isModified(): Boolean {
        val s = settings
        return commandIncludesString.split('\n').map { it.trim() }.filter { it.isNotEmpty() } != s.commandIncludes ||
                symbolIncludesJson.trim() != s.symbolIncludesJson.trim() ||
                commandConfigFilepath != s.commandConfigFilepath ||
                (poryscriptPlsPath?.trim() ?: "") != (s.poryscriptPlsPath?.trim() ?: "")
    }

    override fun apply() {
        val s = settings
        s.commandIncludes = commandIncludesString.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        s.symbolIncludesJson = symbolIncludesEditor?.document?.text ?: symbolIncludesJson
        s.commandConfigFilepath = commandConfigFilepath
        s.poryscriptPlsPath = poryscriptPlsPath?.trim().takeIf { !it.isNullOrBlank() }

        // restart server with new configuration
        LanguageServerManager.getInstance(project).start("poryscript")
    }

    override fun reset() {
        val s = settings
        commandIncludesString = s.commandIncludes.joinToString("\n")
        symbolIncludesJson = s.symbolIncludesJson
        commandConfigFilepath = s.commandConfigFilepath
        poryscriptPlsPath = s.poryscriptPlsPath
        symbolIncludesEditor?.let { runWriteAction { it.document.setText(symbolIncludesJson) } }
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
        val document = FileDocumentManager.getInstance().getDocument(vFile)
            ?: EditorFactory.getInstance().createDocument(initialText)
        val editor = EditorFactory.getInstance().createEditor(document, project, fileType, false) as EditorEx

        val jsonFileType = FileTypeManager.getInstance().getFileTypeByExtension("json")
        editor.highlighter = HighlighterFactory.createHighlighter(project, jsonFileType)

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

        val listener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                onTextChanged(document.text)
            }
        }

        val disposable = this.disposable
        if (disposable == null) {
            document.addDocumentListener(listener)
        } else {
            document.addDocumentListener(listener, disposable)
        }

        if (initialText === symbolIncludesJson) symbolIncludesEditor = editor

        return editor
    }

    fun EditorEx.withScrollPane(heightPx: Int = 120): JBScrollPane {
        val pane = JBScrollPane(this.component)
        pane.preferredSize = java.awt.Dimension(0, heightPx)
        pane.minimumSize = java.awt.Dimension(0, 80)
        return pane
    }
}

fun JTextComponent.onSwingTextChanged(cb: (String) -> Unit) {
    this.document.addDocumentListener(object : SwingDocumentListener {
        override fun insertUpdate(e: SwingDocumentEvent?) = cb(this@onSwingTextChanged.text)
        override fun removeUpdate(e: SwingDocumentEvent?) = cb(this@onSwingTextChanged.text)
        override fun changedUpdate(e: SwingDocumentEvent?) = cb(this@onSwingTextChanged.text)
    })
}
