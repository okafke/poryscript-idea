package com.github.okafke.poryscriptidea.lsp.util

import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.orNull
import java.nio.file.Path

fun findRelativeFile(project: Project, relativePath: String): VirtualFile? {
    return project.getBaseDirectories()
        .stream()
        .map { it.findFileByRelativePath(relativePath) }
        .filter { it?.exists() == true }
        .findFirst()
        .orNull()
}

fun relativizePath(project: Project, path: Path?): Path? {
    for (baseDir in project.getBaseDirectories()) {
        val baseDirPath = baseDir.fileSystem.getNioPath(baseDir)
        if (baseDirPath != null && path?.startsWith(baseDirPath) == true) {
            return baseDirPath.relativize(path)
        }
    }

    return null
}
