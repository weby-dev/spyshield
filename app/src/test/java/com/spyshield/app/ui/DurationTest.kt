package com.spyshield.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationTest {

    @Test fun subSecond() {
        assertEquals("<1s", duration(0))
        assertEquals("<1s", duration(999))
        assertEquals("<1s", duration(-500))
    }

    @Test fun seconds() {
        assertEquals("1s", duration(1_000))
        assertEquals("1s", duration(1_999))
        assertEquals("59s", duration(59_999))
    }

    @Test fun minutes() {
        assertEquals("1m 0s", duration(60_000))
        assertEquals("2m 5s", duration(125_000))
        assertEquals("59m 59s", duration(3_599_999))
    }

    @Test fun hours() {
        assertEquals("1h 0m", duration(3_600_000))
        assertEquals("1h 1m", duration(3_661_000))
        assertEquals("25h 30m", duration(25L * 3_600_000 + 30 * 60_000))
    }
}
