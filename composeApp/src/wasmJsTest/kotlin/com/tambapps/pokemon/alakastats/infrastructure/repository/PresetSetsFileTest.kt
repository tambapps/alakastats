package com.tambapps.pokemon.alakastats.infrastructure.repository

import alakastats.composeapp.generated.resources.Res
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.ui.screen.damagecalc.MAX_STAT_POINTS_PER_STAT
import com.tambapps.pokemon.alakastats.ui.screen.damagecalc.MAX_TOTAL_STAT_POINTS
import com.tambapps.pokemon.alakastats.ui.screen.damagecalc.PICKABLE_POKEMON_NAMES
import com.tambapps.pokemon.champions.data.Ability
import com.tambapps.pokemon.champions.data.ChampionsDex
import com.tambapps.pokemon.champions.data.Item
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The bundled sets (converted from the Nerd of Now calculator's) can all be used by the damage calc: every species,
 * ability, item and move is known to Champions. A failure lists every problem, for the conversion script or the
 * Champions data to be fixed
 */
class PresetSetsFileTest {

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun entity() = json.decodeFromString<PresetSetsEntity>(Res.readBytes(PRESET_SETS_FILE).decodeToString())

    @Test
    fun everySetIsKept() = runTest {
        val entity = entity()
        val sets = entity.toDomain()
        assertTrue(entity.isNotEmpty())
        // a set with an unknown nature would be left out
        assertEquals(entity.values.sumOf { it.size }, entity.keys.sumOf { sets.of(PokemonName(it)).size })
    }

    @Test
    fun everySetCanBeUsed() = runTest {
        val pickableSpecies = PICKABLE_POKEMON_NAMES.map { it.normalized }.toSet()
        val problems = mutableListOf<String>()
        for (set in entity().toDomain().let { sets -> sets.species.flatMap { sets.of(it) } }) {
            val where = "${set.species.value} (${set.name})"
            if (set.species.normalized !in pickableSpecies) problems += "$where: species not pickable"
            val ability = set.ability
            if (ability == null || Ability.from(ability) == Ability.NO_ABILITY) problems += "$where: unknown ability $ability"
            set.item?.let { if (Item.from(it) == null) problems += "$where: unknown item ${it.value}" }
            set.moves.filter { ChampionsDex.moveOrNull(it) == null }.forEach { problems += "$where: unknown move ${it.value}" }
            val statPoints = Stat.entries.map { set.statPoints[it] }
            if (statPoints.any { it !in 0..MAX_STAT_POINTS_PER_STAT } || statPoints.sum() > MAX_TOTAL_STAT_POINTS) {
                problems += "$where: invalid stat points ${set.statPoints}"
            }
        }
        assertEquals(emptyList(), problems)
    }

    @Test
    fun aSetWithoutAbilityHasTheSpeciesDefaultOne() = runTest {
        val sets = entity().toDomain()
        val scizor = sets.of(PokemonName("Scizor")).first()
        assertEquals(ChampionsDex.species(PokemonName("Scizor")).defaultAbility, scizor.ability)
    }

    @Test
    fun theSetsAreInTheSourceOrder() = runTest {
        val sets = entity().toDomain()
        assertEquals(
            listOf("Sun Sleep Offense", "Bulky Mega Offense"),
            sets.of(PokemonName("Venusaur")).map { it.name },
        )
        assertEquals(ItemName("Venusaurite"), sets.of(PokemonName("Venusaur"))[1].item)
        assertEquals(MoveName("Leaf Storm"), sets.of(PokemonName("Venusaur")).first().moves.first())
    }
}
