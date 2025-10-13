package com.github.okafke.poryscriptidea.textmate

import com.intellij.openapi.application.PathManager
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider.PluginBundle
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path


class PsTextMateBundleProvider: TextMateBundleProvider {
    override fun getBundles(): List<PluginBundle> {
        try {
            val bundleTempDir = Files.createTempDirectory(Path.of(PathManager.getTempPath()), "textmate-poryscript")
            for (fileToCopy in listOf(
                "package.json",
                "language-configuration.json",
                "syntaxes/poryscript.tmLanguage.json",
                "syntaxes/poryscript-asm.tmLanguage.json"
            )) {
                val resource: URL = PsTextMateBundleProvider::class.java.getClassLoader()
                    .getResource("textmate/poryscript/$fileToCopy")!!

                resource.openStream().use { resourceStream ->
                    val target = bundleTempDir.resolve(fileToCopy)
                    Files.createDirectories(target.parent)
                    Files.copy(resourceStream, target)
                }
            }

            return listOf(PluginBundle("poryscript", bundleTempDir!!))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

}