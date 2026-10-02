package com.vibearc.app

import android.webkit.CookieManager
import java.net.URI
import java.security.MessageDigest

private const val YouTubeMusicOrigin = "https://music.youtube.com"

internal fun parseCookieHeader(value: String): Map<String, String> = buildMap {
    value.split(';').forEach { part ->
        val fields = part.trim().split('=', limit = 2)
        if (fields.size == 2 && fields[0].isNotBlank()) put(fields[0], fields[1])
    }
}

internal fun youtubeSessionAuthorization(
    cookies: Map<String, String>,
    timestampSeconds: Long = System.currentTimeMillis() / 1_000,
): String? {
    val sapisid = listOf("__Secure-3PAPISID", "SAPISID", "APISID")
        .firstNotNullOfOrNull { cookies[it]?.takeIf(String::isNotBlank) } ?: return null
    val input = "$timestampSeconds $sapisid $YouTubeMusicOrigin"
    val hash = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return "SAPISIDHASH ${timestampSeconds}_$hash"
}

internal fun isTrustedYouTubeLoginUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    val host = uri.host?.lowercase() ?: return@runCatching false
    uri.scheme == "https" && listOf(
        "google.com",
        "youtube.com",
        "googleusercontent.com",
        "gstatic.com",
        "ytimg.com",
    ).any { host == it || host.endsWith(".$it") }
}.getOrDefault(false)

internal object YouTubeWebSession {
    private val cookieManager get() = CookieManager.getInstance()

    fun cookies(): Map<String, String> = buildMap {
        listOf("https://music.youtube.com", "https://www.youtube.com").forEach { origin ->
            parseCookieHeader(cookieManager.getCookie(origin).orEmpty()).forEach { (name, value) ->
                putIfAbsent(name, value)
            }
        }
    }

    fun cookieHeader(): String? = cookies().takeIf(Map<String, String>::isNotEmpty)
        ?.entries?.joinToString("; ") { (name, value) -> "$name=$value" }

    fun authorization(): String? = youtubeSessionAuthorization(cookies())

    fun isAuthenticated(): Boolean = authorization() != null

    fun clear(onCleared: () -> Unit = {}) {
        cookieManager.removeAllCookies { cookieManager.flush(); onCleared() }
    }
}
