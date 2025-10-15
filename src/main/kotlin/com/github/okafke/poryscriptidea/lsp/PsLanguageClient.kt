package com.github.okafke.poryscriptidea.lsp

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.redhat.devtools.lsp4ij.client.IndexAwareLanguageClient
import java.io.InputStreamReader
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import kotlin.io.path.readBytes

/**
 * Implementations of the [PsLanguageClientApi].
 * Provides the server with the `poryscript-config.json`.
 */
class PsLanguageClient(private val project: Project) : IndexAwareLanguageClient(project), PsLanguageClientApi {
    private fun getWorkspaceRoot(): VirtualFile? {
        return project.baseDir ?: project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
    }

    override fun createSettings(): Any {
        val resourceStream = javaClass.classLoader.getResourceAsStream("poryscript-config.json")
            ?: throw IllegalStateException("Resource 'poryscript-config.json' not found in classpath")
        return InputStreamReader(resourceStream).use { reader ->
            Gson().fromJson(reader, JsonObject::class.java)
        }
    }

    // TODO: cleanup dirty ports of the VS Code implementations of the Json Requests

    override fun readfile(file: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            runReadAction {
                val root = getWorkspaceRoot()
                val vfile = root?.findFileByRelativePath(file)
                if (vfile != null && vfile.exists()) {
                    val bytes = vfile.contentsToByteArray()
                    String(bytes, StandardCharsets.UTF_8)
                } else {
                    ""
                }
            }
        }
    }

    override fun readfs(file: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            runReadAction {
                Paths.get(URI.create(file))
                    .readBytes()
                    .toString(StandardCharsets.UTF_8)
            }
        }
    }

    override fun getPoryscriptFiles(): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            runReadAction {
                val root = getWorkspaceRoot() ?: return@runReadAction emptyList()
                val result = mutableListOf<String>()
                VfsUtilCore.iterateChildrenRecursively(root, null) { vfile ->
                    if (vfile.isFile && vfile.extension.equals("pory", ignoreCase = true)) {
                        result.add(vfile.path)
                    }
                    true
                }

                result
            }
        }
    }

    override fun getFileUri(file: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            runReadAction {
                val root = getWorkspaceRoot()
                val vfile = root?.findFileByRelativePath(file)
                vfile?.url ?: ""
            }
        }
    }

}