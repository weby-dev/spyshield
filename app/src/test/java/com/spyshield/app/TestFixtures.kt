package com.spyshield.app

import com.spyshield.app.data.AppPrivacyInfo

/** Builds an [AppPrivacyInfo] where only the fields the logic under test reads matter. */
fun testApp(
    riskScore: Int,
    isSystem: Boolean = false,
    packageName: String = "com.example.app$riskScore",
) = AppPrivacyInfo(
    packageName = packageName,
    label = packageName,
    isSystem = isSystem,
    cameraRequested = true,
    cameraGranted = true,
    micRequested = false,
    micGranted = false,
    sideloaded = false,
    lastUsed = 0L,
    riskScore = riskScore,
    reasons = emptyList(),
    uses = 0,
    suspiciousUses = 0,
)
