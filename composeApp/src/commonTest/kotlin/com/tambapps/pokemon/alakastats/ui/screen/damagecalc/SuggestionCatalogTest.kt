package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SuggestionCatalogTest {

    private val catalog = SuggestionCatalog(listOf("Moonblast", "Fake Out"))

    @Test
    fun aValueIsMatchedWithTheCatalogSpelling() {
        assertEquals("Moonblast", catalog.match("moonblast"))
        assertEquals("Fake Out", catalog.match("  fake out "))
    }

    @Test
    fun anUnknownValueIsNotMatched() {
        assertNull(catalog.match("Moon"))
    }

    @Test
    fun aBlankValueOnlyMatchesWhenAllowed() {
        assertNull(catalog.match(" "))
        assertEquals("", SuggestionCatalog(listOf("Leftovers"), allowBlank = true).match(" "))
    }

    @Test
    fun anEmptyMoveSlotAndNoItemAreAllowedButNoAbility() {
        assertEquals("", MOVE_CATALOG.match(""))
        assertEquals("", ITEM_CATALOG.match(""))
        assertNull(ABILITY_CATALOG.match(""))
    }
}
