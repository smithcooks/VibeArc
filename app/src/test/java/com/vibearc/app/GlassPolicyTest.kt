package com.vibearc.app

import org.junit.Assert.*
import org.junit.Test

class GlassPolicyTest {
    @Test fun supportedDevicesUseBackdrop() {
        assertTrue(glassBackdropEnabled(true, 31, false))
        assertTrue(glassBackdropEnabled(true, 36, false))
    }
    @Test fun unsupportedOrDisabledGlassNeverRecordsBackdrop() {
        assertFalse(glassBackdropEnabled(true, 30, false))
        assertFalse(glassBackdropEnabled(true, 31, true))
        assertFalse(glassBackdropEnabled(false, 36, false))
    }
}
