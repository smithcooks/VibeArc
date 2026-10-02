package com.vibearc.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

internal enum class InstallLaunchResult { Started, PermissionRequired }

internal object VerifiedUpdateInstaller {
    private const val MaxApkBytes = 300L * 1024 * 1024
    private const val MaxChecksumBytes = 4L * 1024
    private const val MaxRedirects = 5

    fun downloadAndVerify(context: Context, update: AppUpdate): File {
        val expected = parseSha256(downloadBytes(update.checksumUrl, MaxChecksumBytes).decodeToString())
            ?: error("The release checksum is invalid")
        val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }.canonicalFile
        val target = File(updateDir, "VibeArc-${update.version}.apk").canonicalFile
        require(target.parentFile == updateDir) { "Invalid update path" }
        target.delete()
        try {
            downloadFile(update.apkUrl, target, MaxApkBytes)
            val actual = target.inputStream().buffered().use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
                digest.digest()
            }
            require(MessageDigest.isEqual(actual, expected.hexBytes())) { "The APK checksum does not match" }
            verifyPackage(context, target)
            return target
        } catch (failure: Throwable) {
            target.delete()
            throw failure
        }
    }

    fun launchInstaller(context: Context, apk: File): InstallLaunchResult {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            return InstallLaunchResult.PermissionRequired
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return InstallLaunchResult.Started
    }

    private fun downloadBytes(url: String, maxBytes: Long): ByteArray {
        val result = java.io.ByteArrayOutputStream()
        copyResponse(url, maxBytes) { bytes, count -> result.write(bytes, 0, count) }
        return result.toByteArray()
    }

    private fun downloadFile(url: String, target: File, maxBytes: Long) {
        target.outputStream().buffered().use { output ->
            copyResponse(url, maxBytes) { bytes, count -> output.write(bytes, 0, count) }
        }
    }

    private fun copyResponse(url: String, maxBytes: Long, write: (ByteArray, Int) -> Unit) {
        var current = URI(url)
        repeat(MaxRedirects + 1) { redirect ->
            require(isTrustedDownloadUri(current)) { "Untrusted update download URL" }
            val connection = current.toURL().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 10_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("Accept", "application/octet-stream")
                connection.setRequestProperty("User-Agent", "VibeArc/${BuildConfig.VERSION_NAME}")
                if (connection.responseCode in 300..399) {
                    require(redirect < MaxRedirects) { "Too many update redirects" }
                    current = current.resolve(connection.getHeaderField("Location") ?: error("Missing redirect location"))
                    return@repeat
                }
                require(connection.responseCode in 200..299) { "Update download failed (${connection.responseCode})" }
                val length = connection.contentLengthLong
                require(length < 0 || length <= maxBytes) { "Update file is too large" }
                var total = 0L
                connection.inputStream.buffered().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= maxBytes) { "Update file is too large" }
                        write(buffer, count)
                    }
                }
                return
            } finally {
                connection.disconnect()
            }
        }
        error("Update download failed")
    }

    private fun isTrustedDownloadUri(uri: URI): Boolean {
        if (uri.scheme != "https" || uri.userInfo != null || uri.port !in listOf(-1, 443)) return false
        return when (uri.host?.lowercase()) {
            "github.com" -> uri.path.startsWith("/$UpdateRepository/releases/download/")
            "objects.githubusercontent.com", "release-assets.githubusercontent.com" -> true
            else -> false
        } && !uri.path.contains("..")
    }

    @Suppress("DEPRECATION")
    private fun verifyPackage(context: Context, apk: File) {
        val manager = context.packageManager
        val archive = manager.getPackageArchiveInfo(
            apk.path,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES,
        ) ?: error("The downloaded file is not an Android package")
        require(archive.packageName == context.packageName) { "The APK belongs to another app" }
        val installed = manager.getPackageInfo(
            context.packageName,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES,
        )
        val nextVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) archive.longVersionCode else archive.versionCode.toLong()
        val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) installed.longVersionCode else installed.versionCode.toLong()
        require(nextVersion > currentVersion) { "The APK is not newer than this installation" }
        val archiveSigners = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archive.signingInfo?.let { info ->
                (if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory).orEmpty()
            }.orEmpty().map { it.toByteArray().sha256() }
        } else archive.signatures.orEmpty().map { it.toByteArray().sha256() }
        val installedSigners = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            installed.signingInfo?.signingCertificateHistory.orEmpty().map { it.toByteArray().sha256() }
        } else installed.signatures.orEmpty().map { it.toByteArray().sha256() }
        require(archiveSigners.isNotEmpty() && archiveSigners.any { candidate -> installedSigners.any { it.contentEquals(candidate) } }) {
            "The APK signature does not match this installation"
        }
    }
}

private fun String.hexBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
private fun ByteArray.sha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(this)
