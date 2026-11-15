package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.DamageClass
import com.tambapps.pokemon.alakastats.domain.model.Pokedex
import com.tambapps.pokemon.alakastats.domain.model.PokedexMove
import com.tambapps.pokemon.alakastats.domain.model.PokedexSpecies
import com.tambapps.pokemon.alakastats.domain.repository.PokedexRepository
import com.tambapps.pokemon.alakastats.ui.model.PokedexMoveSort
import com.tambapps.pokemon.alakastats.ui.model.matching
import com.tambapps.pokemon.alakastats.ui.model.sortedBy

/**
 * The sheet of a Pokémon of the Pokédex, its forms (the Pokémon and its Megas) to switch between, and its moves
 */
class PokedexViewModel(
    private val pokedexRepository: PokedexRepository,
) : ViewModel() {

    // the form shown, normalized, null until one is
    var selectedForm by mutableStateOf<PokemonName?>(null)
        private set
    var species by mutableStateOf<PokedexSpecies?>(null)
        private set
    // the Pokémon, then its Megas, normalized
    var forms by mutableStateOf(emptyList<PokemonName>())
        private set
    var isLoading by mutableStateOf(false)
        private set
    // the form to show instead of the one asked, which doesn't exist (e.g. a Mega the Pokémon doesn't have, in an URL)
    var formToShowInstead by mutableStateOf<PokemonName?>(null)
        private set
    // the form asked isn't a Pokémon of the Pokédex (e.g. in an URL)
    var isUnknown by mutableStateOf(false)
        private set

    // null while it's loading or when it couldn't be
    val baseStats get() = species?.baseStats

    // the moves the Pokémon learns: searched by name, filtered by category and type, sorted. Kept when changing of form
    var moveQuery by mutableStateOf("")
    var moveCategory by mutableStateOf<DamageClass?>(null)
    var moveType by mutableStateOf<PokeType?>(null)
    var moveSort by mutableStateOf(PokedexMoveSort())

    /** The moves of the Pokémon shown matching the search, the category and the type, in the order of the sort */
    val moves: List<PokedexMove> by derivedStateOf {
        species?.moves.orEmpty().toList()
            .matching(moveQuery)
            .filter { (moveCategory == null || it.damageClass == moveCategory) && (moveType == null || it.type == moveType) }
            .sortedBy(moveSort)
    }

    /** The types of the moves of the Pokémon shown, for the type filter to only propose them */
    val moveTypes: Set<PokeType> by derivedStateOf { species?.moves.orEmpty().mapTo(HashSet()) { it.type } }

    private var pokedex: Pokedex? = null

    /**
     * Shows a form, loading the Pokédex unless it already is
     *
     * @param form the form, null for none
     */
    suspend fun show(form: PokemonName?) {
        formToShowInstead = null
        isUnknown = false
        selectedForm = form?.normalized
        if (form == null) {
            species = null
            forms = emptyList()
            return
        }
        // a failure to load isn't an unknown form: the sheet tells its stats aren't available
        val pokedex = loadPokedex() ?: return
        val shown = pokedex.species(form)
        if (shown == null) {
            // e.g. metagross-mega-x: its Pokémon, if it's one
            val pokemon = pokedex.species(form.baseNormalized)
            if (pokemon != null) formToShowInstead = pokemon.pokemonName.normalized else isUnknown = true
            return
        }
        species = shown
        selectedForm = shown.pokemonName.normalized
        forms = pokedex.formsOf(shown).map { it.pokemonName.normalized }
    }

    private suspend fun loadPokedex(): Pokedex? {
        pokedex?.let { return it }
        isLoading = true
        species = null
        forms = emptyList()
        return pokedexRepository.getPokedex().getOrNull().also {
            pokedex = it
            isLoading = false
        }
    }
}
