package com.github.okafke.poryscriptidea.lsp

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.readBytes
import com.redhat.devtools.lsp4ij.client.IndexAwareLanguageClient
import okio.IOException
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
    private fun getWorkspaceRoot(): VirtualFile {
        return project.getBaseDirectories().stream().findFirst().orElseThrow { IOException("Project ${project.name} had no base directory!") }
    }

    override fun createSettings(): Any {
        val resourceStream = javaClass.classLoader.getResourceAsStream("poryscript-config.json")
            ?: throw IllegalStateException("Resource 'poryscript-config.json' not found in classpath")
        return InputStreamReader(resourceStream).use { reader ->
            Gson().fromJson(reader, JsonObject::class.java)
        }
    }

    override fun readfile(relativePath: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            runReadAction {
                val file = getWorkspaceRoot().findFileByRelativePath(relativePath)
                if (file == null || !file.exists()) {
                    throw IOException("Failed to find file $relativePath in project ${project.name}")
                }

                String(file.readBytes(), StandardCharsets.UTF_8)
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
                val result = mutableListOf<String>()
                VfsUtilCore.iterateChildrenRecursively(getWorkspaceRoot(), null) { file ->
                    if (file.isFile && file.extension.equals("pory", ignoreCase = true)) {
                        result.add(file.path)
                    }
                    true
                }

                result
            }
        }
    }

    override fun getFileUri(relativePath: String): CompletableFuture<String> {
        return CompletableFuture.supplyAsync {
            runReadAction {
                getWorkspaceRoot().findFileByRelativePath(relativePath)?.url
                    ?: throw IOException("Failed to to find relative path $relativePath in project ${project.name}")
            }
        }
    }

}