package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppearanceSettingsTest {
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
