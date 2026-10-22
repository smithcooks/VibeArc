package com.vibearc.app

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Public metadata only. No account login, audio, cookie import, or disk token cache.
// Protocol reference: Aran404/SpotAPI (GPL-3.0). VibeArc's Kotlin adaptation, 2026-10-22.
// Promoted after the user verified complete retrieval in the separate playlist test APK.
internal data class PublicSpotifyPage(
    val name: String, val total: Int, val rowCount: Int,
    val songs: List<SpotifyImportSong>, val offset: Int?,
)

internal fun parsePublicSpotifyPage(json: String, id: String): PublicSpotifyPage {
    require(json.length<=4*1024*1024) { "Playlist response is too large" }
    val root=JsonParser.`object`().from(json)
    check(root.getArray("errors").isNullOrEmpty()) { "Spotify rejected the public playlist query. Retry later." }
    val playlist=root.getObject("data")?.getObject("playlistV2")
        ?: error("Spotify did not return a public playlist")
    require(playlist.getString("uri","")=="spotify:playlist:$id") { "Spotify returned a different playlist" }
    val content=playlist.getObject("content") ?: error("Full playlist contents are unavailable")
    val totalValue=(content["totalCount"] as? Number)?.toLong() ?: error("Missing full playlist count")
    require(totalValue in 1..10_000) { "Playlist import supports 1 to 10,000 entries" }
    val total=totalValue.toInt()
    val rows=content.getArray("items") ?: error("Missing playlist page")
    require(rows.size<=500) { "Unexpected playlist page size" }
    val songs=rows.mapNotNull { raw ->
        val row=raw as? JsonObject ?: return@mapNotNull null
        val node=(row.getObject("itemV2") ?: row.getObject("item"))?.getObject("data") ?: return@mapNotNull null
        val uri=node.getString("uri","")
        if(!uri.matches(Regex("spotify:track:[A-Za-z0-9]{22}"))) return@mapNotNull null
        val title=node.getString("name","").trim().take(500)
        val artist=node.getObject("artists")?.getArray("items")?.mapNotNull {
            (it as? JsonObject)?.let { a -> a.getObject("profile")?.getString("name") ?: a.getString("name") }
        }?.filter { it.isNotBlank() }?.joinToString(", ")?.take(500).orEmpty()
        val duration=((node.getObject("trackDuration") ?: node.getObject("duration"))?.get("totalMilliseconds") as? Number)?.toLong()?.coerceAtLeast(0) ?: 0
        if(title.isBlank() || artist.isBlank()) null else SpotifyImportSong(uri,title,artist,duration)
    }
    return PublicSpotifyPage(playlist.getString("name","Spotify playlist").trim().take(200).ifBlank { "Spotify playlist" },
        total,rows.size,songs,(content.getObject("pagingInfo")?.get("offset") as? Number)?.toInt())
}

internal fun collectPublicSpotifyPlaylist(id: String, progress: (Int,Int)->Unit, page: (Int)->PublicSpotifyPage): SpotifyImportPlaylist {
    val songs=mutableListOf<SpotifyImportSong>()
    var offset=0;var total:Int?=null;var name="Spotify playlist";var pages=0
    while(total==null || offset<total) {
        require(++pages<=200) { "Playlist pagination exceeded the safety limit" }
        if(Thread.currentThread().isInterrupted) throw InterruptedException("Import cancelled")
        val next=page(offset)
        require(next.offset==null || next.offset==offset) { "Spotify repeated a playlist page. Nothing was imported." }
        require(total==null || total==next.total) { "Playlist changed while importing. Please retry." }
        total=next.total;name=next.name
        require(next.rowCount>0 && next.songs.size<=next.rowCount && offset+next.rowCount<=total) { "Spotify returned an incomplete playlist. Please retry." }
        songs+=next.songs
        offset+=next.rowCount
        progress(offset,total)
    }
    require(songs.isNotEmpty()) { "The playlist contains no readable songs" }
    return SpotifyImportPlaylist(id,name,songs,total,total!!-songs.size)
}

internal fun spotifyGuestCode(values: List<Int>, timeMs: Long): String {
    require(values.size in 8..256 && values.all { it in 0..255 }) { "Invalid public client configuration" }
    val key=values.mapIndexed { i,v -> v xor ((i%33)+9) }.joinToString("").toByteArray(Charsets.UTF_8)
    val mac=Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(key,"HmacSHA1")) }
    val digest=mac.doFinal(ByteBuffer.allocate(8).putLong(timeMs/30_000).array())
    val start=digest.last().toInt() and 15
    val value=((digest[start].toInt() and 127) shl 24) or ((digest[start+1].toInt() and 255) shl 16) or
        ((digest[start+2].toInt() and 255) shl 8) or (digest[start+3].toInt() and 255)
    return (value%1_000_000).toString().padStart(6,'0')
}

internal fun publicPlaylistQueryHash(script: String): String? =
    Regex("[\"']fetchPlaylist[\"']\\s*,\\s*[\"']query[\"']\\s*,\\s*[\"']([a-f0-9]{64})[\"']")
        .find(script)?.groupValues?.get(1)

internal object PublicSpotifyPlaylist {
    @JvmOverloads
    fun fetch(input: String, progress: (Int,Int)->Unit = {_,_->}): SpotifyImportPlaylist {
        val id=spotifyPlaylistId(input) ?: error("Paste a full public Spotify playlist link")
        val session=GuestSession()
        return collectPublicSpotifyPlaylist(id,progress) { offset ->
            parsePublicSpotifyPage(session.playlist(id,offset),id)
        }
    }

    private class GuestSession {
        private val cookies=linkedMapOf<String,String>()
        private val userAgent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private var serverTime=System.currentTimeMillis()
        private var token="";private var clientToken="";private var version="";private var hash=""
        private var expires=0L
        private var clientId=""

        fun playlist(id: String, offset: Int): String {
            if(token.isBlank()) initialize()
            if(System.currentTimeMillis()+30_000>=expires) authorize()
            val variables=json(mapOf("uri" to "spotify:playlist:$id","offset" to offset,"limit" to 100,"enableWatchFeedEntrypoint" to false))
            val extensions=json(mapOf("persistedQuery" to mapOf("version" to 1,"sha256Hash" to hash)))
            val query=mapOf("operationName" to "fetchPlaylist","variables" to variables,"extensions" to extensions)
                .entries.joinToString("&") { "${it.key}=${encode(it.value)}" }
            return request("https://api-partner.spotify.com/pathfinder/v1/query?$query","playlist page",post="",
                headers=mapOf("Authorization" to "Bearer $token","Client-Token" to clientToken,"Spotify-App-Version" to version))
        }

        private fun initialize() {
            val html=request("https://open.spotify.com/","public session")
            val configPayload=Regex("<script\\b[^>]*id=[\"']appServerConfig[\"'][^>]*>(.*?)</script>",RegexOption.DOT_MATCHES_ALL)
                .find(html)?.groupValues?.get(1) ?: error("Spotify changed its public session page")
            val config=JsonParser.`object`().from(String(java.util.Base64.getDecoder().decode(configPayload.trim()),Charsets.UTF_8))
            version=config.getString("clientVersion","")
            require(version.isNotBlank() && version.length<=200) { "Missing Spotify public client version" }
            authorize()
            val device=cookies["sp_t"] ?: UUID.randomUUID().toString()
            val body=json(mapOf("client_data" to mapOf("client_version" to version,"client_id" to clientId,
                "js_sdk_data" to mapOf("device_brand" to "unknown","device_model" to "unknown","os" to "windows",
                    "os_version" to "NT 10.0","device_id" to device,"device_type" to "computer"))))
            val result=JsonParser.`object`().from(request("https://clienttoken.spotify.com/v1/clienttoken","public client approval",post=body))
            check(result.getString("response_type","")=="RESPONSE_GRANTED_TOKEN_RESPONSE") {
                "Spotify requested a browser verification. The playlist importer cannot complete it."
            }
            clientToken=result.getObject("granted_token")?.getString("token").orEmpty()
            check(clientToken.isNotBlank()) { "Spotify did not approve the public client" }
            val scriptUrl=Regex("<script\\b[^>]*src=[\"'](https://open\\.spotifycdn\\.com/cdn/build/web-player/web-player[^\"']+\\.js)[\"']")
                .find(html)?.groupValues?.get(1) ?: error("Spotify changed its public client script")
            val script=request(scriptUrl,"public query configuration",maxBytes=8*1024*1024)
            hash=publicPlaylistQueryHash(script).orEmpty()
            if(hash.isBlank()) {
                // Inspect data-only chunk filename maps; never execute fetched JavaScript.
                val maps=Regex("\\{[0-9]+:\"[^\"]+\"(?:,[0-9]+:\"[^\"]+\")*}").findAll(script).map { match ->
                    Regex("([0-9]+):\"([^\"]+)\"").findAll(match.value).associate { it.groupValues[1] to it.groupValues[2] }
                }.toList()
                val names=maps.filter { it.values.any { v -> !v.matches(Regex("[a-f0-9]{6,64}")) } }
                val hashes=maps.filter { it.values.all { v -> v.matches(Regex("[a-f0-9]{6,64}")) } }
                val files=names.flatMap { n -> hashes.flatMap { h -> n.mapNotNull { (key,value) ->
                    h[key]?.let { if(value.matches(Regex("[A-Za-z0-9_~-]+"))) "$value.$it.js" else null }
                } } }.distinct().sortedByDescending { it.contains("playlist",true) || it.contains("xpui",true) }
                check(files.size<=2000) { "Unexpected public client configuration" }
                var bytes=script.length
                for(file in files.take(24)) {
                    val chunk=request("https://open.spotifycdn.com/cdn/build/web-player/$file","public query configuration",maxBytes=4*1024*1024)
                    bytes+=chunk.length;check(bytes<=32*1024*1024) { "Public client configuration is too large" }
                    hash=publicPlaylistQueryHash(chunk).orEmpty()
                    if(hash.isNotBlank()) break
                }
            }
            check(hash.isNotBlank()) { "Spotify changed its playlist query. No partial playlist was imported." }
        }

        private fun authorize() {
            // Bounded data-only public web-client configuration, not account credentials or executable code.
            val config=JsonParser.`object`().from(request(
                "https://code.thetadev.de/ThetaDev/spotify-secrets/raw/branch/main/secrets/secretDict.json","public client configuration",maxBytes=64*1024))
            val latest=config.keys.mapNotNull { it.toIntOrNull() }.filter { it in 1..100_000 }.maxOrNull()
                ?: error("Public client configuration is unavailable")
            val values=config.getArray(latest.toString())?.map { (it as? Number)?.toInt() ?: -1 }
                ?: error("Invalid public client configuration")
            val code=spotifyGuestCode(values,serverTime)
            val result=JsonParser.`object`().from(request("https://open.spotify.com/api/token?reason=init&productType=web-player&totp=$code&totpVer=$latest&totpServer=$code","public session approval",maxBytes=64*1024))
            token=result.getString("accessToken","")
            clientId=result.getString("clientId","")
            expires=(result["accessTokenExpirationTimestampMs"] as? Number)?.toLong() ?: 0
            check(token.isNotBlank() && clientId.isNotBlank() && expires>System.currentTimeMillis()) { "Spotify did not approve the public session" }
        }

        private fun request(url: String, stage: String, post: String?=null, headers: Map<String,String> = emptyMap(), maxBytes: Int=4*1024*1024, retry: Int=0): String {
            val uri=URI(url)
            require(uri.scheme=="https" && uri.rawUserInfo==null && uri.port in listOf(-1,443) && uri.host in
                setOf("open.spotify.com","open.spotifycdn.com","api-partner.spotify.com","clienttoken.spotify.com","code.thetadev.de")) { "Untrusted public metadata endpoint" }
            if(Thread.currentThread().isInterrupted) throw InterruptedException("Import cancelled")
            val connection=uri.toURL().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects=false;connection.connectTimeout=10_000;connection.readTimeout=20_000
                connection.setRequestProperty("User-Agent",userAgent)
                connection.setRequestProperty("Accept",if(uri.host=="clienttoken.spotify.com") "application/json" else "application/json,text/html,*/*")
                connection.setRequestProperty("Accept-Language","en")
                if(uri.host=="open.spotify.com" && cookies.isNotEmpty()) connection.setRequestProperty("Cookie",cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
                headers.forEach { (key,value) -> connection.setRequestProperty(key,value) }
                if(post!=null) {
                    connection.requestMethod="POST";connection.doOutput=true
                    connection.setRequestProperty("Content-Type","application/json")
                    connection.outputStream.use { it.write(post.toByteArray(Charsets.UTF_8)) }
                }
                val status=connection.responseCode
                if(status in setOf(500,502,503,504) && retry==0) {
                    connection.disconnect()
                    Thread.sleep(300)
                    return request(url,stage,post,headers,maxBytes,retry=1)
                }
                check(status==200) { "Spotify $stage failed (HTTP $status). Retry later or try another network. No partial playlist was saved." }
                serverTime=connection.date.takeIf { it>0 } ?: System.currentTimeMillis()
                if(uri.host=="open.spotify.com") connection.headerFields.entries.filter { it.key.equals("Set-Cookie",true) }.flatMap { it.value }.forEach {
                    val pair=it.substringBefore(';').split('=',limit=2)
                    if(pair.size==2 && pair[0] in setOf("sp_t","sp_landing")) cookies[pair[0]]=pair[1]
                }
                return connection.inputStream.use { it.readUtf8Limited(maxBytes) }
            } finally { connection.disconnect() }
        }
        private fun json(value: Map<String,Any>): String = JsonWriter.string().value(value).done()
        private fun encode(value: String)=URLEncoder.encode(value,"UTF-8")
    }
}

