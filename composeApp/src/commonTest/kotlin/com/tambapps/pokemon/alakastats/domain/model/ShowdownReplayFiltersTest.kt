package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.PokemonName
import kotlin.test.Test
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
        assertTrue(ShowdownReplayFilters(broughtPokemons = names("garchomp")).matches(replay))
        // in the team, but not brought
        assertFalse(ShowdownReplayFilters(broughtPokemons = names("kingambit")).matches(replay))
    }

    @Test
    fun thePokemonsMustAllBeOnTheSamePlayersSide() {
        assertTrue(ShowdownReplayFilters(teamPokemons = names("whimsicott", "incineroar")).matches(replay))
        assertFalse(ShowdownReplayFilters(teamPokemons = names("whimsicott", "pelipper")).matches(replay))
        assertFalse(ShowdownReplayFilters(teamPokemons = names("whimsicott"), broughtPokemons = names("pelipper")).matches(replay))
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
    fun aMinimumRatingLeavesOutTheLowerAndTheUnratedReplays() {
        assertTrue(ShowdownReplayFilters(minRating = 1500).matches(replay))
        assertFalse(ShowdownReplayFilters(minRating = 1501).matches(replay))
        assertFalse(ShowdownReplayFilters(minRating = 1000).matches(replay.copy(rating = null)))
    }
}
