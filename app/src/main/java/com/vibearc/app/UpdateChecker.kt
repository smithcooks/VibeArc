package com.vibearc.app

import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

internal data class AppUpdate(
    val version: String,
    val pageUrl: String,
    val apkUrl: String,
    val checksumUrl: String,
)

internal fun isNewerVersion(candidate: String, current: String): Boolean {
    fun parts(value: String) = value.removePrefix("v").substringBefore('-').split('.')
        .map { it.toIntOrNull() ?: 0 }.let { it + List((3 - it.size).coerceAtLeast(0)) { 0 } }
    val next = parts(candidate)
    val installed = parts(current)
    return (0 until maxOf(next.size, installed.size)).firstNotNullOfOrNull { index ->
        next.getOrElse(index) { 0 }.compareTo(installed.getOrElse(index) { 0 }).takeIf { it != 0 }
    }?.let { it > 0 } ?: false
}

internal fun parseAppUpdate(json: String): AppUpdate? = runCatching {
    val root = JsonParser.`object`().from(json)
    val version = root.getString("tag_name", "").removePrefix("v")
    val pageUrl = root.getString("html_url", "")
    val uri = URI(pageUrl)
    require(version.matches(Regex("\\d+(?:\\.\\d+){1,3}(?:[-+][A-Za-z0-9.-]+)?")))
    require(uri.scheme == "https" && uri.host == "github.com")
    require(uri.path.startsWith("/Akumukage/VibeArc/releases/tag/") && !uri.path.contains(".."))
    val assets = (root["assets"] as? List<*>).orEmpty().mapNotNull { it as? JsonObject }
    fun assetUrl(suffix: String): String = assets.firstNotNullOfOrNull { asset ->
        asset.getString("name", "").takeIf { it.endsWith(suffix, true) }
            ?.let { asset.getString("browser_download_url", "") }
            ?.takeIf(::isTrustedUpdateAssetUrl)
    } ?: error("Missing verified $suffix asset")
    val apkUrl = assetUrl(".apk")
    AppUpdate(version, pageUrl, apkUrl, assets.firstNotNullOfOrNull { asset ->
        val name = asset.getString("name", "")
        asset.getString("browser_download_url", "")
            .takeIf { name.equals(apkUrl.substringAfterLast('/') + ".sha256", true) }
            ?.takeIf(::isTrustedUpdateAssetUrl)
    } ?: error("Missing matching APK checksum"))
}.getOrNull()

private fun isTrustedUpdateAssetUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme == "https" && uri.host == "github.com" &&
        uri.path.startsWith("/Akumukage/VibeArc/releases/download/") && !uri.path.contains("..")
}.getOrDefault(false)

internal fun parseSha256(value: String): String? {
    val line = value.trim()
    if (!line.matches(Regex("(?i)^[0-9a-f]{64}(?:\\s+\\*?[^\\r\\n]+)?$"))) return null
    return line.take(64).lowercase()
}

internal object AppUpdateChecker {
    private const val LatestRelease = "https://api.github.com/repos/Akumukage/VibeArc/releases/latest"
    private const val MaxResponseBytes = 256 * 1024

    fun available(currentVersion: String): AppUpdate? {
        val connection = URL(LatestRelease).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "VibeArc/${BuildConfig.VERSION_NAME}")
            if (connection.responseCode !in 200..299) return null
            val update = connection.inputStream.use { parseAppUpdate(it.readUtf8Limited(MaxResponseBytes)) }
            return update?.takeIf { isNewerVersion(it.version, currentVersion) }
        } finally {
            connection.disconnect()
        }
    }
}
