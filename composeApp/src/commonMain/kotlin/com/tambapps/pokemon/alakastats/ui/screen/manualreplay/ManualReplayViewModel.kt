package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GameOutcome
import com.tambapps.pokemon.alakastats.domain.model.MANUAL_REFERENCE_PREFIX
import com.tambapps.pokemon.alakastats.domain.model.MegaEvolution
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.getGameOutcome
import com.tambapps.pokemon.alakastats.domain.model.getPlayers
import com.tambapps.pokemon.alakastats.domain.model.TeamPreview
import com.tambapps.pokemon.alakastats.domain.model.TeamPreviewPokemon
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.util.MegaUtils
import com.tambapps.pokemon.util.MegaUtils.canMega
import com.tambapps.pokemon.util.MegaUtils.toMega
import kotlin.time.Clock
import kotlin.uuid.Uuid

internal const val MAX_OPPONENT_POKEMONS = 6
internal const val MAX_SELECTION = 4
internal const val LEAD_SIZE = 2
private const val VERSION = "1.0"
private val OPPONENT_NAME = UserName("opponent")

enum class ManualReplayPage(val title: String) {
    YOU("You"),
    OPPONENT("Opponent"),
    NOTES("Notes")
}

data class ManualReplayValidationError(val page: ManualReplayPage, val message: String)

class ManualReplayViewModel(private val team: Teamlytics) : ScreenModel {

    val youPokemonStates = team.pokePaste.pokemons.map(::YouPokemonState)

    // ordered, as the first selected pokemons are the lead
    val youSelection = mutableStateListOf<ManualPokemonState>()

    val opponentPokemonStates = mutableStateListOf<OpponentPokemonState>()

    val opponentSelection = mutableStateListOf<ManualPokemonState>()

    // null until the user tells the outcome of the game
    var won by mutableStateOf<Boolean?>(null)
        private set

    var notes by mutableStateOf("")
        private set

    val isEditing get() = editedReplay != null

    private val youName = team.sdNames.firstOrNull() ?: UserName("you")

    // the replay being edited, if we're not creating a new one
    private var editedReplay: ReplayAnalytics? = null

    // restores the state of an already entered replay, so that it can be edited
    fun prepareEdition(replay: ReplayAnalytics) {
        if (editedReplay != null) {
            return
        }
        editedReplay = replay
        val (youPlayer, opponentPlayer) = team.getPlayers(replay)
        won = team.getGameOutcome(replay) == GameOutcome.WIN
        notes = replay.notes.orEmpty()

        opponentPokemonStates.addAll(
            opponentPlayer.teamPreview.pokemons.map { OpponentPokemonState(it.name) }
        )
        restoreSelection(youPlayer, youPokemonStates, youSelection)
        restoreSelection(opponentPlayer, opponentPokemonStates, opponentSelection)
        restoreMega(youPlayer, youPokemonStates)
        restoreMega(opponentPlayer, opponentPokemonStates)
    }

    private fun restoreSelection(
        player: Player,
        pokemonStates: List<ManualPokemonState>,
        selection: MutableList<ManualPokemonState>
    ) {
        player.selection.forEach { selected ->
            pokemonStates.find { it.name.baseMatches(selected) && it !in selection }
                ?.let(selection::add)
        }
    }

    private fun restoreMega(player: Player, pokemonStates: List<ManualPokemonState>) {
        val megaEvolution = player.megaEvolution ?: return
        pokemonStates.find { it.name.baseMatches(megaEvolution.pokemon) }
            ?.let { applyMega(it, megaEvolution.item) }
    }

    var showSelectPokemonDialog by mutableStateOf(false)
        private set

    // the pokemon we're asking the mega stone of, when it has several ones
    var megaStoneSelectionFor by mutableStateOf<ManualPokemonState?>(null)
        private set

    val canAddOpponentPokemon get() = opponentPokemonStates.size < MAX_OPPONENT_POKEMONS

    // only the first missing thing, as there is no point overwhelming the user with all of them
    val validationError get() = when {
        // without it the replay has no known outcome, and doesn't count in any stat
        won == null -> ManualReplayValidationError(
            ManualReplayPage.YOU,
            "Tell whether you won or lost this game"
        )
        youSelection.size < MAX_SELECTION -> ManualReplayValidationError(
            ManualReplayPage.YOU,
            "Select the $MAX_SELECTION pokemons you brought"
        )
        opponentPokemonStates.isEmpty() -> ManualReplayValidationError(
            ManualReplayPage.OPPONENT,
            "Add at least one pokemon of the opponent's team"
        )
        // the opponent's lead is always known, whatever happened next
        opponentSelection.size < LEAD_SIZE -> ManualReplayValidationError(
            ManualReplayPage.OPPONENT,
            "Select at least the $LEAD_SIZE pokemons the opponent led with"
        )
        else -> null
    }

    val canBuildReplayAnalytics get() = validationError == null

    fun updateWon(won: Boolean) {
        this.won = won
    }

    fun updateNotes(notes: String) {
        this.notes = notes
    }

    fun showSelectPokemonDialog() {
        showSelectPokemonDialog = true
    }

    fun hideSelectPokemonDialog() {
        showSelectPokemonDialog = false
    }

    fun containsOpponentPokemon(pokemonName: PokemonName) =
        opponentPokemonStates.any { it.name == pokemonName.baseForm }

    fun addOpponentPokemon(pokemonName: PokemonName) {
        if (!canAddOpponentPokemon || containsOpponentPokemon(pokemonName)) {
            return
        }
        val pokemonState = OpponentPokemonState(pokemonName.baseForm)
        opponentPokemonStates.add(pokemonState)
        if (pokemonName.isMega) {
            megaStoneOf(pokemonName)?.let { applyMega(pokemonState, it) }
        }
        hideSelectPokemonDialog()
    }

    // mega forms are stored as their base form, with the mega switch turned on
    private val PokemonName.baseForm get() = if (isMega) baseNormalized else normalized

    // the picked mega form tells us which mega stone it holds
    private fun megaStoneOf(megaName: PokemonName) = MegaUtils.getMegaStones(megaName)
        .find { MegaUtils.getMegaPokemon(it)?.matches(megaName) == true }

    fun removeOpponentPokemon(pokemonState: OpponentPokemonState) {
        opponentPokemonStates.remove(pokemonState)
        opponentSelection.remove(pokemonState)
    }

    fun selectionIndexOf(pokemonState: ManualPokemonState) =
        selectionOf(pokemonState).indexOf(pokemonState)

    fun toggleSelected(pokemonState: ManualPokemonState) {
        val selection = selectionOf(pokemonState)
        if (selection.remove(pokemonState)) {
            // it can't have mega evolved in a battle it wasn't brought to
            applyMega(pokemonState, null)
        } else if (selection.size < MAX_SELECTION) {
            selection.add(pokemonState)
        }
    }

    private fun selectionOf(pokemonState: ManualPokemonState) =
        if (pokemonState is YouPokemonState) youSelection else opponentSelection

    fun updateMegaSelected(pokemonState: ManualPokemonState, megaSelected: Boolean) {
        if (!megaSelected) {
            applyMega(pokemonState, null)
            return
        }
        // no need to ask when we already know the stone it holds
        pokemonState.heldMegaStone?.let {
            applyMega(pokemonState, it)
            return
        }
        val megaStones = MegaUtils.getMegaStones(pokemonState.name)
        if (megaStones.size > 1) {
            // we can't guess which one it mega evolved into, let the user tell us
            megaStoneSelectionFor = pokemonState
        } else {
            applyMega(pokemonState, megaStones.firstOrNull())
        }
    }

    fun selectMegaStone(pokemonState: ManualPokemonState, megaStone: ItemName) {
        applyMega(pokemonState, megaStone)
        hideMegaStoneSelectionDialog()
    }

    fun hideMegaStoneSelectionDialog() {
        megaStoneSelectionFor = null
    }

    // only one pokemon can mega evolve per battle, for each player
    private fun applyMega(pokemonState: ManualPokemonState, megaStone: ItemName?) {
        val playerStates =
            if (pokemonState is YouPokemonState) youPokemonStates
            else opponentPokemonStates
        playerStates.forEach {
            it.updateMega(if (it === pokemonState) megaStone else null)
        }
    }

    fun buildReplayAnalytics() = ReplayAnalytics(
        players = listOf(youPlayer(), opponentPlayer()),
        // an edited replay keeps identifying with what it was created with
        uploadTime = editedReplay?.uploadTime ?: Clock.System.now().epochSeconds,
        format = team.format.name,
        rating = null,
        version = VERSION,
        winner = when (won) {
            true -> youName
            false -> OPPONENT_NAME
            null -> null
        },
        url = null,
        reference = editedReplay?.reference ?: (MANUAL_REFERENCE_PREFIX + Uuid.random()),
        nextBattleRef = null,
        notes = notes.takeIf { it.isNotBlank() },
    )

    private fun youPlayer() = emptyPlayer(
        name = youName,
        teamPreviewPokemons = youPokemonStates.map { TeamPreviewPokemon(it.name, it.pokemon.level) },
        selection = youSelection.map { it.name },
        megaEvolution = megaEvolutionOf(youPokemonStates)
    )

    private fun opponentPlayer() = emptyPlayer(
        name = OPPONENT_NAME,
        teamPreviewPokemons = opponentPokemonStates.map {
            TeamPreviewPokemon(it.name, team.format.pokemonLevel)
        },
        selection = opponentSelection.map { it.name },
        megaEvolution = megaEvolutionOf(opponentPokemonStates)
    )

    private fun megaEvolutionOf(playerStates: List<ManualPokemonState>) =
        playerStates.firstNotNullOfOrNull { pokemonState ->
            pokemonState.megaStone?.let { MegaEvolution(pokemonState.name, it) }
        }

    // only what was entered on this screen is known, everything else is left empty
    private fun emptyPlayer(
        name: UserName,
        teamPreviewPokemons: List<TeamPreviewPokemon>,
        selection: List<PokemonName> = emptyList(),
        megaEvolution: MegaEvolution? = null
    ) = Player(
        name = name,
        teamPreview = TeamPreview(teamPreviewPokemons),
        selection = selection,
        beforeElo = null,
        afterElo = null,
        terastallization = null,
        megaEvolution = megaEvolution,
        ots = null,
        movesUsage = emptyMap()
    )
}

sealed class ManualPokemonState(val name: PokemonName) {
    var megaStone by mutableStateOf<ItemName?>(null)
        private set

    val megaSelected get() = megaStone != null

    open val canMega get() = name.canMega

    // can differ from name as name is the base form, because it's simpler to handle megas
    protected open val actualName get() = name

    // the mega stone tells which mega form it evolved into (charizard-mega-x vs charizard-mega-y)
    val displayedName get() = megaStone?.let { name.toMega(it) } ?: actualName

    // the mega stone we already know it holds, if any
    open val heldMegaStone: ItemName? get() = null

    open fun updateMega(megaStone: ItemName?) {
        this.megaStone = megaStone
    }
}

// a pokemon of the user's team, it is already known, it just has to be selected.
// pokepastes sometimes reference the mega form itself, we always start from the base one
class YouPokemonState(val pokemon: Pokemon) : ManualPokemonState(pokemon.name.baseNormalized) {

    // the team's pokemon form (ogerpon-hearthflame, landorus-therian...), unless it is the mega one
    override val actualName = if (pokemon.name.isMega) name else pokemon.name

    override val heldMegaStone = pokemon.item?.takeIf { pokemon.name.baseNormalized.toMega(it) != null }

    // we know the user's items, it can only mega evolve if it holds the right stone
    override val canMega get() = heldMegaStone != null
}

// a pokemon of the opponent's team, everything about it has to be entered
class OpponentPokemonState(name: PokemonName) : ManualPokemonState(name) {
    var item by mutableStateOf("")
        private set

    fun updateItem(item: String) {
        this.item = item
    }

    override fun updateMega(megaStone: ItemName?) {
        super.updateMega(megaStone)
        if (megaStone != null) {
            item = megaStone.pretty
        }
    }
}
