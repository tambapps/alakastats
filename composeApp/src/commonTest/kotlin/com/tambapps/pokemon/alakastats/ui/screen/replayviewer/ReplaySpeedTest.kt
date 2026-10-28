package com.tambapps.pokemon.alakastats.ui.screen.replayviewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReplaySpeedTest {

    @Test
    fun savedSpeedIsFoundBackByItsName() {
        assertEquals(ReplaySpeed.FAST, ReplaySpeed.fromName(ReplaySpeed.FAST.name))
        // never chosen, or from a later version
        assertEquals(ReplaySpeed.NORMAL, ReplaySpeed.fromName(null))
        assertEquals(ReplaySpeed.NORMAL, ReplaySpeed.fromName("WARP"))
    }

    @Test
    fun chosenSpeedIsToRemember() {
        val chosen = mutableListOf<ReplaySpeed>()
        val player = ReplayPlayer(showLog = true, onSpeedChange = chosen::add)

        player.startWith(ReplaySpeed.SLOW)
        player.changeSpeed(ReplaySpeed.HYPERFAST)

        // the speed started with was already remembered
        assertEquals(listOf(ReplaySpeed.HYPERFAST), chosen)
        assertEquals(ReplaySpeed.HYPERFAST, player.speed)
    }

    @Test
    fun pageStartsAtTheSpeed() {
        val html = replayHtml("|turn|1", asP2 = false, dark = false, showLog = true, speed = ReplaySpeed.REALLY_SLOW)

        assertTrue(html.contains("setSpeed('reallyslow');"))
    }
}
