package com.spyshield.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Legacy hero state. SAFE splits into CALM (score >= 70) or CAUTION; see [toHeroState]. */
enum class OrbState { OFF, SAFE, ALERT }

fun OrbState.toHeroState(score: Int): HeroState = when (this) {
    OrbState.OFF -> HeroState.OFF
    OrbState.ALERT -> HeroState.ALERT
    OrbState.SAFE -> if (score >= 70) HeroState.CALM else HeroState.CAUTION
}

/**
 * Legacy entry point, kept so existing callers compile. Renders the v2 [ShieldHero] with a
 * compact [ScoreReadout] underneath. New code should use [HeroCard] or [ShieldHero] directly.
 */
@Composable
fun ShieldOrb(score: Int, state: OrbState, modifier: Modifier = Modifier) {
    val hero = state.toHeroState(score)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ShieldHero(hero, size = 176.dp)
        Spacer(Modifier.height(Space.m))
        ScoreReadout(score, hero)
    }
}
