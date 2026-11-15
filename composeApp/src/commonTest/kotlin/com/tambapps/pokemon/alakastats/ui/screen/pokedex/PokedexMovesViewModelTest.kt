package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.domain.model.DamageClass
import com.tambapps.pokemon.alakastats.domain.model.MoveClassification
import com.tambapps.pokemon.alakastats.infrastructure.repository.LocalPokedexRepository
import com.tambapps.pokemon.alakastats.ui.model.MoveAccuracyRange
import com.tambapps.pokemon.alakastats.ui.model.MovePowerRange
import com.tambapps.pokemon.alakastats.ui.model.MoveTargetGroup
import com.tambapps.pokemon.alakastats.ui.model.PokedexMoveFilters
import com.tambapps.pokemon.alakastats.ui.model.PokedexMoveSort
import com.tambapps.pokemon.alakastats.ui.model.PokedexMoveSortCriterion
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The list of the moves, on the bundled Pokédex */
class PokedexMovesViewModelTest {

    private val viewModel = PokedexMovesViewModel(LocalPokedexRepository(Json { ignoreUnknownKeys = true }))

    private fun ids() = viewModel.moves.map { it.id }

    @Test
    fun everyMoveIsListedByName() = runTest {
        viewModel.load()
        assertEquals(viewModel.allMoves.size, viewModel.moves.size)
        assertEquals(viewModel.moves.sortedBy { it.name.en }, viewModel.moves)
    }

    @Test
    fun aMoveIsSearchedByNameOrDescription() = runTest {
        viewModel.load()
        viewModel.query = "Distorsion"
        assertEquals(listOf("trick-room"), ids())
        viewModel.query = "first turn out"
        assertEquals(listOf("fake-out", "first-impression"), ids())
    }

    @Test
    fun theMovesAreFilteredByClassification() = runTest {
        viewModel.load()
        viewModel.filters = PokedexMoveFilters(classification = MoveClassification.PUNCHING)
        assertTrue("drain-punch" in ids())
        assertTrue(viewModel.moves.all { MoveClassification.PUNCHING in it.classifications })
        viewModel.filters = PokedexMoveFilters(classification = MoveClassification.EXPLOSIVE)
        assertTrue("explosion" in ids())
        viewModel.filters = PokedexMoveFilters(classification = MoveClassification.MENTAL)
        assertTrue("taunt" in ids())
    }

    @Test
    fun theFiltersAreCombined() = runTest {
        viewModel.load()
        viewModel.filters = PokedexMoveFilters(
            category = DamageClass.SPECIAL,
            type = PokeType.FIRE,
            target = MoveTargetGroup.ALL_FOES,
        )
        assertTrue("heat-wave" in ids())
        assertTrue(viewModel.moves.all { it.damageClass == DamageClass.SPECIAL && it.type == PokeType.FIRE && it.target == "allAdjacentFoes" })
        assertEquals(3, viewModel.filters.activeCount)
        viewModel.clearFilters()
        assertEquals(viewModel.allMoves.size, viewModel.moves.size)
    }

    @Test
    fun theMovesAreFilteredByPriorityPpPowerAndAccuracy() = runTest {
        viewModel.load()
        viewModel.filters = PokedexMoveFilters(priority = 2)
        assertTrue("extreme-speed" in ids() && "follow-me" in ids())
        // Champions' PP: Protect has 8
        viewModel.filters = PokedexMoveFilters(pp = 8)
        assertTrue("protect" in ids())
        viewModel.filters = PokedexMoveFilters(power = MovePowerRange.NONE)
        assertTrue(viewModel.moves.all { it.damageClass == DamageClass.STATUS || it.basePower == 0 })
        viewModel.filters = PokedexMoveFilters(accuracy = MoveAccuracyRange.NEVER_MISSES)
        assertTrue("aerial-ace" in ids())
        // the values of the filters are the ones of the moves
        assertEquals(5, viewModel.priorities.first())
        assertEquals(listOf(1, 8, 12, 16, 20), viewModel.pps)
    }

    @Test
    fun theMovesAreSortedByPriority() = runTest {
        viewModel.load()
        viewModel.sort = PokedexMoveSort(PokedexMoveSortCriterion.PRIORITY)
        assertEquals("helping-hand", ids().first())
        val priorities = viewModel.moves.map { it.priority }
        assertEquals(priorities.sortedDescending(), priorities)
    }
}
