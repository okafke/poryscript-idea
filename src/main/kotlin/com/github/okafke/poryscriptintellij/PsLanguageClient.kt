package com.github.okafke.poryscriptintellij

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.redhat.devtools.lsp4ij.client.IndexAwareLanguageClient
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

/**
client.onRequest("poryscript/readfile", file => {
            let openPath = path.join(workspace.workspaceFolders[0].uri.fsPath, file);
            if (fs.existsSync(openPath)) {
                let uri = Uri.file(openPath);
                return workspace.openTextDocument(uri).then(doc => doc.getText());
            }
            return "";
        });
        client.onRequest("poryscript/readfs", file => {
            let openPath = Uri.parse(file).fsPath;
            if (fs.existsSync(openPath)) {
                let uri = Uri.file(openPath);
                return workspace.openTextDocument(uri).then(doc => doc.getText());
            }
        });
        client.onRequest("poryscript/getPoryscriptFiles", async () => {
            let folder = workspace.workspaceFolders[0];
            return await (await workspace.findFiles("**
/*.{pory}", null, 1024)).map(uri => uri.path);
});
client.onRequest("poryscript/getfileuri", file => {
    return url.pathToFileURL(path.join(workspace.workspaceFolders[0].uri.fsPath, file)).toString();
});
 */*/
class PsLanguageClient(private val project: Project) : IndexAwareLanguageClient(project), PsLanguageClientApi {
    private fun getWorkspaceRoot(): VirtualFile? {
        return project.baseDir ?: project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
    }

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
                val absoluteFile = File(file)
                val vfile = if (absoluteFile.isAbsolute) {
                    LocalFileSystem.getInstance().findFileByIoFile(absoluteFile)
                } else {
                    getWorkspaceRoot()?.findFileByRelativePath(file)
                }

                project.thisLogger().warn("Read files: $file $vfile")
                if (vfile != null && vfile.exists()) {
                    val bytes = vfile.contentsToByteArray()
                    String(bytes, StandardCharsets.UTF_8)
                } else {
                    project.thisLogger().warn("File null or empty: $vfile")
                    ""
                }
            }
        }
    }

    override fun getPoryscriptFiles(): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            runReadAction {
                val root = getWorkspaceRoot()
                if (root == null) return@runReadAction emptyList()
                val result = mutableListOf<String>()
                VfsUtilCore.iterateChildrenRecursively(root, null) { vfile ->
                    if (vfile.isFile && vfile.extension == "pory") {
                        result.add(vfile.path) // or vfile.url/uri if you need
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