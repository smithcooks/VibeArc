package com.vibearc.app

import java.io.*
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

internal class DownloadPaused: IOException("Download paused")
internal data class AudioRange(val start: Long,val end: Long,val total: Long)
internal fun parseAudioRange(value: String?): AudioRange? {
    val match=Regex("bytes (\\d+)-(\\d+)/(\\d+)").matchEntire(value.orEmpty()) ?: return null
    val start=match.groupValues[1].toLongOrNull() ?: return null
    val end=match.groupValues[2].toLongOrNull() ?: return null
    val total=match.groupValues[3].toLongOrNull() ?: return null
    return AudioRange(start,end,total).takeIf { start<=end && end<total && total<=1024L*1024*1024 }
}
internal fun canResumeDownload(offset: Long, code: Int, range: String?, oldValidator: String, validator: String): Boolean =
    offset > 0 && code == 206 && parseAudioRange(range)?.start==offset && oldValidator.isNotBlank() && !oldValidator.startsWith("W/") && oldValidator == validator

internal fun copyAudioBytes(input: InputStream, file: File, append: Boolean, total: Long, paused: () -> Boolean, progress: (Long, Long) -> Unit) {
    var bytes = if (append) file.length() else 0L
    val buffer = ByteArray(32 * 1024)
    FileOutputStream(file, append).use { out ->
        while (true) {
            if (paused()) throw DownloadPaused()
            val n = input.read(buffer)
            if (n < 0) break
            if (bytes + n > 1024L * 1024 * 1024) throw IOException("Audio exceeds the 1 GB safety limit")
            if (file.parentFile!!.usableSpace < n + 4L * 1024 * 1024) throw IOException("Not enough storage")
            out.write(buffer, 0, n)
            bytes += n
            progress(bytes, total)
        }
        out.fd.sync()
    }
    if (bytes == 0L || (total > 0 && bytes != total)) throw EOFException("Audio download was interrupted")
}

/** Validate every redirect and resolved address; cookies/auth headers are never forwarded. */
internal fun openAuthorizedAudio(url: String, offset: Long, validator: String, playbackSource: Boolean = false): HttpURLConnection {
    var current = url
    repeat(5) {
        require(if(playbackSource) isPlaybackDownloadUrl(current) else isLegalAudioUrl(current)) { "Unsupported audio download URL" }
        val host = URL(current).host
        require(InetAddress.getAllByName(host).all { !it.isAnyLocalAddress && !it.isLoopbackAddress && !it.isLinkLocalAddress && !it.isSiteLocalAddress && !it.isMulticastAddress && !(it.address.size==16 && (it.address[0].toInt() and 0xfe)==0xfc) }) { "Private-network audio links are not supported" }
        val connection = URL(current).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", "VibeArc/${BuildConfig.VERSION_NAME}")
        if (playbackSource) connection.setRequestProperty("Range", "bytes=0-")
        if (offset > 0 && validator.isNotBlank()) {
            connection.setRequestProperty("Range", "bytes=$offset-")
            connection.setRequestProperty("If-Range", validator)
        }
        try {
            val code = connection.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                current = URL(URL(current), connection.getHeaderField("Location") ?: error("Missing audio redirect")).toString()
                connection.disconnect()
            } else return connection
        } catch (e: Exception) { connection.disconnect(); throw e }
    }
    throw IOException("Too many audio redirects")
}

/** Accept resolver-produced media, never user-entered service URLs. */
internal fun supportsPlaybackDownload(track: Track, enabled: Boolean = true): Boolean =
    enabled && track.uri.startsWith("https://") && isYouTubeWatchUri(track.catalogUri) &&
        runCatching { java.net.URI(track.catalogUri).rawUserInfo == null }.getOrDefault(false)

internal fun isPlaybackDownloadUrl(value: String): Boolean = runCatching {
    val uri=java.net.URI(value)
    val host=uri.host?.lowercase().orEmpty()
    uri.scheme=="https" && uri.rawUserInfo==null && uri.fragment==null && uri.port in listOf(-1,443) &&
        (host=="googlevideo.com" || host.endsWith(".googlevideo.com")) && value.length<=8192
}.getOrDefault(false)

internal fun downloadPlaybackAudio(
    track: Track, destination: File, cancelled: ()->Boolean, progress: (Long,Long)->Unit,
    resolve: (Track)->Track = { OnlineMusic.resolve(it) },
    invalidate: (String)->Unit = OnlineMusic::invalidate,
    open: (String)->HttpURLConnection = { openAuthorizedAudio(it,0,"",playbackSource=true) },
    connectionChanged: (HttpURLConnection?)->Unit = {},
) {
    require(supportsPlaybackDownload(track,true)) { "This recording has no supported playback download source" }
    var lastError: Exception?=null
    repeat(2) { attempt ->
        if(cancelled()) throw DownloadPaused()
        if(attempt>0) invalidate(playbackSourceUri(track))
        var connection: HttpURLConnection?=null
        try {
            val resolved=resolve(track)
            require(isPlaybackDownloadUrl(resolved.uri)) { "The player did not return a supported audio URL" }
            if(cancelled()) throw DownloadPaused()
            val response=open(resolved.uri)
            connection=response
            connectionChanged(response)
            if(cancelled()) throw DownloadPaused()
            require(isPlaybackDownloadUrl(response.url.toString())) { "Unsupported audio redirect" }
            val code=response.responseCode
            check(code==200 || code==206) { "Audio source returned HTTP $code" }
            val range=if(code==206) parseAudioRange(response.getHeaderField("Content-Range")) else null
            check(code!=206 || (range!=null && range.start==0L && range.end+1==range.total)) { "Incomplete audio range" }
            val responseLength=response.contentLengthLong.takeIf { it>=0 }
            check(responseLength==null || responseLength in 1..1024L*1024*1024) { "Invalid audio size" }
            check(range==null || responseLength==null || responseLength==range.total) { "Audio length does not match its range" }
            val mime=response.contentType.orEmpty().substringBefore(';').trim().lowercase()
            check(mime.startsWith("audio/") || mime in setOf("application/octet-stream","video/webm","video/mp4")) { "Source did not return audio" }
            response.inputStream.use {
                // A refresh or provider switch always starts from zero; never append unrelated bytes.
                copyAudioBytes(it,destination,false,range?.total ?: responseLength ?: -1,cancelled,progress)
            }
            return
        } catch(error: DownloadPaused) { throw error }
        catch(error: Exception) {
            if(cancelled()) throw DownloadPaused()
            lastError=error
        } finally {
            connection?.disconnect()
            connectionChanged(null)
        }
    }
    throw IllegalStateException("Playback audio could not be saved. Try playing the song once, then retry its download.",lastError)
}
