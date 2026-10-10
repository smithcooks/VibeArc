package com.vibearc.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
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
import androidx.media3.common.Player

private fun Context.standbyActivity(): Activity? = when(this) {
    is Activity -> this
    is ContextWrapper -> baseContext.takeUnless { it === this }?.standbyActivity()
    else -> null
}

@Composable
internal fun StandbyScreen(player: Player, track: Track, playing: Boolean, wavy: Boolean, lyricMotion: Boolean) {
    val window = LocalContext.current.standbyActivity()?.window
    DisposableEffect(window) {
        val alreadyAwake = (window?.attributes?.flags ?: 0).and(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { if(!alreadyAwake) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
        val wide = maxWidth > maxHeight
        val coverSize = minOf(if(wide) maxHeight * .30f else 144.dp, 240.dp)
        val media: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SlidingArtwork(player, track, Modifier.size(coverSize))
                Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                PlayerTimeControls(player, track, playing, wavy, compact = true, timeLabels = false)
            }
        }
        val lyrics: @Composable () -> Unit = { LyricsScreen(player, track, playing, wavy, lyricMotion, embedded = true) }
        if(wide) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ReferenceSurface(Modifier.weight(.42f).fillMaxHeight(), RoundedCornerShape(32.dp), floating = true, content = media)
            ReferenceSurface(Modifier.weight(.58f).fillMaxHeight(), RoundedCornerShape(32.dp), floating = true, content = lyrics)
        } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ReferenceSurface(Modifier.fillMaxWidth(), RoundedCornerShape(32.dp), floating = true, content = media)
            ReferenceSurface(Modifier.fillMaxWidth().weight(1f), RoundedCornerShape(32.dp), floating = true, content = lyrics)
        }
    }
}
