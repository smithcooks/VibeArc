package com.vibearc.app

import android.app.ActivityManager
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

internal class GlassBackdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

internal val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

@Composable
internal fun rememberGlassBackdrop(enabled: Boolean): GlassBackdrop? {
    val context = LocalContext.current
    val lowRam = remember(context) { context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true }
    if (!glassBackdropEnabled(enabled, Build.VERSION.SDK_INT, lowRam)) return null
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

@Composable
internal fun GlassContent(backdrop: GlassBackdrop?, content: @Composable () -> Unit) {
    // Never let content record a glass surface that samples its own ancestor.
    CompositionLocalProvider(LocalGlassBackdrop provides null) {
        val recording = if (backdrop == null) Modifier else Modifier
            .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
            .drawWithContent {
                backdrop.layer.record { this@drawWithContent.drawContent() }
                drawLayer(backdrop.layer)
            }
        Box(Modifier.fillMaxSize().then(recording)) { content() }
    }
}

@Composable
internal fun Modifier.glassMaterial(base: Color, floating: Boolean, source: MutableInteractionSource?): Modifier {
    val glass = LocalGlass.current
    val light = MaterialTheme.colorScheme.background.luminance() > .5f
    val motion = LocalMotionEnabled.current
    val backdrop = LocalGlassBackdrop.current.takeIf { glass && floating }
    val lens = if (backdrop != null) rememberGraphicsLayer() else null
    var origin by remember { mutableStateOf(Offset.Zero) }
    var press by remember(source) { mutableStateOf<PressInteraction.Press?>(null) }
    var touch by remember(source) { mutableStateOf(Offset.Zero) }
    LaunchedEffect(source) {
        source?.interactions?.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> { touch = interaction.pressPosition; press = interaction }
                is PressInteraction.Release -> if (press == interaction.press) press = null
                is PressInteraction.Cancel -> if (press == interaction.press) press = null
            }
        }
    }
    val illumination = animateFloatAsState(if (glass && motion && press != null) 1f else 0f,
        tween(if (motion) 240 else 0), label = "Glass touch light")
    val positioned = if (backdrop == null) Modifier else Modifier.onGloballyPositioned { origin = it.positionInRoot() }
    return this.then(positioned).drawWithCache {
        val delta = backdrop?.origin?.minus(origin) ?: Offset.Zero
        var recorded = false
        val sheen = Brush.linearGradient(listOf(Color.White.copy(alpha = if (floating) .12f else .035f),
            Color.Transparent, Color.White.copy(alpha = if (floating) .035f else .01f)),
            start = Offset.Zero, end = Offset(size.width, size.height))
        if (lens != null) {
            lens.clip = true
            lens.renderEffect = BlurEffect(12.dp.toPx(), 12.dp.toPx(), TileMode.Clamp)
        }
        onDrawBehind {
            if (backdrop != null && lens != null && backdrop.layer.size.width > 0) {
                // Retain the display-list reference; scrolling updates the source RenderNode.
                if (!recorded) {
                    lens.record {
                        scale(1.025f) {
                            translate(delta.x, delta.y) { drawLayer(backdrop.layer) }
                        }
                    }
                    recorded = true
                }
                drawLayer(lens)
            }
            // Keep a legibility tint over artwork, with clearer controls in light mode.
            drawRect(base.copy(alpha = when {
                !glass -> 1f
                light && floating -> .58f
                light -> .86f
                floating -> .78f
                else -> .94f
            }))
            if (glass) {
                drawRect(sheen)
                val light = illumination.value
                if (light > .001f) drawRect(Brush.radialGradient(
                    listOf(Color.White.copy(alpha = .10f * light), Color.Transparent),
                    center = touch, radius = size.maxDimension.coerceAtLeast(1f)))
            }
        }
    }
}
