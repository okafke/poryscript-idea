package com.github.okafke.poryscriptidea.lsp.util

import java.nio.file.Path
import java.nio.file.Paths

fun getPlsBinaryName(): String? {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    return if (arch.contains("64") || arch.contains("86")) {
        when {
            os.contains("linux") -> "poryscript-pls-linux"
            os.contains("mac") -> "poryscript-pls-mac"
            os.contains("win") -> "poryscript-pls-windows.exe"
            else -> null
        }
    } else null
}

fun getInstallDir(): Path? {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("linux") || os.contains("mac") -> {
            val home = System.getProperty("user.home")
            home?.let { Paths.get(it, ".local", "bin") }
        }
        os.contains("win") -> {
            val localAppData = System.getenv("LOCALAPPDATA")
            localAppData?.let { Paths.get(it, "poryscript-pls") }
        }
        else -> null
    }
}

fun getPlsBinaryPath(): Path? {
    val binaryName = getPlsBinaryName() ?: return null
    val installDir = getInstallDir() ?: return null
    return installDir.resolve(binaryName)
}
