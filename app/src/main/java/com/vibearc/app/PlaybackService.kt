package com.vibearc.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.util.concurrent.Executors

class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private val lastFmExecutor = Executors.newSingleThreadExecutor()
    private var trackedMediaId = ""
    private var listenedMs = 0L
    private var lastPollMs = 0L
    private var startedAtSeconds = 0L
    private var scrobbled = false
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId?.takeIf(String::isNotBlank) ?: return
            saveRecentUris(loadRecentUris().recordRecentUri(mediaId))
            trackedMediaId = mediaId
            listenedMs = 0L
            lastPollMs = android.os.SystemClock.elapsedRealtime()
            startedAtSeconds = System.currentTimeMillis() / 1_000
            scrobbled = false
            submitNowPlaying(mediaItem)
        }
    }
    private val sleepTimerCheck = object : Runnable {
        override fun run() {
            loadSleepDeadlineMillis()?.let { deadline ->
                if (System.currentTimeMillis() >= deadline) {
                    player.pause()
                    saveSleepDeadlineMillis(null)
                }
            }
            val now = android.os.SystemClock.elapsedRealtime()
            if (player.isPlaying && player.currentMediaItem?.mediaId == trackedMediaId) {
                listenedMs += (now - lastPollMs).coerceIn(0L, SleepTimerPollMillis * 2)
                val track = player.currentMediaItem?.lastFmTrack(player.duration)
                if (!scrobbled && track != null && shouldScrobble(track, listenedMs, loadLastFmExcludedUris())) {
                    scrobbled = true
                    submitScrobble(track, startedAtSeconds)
                }
            }
            lastPollMs = now
            handler.postDelayed(this, SleepTimerPollMillis)
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().also { it.addListener(playerListener) }
        mediaSession = MediaSession.Builder(this, player).build()
        lastPollMs = android.os.SystemClock.elapsedRealtime()
        handler.post(sleepTimerCheck)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        handler.removeCallbacks(sleepTimerCheck)
        player.removeListener(playerListener)
        lastFmExecutor.shutdownNow()
        mediaSession?.release()
        mediaSession = null
        player.release()
        super.onDestroy()
    }

    private fun submitNowPlaying(mediaItem: MediaItem) {
        val track = mediaItem.lastFmTrack(player.duration) ?: return
        if (!lastFmScrobblingEnabled() || track.catalogUri in loadLastFmExcludedUris()) return
        val session = loadLastFmSession() ?: return
        if (!lastFmSignerConfigured()) return
        lastFmExecutor.execute {
            runCatching { LastFmSignerApi.updateNowPlaying(BuildConfig.LASTFM_SIGNER_URL, BuildConfig.LASTFM_SIGNER_TOKEN, session, track) }
        }
    }

    private fun submitScrobble(track: Track, timestamp: Long) {
        val session = loadLastFmSession() ?: return
        if (!lastFmSignerConfigured()) return
        lastFmExecutor.execute {
            runCatching { LastFmSignerApi.scrobble(BuildConfig.LASTFM_SIGNER_URL, BuildConfig.LASTFM_SIGNER_TOKEN, session, track, timestamp) }
        }
    }

    private fun lastFmSignerConfigured(): Boolean =
        BuildConfig.LASTFM_SIGNER_URL.isNotBlank() && BuildConfig.LASTFM_SIGNER_TOKEN.isNotBlank()
}

private fun MediaItem.lastFmTrack(playerDurationMs: Long): Track? {
    val title = mediaMetadata.title?.toString().orEmpty()
    val artist = mediaMetadata.artist?.toString().orEmpty()
    if (title.isBlank() || artist.isBlank()) return null
    return Track(
        title = title,
        artist = artist,
        album = mediaMetadata.albumTitle?.toString().orEmpty(),
        uri = localConfiguration?.uri?.toString().orEmpty().ifBlank { mediaId },
        durationMs = playerDurationMs.takeIf { it > 0 }
            ?: mediaMetadata.extras?.getLong("durationMs")?.takeIf { it > 0 }
            ?: 0,
        sourceUri = mediaMetadata.extras?.getString("sourceUri").orEmpty().ifBlank { mediaId },
    )
}

private const val PlaybackPreferencesName = "vibearc_playback"
private const val RecentUrisKey = "recent_uris"
private const val SleepDeadlineKey = "sleep_deadline"
private const val SleepTimerPollMillis = 1_000L

internal fun Context.loadRecentUris(): List<String> = RecentUriCodec.decode(
    getSharedPreferences(PlaybackPreferencesName, Context.MODE_PRIVATE)
        .getString(RecentUrisKey, "")
        .orEmpty(),
)

internal fun Context.saveRecentUris(uris: List<String>) {
    getSharedPreferences(PlaybackPreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(RecentUrisKey, RecentUriCodec.encode(uris))
        .apply()
}

internal fun Context.loadSleepDeadlineMillis(): Long? =
    getSharedPreferences(PlaybackPreferencesName, Context.MODE_PRIVATE)
        .getLong(SleepDeadlineKey, 0L)
        .takeIf { it > 0L }

internal fun Context.saveSleepDeadlineMillis(deadlineMillis: Long?) {
    getSharedPreferences(PlaybackPreferencesName, Context.MODE_PRIVATE)
        .edit()
        .apply {
            if (deadlineMillis == null) remove(SleepDeadlineKey)
            else putLong(SleepDeadlineKey, deadlineMillis)
        }
        .apply()
}
