package com.github.okafke.poryscriptidea

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.highlighter.HighlighterFactory
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
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
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import java.awt.KeyboardFocusManager
import java.awt.Point
import java.awt.event.MouseEvent
import java.nio.file.Files
import java.nio.file.Path
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
                    val baseDirs = project.getBaseDirectories().stream().toList()
                    val baseDirPaths = baseDirs.map { it.path }

                    val commandConfigDescriptor = FileChooserDescriptorFactory.singleFile()
                        .withRoots(baseDirs)
                        .withHideIgnored(true)
                        .withShowFileSystemRoots(false)
                        .withTitle("Select Command Config File")
                        .withDescription("Select or enter the path to command_config.json within your project.")

                    val tfwb = textFieldWithBrowseButton(commandConfigDescriptor, project) { file ->
                        val baseDir = project.basePath ?: return@textFieldWithBrowseButton file.path
                        val relative = try {
                            Path.of(baseDir).relativize(Path.of(file.path)).toString()
                        } catch (_: Exception) {
                            file.path
                        }
                        relative
                    }.align(AlignX.FILL)
                        .comment("The filepath for Poryscript's command config file (command_config.json). This is the file that defines the available autovar commands.")
                        .component

                    tfwb.text = project.basePath?.let { base ->
                        Path.of(base).relativize(Path.of(commandConfigFilepath)).toString()
                    } ?: commandConfigFilepath

                    tfwb.textField.onSwingTextChanged { relPath ->
                        val absPath = project.basePath?.let { base ->
                            if (relPath.isBlank()) "" else Path.of(base, relPath).normalize().toString()
                        } ?: ""
                        if (commandConfigFilepath != absPath) {
                            commandConfigFilepath = absPath
                            dialogPanel.apply()
                        }
                    }

                    ComponentValidator(this@PsConfigurable.disposable ?: Disposer.newDisposable("PsConfigurable-ComponentValidator"))
                        .installOn(tfwb.textField)
                        .andRegisterOnDocumentListener(tfwb.textField)
                        .withValidator {
                            val relPath = tfwb.text.trim()
                            if (relPath.isBlank()) return@withValidator null

                            val absPath = project.basePath?.let { base -> Path.of(base, relPath).normalize() }
                            if (absPath == null || !Files.exists(absPath)) {
                                ValidationInfo("The selected file does not exist.", tfwb.textField)
                            } else if (!absPath.extension.equals("json", ignoreCase = true)) {
                                ValidationInfo("Please select a .json file.", tfwb.textField)
                            } else if (baseDirPaths.none { absPath.startsWith(it) }) {
                                ValidationInfo("The file must be inside the project base directory.", tfwb.textField)
                            } else null
                        }
                }

                row("Command Includes") {
                    val area = textArea().rows(4).align(AlignX.FILL).component
                    area.text = commandIncludesString
                    area.onSwingTextChanged {
                        if (commandIncludesString != it) {
                            commandIncludesString = it
                            dialogPanel.apply()
                        }
                    }

                    comment("Macro files read by IntelliSense. One path per line.")
                }

                row("Symbol Includes") {
                    cell(createJsonEditor(symbolIncludesJson) { text ->
                        symbolIncludesJson = text
                        dialogPanel.apply()
                    }.withScrollPane())
                        .align(AlignX.FILL)
                        .resizableColumn()
                        .comment("JSON expressions defining additional symbol sources.")
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
