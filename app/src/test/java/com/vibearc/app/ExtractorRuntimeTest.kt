package com.vibearc.app

import org.junit.Assert.assertEquals
import org.junit.Test
import org.schabi.newpipe.extractor.utils.JavaScript

class ExtractorRuntimeTest {
    @Test fun extractorUsesTheInterpretedJavascriptPath() {
        assertEquals("cba", JavaScript.run(
            "function reverse(value) { return value.split('').reverse().join(''); }",
            "reverse", "abc",
        ))
    }
}
