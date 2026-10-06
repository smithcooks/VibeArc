package com.vibearc.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.util.concurrent.atomic.AtomicReference

/** One serialized transfer, bounded buffers, durable progress; no Activity lifetime dependency. */
class DownloadService: Service() {
    private val scope = CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private var job: Job? = null
    @Volatile private var requestedAgain=false
    private var latestStartId=0
    private val store by lazy { OfflineDownloads.get(this) }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("downloads","Offline downloads",NotificationManager.IMPORTANCE_LOW))
    }
    private fun notification(text: String) = NotificationCompat.Builder(this,"downloads")
        .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("VibeArc downloads")
        .setContentText(text).setOnlyAlertOnce(true).setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)).build()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId=startId
        if(Build.VERSION.SDK_INT>=29) startForeground(803,notification("Preparing offline music"),ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(803,notification("Preparing offline music"))
        if(job!=null) requestedAgain=true
        else {
            requestedAgain=false
            job=scope.launch {
            try {
                store.load()
                var waitingSince = 0L
                while(isActive) {
                    val ready = networkAllowsDownload(this@DownloadService,store.wifiOnly)
                    if(ready) store.entries.value.filter { it.status==DownloadStatus.PAUSED && it.autoResume }.forEach { e -> store.update(e.id) { if(it.status==DownloadStatus.PAUSED && it.autoResume) it.copy(status=DownloadStatus.QUEUED,error="",autoResume=false) else it } }
                    val entry=store.entries.value.firstOrNull { it.status==DownloadStatus.QUEUED }
                    if(entry==null) {
                        if(store.entries.value.none { it.autoResume }) break
                        if(waitingSince==0L) waitingSince=System.currentTimeMillis()
                        if(System.currentTimeMillis()-waitingSince>120_000) {
                            store.entries.value.filter { it.autoResume }.forEach { e -> store.update(e.id) { if(it.autoResume) it.copy(autoResume=false,error="Waiting for a connection. Tap Resume when ready.") else it } }; break
                        }
                        delay(2_000); continue
                    }
                    if(!ready) {
                        store.update(entry.id) { if(it.status==DownloadStatus.QUEUED) it.copy(status=DownloadStatus.PAUSED,autoResume=true,error=if(store.wifiOnly) "Waiting for Wi-Fi" else "Waiting for a connection") else it }
                        continue
                    }
                    waitingSince=0
                    transfer(entry)
                }
            } catch (_: CancellationException) { throw CancellationException() }
            catch (_: Exception) { android.util.Log.w("VibeArcDownloads","Download queue stopped; saved state retained") }
            finally {
                val finishingJob=coroutineContext[Job]
                android.os.Handler(mainLooper).post {
                    if(job!==finishingJob) return@post
                    job=null
                    if(requestedAgain && scope.isActive) onStartCommand(null,0,latestStartId)
                    else { stopForeground(STOP_FOREGROUND_REMOVE); stopSelfResult(latestStartId) }
                }
            }
        }
        }
        return START_NOT_STICKY
    }
    private suspend fun transfer(initial: DownloadEntry) {
        store.update(initial.id) { if(it.status==DownloadStatus.QUEUED) it.copy(status=DownloadStatus.RESOLVING,error="") else it }
        if(store.entries.value.firstOrNull { it.id==initial.id }?.status!=DownloadStatus.RESOLVING) return
        run {
            val partial=store.file(initial.id,"part")
            try {
                val current=store.entries.value.firstOrNull { it.id==initial.id } ?: run { partial.delete(); return }
                if(current.status !in setOf(DownloadStatus.RESOLVING,DownloadStatus.DOWNLOADING)) return
                activeId=initial.id
                store.update(initial.id) { if(it.status==DownloadStatus.RESOLVING) it.copy(status=DownloadStatus.DOWNLOADING,provider="playback",validator="",bytes=0,total=0,error="",
                    sourceUrl="",permission="Original playback audio; lossless not guaranteed") else it }
                var lastSaved=0L
                downloadPlaybackAudio(initial.track,partial,{
                    val status=store.entries.value.firstOrNull { it.id==initial.id }?.status
                    status!=DownloadStatus.DOWNLOADING || !scope.isActive || !networkAllowsDownload(this,store.wifiOnly)
                },{ bytes,size ->
                    val now=System.currentTimeMillis()
                    if(now-lastSaved>=2_000 || bytes==size) {
                        store.update(initial.id) { e -> if(e.status==DownloadStatus.DOWNLOADING) e.copy(bytes=bytes,total=size) else e }
                        getSystemService(NotificationManager::class.java).notify(803,notification("${initial.track.title} · ${bytes/1024} KB")); lastSaved=now
                    }
                },connectionChanged={ activeConnection.set(it) })
                if(store.entries.value.firstOrNull { it.id==initial.id }?.status!=DownloadStatus.DOWNLOADING) throw DownloadPaused()
                val metadata=MediaMetadataRetriever()
                val flac=runCatching { partial.inputStream().use { input ->
                    val header=ByteArray(42);java.io.DataInputStream(input).readFully(header);FlacStreamInfo.read(header)
                } }.getOrNull()
                val codec: String
                val bitrate: Int
                val sampleRate: Int
                val duration: Long
                try {
                    metadata.setDataSource(partial.absolutePath)
                    val actualMime=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE).orEmpty()
                    check(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)=="yes" && metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)!="yes") { "This source is not an audio-only file" }
                    duration=metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
                    check(duration>0) { "This file is not playable audio" }
                    check(!actualMime.contains("flac") || flac!=null) { "FLAC STREAMINFO could not be verified" }
                    codec=when { flac!=null->"FLAC"; actualMime.contains("mpeg")->"MPEG audio"; actualMime.contains("ogg")->"Ogg audio"; actualMime.contains("mp4")->"MP4 audio"; actualMime.contains("webm")->"WebM audio"; actualMime.contains("wav")->"WAV audio"; else->actualMime }
                    bitrate=(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()?.div(1000) ?: 0).toInt()
                    sampleRate=flac?.sampleRateHz ?: if(Build.VERSION.SDK_INT>=31) metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 0 else 0
                } finally { metadata.release() }
                val audio=store.file(initial.id,"audio")
                val art=runCatching { ArtworkCache.load(initial.track.artworkUri,768)?.let { artwork ->
                    val file=store.file(initial.id,"jpg")
                    file.outputStream().use { artwork.bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,it) }
                    Uri.fromFile(file).toString()
                } }.getOrNull().orEmpty()
                // Reuse already matched lyrics; downloading audio never waits for a new lyrics search.
                runCatching { LyricsDiskCache(this).read(initial.track)?.syncedLines?.takeIf { it.isNotEmpty() }?.let {
                    store.file(initial.id,"lrc").writeText(encodeEnhancedLrc(it))
                } }
                val completed=store.complete(initial.id) { it.copy(track=it.track.copy(durationMs=duration),status=DownloadStatus.COMPLETED,bytes=audio.length(),total=audio.length(),localUri=Uri.fromFile(audio).toString(),artworkUri=art,
                    codec=codec,quality=qualityForSource(bitrate,codec=="FLAC"),bitrateKbps=bitrate,sampleRateHz=sampleRate,bitDepth=flac?.bitDepth ?: 0,lossless=codec=="FLAC",completedAt=System.currentTimeMillis(),error="",autoResume=false) }
                if(!completed && store.entries.value.firstOrNull { it.id==initial.id }?.status in setOf(null,DownloadStatus.CANCELLED)) {
                    partial.delete();store.file(initial.id,"jpg").delete();store.file(initial.id,"lrc").delete()
                }
                return
            } catch(e: Exception) {
                val current=store.entries.value.firstOrNull { it.id==initial.id } ?: run { partial.delete();return }
                if(current.status in setOf(DownloadStatus.PAUSED,DownloadStatus.CANCELLED)) {
                    if(current.status==DownloadStatus.CANCELLED) partial.delete()
                    return
                }
                if(e is DownloadPaused || !networkAllowsDownload(this,store.wifiOnly)) {
                    store.update(initial.id) { if(it.status in setOf(DownloadStatus.RESOLVING,DownloadStatus.DOWNLOADING)) it.copy(status=DownloadStatus.PAUSED,bytes=partial.length(),autoResume=true,error="Waiting for a connection") else it }; return
                }
                val message=when(e) { is IllegalArgumentException,is IllegalStateException -> e.message?.take(160); else -> "Connection interrupted. Check the link and retry." }
                store.update(initial.id) { if(it.status in setOf(DownloadStatus.RESOLVING,DownloadStatus.DOWNLOADING)) it.copy(status=DownloadStatus.FAILED,bytes=partial.length(),validator="",error=message ?: "Download failed") else it }
            } finally { activeId="" }
        }
    }
    override fun onTimeout(startId: Int, fgsType: Int) { activeConnection.getAndSet(null)?.disconnect(); scope.cancel(); stopSelf() }
    override fun onDestroy() { activeConnection.getAndSet(null)?.disconnect(); scope.cancel(); super.onDestroy() }
    companion object {
        @Volatile private var activeId=""
        private val activeConnection=AtomicReference<HttpURLConnection?>()
        internal fun interrupt(id: String) { if(activeId==id) activeConnection.getAndSet(null)?.disconnect() }
        internal fun awaitIdle(id: String) {
            repeat(300) { if(activeId!=id) return; Thread.sleep(50) }
            check(activeId!=id) { "Download is stopping. Try removing it again in a moment." }
        }
        internal fun start(context: Context) { ContextCompat.startForegroundService(context,Intent(context,DownloadService::class.java)) }
    }
}
internal fun networkAllowsDownload(context: Context,wifiOnly: Boolean): Boolean {
    val manager=context.getSystemService(ConnectivityManager::class.java)
    val capabilities=manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return downloadNetworkAllowed(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),wifiOnly)
}
internal const val DefaultDownloadWifiOnly = false
internal fun downloadNetworkAllowed(internet: Boolean,validated: Boolean,wifi: Boolean,wifiOnly: Boolean) =
    internet && validated && (!wifiOnly || wifi)
