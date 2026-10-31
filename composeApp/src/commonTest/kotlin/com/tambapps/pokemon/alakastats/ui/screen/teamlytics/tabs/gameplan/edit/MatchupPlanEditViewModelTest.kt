package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.gameplan.edit

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GamePlan
import com.tambapps.pokemon.alakastats.domain.model.MatchupPlan
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class MatchupPlanEditViewModelTest {

    private val paste = """
        Incineroar @ Sitrus Berry
        Ability: Intimidate
        - Fake Out

        Garchomp @ Choice Scarf
        Ability: Rough Skin
        - Earthquake
    """.trimIndent()

    // a Pokémon without moves doesn't parse
    private val invalidPaste = "Incineroar @ Sitrus Berry\nAbility: Intimidate"

    private val pokepasteParser = PokepasteParser()
    private val viewModel = MatchupPlanEditViewModel(
        pokepasteParser,
        HttpClient(MockEngine { respond("", HttpStatusCode.NotFound) })
    )

    private val incineroar = PokemonName("Incineroar")
    private val garchomp = PokemonName("Garchomp")

    private val matchupPlan = MatchupPlan(
        id = Uuid.random(),
        name = "Sun",
        pokePaste = pokepasteParser.parse(paste),
        gamePlans = listOf(
            GamePlan(description = "Fake Out the sun setter", composition = listOf(incineroar, garchomp), exampleReplays = emptyList()),
            GamePlan(description = "Play around Trick Room", composition = null, exampleReplays = emptyList()),
        )
    )

    @Test
    fun creationStartsWithAnEmptyGamePlan() {
        viewModel.prepareCreation()

        assertEquals(1, viewModel.gamePlanStates.size)
        assertFalse(viewModel.hasChanges)
        assertEquals("Name the matchup", viewModel.invalidReason)
    }

    @Test
    fun invalidReasonTellsWhatIsMissing() {
        viewModel.prepareCreation()
        viewModel.updateName("Sun")
        assertEquals("Game plan 1 is empty: choose its Pokémon or describe it", viewModel.invalidReason)

        viewModel.gamePlanStates.first().updateDescription("Fake Out the sun setter")
        assertNull(viewModel.invalidReason)
        assertTrue(viewModel.isFormValid)

        viewModel.deleteGamePlan(viewModel.gamePlanStates.first())
        assertEquals("Add at least one game plan", viewModel.invalidReason)
    }

    @Test
    fun deletingAGamePlanRemovesItFromTheMatchup() {
        viewModel.prepareEdition(matchupPlan)
        val first = viewModel.gamePlanStates.first()
        viewModel.gamePlanToDelete = first

        viewModel.deleteGamePlan(first)

        assertNull(viewModel.gamePlanToDelete)
        val edited = viewModel.generateMatchupPlan(matchupPlan)
        assertEquals(matchupPlan.id, edited.id)
        assertEquals(listOf("Play around Trick Room"), edited.gamePlans.map { it.description })
        assertTrue(viewModel.hasChanges)
    }

    @Test
    fun anUntouchedMatchupHasNoChanges() {
        viewModel.prepareEdition(matchupPlan)

        // the second plan's null composition comes back as an empty one
        assertFalse(viewModel.hasChanges)
        assertSame(matchupPlan.pokePaste, viewModel.generateMatchupPlan(matchupPlan).pokePaste)
    }

    @Test
    fun cancellingThePasteEditionRestoresThePaste() {
        viewModel.prepareEdition(matchupPlan)
        val pasteBefore = viewModel.pokepaste

        viewModel.startPokepasteEdition()
        viewModel.updatePokepaste(invalidPaste)
        viewModel.cancelPokepasteEdition()

        assertFalse(viewModel.isEditingPokepaste)
        assertEquals(pasteBefore, viewModel.pokepaste)
        assertNull(viewModel.pokepasteError)
        assertSame(matchupPlan.pokePaste, viewModel.opponentPokepaste)
        assertFalse(viewModel.hasChanges)
    }

    @Test
    fun applyingAPasteSetsTheOpponentTeam() {
        viewModel.prepareCreation()

        viewModel.startPokepasteEdition()
        viewModel.updatePokepaste(paste)
        assertTrue(viewModel.canApplyPokepaste)
        viewModel.applyPokepasteEdition()

        assertFalse(viewModel.isEditingPokepaste)
        assertEquals(pokepasteParser.parse(paste), viewModel.opponentPokepaste)
        assertTrue(viewModel.hasChanges)
    }

    @Test
    fun anInvalidPasteCantBeApplied() {
        viewModel.prepareEdition(matchupPlan)

        viewModel.startPokepasteEdition()
        viewModel.updatePokepaste(invalidPaste)
        assertNotNull(viewModel.pokepasteError)
        assertFalse(viewModel.canApplyPokepaste)
        viewModel.applyPokepasteEdition()

        assertTrue(viewModel.isEditingPokepaste)
        assertSame(matchupPlan.pokePaste, viewModel.opponentPokepaste)
    }

    @Test
    fun clearingThePasteRemovesTheOpponentTeam() {
        viewModel.prepareEdition(matchupPlan)

        viewModel.startPokepasteEdition()
        viewModel.updatePokepaste("")
        assertTrue(viewModel.canApplyPokepaste)
        viewModel.applyPokepasteEdition()

        assertNull(viewModel.opponentPokepaste)
        assertNull(viewModel.generateMatchupPlan(matchupPlan).pokePaste)
    }

    @Test
    fun aPasteWithoutEvsIsShownAsAnOts() {
        // as written by COPY on a replay's OTS: IVs all at 0 and a nature, but no EVs
        val copiedOts = pokepasteParser.parse("""
            Incineroar @ Sitrus Berry
            Ability: Intimidate
            Level: 50
            Careful Nature
            IVs: 0 HP / 0 Atk / 0 Def / 0 SpA / 0 SpD / 0 Spe
            - Fake Out
        """.trimIndent())
        assertFalse(copiedOts.isOts)

        assertTrue(copiedOts.asOtsIfNoStats().isOts)
        assertEquals(copiedOts.pokemons, copiedOts.asOtsIfNoStats().pokemons)
    }

    @Test
    fun aPasteWithEvsIsNotShownAsAnOts() {
        val withEvs = pokepasteParser.parse("""
            Incineroar @ Sitrus Berry
            Ability: Intimidate
            EVs: 252 HP / 4 Atk
            Careful Nature
            - Fake Out
        """.trimIndent())

        assertSame(withEvs, withEvs.asOtsIfNoStats())
    }

    @Test
    fun aClearedPasteHasNoError() {
        viewModel.prepareCreation()
        viewModel.startPokepasteEdition()
        viewModel.updatePokepaste(invalidPaste)
        viewModel.updatePokepaste("")

        assertNull(viewModel.pokepasteError)
    }
}
