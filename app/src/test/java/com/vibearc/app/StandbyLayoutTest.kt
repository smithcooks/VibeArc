package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StandbyLayoutTest {
    @Test fun `wrapped active verse moves from bottom to viewport center`() {
        assertEquals(124f, lyricCenterScrollDelta(0, 300, 240, 68), .01f)
    }
    @Test fun `short verse near top moves down to center`() {
        assertEquals(-132f, lyricCenterScrollDelta(0, 300, 0, 36), .01f)
    }
    @Test fun `content padding coordinates and centered verse need no correction`() {
        assertEquals(0f, lyricCenterScrollDelta(-150, 150, -34, 68), .01f)
    }
    @Test fun `lyrics cover leaves room for metadata transport and seek controls on short landscape`() {
        val size = standbyCoverSize(360f, 224f, true)
        assertTrue(size <= 80f)
        assertTrue(size + 144f <= 224f)
    }
    @Test fun `artwork mode has large artwork without exceeding available bounds`() {
        assertEquals(288f, standbyCoverSize(400f, 288f, false), .01f)
        assertEquals(120f, standbyCoverSize(120f, 400f, false), .01f)
    }
}
