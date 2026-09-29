package com.vibearc.app

import com.grack.nanojson.JsonParser
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

internal data class AppUpdate(val version: String, val pageUrl: String)

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
    require(uri.path.startsWith("/Akumukage/VibeArc/releases/"))
    AppUpdate(version, pageUrl)
}.getOrNull()

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
