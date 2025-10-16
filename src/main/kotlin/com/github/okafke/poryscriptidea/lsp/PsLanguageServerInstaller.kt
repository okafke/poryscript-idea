package com.github.okafke.poryscriptidea.lsp

import com.github.okafke.poryscriptidea.lsp.util.*
import com.google.gson.Gson
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.installation.ServerInstallerBase
import org.intellij.markdown.html.URI
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.nio.file.Files
import java.nio.file.Path

class PsLanguageServerInstaller() : ServerInstallerBase() {
    override fun getProject(): Project? {
        return null
    }

    override fun checkServerInstalled(indictator: ProgressIndicator): Boolean {
        val path = getPlsBinaryPath()
        return path != null && Files.exists(path) && !Files.isDirectory(path)
    }

    override fun install(indicator: ProgressIndicator) {
        val binaryName = getPlsBinaryName()
            ?: throw IOException("Cannot install poryscript-pls, unsupported platform!")

        val installDir = getInstallDir()
            ?: throw IOException("Cannot install poryscript-pls, unable to find installation directory!")

        Files.createDirectories(installDir)
        val executable = installDir.resolve(binaryName)

        indicator.text = "Fetching available releases..."
        val releases = fetchAvailableReleases()

        val newest = getNewestRelease(REQUESTED_MAJOR_VERSION, releases)
            ?: throw IOException("Could not find poryscript-pls release with requested major version $REQUESTED_MAJOR_VERSION")

        val asset = newest.assets.find { it.name == binaryName }
            ?: throw IOException("Github Release does not contain asset for this platform ($binaryName)")

        indicator.text = "Downloading ${asset.name}"
        downloadFileWithProgress(asset.browserDownloadUrl, executable, indicator)
        executable.toFile().setExecutable(true)
    }

    companion object {
        private const val GITHUB_API = "https://api.github.com/repos/huderlem/poryscript-pls/releases"
        private const val REQUESTED_MAJOR_VERSION = "1"
    }

    private fun fetchAvailableReleases(): List<GithubRelease> {
        val conn = URI.create(GITHUB_API).toURL().openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        if (conn.responseCode >= 400) {
            throw IOException("Failed to fetch releases: HTTP ${conn.responseCode}")
        }

        conn.inputStream.use { stream ->
            val reader = InputStreamReader(stream)
            return Gson().fromJson(reader, Array<GithubRelease>::class.java).toList()
        }
    }

    private fun downloadFileWithProgress(url: String, dest: Path, indicator: ProgressIndicator) {
        val conn = URI.create(url).toURL().openConnection() as HttpURLConnection
        val total = conn.contentLengthLong
        require(total > 0) { "Invalid content length" }

        conn.inputStream.use { inc ->
            Files.newOutputStream(dest).use { out ->
                val buf = ByteArray(8192)
                var sum = 0L
                var read: Int
                indicator.isIndeterminate = false
                while (inc.read(buf).also { read = it } != -1) {
                    if (indicator.isCanceled) throw IOException("Download cancelled")
                    out.write(buf, 0, read)
                    sum += read
                    indicator.fraction = (sum.coerceAtMost(total).toDouble() / total)
                    indicator.text2 = "${sum / 1024} KB / ${total / 1024} KB"
                }
            }
        }
    }

}