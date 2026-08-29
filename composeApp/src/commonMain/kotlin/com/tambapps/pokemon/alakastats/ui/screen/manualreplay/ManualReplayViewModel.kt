package com.tambapps.pokemon.alakastats.ui.screen.manualreplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import cafe.adriel.voyager.core.model.ScreenModel
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.util.MegaUtils.canMega

internal const val MAX_POKEMONS = 6
private const val MOVES_COUNT = 4

class ManualReplayViewModel : ScreenModel {

    val pokemonStates = mutableStateListOf<ManualPokemonState>()

    var showSelectPokemonDialog by mutableStateOf(false)
        private set

    val canAddPokemon get() = pokemonStates.size < MAX_POKEMONS

    fun showSelectPokemonDialog() {
        showSelectPokemonDialog = true
    }

    fun hideSelectPokemonDialog() {
        showSelectPokemonDialog = false
    }

    fun contains(pokemonName: PokemonName) = pokemonStates.any { it.name == pokemonName }

    fun addPokemon(pokemonName: PokemonName) {
        if (!canAddPokemon || contains(pokemonName)) {
            return
        }
        pokemonStates.add(ManualPokemonState(pokemonName))
        hideSelectPokemonDialog()
    }

    fun removePokemon(pokemonState: ManualPokemonState) {
        pokemonStates.remove(pokemonState)
    }

    // only one pokemon can mega evolve per battle
    fun updateMegaSelected(pokemonState: ManualPokemonState, megaSelected: Boolean) {
        pokemonStates.forEach { it.updateMegaSelected(megaSelected && it === pokemonState) }
    }
}

class ManualPokemonState(val name: PokemonName) {
    var item by mutableStateOf("")
        private set

    var megaSelected by mutableStateOf(false)
        private set

    val moves = List(MOVES_COUNT) { "" }.toMutableStateList()

    val canMega get() = name.canMega

    val displayedName get() =
        if (megaSelected) PokemonName("${name.normalized.value}-mega")
        else name

    fun updateItem(item: String) {
        this.item = item
    }

    fun updateMegaSelected(megaSelected: Boolean) {
        this.megaSelected = megaSelected
    }

    fun updateMove(index: Int, move: String) {
        moves[index] = move
    }
}
