package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShowdownReplayFiltersTest {

    private fun player(name: String, team: List<String>, brought: List<String>) = ShowdownReplayPlayer(
        name = UserName(name),
        team = team.map(::PokemonName),
        items = emptyList(),
        brought = brought.map(::PokemonName),
        beforeElo = null,
        afterElo = null,
    )

    private val replay = ShowdownReplaySummary(
        id = "gen9championsvgc2026regmc-1",
        formatId = "gen9championsvgc2026regmc",
        uploadTime = 0,
        rating = 1500,
        winner = UserName("Alaka"),
        players = listOf(
            player("Alaka", listOf("whimsicott", "incineroar", "garchomp", "sneasler", "kingambit", "basculegion"), listOf("whimsicott", "garchomp", "incineroar")),
            player("Opponent", listOf("pelipper", "archaludon", "amoonguss", "dragonite", "sinistcha", "ursaluna"), listOf("pelipper", "archaludon")),
        ),
        bestOf = null,
    )

    private fun names(vararg names: String) = names.map(::PokemonName)

    private fun brought(vararg names: String) = names.map { PokemonFilter(PokemonName(it), asLead = false) }

    @Test
    fun theBroughtPokemonsHaveTheItemOfTheirTeamsOne() {
        val player = ShowdownReplayPlayer(
            name = UserName("Alaka"),
            team = names("charizard", "whimsicott", "garchomp"),
            items = listOf(com.tambapps.pokemon.ItemName("charizardite-y"), null, com.tambapps.pokemon.ItemName("focus-sash")),
            brought = names("garchomp", "charizard", "whimsicott"),
            beforeElo = null,
            afterElo = null,
        )
        assertEquals(listOf("focus-sash", "charizardite-y", null), player.broughtItems.map { it?.value })
    }

    @Test
    fun noFilterKeepsEverything() {
        assertTrue(ShowdownReplayFilters().matches(replay))
        assertTrue(ShowdownReplayFilters().isEmpty)
    }

    @Test
    fun aPokemonInATeamIsLookedForInBothTeams() {
        assertTrue(ShowdownReplayFilters(teamPokemons = names("whimsicott")).matches(replay))
        assertTrue(ShowdownReplayFilters(teamPokemons = names("amoonguss")).matches(replay))
        assertFalse(ShowdownReplayFilters(teamPokemons = names("rillaboom")).matches(replay))
    }

    @Test
    fun aPokemonInATeamMatchesItsOtherForms() {
        assertTrue(ShowdownReplayFilters(teamPokemons = names("ursaluna-bloodmoon")).matches(replay))
    }

    @Test
    fun aPokemonBroughtMustHaveGoneOnTheField() {
        assertTrue(ShowdownReplayFilters(broughtPokemons = brought("garchomp")).matches(replay))
        // in the team, but not brought
        assertFalse(ShowdownReplayFilters(broughtPokemons = brought("kingambit")).matches(replay))
    }

    @Test
    fun thePokemonsMustAllBeOnTheSamePlayersSide() {
        assertTrue(ShowdownReplayFilters(teamPokemons = names("whimsicott", "incineroar")).matches(replay))
        assertFalse(ShowdownReplayFilters(teamPokemons = names("whimsicott", "pelipper")).matches(replay))
        assertFalse(ShowdownReplayFilters(teamPokemons = names("whimsicott"), broughtPokemons = brought("pelipper")).matches(replay))
    }

    @Test
    fun aUserNameIsAPartOfAPlayersNameWhateverItsCase() {
        assertTrue(ShowdownReplayFilters(userName = "alak").matches(replay))
        assertTrue(ShowdownReplayFilters(userName = " OPPO ").matches(replay))
        assertFalse(ShowdownReplayFilters(userName = "someone").matches(replay))
    }

    @Test
    fun aUserNameAndPokemonsMustBeTheSamePlayers() {
        assertTrue(ShowdownReplayFilters(userName = "alaka", teamPokemons = names("whimsicott")).matches(replay))
        assertFalse(ShowdownReplayFilters(userName = "opponent", teamPokemons = names("whimsicott")).matches(replay))
    }

    @Test
    fun aPokemonAsLeadMustBeAmongTheFirstTwoBrought() {
        assertTrue(ShowdownReplayFilters(broughtPokemons = listOf(PokemonFilter(PokemonName("garchomp"), asLead = true))).matches(replay))
        // brought third
        assertFalse(ShowdownReplayFilters(broughtPokemons = listOf(PokemonFilter(PokemonName("incineroar"), asLead = true))).matches(replay))
        assertTrue(ShowdownReplayFilters(broughtPokemons = listOf(PokemonFilter(PokemonName("incineroar"), asLead = false))).matches(replay))
    }

    @Test
    fun winnerOnlyLooksForThePokemonsOnTheWinnersSide() {
        assertTrue(ShowdownReplayFilters(teamPokemons = names("whimsicott"), winnerOnly = true).matches(replay))
        assertFalse(ShowdownReplayFilters(teamPokemons = names("pelipper"), winnerOnly = true).matches(replay))
        assertFalse(ShowdownReplayFilters(winnerOnly = true).matches(replay.copy(winner = null)))
        assertFalse(ShowdownReplayFilters(winnerOnly = true).isEmpty)
    }

    @Test
    fun aMinimumRatingLeavesOutTheLowerAndTheUnratedReplays() {
        assertTrue(ShowdownReplayFilters(minRating = 1500).matches(replay))
        assertFalse(ShowdownReplayFilters(minRating = 1501).matches(replay))
        assertFalse(ShowdownReplayFilters(minRating = 1000).matches(replay.copy(rating = null)))
    }
}
