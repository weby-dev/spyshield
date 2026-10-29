package com.spyshield.app.ui

import com.spyshield.app.testApp
import org.junit.Assert.assertEquals
import org.junit.Test

class PrivacyScoreTest {

    private val high get() = testApp(riskScore = 80)
    private val medium get() = testApp(riskScore = 40)
    private val low get() = testApp(riskScore = 10)

    @Test fun perfectSetupScores100() =
        assertEquals(100, privacyScore(protection = true, hasUsage = true, apps = emptyList(), alerts24h = 0))

    @Test fun protectionOffCosts20() =
        assertEquals(80, privacyScore(false, true, emptyList(), 0))

    @Test fun missingUsageAccessCosts10() =
        assertEquals(90, privacyScore(true, false, emptyList(), 0))

    @Test fun bothSetupPenaltiesStack() =
        assertEquals(70, privacyScore(false, false, emptyList(), 0))

    @Test fun riskLevelsPenaliseByWeight() {
        assertEquals(94, privacyScore(true, true, listOf(high), 0))
        assertEquals(98, privacyScore(true, true, listOf(medium), 0))
        assertEquals(100, privacyScore(true, true, listOf(low), 0))
        assertEquals(92, privacyScore(true, true, listOf(high, medium, low), 0))
    }

    @Test fun appPenaltyCappedAt40() {
        // 6 HIGH = 36, still under the cap.
        assertEquals(64, privacyScore(true, true, List(6) { high }, 0))
        // 7 HIGH = 42 -> capped to 40.
        assertEquals(60, privacyScore(true, true, List(7) { high }, 0))
        assertEquals(60, privacyScore(true, true, List(50) { high }, 0))
    }

    @Test fun systemAppsIgnored() {
        val sys = List(10) { testApp(riskScore = 99, isSystem = true) }
        assertEquals(100, privacyScore(true, true, sys, 0))
        assertEquals(94, privacyScore(true, true, sys + high, 0))
    }

    @Test fun alertPenaltyCappedAt30() {
        assertEquals(90, privacyScore(true, true, emptyList(), 1))
        assertEquals(80, privacyScore(true, true, emptyList(), 2))
        assertEquals(70, privacyScore(true, true, emptyList(), 3))
        assertEquals(70, privacyScore(true, true, emptyList(), 100))
    }

    @Test fun worstCaseBottomsOutAtZero() =
        assertEquals(0, privacyScore(false, false, List(20) { high }, 20))

    @Test fun resultClampedTo100() =
        // A nonsensical negative count must never push the score above 100.
        assertEquals(100, privacyScore(true, true, emptyList(), -5))
}
