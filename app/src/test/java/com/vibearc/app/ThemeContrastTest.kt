package com.vibearc.app

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test fun `all preset accents remain readable on light and dark surfaces`() {
        val accents = AccentPreset.entries.map { Color(it.argb ?: DEFAULT_ACCENT_ARGB) } +
            listOf(Color.Black, Color.White, Color.Transparent)
        for (surface in listOf(Color(0xFFFAFAFC), Color(0xFF242424))) {
            for (accent in accents) assertTrue(contrastRatio(readableAccent(accent, surface), surface) >= 4.5f)
        }
    }
}
