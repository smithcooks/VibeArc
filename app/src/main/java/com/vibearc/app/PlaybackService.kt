package com.vibearc.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.common.PlaybackException
import android.net.Uri
import java.io.IOException
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var crossfadePlayer: ExoPlayer
    private var mediaSession: MediaSession? = null
    private lateinit var audioEffects: AudioEffectsController
    private lateinit var bitPerfect: BitPerfectController
    private val handler = Handler(Looper.getMainLooper())
    private val lastFmExecutor = Executors.newSingleThreadExecutor()
    private var trackedMediaId = ""
    private var listenedMs = 0L
    private var lastPollMs = 0L
    private var startedAtSeconds = 0L
    private var scrobbled = false
    private var crossfadeNextIndex = C.INDEX_UNSET
    private var lastAudioRefreshMs = 0L
    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            android.widget.Toast.makeText(this@PlaybackService,
                "Could not play this song. Check your connection or try another streaming format.",
                android.widget.Toast.LENGTH_LONG).show()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            cancelCrossfade()
            val mediaId = mediaItem?.mediaId?.takeIf(String::isNotBlank) ?: return
            saveRecentUris(loadRecentUris().recordRecentUri(mediaId))
            trackedMediaId = mediaId
            listenedMs = 0L
            lastPollMs = android.os.SystemClock.elapsedRealtime()
            startedAtSeconds = System.currentTimeMillis() / 1_000
            scrobbled = false
            submitNowPlaying(mediaItem)
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) = audioEffects.attach(audioSessionId)

        override fun onTracksChanged(tracks: Tracks) {
            saveBitPerfectStatus(bitPerfect.configure(player.currentAudioDetails()))
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) cancelCrossfade()
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
            if (now - lastAudioRefreshMs >= 1_000L) {
                audioEffects.refresh()
                lastAudioRefreshMs = now
            }
            updateCrossfade()
            handler.postDelayed(this, SleepTimerPollMillis)
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioEffects = AudioEffectsController(this)
        bitPerfect = BitPerfectController(this)
        val http = DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36")
        val sources = ResolvingDataSource.Factory(DefaultDataSource.Factory(this, http)) { spec ->
            try {
                val audio = resolvePlaybackSource(spec.uri.toString()) { track ->
                    OnlineMusic.resolve(track, streamAudioFormat(), streamAudioQuality())
                }
                spec.withUri(Uri.parse(audio))
            } catch (error: Exception) {
                throw IOException("Could not resolve the audio source", error)
            }
        }
        val mediaSources = DefaultMediaSourceFactory(sources)
        player = ExoPlayer.Builder(this).setMediaSourceFactory(mediaSources).build().also { it.addListener(playerListener) }
        crossfadePlayer = ExoPlayer.Builder(this).setMediaSourceFactory(mediaSources).build()
        mediaSession = MediaSession.Builder(this, player).build()
        lastPollMs = android.os.SystemClock.elapsedRealtime()
        handler.post(sleepTimerCheck)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        handler.removeCallbacks(sleepTimerCheck)
        player.removeListener(playerListener)
        cancelCrossfade()
        bitPerfect.clear()
        audioEffects.release()
        lastFmExecutor.shutdownNow()
        mediaSession?.release()
        mediaSession = null
        player.release()
        crossfadePlayer.release()
        super.onDestroy()
    }

    private fun updateCrossfade() {
        val seconds = loadAudioTuning().crossfadeSeconds
        if (seconds <= 0 || !player.isPlaying || player.repeatMode == Player.REPEAT_MODE_ONE) {
            cancelCrossfade()
            return
        }
        val duration = player.duration.takeIf { it > 0 } ?: return
        val remaining = duration - player.currentPosition
        val fadeMs = seconds * 1_000L
        if (crossfadeNextIndex == C.INDEX_UNSET) {
            if (remaining > fadeMs || player.nextMediaItemIndex == C.INDEX_UNSET) return
            crossfadeNextIndex = player.nextMediaItemIndex
            crossfadePlayer.setMediaItem(player.getMediaItemAt(crossfadeNextIndex))
            crossfadePlayer.volume = 0f
            crossfadePlayer.prepare()
            crossfadePlayer.play()
        }
        if (!crossfadePlayer.isPlaying) return
        val progress = (crossfadePlayer.currentPosition.toFloat() / fadeMs).coerceIn(0f, 1f)
        val gains = crossfadeGains(progress)
        player.volume = gains.outgoing
        crossfadePlayer.volume = gains.incoming
        if (progress >= .98f || remaining <= 75L) {
            val position = crossfadePlayer.currentPosition
            crossfadePlayer.pause()
            player.volume = 1f
            player.seekTo(crossfadeNextIndex, position)
            player.play()
            crossfadePlayer.clearMediaItems()
            crossfadeNextIndex = C.INDEX_UNSET
        }
    }

    private fun cancelCrossfade() {
        if (::crossfadePlayer.isInitialized) {
            crossfadePlayer.pause()
            crossfadePlayer.clearMediaItems()
        }
        if (::player.isInitialized) player.volume = 1f
        crossfadeNextIndex = C.INDEX_UNSET
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
private const val SleepTimerPollMillis = 250L

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
