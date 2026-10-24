package com.astrolingo.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyTest {
    // A day with sunrise at 07:38 and sunset at 17:47, counted in minutes from midnight.
    private val rise = minutes(7, 38)
    private val set = minutes(17, 47)

    private fun minutes(h: Int, m: Int): Long = (h * 60L + m) * 60_000L

    private fun at(h: Int, m: Int) = SkyPhase.at(minutes(h, m), rise, set, hour = h)

    @Test
    fun followsSunriseAndSunset() {
        assertEquals(SkyPhase.NIGHT, at(5, 0))
        assertEquals(SkyPhase.DAWN, at(7, 0))
        assertEquals(SkyPhase.DAWN, at(8, 30))
        assertEquals(SkyPhase.DAY, at(10, 42))
        assertEquals(SkyPhase.DUSK, at(17, 0))
        assertEquals(SkyPhase.DUSK, at(18, 30))
        assertEquals(SkyPhase.NIGHT, at(20, 0))
    }

    @Test
    fun withoutSunriseTheHourDecides() {
        assertEquals(SkyPhase.NIGHT, SkyPhase.at(0, null, null, hour = 2))
        assertEquals(SkyPhase.DAWN, SkyPhase.at(0, null, null, hour = 6))
        assertEquals(SkyPhase.DAY, SkyPhase.at(0, null, null, hour = 12))
        assertEquals(SkyPhase.DUSK, SkyPhase.at(0, null, null, hour = 18))
        assertEquals(SkyPhase.NIGHT, SkyPhase.at(0, null, null, hour = 22))
    }

    @Test
    fun onlyTheNightSkyShowsTheMoon() {
        assertEquals(listOf(SkyPhase.NIGHT), SkyPhase.entries.filter { !it.sunUp })
    }

    @Test
    fun moonGrowsToFullThenShrinks() {
        assertTrue(moonLit(1) < 0.05f)
        assertEquals(0.5f, moonLit(8), 0.06f)
        assertTrue(moonLit(15) > 0.98f)
        assertEquals(0.5f, moonLit(23), 0.06f)
        assertTrue(moonLit(30) < 0.05f)
        assertTrue((1..14).all { moonLit(it) < moonLit(it + 1) })
    }
}
