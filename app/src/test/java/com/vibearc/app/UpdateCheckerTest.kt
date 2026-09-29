package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun `semantic versions compare numerically`() {
        assertTrue(isNewerVersion("0.10.0", "0.9.9"))
        assertEquals(false, isNewerVersion("0.9.0", "0.9.0-beta"))
    }

    @Test
    fun `release response accepts only the VibeArc GitHub page`() {
        assertEquals(
            AppUpdate("0.10.0", "https://github.com/Akumukage/VibeArc/releases/tag/v0.10.0"),
            parseAppUpdate("""{"tag_name":"v0.10.0","html_url":"https://github.com/Akumukage/VibeArc/releases/tag/v0.10.0"}"""),
        )
        assertEquals(null, parseAppUpdate("""{"tag_name":"v9","html_url":"https://evil.example/release"}"""))
    }
}
