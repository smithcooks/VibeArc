package com.vibearc.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackSeekBar(position: Long, duration: Long, playing: Boolean, dragging: Boolean,
    wavy: Boolean, onChange: (Float) -> Unit, onFinish: () -> Unit) {
    val motion = LocalMotionEnabled.current
    // Only this slider interpolates progress; the player/artwork tree is not a frame subscriber.
    val displayed by animateFloatAsState(position.coerceIn(0L, duration).toFloat(),
        if (playing && !dragging && motion) tween(500, easing = LinearEasing) else snap(), label = "Seek progress")
    val accent = MaterialTheme.colorScheme.primary
    Slider(value = displayed, onValueChange = onChange, onValueChangeFinished = onFinish,
        valueRange = 0f..duration.toFloat(),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Playback position" },
        thumb = { Box(Modifier.size(18.dp).background(accent, CircleShape)) },
        track = { state -> SeekWaveTrack(state, duration.toFloat(), wavy, playing && !dragging && motion) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekWaveTrack(state: SliderState, duration: Float, wavy: Boolean, moving: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = .22f)
    val phase = if (wavy && moving) rememberInfiniteTransition(label = "Seek wave").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "Wave travel")
        else remember { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxWidth().height(30.dp).drawWithCache {
        val wavelength = 28.dp.toPx()
        val amplitude = 3.dp.toPx()
        val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
        val gap = 11.dp.toPx()
        val path = Path()
        // One cached curve; animation only translates it, never rebuilds overlapping waves.
        val steps = kotlin.math.ceil((size.width + wavelength) / wavelength * 16).toInt()
        for (i in 0..steps) {
            val x = i * wavelength / 16f
            val y = size.height / 2 + seekWaveY(x, wavelength, amplitude)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        onDrawBehind {
            val end = size.width * (state.value / duration).coerceIn(0f, 1f)
            val activeEnd = (end - gap).coerceAtLeast(0f)
            val inactiveStart = (end + gap).coerceAtMost(size.width)
            if (inactiveStart < size.width) drawLine(inactive, Offset(inactiveStart, center.y),
                Offset(size.width, center.y), stroke.width, StrokeCap.Round)
            if (activeEnd > 0f) {
                if (wavy) clipRect(right = activeEnd) {
                    translate(left = -phase.value * wavelength) { drawPath(path, accent, style = stroke) }
                } else drawLine(accent, Offset(0f, center.y), Offset(activeEnd, center.y), stroke.width, StrokeCap.Round)
            }
        }
    })
}
