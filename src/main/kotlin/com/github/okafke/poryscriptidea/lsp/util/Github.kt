package com.github.okafke.poryscriptidea.lsp.util

import com.google.gson.annotations.SerializedName


data class GithubRelease(
    @SerializedName("name") val name: String,
    @SerializedName("id") val id: Int,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("assets") val assets: List<GithubAsset>
)

data class GithubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String
)

// TODO: this was just a quick dirty port from SBird1337/poryscript-language but we should take the best major release?
fun getNewestRelease(majorVersion: String, releases: List<GithubRelease>): GithubRelease? {
    var found: GithubRelease? = null
    var bestMinor = -1
    var bestFix = -1
    for (r in releases) {
        val semantic = r.name.split('.')
        if (semantic.size != 3) continue
        if (semantic[0] != majorVersion) continue
        val currMinor = semantic[1].toIntOrNull() ?: continue
        val currFix = semantic[2].toIntOrNull() ?: continue
        if (currMinor > bestMinor || (currMinor == bestMinor && currFix > bestFix)) {
            found = r
            bestMinor = currMinor
            bestFix = currFix
        }
    }

    return found
}
