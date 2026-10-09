package com.vibearc.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

internal fun contrastRatio(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    return (maxOf(a, b) + .05f) / (minOf(a, b) + .05f)
}

internal fun readableAccent(accent: Color, surface: Color): Color {
    val opaque = accent.copy(alpha = 1f)
    val target = if (surface.luminance() > .5f) Color.Black else Color.White
    for (step in 0..20) {
        val candidate = lerp(opaque, target, step / 20f)
        if (contrastRatio(candidate, surface) >= 4.5f) return candidate
    }
    return target
}
