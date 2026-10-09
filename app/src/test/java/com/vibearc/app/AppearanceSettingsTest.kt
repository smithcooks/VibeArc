package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppearanceSettingsTest {
    @Test fun `theme choices follow system only when requested and retain old dark default`() {
        assertEquals(true, ThemeMode.Dark.isDark(false))
        assertEquals(false, ThemeMode.Light.isDark(true))
        assertEquals(true, ThemeMode.System.isDark(true))
        assertEquals(false, ThemeMode.System.isDark(false))
        assertEquals(ThemeMode.Dark, parseThemeMode(null))
        assertEquals(ThemeMode.Dark, parseThemeMode("invalid"))
        ThemeMode.entries.forEach { assertEquals(it, parseThemeMode(it.name)) }
        assertEquals(ThemeMode.Dark, AppearanceConfig().themeMode)
    }
    @Test
    fun `UI motion stops when reduced motion system animations or foreground state disallow it`() {
        assertEquals(true, uiMotionEnabled(false, true, true))
        assertEquals(false, uiMotionEnabled(true, true, true))
        assertEquals(false, uiMotionEnabled(false, false, true))
        assertEquals(false, uiMotionEnabled(false, true, false))
        assertEquals(false, AppearanceConfig().reduceMotion)
        assertEquals(true, AppearanceConfig().copy(reduceMotion = true).reduceMotion)
    }

    @Test
    fun `lyrics animation can be turned off without changing the theme`() {
        val original = AppearanceConfig()
        val disabled = original.copy(lyricsAnimationEnabled = false)
        assertEquals(true, original.lyricsAnimationEnabled)
        assertEquals(false, disabled.lyricsAnimationEnabled)
        assertEquals(original.resolvedAccentArgb(), disabled.resolvedAccentArgb())
        assertEquals(1f, lyricMotionTarget(true, false).first)
        assertEquals(0f, lyricMotionTarget(true, false).second)
        assertEquals(1.035f, lyricMotionTarget(true, true).first)
        assertEquals(-3f, lyricMotionTarget(true, true).second)
        assertEquals(1f, lyricMotionTarget(false, true).first)
    }

    @Test
    fun `six digit accent gains an opaque alpha channel`() {
        assertEquals(0xFFFF6B6BL, parseAccentHex("#ff6b6b"))
    }

    @Test
    fun `eight digit accent preserves its alpha channel`() {
        assertEquals(0x80D7A24AL, parseAccentHex("80D7A24A"))
    }

    @Test
    fun `malformed accent is rejected`() {
        assertNull(parseAccentHex("gold"))
        assertNull(parseAccentHex("#12345"))
    }

    @Test
    fun `custom preset and default accents resolve`() {
        val custom = AppearanceConfig(
            accentPreset = AccentPreset.Custom,
            customAccentArgb = 0xFF12AB34L,
        )

        assertEquals(0xFF12AB34L, custom.resolvedAccentArgb())
        assertEquals(0xFFE76F51L, AppearanceConfig(accentPreset = AccentPreset.Coral).resolvedAccentArgb())
        assertEquals(0xFFD5D5D5L, AppearanceConfig().resolvedAccentArgb())
    }

    @Test
    fun `playing artwork overrides the manual accent only when enabled`() {
        val artwork = 0xFF247BA0L
        val enabled = AppearanceConfig(
            dynamicNowPlayingEnabled = true,
            accentPreset = AccentPreset.Coral,
        )
        val disabled = enabled.copy(dynamicNowPlayingEnabled = false)

        assertEquals(artwork, enabled.activeAccentArgb(artwork))
        assertEquals(0xFFE76F51L, disabled.activeAccentArgb(artwork))
        assertEquals(0xFFE76F51L, enabled.activeAccentArgb(null))
    }
}
