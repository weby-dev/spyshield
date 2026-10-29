package com.spyshield.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uses the real org.json (testImplementation) since android.jar only ships stubs. */
class AccessEventJsonTest {

    private fun roundTrip(e: AccessEvent): AccessEvent =
        AccessEvent.fromJson(JSONObject(e.toJson().toString()))

    @Test fun fullEventRoundTrips() {
        val e = AccessEvent(
            id = 42L,
            sensor = Sensor.CAMERA,
            packageName = "com.sneaky.app",
            appLabel = "Sneaky",
            start = 1_700_000_000_000L,
            end = 1_700_000_060_000L,
            suspicious = true,
            reason = "Used in background",
            suspects = listOf("com.a", "com.b"),
            blocked = true,
            confident = true,
            autoBlocked = true,
        )
        assertEquals(e, roundTrip(e))
    }

    @Test fun nullPackageAndOngoingEventRoundTrip() {
        val e = AccessEvent(
            id = 7L,
            sensor = Sensor.MIC,
            packageName = null,
            appLabel = "Unknown app",
            start = 123L,
            end = null,
            suspicious = false,
            reason = "",
        )
        val back = roundTrip(e)
        assertNull(back.packageName)
        assertNull(back.end)
        assertTrue(back.suspects.isEmpty())
        assertFalse(back.blocked)
        assertFalse(back.confident)
        assertFalse(back.autoBlocked)
        assertEquals(e, back)
    }

    @Test fun zeroEndIsKept() {
        // Only the -1 sentinel means "still in use"; 0 is a valid timestamp.
        val e = AccessEvent(1L, Sensor.MIC, "p", "P", 0L, 0L, false, "")
        assertEquals(0L, roundTrip(e).end)
    }

    @Test fun legacyJsonWithoutNewFieldsDecodes() {
        val o = JSONObject()
            .put("id", 3L)
            .put("sensor", "CAMERA")
            .put("pkg", "com.x")
            .put("label", "X")
            .put("start", 10L)
            .put("end", 20L)
            .put("suspicious", true)
            .put("reason", "r")
        val e = AccessEvent.fromJson(o)
        assertEquals("com.x", e.packageName)
        assertEquals(20L, e.end)
        assertTrue(e.suspects.isEmpty())
        assertFalse(e.blocked)
        assertFalse(e.confident)
        assertFalse(e.autoBlocked)
    }
}
