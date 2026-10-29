package com.spyshield.app.data

import com.spyshield.app.testApp
import org.junit.Assert.assertEquals
import org.junit.Test

class RiskLevelTest {

    @Test fun thresholds() {
        assertEquals(RiskLevel.LOW, testApp(0).level)
        assertEquals(RiskLevel.LOW, testApp(34).level)
        assertEquals(RiskLevel.MEDIUM, testApp(35).level)
        assertEquals(RiskLevel.MEDIUM, testApp(59).level)
        assertEquals(RiskLevel.HIGH, testApp(60).level)
        assertEquals(RiskLevel.HIGH, testApp(100).level)
    }

    @Test fun negativeScoreIsLow() = assertEquals(RiskLevel.LOW, testApp(-10).level)
}
