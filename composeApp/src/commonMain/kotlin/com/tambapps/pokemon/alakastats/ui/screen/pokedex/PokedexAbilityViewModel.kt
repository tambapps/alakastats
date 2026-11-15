package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.AbilityName
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.domain.repository.PokedexRepository
import com.tambapps.pokemon.alakastats.domain.model.PokedexSpecies
import com.tambapps.pokemon.alakastats.ui.model.PokedexPokemonSort
import com.tambapps.pokemon.alakastats.ui.model.sortedBy

/**
 * The sheet of an ability: what it does, and the Pokémon having it, filtered and sorted like the Pokémon's list
 */
class PokedexAbilityViewModel(
    private val pokedexRepository: PokedexRepository,
) : ViewModel() {

    var holders by mutableStateOf<AbilityHolders?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var loadingFailed by mutableStateOf(false)
        private set
    // the ability asked isn't one of the Pokédex (e.g. in an URL)
    var isUnknown by mutableStateOf(false)
        private set

    var sort by mutableStateOf(PokedexPokemonSort())
    var type by mutableStateOf<PokeType?>(null)
    // the Megas having it: only listed on demand, but for an ability of Megas
    var includeMegas by mutableStateOf(false)

    /** The Pokémon having the ability matching the filters, in the order of the sort */
    val pokemons: List<PokedexSpecies> by derivedStateOf {
        val holders = holders ?: return@derivedStateOf emptyList()
        val listed = if (includeMegas || holders.isMegaAbility) holders.pokemons + holders.megas else holders.pokemons
        listed.filter { type == null || type in it.types }.sortedBy(sort)
    }

    /** The types of the Pokémon having the ability, for the type filter to only propose them */
    val types: Set<PokeType> by derivedStateOf {
        holders?.let { (it.pokemons + it.megas).flatMapTo(HashSet()) { pokemon -> pokemon.types } }.orEmpty()
    }

    suspend fun load(id: String) {
        if (holders?.ability?.id == id) return
        isLoading = true
        isUnknown = false
        pokedexRepository.getPokedex()
            .onRight { pokedex ->
                val ability = pokedex.ability(AbilityName(id))
                holders = ability?.let {
                    val (megas, pokemons) = pokedex.withAbility(it).partition { pokemon -> pokemon.isMega }
                    AbilityHolders(it, pokemons, megas)
                }
                isUnknown = ability == null
            }
            .onLeft { loadingFailed = true }
        isLoading = false
    }
}
