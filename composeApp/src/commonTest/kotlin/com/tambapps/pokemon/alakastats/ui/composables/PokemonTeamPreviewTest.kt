package com.tambapps.pokemon.alakastats.ui.composables

import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.PokemonName
import kotlin.test.Test
import kotlin.test.assertEquals

class PokemonTeamPreviewTest {

    private val tyranitar = PokemonName("Tyranitar")
    private val garchomp = PokemonName("Garchomp")

    @Test
    fun aPokemonHoldingItsMegaStoneShowsAsItsMegaWithoutItem() {
        val shown = withMegaForms(listOf(tyranitar), listOf(ItemName("Tyranitarite")))

        assertEquals(listOf(PokemonName("tyranitar-mega") to null), shown)
    }

    @Test
    fun anotherPokemonsMegaStoneIsShownAsAnItem() {
        val stone = ItemName("Tyranitarite")

        val shown = withMegaForms(listOf(garchomp), listOf(stone))

        assertEquals(listOf(garchomp to stone), shown)
    }

    @Test
    fun otherItemsAreKeptAndMissingOnesAreNull() {
        val berry = ItemName("Sitrus Berry")

        val shown = withMegaForms(listOf(tyranitar, garchomp), listOf(berry))

        assertEquals(listOf(tyranitar to berry, garchomp to null), shown)
    }
}
