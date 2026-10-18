package com.vibearc.app

import org.junit.Assert.assertNull
import org.junit.Test

class DownloadLyricsBudgetTest {
    @Test(timeout=1000) fun `expired download lyrics budget skips every network fallback`() {
        assertNull(LyricsProvider.fetch(Track("Budget test recording", "Budget test artist", "",
            "https://music.youtube.com/watch?v=abcdefghijk"),budgetMs=0))
    }
}
