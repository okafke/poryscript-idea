package com.github.okafke.poryscriptidea.lsp

import com.github.okafke.poryscriptidea.PsSettings
import com.github.okafke.poryscriptidea.lsp.util.findRelativeFile
import com.github.okafke.poryscriptidea.lsp.util.relativizePath
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.readBytes
import com.redhat.devtools.lsp4ij.client.IndexAwareLanguageClient
import okio.IOException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import kotlin.io.path.readBytes

/**
 * Implementations of the [PsLanguageClientApi].
 * Also provides the server with the config defined by the settings.
 */
class PsLanguageClient(private val project: Project) : IndexAwareLanguageClient(project), PsLanguageClientApi {
    override fun createSettings(): Any {
        val settings = PsSettings.getInstance(project)
        val result = JsonObject()

        val languageServerPoryscript = JsonObject()
        val commandIncludes = JsonArray()
        settings.state.commandIncludes.forEach { commandIncludes.add(it) }
        languageServerPoryscript.add("commandIncludes", commandIncludes)
        val symbolIncludes = JsonParser.parseString(settings.state.symbolIncludesJson)
        languageServerPoryscript.add("symbolIncludes", symbolIncludes)
        languageServerPoryscript.addProperty("commandConfigFilepath", settings.state.commandConfigFilepath)

        result.add("languageServerPoryscript", languageServerPoryscript)
        return result
    }

    override fun readfile(relativePath: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            runReadAction {
                val file = findRelativeFile(project, relativePath)
                if (file == null || !file.exists()) {
                    throw IOException("Failed to find file $relativePath in project ${project.name}")
                }

                return@runReadAction getVirtualFileText(file)
                    ?: String(file.readBytes(), StandardCharsets.UTF_8)
            }
        }
    }

    override fun readfs(file: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            runReadAction {
                // pretty ugly solution but somehow this is needed on windows
                val normalized = if (file.startsWith("file://") && !file.startsWith("file:///")) {
                    file.replaceFirst("file://", "file:///")
                } else file

                val path = Paths.get(URI.create(normalized))

                val relativePath = relativizePath(project, path)
                if (relativePath != null) {
                    var pathString = relativePath.toString().replace('\\', '/')
                    if (!pathString.startsWith("/")) {
                        // otherwise findRelativeFile does not seem to work
                        pathString = "/$pathString"
                    }

                    val relFile = findRelativeFile(project, pathString)
                    if (relFile != null) {
                        val text = getVirtualFileText(relFile)
                        if (text != null) {
                            return@runReadAction text
                        }
                    }
                }

                return@runReadAction path.readBytes().toString(StandardCharsets.UTF_8)
            }
        }
    }

    override fun getPoryscriptFiles(): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            runReadAction {
                val result = mutableListOf<String>()
                for (baseDir in project.getBaseDirectories()) {
                    VfsUtilCore.iterateChildrenRecursively(baseDir, null) { file ->
                        if (file.isFile && file.extension.equals("pory", ignoreCase = true)) {
                            result.add(file.path)
                        }

                        true
                    }
                }

                result
            }
        }
    }

    override fun getFileUri(relativePath: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            runReadAction {
                findRelativeFile(project, relativePath)?.url
                    ?: throw IOException("Failed to to find relative path $relativePath in project ${project.name}")
            }
        }
    }

    private fun getVirtualFileText(virtualFile: VirtualFile): String? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor?.virtualFile == virtualFile) {
            return editor.document.text
        }

        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        if (document != null) {
            return document.text
        }

        return null
    }

}