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
import androidx.media3.datasource.HttpDataSource
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
    @Volatile private var playbackGeneration = 0L
    private var scrobbleInFlight = false
    private var scrobbleAttempts = 0
    private var scrobbleRetryAtMs = 0L
    private var nowPlayingSession: String? = null
    private var crossfadeNextIndex = C.INDEX_UNSET
    private var lastAudioRefreshMs = 0L
    private var refreshedMediaId: String? = null
    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val source = player.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
            val mediaId = player.currentMediaItem?.mediaId
            val causes = generateSequence(error as Throwable) { it.cause }.take(10).toList()
            val status = causes.filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()?.responseCode
            val refresh = shouldRefreshAudioSource(source, status, refreshedMediaId == mediaId)
            android.util.Log.w("VibeArcPlayback", org.json.JSONObject().apply {
                put("event", "playback_failed"); put("entryPoint", "player")
                put("requestId", startedAtSeconds); put("code", error.errorCodeName)
                put("cause", causes.last().javaClass.simpleName); put("httpStatus", status)
                put("refresh", refresh)
            }.toString())
            if (refresh) {
                refreshedMediaId = mediaId
                OnlineMusic.invalidate(source)
                player.prepare()
                return
            }
            val message = when {
                causes.any { it is org.schabi.newpipe.extractor.exceptions.ReCaptchaException } ->
                    "YouTube requested verification. Open YouTube Music and try again later."
                status != null -> "Audio server rejected this stream (HTTP $status). Try again later."
                causes.any { it is java.net.SocketTimeoutException || it is java.net.UnknownHostException } ->
                    "The audio server could not be reached. Check your connection."
                else -> "Could not load this audio source (${error.errorCodeName}). Try another recording."
            }
            android.widget.Toast.makeText(this@PlaybackService,
                message,
                android.widget.Toast.LENGTH_LONG).show()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            cancelCrossfade()
            val mediaId = mediaItem?.mediaId?.takeIf(String::isNotBlank) ?: return
            if (trackedMediaId != mediaId) refreshedMediaId = null
            saveRecentUris(loadRecentUris().recordRecentUri(mediaId))
            trackedMediaId = mediaId
            listenedMs = 0L
            lastPollMs = android.os.SystemClock.elapsedRealtime()
            startedAtSeconds = 0L
            scrobbled = false
            playbackGeneration++
            scrobbleInFlight = false
            scrobbleAttempts = 0
            scrobbleRetryAtMs = 0L
            nowPlayingSession = null
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
                if (startedAtSeconds == 0L) startedAtSeconds = System.currentTimeMillis() / 1_000
                player.currentMediaItem?.let(::submitNowPlaying)
                listenedMs += (now - lastPollMs).coerceIn(0L, SleepTimerPollMillis * 2)
                val track = player.currentMediaItem?.lastFmTrack(player.duration)
                if (!scrobbled && !scrobbleInFlight && scrobbleAttempts < 3 && now >= scrobbleRetryAtMs && track != null &&
                    shouldScrobble(track, listenedMs, loadLastFmExcludedUris(), lastFmScrobblingEnabled())) {
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
                val downloads=OfflineDownloads.get(this)
                val offline=runCatching { downloads.load(); downloads.offlineTrack(Track("","","",spec.uri.toString())) }.getOrNull()
                val audio = offline?.uri ?: resolvePlaybackSource(spec.uri.toString()) { track ->
                    OnlineMusic.resolve(track, streamAudioFormat(), streamAudioQuality())
                }
                // The audio CDN's un-ranged initial response is slower; native seek ranges take priority.
                spec.withUri(Uri.parse(audio)).withRequestHeaders(playbackRequestHeaders(spec.uri.toString(), spec.httpRequestHeaders))
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
        if (!lastFmScrobblingEnabled()) { nowPlayingSession = null; return }
        val session = loadLastFmSession() ?: return
        if (nowPlayingSession == session.sessionKey) return
        val track = mediaItem.lastFmTrack(player.duration) ?: return
        if (track.catalogUri in loadLastFmExcludedUris()) return
        val config = loadLastFmConfiguration()
        if (!config.signerConfigured) return
        val generation = playbackGeneration
        nowPlayingSession = session.sessionKey
        lastFmExecutor.execute {
            if (playbackGeneration != generation || !lastFmScrobblingEnabled() || track.catalogUri in loadLastFmExcludedUris() || loadLastFmSession() != session) return@execute
            val submitted = runCatching { LastFmSignerApi.updateNowPlaying(config.signerUrl, config.clientToken, session, track) }
            saveLastFmSubmissionStatus(if (submitted.isSuccess) "Now Playing accepted by Last.fm" else "Now Playing failed. Check your signer and Last.fm session.")
        }
    }

    private fun submitScrobble(track: Track, timestamp: Long) {
        if (!lastFmScrobblingEnabled() || track.catalogUri in loadLastFmExcludedUris()) return
        val session = loadLastFmSession() ?: return
        val config = loadLastFmConfiguration()
        if (!config.signerConfigured) return
        val generation = playbackGeneration
        scrobbleInFlight = true
        scrobbleAttempts++
        lastFmExecutor.execute {
            val submitted = runCatching {
                check(lastFmScrobblingEnabled() && track.catalogUri !in loadLastFmExcludedUris() && loadLastFmSession() == session)
                LastFmSignerApi.scrobble(config.signerUrl, config.clientToken, session, track, timestamp)
            }
            saveLastFmSubmissionStatus(if (submitted.isSuccess) "Scrobble accepted by Last.fm" else "Scrobble not accepted. Check your signer and Last.fm session.")
            handler.post {
                if (playbackGeneration == generation) {
                    scrobbleInFlight = false
                    scrobbled = submitted.isSuccess
                    scrobbleRetryAtMs = android.os.SystemClock.elapsedRealtime() + 60_000L
                }
            }
        }
    }
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
