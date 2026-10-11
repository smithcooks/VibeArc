package com.vibearc.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private fun Context.standbyActivity(): Activity? = when(this) {
    is Activity -> this
    is ContextWrapper -> baseContext.takeUnless { it === this }?.standbyActivity()
    else -> null
}

@Composable
internal fun StandbyScreen(player: Player, track: Track, playing: Boolean, wavy: Boolean, lyricMotion: Boolean,
    showLyrics: Boolean) {
    val window = LocalContext.current.standbyActivity()?.window
    DisposableEffect(window) {
        val alreadyAwake = (window?.attributes?.flags ?: 0).and(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { if(!alreadyAwake) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    ReferenceSurface(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        RoundedCornerShape(32.dp), floating = true) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
            val wide = maxWidth > maxHeight
            if(wide && !showLyrics) {
                val coverSize = standbyCoverSize((maxWidth * .45f).value, maxHeight.value, false).dp
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.weight(.45f), contentAlignment = Alignment.Center) {
                        SlidingArtwork(player, track, Modifier.size(coverSize))
                    }
                    Column(Modifier.weight(.55f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        StandbyMetadata(track, false)
                        Spacer(Modifier.height(16.dp))
                        StandbyControls(player, track, playing, wavy, false)
                    }
                }
            } else if(wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    BoxWithConstraints(Modifier.weight(.40f).fillMaxHeight()) {
                        val coverSize = standbyCoverSize(maxWidth.value, maxHeight.value, true).dp
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            SlidingArtwork(player, track, Modifier.size(coverSize))
                            Spacer(Modifier.height(8.dp))
                            StandbyMetadata(track, true)
                            StandbyControls(player, track, playing, wavy, true)
                        }
                    }
                    Box(Modifier.weight(.60f).fillMaxHeight()) {
                        LyricsScreen(player, track, playing, wavy, lyricMotion, embedded = true)
                    }
                }
            } else {
                val coverSize = standbyCoverSize(maxWidth.value, if(showLyrics) maxHeight.value * .45f else maxHeight.value - 220f, false).dp
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if(showLyrics) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SlidingArtwork(player, track, Modifier.size(minOf(coverSize, 96.dp)))
                            Column(Modifier.weight(1f)) { StandbyMetadata(track, true); StandbyControls(player, track, playing, wavy, true) }
                        }
                        Box(Modifier.weight(1f)) { LyricsScreen(player, track, playing, wavy, lyricMotion, embedded = true) }
                    } else {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            SlidingArtwork(player, track, Modifier.size(coverSize))
                            Spacer(Modifier.height(16.dp))
                            StandbyMetadata(track, false)
                            Spacer(Modifier.height(16.dp))
                            StandbyControls(player, track, playing, wavy, false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandbyMetadata(track: Track, compact: Boolean) {
    Text(track.title, fontSize = if(compact) 18.sp else 28.sp, lineHeight = if(compact) 20.sp else 32.sp,
        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(track.artist, color = MutedText, fontSize = if(compact) 14.sp else 18.sp,
        lineHeight = if(compact) 16.sp else 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun StandbyControls(player: Player, track: Track, playing: Boolean, wavy: Boolean, compact: Boolean) {
    var position by remember(track.catalogUri) { mutableLongStateOf(0L) }
    var duration by remember(track.catalogUri) { mutableLongStateOf(track.durationMs.coerceAtLeast(1L)) }
    var dragging by remember(track.catalogUri) { mutableStateOf(false) }
    LaunchedEffect(player, track.catalogUri, playing) {
        while(isActive) {
            if(!dragging) position = player.currentPosition.coerceAtLeast(0)
            duration = player.duration.takeIf { it > 0 } ?: track.durationMs.coerceAtLeast(1)
            delay(if(playing) 500 else 1_000)
        }
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            PlayerAction("Previous track", "previous", enabled = player.hasPreviousMediaItem()) { player.seekToPreviousMediaItem() }
            PlayerAction(if(playing) "Pause" else "Play", if(playing) "pause" else "play", if(compact) 48.dp else 64.dp) {
                if(player.isPlaying) player.pause() else player.play()
            }
            PlayerAction("Next track", "next", enabled = player.hasNextMediaItem()) { player.seekToNextMediaItem() }
        }
        PlaybackSeekBar(position, duration, playing, dragging, wavy,
            { dragging = true; position = it.toLong() }, { player.seekTo(position); dragging = false })
        if(!compact) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(DateUtils.formatElapsedTime(position / 1_000), color = MutedText, fontSize = 12.sp)
            Text("−" + DateUtils.formatElapsedTime((duration - position).coerceAtLeast(0) / 1_000), color = MutedText, fontSize = 12.sp)
        }
    }
}
