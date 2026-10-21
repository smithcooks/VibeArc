package com.vibearc.app

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

internal data class SpotifyImportSong(val id: String, val title: String, val artist: String, val durationMs: Long)
internal data class SpotifyImportPlaylist(val id: String, val name: String, val songs: List<SpotifyImportSong>)
internal data class SpotifyImportMatch(val song: SpotifyImportSong, val track: Track?)
internal data class SpotifyImportPreview(val playlist: SpotifyImportPlaylist, val matches: List<SpotifyImportMatch>)

internal fun spotifyPlaylistId(raw: String): String? {
    val value=raw.trim()
    val idPattern=Regex("[A-Za-z0-9]{22}")
    if(idPattern.matches(value)) return value
    Regex("spotify:playlist:([A-Za-z0-9]{22})").matchEntire(value)?.let { return it.groupValues[1] }
    return runCatching {
        val uri=URI(value)
        if(uri.scheme!="https" || uri.host!="open.spotify.com" || uri.rawUserInfo!=null || uri.port !in listOf(-1,443)) return null
        val parts=uri.path.trim('/').split('/')
        val clean=if(parts.firstOrNull()=="embed" || parts.firstOrNull()?.matches(Regex("intl-[a-zA-Z-]+"))==true) parts.drop(1) else parts
        clean.takeIf { it.size==2 && it[0]=="playlist" && idPattern.matches(it[1]) }?.get(1)
    }.getOrNull()
}

internal fun parseSpotifyPlaylist(html: String, id: String): SpotifyImportPlaylist {
    require(html.length<=2*1024*1024) { "Spotify playlist page is too large" }
    val payload=Regex("""<script\b[^>]*\bid=["']__NEXT_DATA__["'][^>]*>(.*?)</script\s*>""",setOf(RegexOption.DOT_MATCHES_ALL,RegexOption.IGNORE_CASE))
        .find(html)?.groupValues?.get(1) ?: error("Spotify did not expose this playlist. Make sure it is public.")
    // Decode an HTML-escaped JSON document before parsing, not after a failed parse.
    // Valid raw JSON values containing literal entity text must remain unchanged.
    val json=if(payload.trimStart().startsWith("{&quot;")) payload.replace("&quot;","\"").replace("&#39;","'")
        .replace("&apos;","'").replace("&lt;","<").replace("&gt;",">").replace("&amp;","&") else payload
    val root=JsonParser.`object`().from(json)
    val entity=root.getObject("props")?.getObject("pageProps")?.getObject("state")?.getObject("data")?.getObject("entity")
        ?: error("Spotify changed its playlist page. Nothing was imported.")
    require(entity.getString("uri","")=="spotify:playlist:$id" && entity.getString("type","")=="playlist") { "Spotify did not return the requested playlist" }
    val rows=entity.getArray("trackList") ?: error("This playlist has no public tracks")
    require(rows.size<=500) { "The public playlist preview is too large" }
    val songs=rows.mapNotNull { raw ->
        val row=raw as? JsonObject ?: return@mapNotNull null
        val trackId=row.getString("uri","").takeIf { it.matches(Regex("spotify:track:[A-Za-z0-9]{22}")) } ?: return@mapNotNull null
        val title=row.getString("title","").replace('\u00a0',' ').trim().take(500)
        val artist=row.getString("subtitle","").replace('\u00a0',' ').trim().take(500)
        if(title.isBlank() || artist.isBlank()) null else SpotifyImportSong(trackId,title,artist,(row["duration"] as? Number)?.toLong()?.coerceAtLeast(0) ?: 0)
    }
    require(songs.isNotEmpty()) { "No readable songs found. Private playlists and Liked Songs are not supported." }
    return SpotifyImportPlaylist(id,entity.getString("title",entity.getString("name","Spotify playlist")).trim().take(200).ifBlank { "Spotify playlist" },songs)
}

private fun importTokens(value: String): Set<String> = Normalizer.normalize(value,Normalizer.Form.NFKD)
    .replace(Regex("\\p{M}"),"").lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+")," ")
    .split(' ').filter { it.isNotBlank() && it !in setOf("official","audio","video","lyrics","lyric","topic") }.toSet()

internal fun matchSpotifySong(song: SpotifyImportSong, candidates: List<Track>): Track? {
    val title=importTokens(song.title)
    val artist=importTokens(song.artist.substringBefore(',').substringBefore('•'))
    if(title.isEmpty() || artist.isEmpty()) return null
    fun overlap(wanted: Set<String>, found: Set<String>)=wanted.intersect(found).size.toDouble()/wanted.size
    return candidates.mapNotNull { track ->
        if(!isYouTubeWatchUri(track.catalogUri)) return@mapNotNull null
        val foundTitle=importTokens(track.title)
        val titleScore=overlap(title,foundTitle)
        val artistScore=overlap(artist,importTokens(track.artist)+foundTitle)
        val unwanted=setOf("cover","remix","live","karaoke","instrumental","slowed","sped").any { it in foundTitle && it !in title }
        val durationDiff=kotlin.math.abs(track.durationMs-song.durationMs)
        if(titleScore<0.8 || artistScore<0.6 || unwanted || (song.durationMs>0 && track.durationMs>0 && durationDiff>20_000)) null
        else track to (titleScore*4+artistScore*2+if(song.durationMs>0 && track.durationMs>0) (1-durationDiff/20_000.0) else 0.0)
    }.maxByOrNull { it.second }?.first
}

internal object SpotifyPlaylistImporter {
    fun fetch(input: String): SpotifyImportPlaylist {
        val id=spotifyPlaylistId(input) ?: error("Paste a Spotify playlist link, URI, or 22-character playlist ID. For short links, open Spotify and copy the full playlist link.")
        val connection=URL("https://open.spotify.com/embed/playlist/$id").openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects=false
            connection.connectTimeout=10_000;connection.readTimeout=15_000
            connection.setRequestProperty("Accept","text/html")
            connection.setRequestProperty("User-Agent","Mozilla/5.0 VibeArc/${BuildConfig.VERSION_NAME}")
            check(connection.responseCode==200) { "Spotify playlist is unavailable (HTTP ${connection.responseCode}). Make sure it is public." }
            return connection.inputStream.use { parseSpotifyPlaylist(it.readUtf8Limited(2*1024*1024),id) }
        } finally { connection.disconnect() }
    }

    suspend fun match(playlist: SpotifyImportPlaylist, progress: (Int,Int)->Unit): SpotifyImportPreview = coroutineScope {
        val limiter=Semaphore(3);val completed=AtomicInteger()
        val unique=playlist.songs.distinctBy { it.id }
        val resolved=unique.map { song -> async(Dispatchers.IO) {
            limiter.withPermit {
                ensureActive()
                val track=try { matchSpotifySong(song,OnlineMusic.search("${song.title} ${song.artist}")) }
                catch(e: CancellationException) { throw e }
                catch(_: Exception) { null }
                progress(completed.incrementAndGet(),unique.size)
                song.id to track
            }
        } }.awaitAll().toMap()
        SpotifyImportPreview(playlist,playlist.songs.map { SpotifyImportMatch(it,resolved[it.id]) })
    }
}
