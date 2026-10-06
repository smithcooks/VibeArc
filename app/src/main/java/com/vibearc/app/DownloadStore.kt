package com.vibearc.app

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

internal enum class DownloadStatus { QUEUED, RESOLVING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED }
internal data class DownloadEntry(
    val track: Track, val status: DownloadStatus = DownloadStatus.QUEUED,
    val sourceUrl: String = "", val provider: String = "", val permission: String = "",
    val codec: String = "", val quality: DownloadQuality? = null, val bitrateKbps: Int = 0, val sampleRateHz: Int = 0,
    val lossless: Boolean = false, val bytes: Long = 0, val total: Long = 0,
    val validator: String = "", val localUri: String = "", val artworkUri: String = "",
    val error: String = "", val createdAt: Long = System.currentTimeMillis(), val completedAt: Long = 0,
    val ownsFile: Boolean = true, val autoResume: Boolean = false,
    val bitDepth: Int = 0,
) {
    val id get() = downloadIdentity(track)
    val progress get() = if (total > 0) (bytes.toFloat()/total).coerceIn(0f,1f) else 0f
}
internal fun legacyOfflineEntry(file: OfflineFile,library: List<Track>): DownloadEntry {
    val catalog=file.sourceUri.ifBlank { file.uri }
    val track=library.firstOrNull { it.catalogUri==catalog } ?: Track(file.name.substringBeforeLast('.'),"On this device","Previously saved",catalog)
    return DownloadEntry(track.withoutPlayCounts(),DownloadStatus.COMPLETED,provider="legacy_local",permission="Previously saved file · original preserved",
        codec=file.format,bitrateKbps=file.bitrateKbps,sampleRateHz=file.sampleRateHz,lossless=file.lossless,
        bytes=file.sizeBytes,total=file.sizeBytes,localUri=file.uri,artworkUri=track.artworkUri,ownsFile=false)
}

internal object DownloadCodec {
    fun encode(entries: List<DownloadEntry>): String {
        val writer = JsonWriter.string().array()
        entries.forEach { e -> writer.`object`().value("track", LibraryCodec.encode(listOf(e.track)))
            .value("status", e.status.name).value("url", e.sourceUrl).value("provider", e.provider).value("permission", e.permission)
            .value("codec", e.codec).value("quality", e.quality?.name.orEmpty()).value("bitrate",e.bitrateKbps).value("sampleRate",e.sampleRateHz).value("bitDepth",e.bitDepth)
            .value("lossless",e.lossless).value("bytes",e.bytes).value("total",e.total).value("validator",e.validator)
            .value("local",e.localUri).value("art",e.artworkUri).value("error",e.error).value("created",e.createdAt)
            .value("completed",e.completedAt).value("owned",e.ownsFile).value("autoResume",e.autoResume).end() }
        return writer.end().done()
    }
    fun decode(text: String): List<DownloadEntry> = JsonParser.array().from(text).map { value ->
        val r = value as JsonObject
        DownloadEntry(LibraryCodec.decode(r.getString("track")).single(), DownloadStatus.valueOf(r.getString("status")),
            r.getString("url",""), r.getString("provider",""), r.getString("permission",""), r.getString("codec",""),
            r.getString("quality","").takeIf(String::isNotBlank)?.let(DownloadQuality::valueOf),r.getInt("bitrate",0),r.getInt("sampleRate",0),
            r.getBoolean("lossless",false),r.getLong("bytes",0),r.getLong("total",0),r.getString("validator",""),r.getString("local",""),
            r.getString("art",""),r.getString("error",""),r.getLong("created",0),r.getLong("completed",0),r.getBoolean("owned",true),r.getBoolean("autoResume",false),r.getInt("bitDepth",0))
    }.also { require(it.size <= 5_000) { "Too many download records" } }
}

/** Persistent jobs live outside the user's library schema. All disk methods are called on IO. */
internal class OfflineDownloads private constructor(private val context: Context) {
    val directory = File(context.filesDir,"downloads")
    private val mutableEntries = MutableStateFlow<List<DownloadEntry>>(emptyList())
    val entries = mutableEntries.asStateFlow()
    private var loaded = false
    private val settings by lazy { context.getSharedPreferences("authorized_downloads", Context.MODE_PRIVATE) }
    // New key replaces the old implicit Wi-Fi-only default, including on upgrades.
    val wifiOnly get() = settings.getBoolean("playback_wifi_only",DefaultDownloadWifiOnly)
    fun setWifiOnly(value: Boolean) { settings.edit().putBoolean("playback_wifi_only",value).apply() }
    @Synchronized fun load() {
        if (loaded) return
        directory.mkdirs()
        val file = File(directory,"jobs.json")
        val records = if (file.exists()) AtomicFile(file).openRead().use { DownloadCodec.decode(it.readUtf8Limited(8*1024*1024)) }
        else {
            val library=context.loadLibrary()
            context.loadOfflineFiles().map { legacyOfflineEntry(it,library) }.distinctBy { it.id }
        }
        val recovered = records.map { entry -> if (entry.status in setOf(DownloadStatus.DOWNLOADING,DownloadStatus.RESOLVING,DownloadStatus.QUEUED))
            entry.copy(status=DownloadStatus.PAUSED,error="Interrupted. Tap Resume.",autoResume=false) else entry }
        persist(recovered)
        loaded = true
    }
    @Synchronized fun update(id: String, change: (DownloadEntry)->DownloadEntry) {
        load()
        val next=mutableEntries.value.map { if(it.id==id) change(it) else it }
        if(next!=mutableEntries.value) persist(next)
    }
    /** Publish only while the job still owns the transfer. Pause/cancel use this same lock. */
    @Synchronized fun complete(id: String, change: (DownloadEntry)->DownloadEntry): Boolean {
        val entry=mutableEntries.value.firstOrNull { it.id==id } ?: return false
        if(entry.status!=DownloadStatus.DOWNLOADING) return false
        val partial=file(id,"part")
        val audio=file(id,"audio")
        check(partial.renameTo(audio)) { "Could not finalize audio" }
        try { persist(mutableEntries.value.map { if(it.id==id) change(it) else it }) }
        catch(e: Exception) { audio.renameTo(partial);throw e }
        return true
    }
    @Synchronized private fun persist(next: List<DownloadEntry>) {
        require(next.size<=5_000) { "The download library is limited to 5,000 records" }
        val encoded=DownloadCodec.encode(next).toByteArray()
        require(encoded.size<=8*1024*1024) { "Download metadata is too large" }
        val file = AtomicFile(File(directory,"jobs.json"))
        val output = file.startWrite()
        try { output.write(encoded); file.finishWrite(output) }
        catch (e: Exception) { file.failWrite(output); throw e }
        mutableEntries.value = next
    }
    @Synchronized fun enqueue(tracks: List<Track>) {
        load()
        var next = mutableEntries.value
        tracks.distinctBy(Track::catalogUri).forEach { track ->
            val existing = next.firstOrNull { it.id==downloadIdentity(track) }
            if (existing?.status==DownloadStatus.COMPLETED && offlineTrack(track)!=null) return@forEach
            if (existing?.status in setOf(DownloadStatus.QUEUED,DownloadStatus.DOWNLOADING,DownloadStatus.RESOLVING)) return@forEach
            val local = track.canCopyOffline()
            val localBytes=if(local) runCatching { context.contentResolver.openAssetFileDescriptor(Uri.parse(track.uri),"r")?.use { it.length.coerceAtLeast(0) } ?: 0 }.getOrDefault(0) else 0
            val entry = if (local) DownloadEntry(track.withoutPlayCounts(),DownloadStatus.COMPLETED,bytes=localBytes,total=localBytes,localUri=track.uri,artworkUri=track.artworkUri,ownsFile=false,completedAt=System.currentTimeMillis())
            else if(supportsPlaybackDownload(track)) DownloadEntry(track.copy(uri=track.catalogUri,sourceUri="").withoutPlayCounts(),provider="playback")
            else DownloadEntry(track,DownloadStatus.FAILED,error="Find this song in Search and download its playable YouTube entry.")
            next = next.filterNot { it.id==entry.id } + entry
        }
        persist(next)
    }
    fun file(id: String, extension: String): File {
        require(id.matches(Regex("[a-f0-9]{64}")) && extension in setOf("part","audio","jpg","lrc"))
        val folder = File(directory,if(extension=="jpg") "artwork" else "tracks").apply { mkdirs() }
        return File(folder,"$id.$extension").also { require(it.canonicalFile.parentFile==folder.canonicalFile) }
    }
    fun offlineTrack(track: Track): Track? {
        val entry = mutableEntries.value.firstOrNull { it.id==downloadIdentity(track) && it.status==DownloadStatus.COMPLETED } ?: return null
        if (entry.localUri.isBlank()) return null
        if (entry.ownsFile && (!file(entry.id,"audio").isFile || file(entry.id,"audio").length()!=entry.bytes)) return null
        return track.copy(uri=entry.localUri,sourceUri=track.catalogUri,artworkUri=entry.artworkUri.ifBlank { track.artworkUri })
    }
    @Synchronized fun remove(id: String) {
        load()
        val record = mutableEntries.value.firstOrNull { it.id==id } ?: return
        // Never delete local originals; only paths derived from this owned record under our root.
        if (record.ownsFile) listOf("audio","part","jpg","lrc").forEach { extension ->
            val target = file(id,extension)
            if(target.exists()) check(target.delete()) { "Could not delete saved audio" }
        }
        persist(mutableEntries.value.filterNot { it.id==id })
    }
    companion object {
        @Volatile private var instance: OfflineDownloads? = null
        fun get(context: Context): OfflineDownloads = instance ?: synchronized(this) { instance ?: OfflineDownloads(context.applicationContext).also { instance=it } }
    }
}
