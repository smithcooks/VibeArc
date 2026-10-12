package com.vibearc.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.*

open class GlassPlayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = MusicWidgets.refresh(context)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = MusicWidgets.refresh(context)
    override fun onDeleted(context: Context, ids: IntArray) = MusicWidgets.refresh(context)
}

class GlassAlbumWidget : GlassPlayerWidget()
class GlassVinylWidget : GlassPlayerWidget()
class GlassConsoleWidget : GlassPlayerWidget()
class GlassLyricsWidget : GlassPlayerWidget()
class GlassMiniWidget : GlassPlayerWidget()
class GlassAuroraWidget : GlassPlayerWidget()

internal object MusicWidgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var active: WidgetPlayback? = null
    private var artworkJob: Job? = null
    private var lyricsJob: Job? = null
    @Volatile private var lyrics: LyricsDocument? = null
    @Volatile private var lyricTrack: Track? = null
    private val main = Handler(Looper.getMainLooper())
    private var lastTickSecond = -1L
    private var placedWidgets = false
    private var placedLyrics = false
    private val layouts = listOf(
        GlassPlayerWidget::class.java to R.layout.widget_player,
        GlassAlbumWidget::class.java to R.layout.widget_album,
        GlassVinylWidget::class.java to R.layout.widget_vinyl,
        GlassConsoleWidget::class.java to R.layout.widget_console,
        GlassLyricsWidget::class.java to R.layout.widget_lyrics,
        GlassMiniWidget::class.java to R.layout.widget_mini,
        GlassAuroraWidget::class.java to R.layout.widget_aurora,
    )

    fun publish(context: Context, player: Player) {
        val track = player.currentMediaItem?.track
        if (lyricTrack != track) {
            lyricsJob?.cancel()
            lyricsJob = null
            lyricTrack = track
            lyrics = null
            if (track != null && hasLyricsWidget()) {
                val app = context.applicationContext
                lyricsJob = scope.launch {
                    val result = runCatching { LyricsOverrideStore(app).load(track)?.first ?: LyricsProvider.fetch(track) }.getOrNull()
                    if (isActive && lyricTrack == track) lyrics = result
                }
            }
        }
        val state = widgetPlayback(track,
            player.playWhenReady && player.playbackState in setOf(Player.STATE_BUFFERING, Player.STATE_READY),
            player.hasPreviousMediaItem(), player.hasNextMediaItem()).copy(
                progress = widgetProgress(player.currentPosition, player.duration),
                lyric = widgetLyric(lyrics, player.currentPosition),
            )
        active = state
        context.getSharedPreferences("vibearc_widget", Context.MODE_PRIVATE).edit()
            .putString("title", state.title).putString("artist", state.artist).putString("artwork", state.artwork).apply()
        refresh(context)
        artworkJob?.cancel()
        if (state.artwork.isBlank() || !hasWidgets()) return
        val app = context.applicationContext
        artworkJob = scope.launch {
            ArtworkCache.initialize(app.cacheDir)
            val artwork = ArtworkCache.load(state.artwork, 384)
            if (isActive && artwork != null) main.post {
                val current = active
                if (current != null && current.artwork == state.artwork) render(app, current, artwork.bitmap)
            }
        }
    }

    fun stopped(context: Context) {
        artworkJob?.cancel()
        lyricsJob?.cancel()
        lyricsJob = null
        lyricTrack = null
        lyrics = null
        active = null
        refresh(context)
    }

    // Reuse the service tick, never a frame loop; only update changed progress/lyrics.
    fun tick(context: Context, player: Player) {
        val second = android.os.SystemClock.elapsedRealtime() / 1_000
        if (lastTickSecond == second || !hasWidgets()) return
        lastTickSecond = second
        val state = active ?: return
        if (hasLyricsWidget() && lyricTrack != null && lyricsJob == null) {
            val track = lyricTrack ?: return
            val app = context.applicationContext
            lyricsJob = scope.launch {
                val result = runCatching { LyricsOverrideStore(app).load(track)?.first ?: LyricsProvider.fetch(track) }.getOrNull()
                if (isActive && lyricTrack == track) lyrics = result
            }
        }
        val updated = state.copy(progress = widgetProgress(player.currentPosition, player.duration),
            lyric = widgetLyric(lyrics, player.currentPosition))
        if (updated != state) {
            active = updated
            refresh(context)
        }
    }

    private fun hasLyricsWidget() = placedLyrics

    fun refresh(context: Context) {
        val prefs = context.getSharedPreferences("vibearc_widget", Context.MODE_PRIVATE)
        val state = active ?: widgetPlayback(null, false, false, false).copy(
            title = prefs.getString("title", "VibeArc").orEmpty(),
            artist = prefs.getString("artist", context.getString(R.string.widget_empty)).orEmpty(),
            artwork = prefs.getString("artwork", "").orEmpty(),
        )
        render(context, state, ArtworkCache.peek(state.artwork, 384)?.bitmap)
    }

    private fun hasWidgets(): Boolean = placedWidgets

    private fun render(context: Context, state: WidgetPlayback, bitmap: android.graphics.Bitmap?) {
        val thumbnail = bitmap?.let {
            if (it.width > 384 || it.height > 384) android.graphics.Bitmap.createScaledBitmap(it, 384, 384, true) else it
        }
        val manager = AppWidgetManager.getInstance(context)
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val open = PendingIntent.getActivity(context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        placedWidgets = false
        placedLyrics = false
        for ((provider, layout) in layouts) {
            val ids = manager.getAppWidgetIds(ComponentName(context, provider))
            if (ids.isEmpty()) continue
            placedWidgets = true
            if (provider == GlassLyricsWidget::class.java) placedLyrics = true
            fun viewFor(layout: Int): RemoteViews {
                val views = RemoteViews(context.packageName, layout)
                views.setTextViewText(R.id.widget_title, state.title)
                views.setTextViewText(R.id.widget_artist, state.artist)
                if (layout == R.layout.widget_lyrics) views.setTextViewText(R.id.widget_lyric, state.lyric.ifBlank { context.getString(R.string.widget_lyrics_empty) })
                if (layout in listOf(R.layout.widget_console, R.layout.widget_lyrics, R.layout.widget_aurora)) {
                    views.setProgressBar(R.id.widget_progress, 100, state.progress, false)
                }
                if (thumbnail != null) views.setImageViewBitmap(R.id.widget_art, thumbnail)
                else views.setImageViewResource(R.id.widget_art, R.mipmap.vibearc_icon)
                for (id in listOf(R.id.widget_open, R.id.widget_title, R.id.widget_artist, R.id.widget_art)) {
                    views.setOnClickPendingIntent(id, open)
                }
                if (layout == R.layout.widget_lyrics) views.setOnClickPendingIntent(R.id.widget_lyric, open)
                views.setImageViewResource(R.id.widget_play,
                    if (state.playing) R.drawable.widget_pause else R.drawable.widget_play)
                views.setContentDescription(R.id.widget_play, if (state.hasTrack) state.playLabel else context.getString(R.string.widget_open))
                for ((id, action, enabled) in listOf(Triple(R.id.widget_previous, "previous", state.previous),
                        Triple(R.id.widget_play, "play", true), Triple(R.id.widget_next, "next", state.next))) {
                    views.setBoolean(id, "setEnabled", enabled)
                    views.setFloat(id, "setAlpha", if (enabled) 1f else .35f)
                    val command = PendingIntent.getBroadcast(context, id,
                        Intent(context, WidgetControlReceiver::class.java).setAction("${context.packageName}.widget.$action"),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(id, if (state.hasTrack) command else open)
                }
                return views
            }
            val full = viewFor(layout)
            val preferred = when (layout) {
                R.layout.widget_vinyl -> android.util.SizeF(196f, 288f)
                R.layout.widget_album -> android.util.SizeF(220f, 320f)
                R.layout.widget_player -> android.util.SizeF(220f, 160f)
                R.layout.widget_aurora -> android.util.SizeF(252f, 160f)
                else -> android.util.SizeF(252f, 192f)
            }
            val views = if (layout == R.layout.widget_mini) full
                else if (android.os.Build.VERSION.SDK_INT >= 31) RemoteViews(mapOf(
                    android.util.SizeF(196f, 112f) to viewFor(R.layout.widget_compact), preferred to full,
                )) else RemoteViews(viewFor(R.layout.widget_compact), full)
            runCatching { manager.updateAppWidget(ids, views) }.onFailure {
                android.util.Log.w("VibeArcWidget", "Widget update failed: ${it.javaClass.simpleName}")
            }
        }
    }
}

class WidgetControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action?.removePrefix("${context.packageName}.widget.")
        if (action !in setOf("play", "previous", "next")) return
        val pending = goAsync()
        val future = MediaController.Builder(context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
        val handler = Handler(Looper.getMainLooper())
        var finished = false
        fun finish() {
            if (finished) return
            finished = true
            MediaController.releaseFuture(future)
            pending.finish()
        }
        val timeout = Runnable { finish() }
        handler.postDelayed(timeout, 7_000)
        future.addListener({
            if (!finished) {
                runCatching {
                    val controller = future.get()
                    when (action) {
                        "previous" -> if (controller.hasPreviousMediaItem()) controller.seekToPreviousMediaItem()
                        "next" -> if (controller.hasNextMediaItem()) controller.seekToNextMediaItem()
                        "play" -> if (controller.playWhenReady) controller.pause() else if (controller.mediaItemCount > 0) controller.play()
                            else Toast.makeText(context, "Open VibeArc to choose your music", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { Toast.makeText(context, "Open VibeArc to resume playback", Toast.LENGTH_SHORT).show() }
                handler.removeCallbacks(timeout)
                handler.postDelayed({ finish() }, 500)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
