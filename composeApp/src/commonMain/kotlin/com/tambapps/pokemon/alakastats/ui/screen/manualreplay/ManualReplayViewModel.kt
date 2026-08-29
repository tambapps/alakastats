package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.MegaEvolution
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamPreview
import com.tambapps.pokemon.alakastats.domain.model.TeamPreviewPokemon
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.util.MegaUtils
import com.tambapps.pokemon.util.MegaUtils.canMega
import com.tambapps.pokemon.util.MegaUtils.toMega
import kotlin.time.Clock
import kotlin.uuid.Uuid

internal const val MAX_POKEMONS = 6
private const val VERSION = "1.0"
private val OPPONENT_NAME = UserName("opponent")

class ManualReplayViewModel(private val team: Teamlytics) : ScreenModel {

    val pokemonStates = mutableStateListOf<ManualPokemonState>()

    var showSelectPokemonDialog by mutableStateOf(false)
        private set

    // the pokemon we're asking the mega stone of, when it has several ones
    var megaStoneSelectionFor by mutableStateOf<ManualPokemonState?>(null)
        private set

    val canAddPokemon get() = pokemonStates.size < MAX_POKEMONS

    fun showSelectPokemonDialog() {
        showSelectPokemonDialog = true
    }

    fun hideSelectPokemonDialog() {
        showSelectPokemonDialog = false
    }

    fun contains(pokemonName: PokemonName) = pokemonStates.any { it.name == pokemonName.baseForm }

    fun addPokemon(pokemonName: PokemonName) {
        if (!canAddPokemon || contains(pokemonName)) {
            return
        }
        val pokemonState = ManualPokemonState(pokemonName.baseForm)
        pokemonStates.add(pokemonState)
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

    fun removePokemon(pokemonState: ManualPokemonState) {
        pokemonStates.remove(pokemonState)
    }

    fun updateMegaSelected(pokemonState: ManualPokemonState, megaSelected: Boolean) {
        if (!megaSelected) {
            applyMega(pokemonState, null)
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

    // only one pokemon can mega evolve per battle
    private fun applyMega(pokemonState: ManualPokemonState, megaStone: ItemName?) {
        pokemonStates.forEach {
            it.updateMega(if (it === pokemonState) megaStone else null)
        }
    }

    fun buildReplayAnalytics() = ReplayAnalytics(
        players = listOf(youPlayer(), opponentPlayer()),
        uploadTime = Clock.System.now().epochSeconds,
        format = team.format.name,
        rating = null,
        version = VERSION,
        winner = null,
        url = null,
        // manual replays have no showdown replay to refer to
        reference = "manual_" + Uuid.random(),
        nextBattleRef = null,
        notes = null,
    )

    private fun youPlayer() = emptyPlayer(
        name = team.sdNames.first(),
        teamPreviewPokemons = team.pokePaste.pokemons.map { TeamPreviewPokemon(it.name, it.level) }
    )

    private fun opponentPlayer() = emptyPlayer(
        name = OPPONENT_NAME,
        teamPreviewPokemons = pokemonStates.map { TeamPreviewPokemon(it.name, team.format.pokemonLevel) },
        megaEvolution = pokemonStates.firstNotNullOfOrNull { pokemonState ->
            pokemonState.megaStone?.let { MegaEvolution(pokemonState.name, it) }
        }
    )

    // only what was entered on this screen is known, everything else is left empty
    private fun emptyPlayer(
        name: UserName,
        teamPreviewPokemons: List<TeamPreviewPokemon>,
        megaEvolution: MegaEvolution? = null
    ) = Player(
        name = name,
        teamPreview = TeamPreview(teamPreviewPokemons),
        selection = emptyList(),
        beforeElo = null,
        afterElo = null,
        terastallization = null,
        megaEvolution = megaEvolution,
        ots = null,
        movesUsage = emptyMap()
    )
}

class ManualPokemonState(val name: PokemonName) {
    var item by mutableStateOf("")
        private set

    var megaStone by mutableStateOf<ItemName?>(null)
        private set

    val megaSelected get() = megaStone != null

    val canMega get() = name.canMega

    // the mega stone tells which mega form it evolved into (charizard-mega-x vs charizard-mega-y)
    val displayedName get() = megaStone?.let { name.toMega(it) } ?: name

    fun updateItem(item: String) {
        this.item = item
    }

    fun updateMega(megaStone: ItemName?) {
        this.megaStone = megaStone
        if (megaStone != null) {
            item = megaStone.pretty
        }
    }
}
