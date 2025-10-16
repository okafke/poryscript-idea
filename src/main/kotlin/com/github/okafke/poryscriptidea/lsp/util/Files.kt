package com.github.okafke.poryscriptidea.lsp.util

import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.orNull

fun findRelativeFile(project: Project, relativePath: String): VirtualFile? {
    return project.getBaseDirectories()
        .stream()
        .map { it.findFileByRelativePath(relativePath) }
        .filter { it?.exists() == true }
        .findFirst()
        .orNull()

}
