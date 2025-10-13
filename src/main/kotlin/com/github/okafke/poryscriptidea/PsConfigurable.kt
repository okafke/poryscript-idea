package com.github.okafke.poryscriptidea

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.NamedConfigurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import com.redhat.devtools.lsp4ij.LanguageServersRegistry
import javax.swing.JComponent
import javax.swing.JPanel

class PsConfigurable(private val project: Project) : NamedConfigurable<PsSettingsState>() {
    private val panel = JPanel()
    private val pathField = TextFieldWithBrowseButton()

    init {
        panel.add(pathField)
        pathField.addBrowseFolderListener(
            null,
            FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Select Poryscript Language Server")
                .withDescription("Choose the executable path to the Poryscript language server.")
        )
    }

    override fun getDisplayName(): String = "Poryscript"

    override fun setDisplayName(name: @NlsSafe String) {

    }

    override fun getEditableObject(): PsSettingsState {
        return PsSettingsState.getInstance(project)
    }

    override fun getBannerSlogan(): @NlsContexts.DetailedDescription String {
        return "test"
    }

    override fun createOptionsPanel(): JComponent {
        return panel
    }

    override fun isModified(): Boolean {
        val settings = PsSettingsState.getInstance(project)
        return pathField.text != settings.languageServerPath
    }

    override fun apply() {
        val settings = PsSettingsState.getInstance(project)
        settings.languageServerPath = pathField.text
    }

    override fun reset() {
        val settings = PsSettingsState.getInstance(project)
        pathField.text = settings.languageServerPath
    }
}