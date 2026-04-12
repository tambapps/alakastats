package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.Mechanic
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import kotlin.time.Instant
import kotlin.uuid.Uuid

enum class Format(
    val displayedName: String,
    val allowedMechanics: List<Mechanic>,
    val pokemonLevel: Int? = null
) {
    NONE("<none>", Mechanic.entries),
    REGULATION_MA("Regulation M-A", allowedMechanics = listOf(Mechanic.MEGA_EVOLUTION), pokemonLevel = 50),
    REGULATION_I("Regulation I", allowedMechanics = listOf(Mechanic.TERASTALLIZATION), pokemonLevel = 50),
    REGULATION_F("Regulation F", allowedMechanics = listOf(Mechanic.TERASTALLIZATION), pokemonLevel = 50),
    REGULATION_H("Regulation H", allowedMechanics = listOf(Mechanic.TERASTALLIZATION), pokemonLevel = 50),
}

val Teamlytics.usesLegacySystem
    get() = when(format) {
        Format.REGULATION_F, Format.REGULATION_H, Format.REGULATION_I -> true
        Format.NONE -> pokePaste.pokemons.any { p -> p.evs.any { it > 32 } }
        else -> false
    }
fun Format.usesLegacySystem(evs: PokeStats) = when(this) {
    Format.REGULATION_F, Format.REGULATION_H, Format.REGULATION_I -> true
    Format.NONE -> evs.any { it > 32 }
    else -> false
}

data class CommonFilters(
    val opponentTeamFilters: List<List<PokemonFilter>>
) {
    val isNotEmpty get() = opponentTeamFilters.isNotEmpty()
}

data class FormatData(
    val popularPokemons: List<PokemonName>,
    val commonFilters: CommonFilters
)

data class Teamlytics(
    val id: Uuid,
    val name: String,
    val pokePaste: PokePaste,
    val replays: List<ReplayAnalytics>,
    val sdNames: List<UserName>,
    val lastUpdatedAt: Instant,
    val data: TeamlyticsData,
    val notes: TeamlyticsNotes?,
    val matchupPlans: List<MatchupPlan>,
    val format: Format
) {
    val winRate get() = computeWinRatePercentage(sdNames, replays)

}

data class MatchupPlan(
    val id: Uuid,
    val name: String,
    val pokePaste: PokePaste?,
    val gamePlans: List<GamePlan>
)

data class GamePlan(
    val description: String,
    val composition: List<PokemonName>?, // the 4 pokemons to bring
    val exampleReplays: List<ReplayAnalytics>
)

data class TeamlyticsNotes(
    val teamNotes: String,
    val pokemonNotes: Map<PokemonName, String>
)

data class TeamlyticsData(
    val pokemonData: Map<PokemonName, PokemonData>
)

data class PokemonData(
    val name: PokemonName,
    val moves: Map<MoveName, PokemonMove>,
    val baseStatsPerForms: Map<PokemonName, PokeStats>,
) {
    fun baseStatsOf(pokemon: PokemonName) = baseStatsPerForms[pokemon.normalized]
    fun shouldReload() = !baseStatsPerForms.containsKey(name.normalized)
}


data class TeamlyticsPreview(
    val id: Uuid,
    val name: String,
    val sdNames: List<String>,
    val pokemons: List<PokemonName>,
    val nbReplays: Int,
    val winrate: Int,
    val lastUpdatedAt: Instant
)

fun Teamlytics.computeWinRatePercentage() = computeWinRatePercentage(sdNames, replays)

fun computeWinRatePercentage(sdNames: List<UserName>, replays: List<ReplayAnalytics>): Int {
    if (replays.isEmpty()) return 0
    val nbGames = replays.size
    val wonGames = replays.count { replay ->
        replay.winner?.let(sdNames::contains) ?: false
    }
    return wonGames * 100 / nbGames
}