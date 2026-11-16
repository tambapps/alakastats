package com.tambapps.pokemon.alakastats.ui.screen.pokedex

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tambapps.pokemon.alakastats.domain.model.PokedexAbility
import com.tambapps.pokemon.alakastats.domain.model.PokedexSpecies
import com.tambapps.pokemon.alakastats.domain.model.foldName
import com.tambapps.pokemon.alakastats.domain.repository.PokedexRepository
import com.tambapps.pokemon.alakastats.ui.model.SortCriterion

/**
 * An ability and the Pokémon having it
 *
 * @property pokemons the ones having it that aren't Megas
 * @property megas the Megas having it: the only ones for the abilities of Megas (e.g. Mega Sol)
 */
data class AbilityHolders(val ability: PokedexAbility, val pokemons: List<PokedexSpecies>, val megas: List<PokedexSpecies>) {
    val isMegaAbility get() = pokemons.isEmpty()
    /** The Pokémon to show for it: the Megas for an ability of Megas */
    val shown get() = pokemons.ifEmpty { megas }
}

enum class PokedexAbilitySortCriterion(
    override val label: String,
    override val descendingLabel: String,
    override val ascendingLabel: String,
    override val defaultDescending: Boolean,
) : SortCriterion {
    NAME("Name", "Z → A", "A → Z", defaultDescending = false),
    POKEMON("Pokémon", "Most first", "Fewest first", defaultDescending = true),
}

data class PokedexAbilitySort(
    val criterion: PokedexAbilitySortCriterion = PokedexAbilitySortCriterion.NAME,
    val descending: Boolean = criterion.defaultDescending,
) {
    val isDefault get() = this == PokedexAbilitySort()
}

/**
 * The abilities of the Pokédex with the Pokémon having them, searched by name or description, filtered and sorted
 */
class PokedexAbilitiesViewModel(
    private val pokedexRepository: PokedexRepository,
) : ViewModel() {

    var query by mutableStateOf("")
    var sort by mutableStateOf(PokedexAbilitySort())
    // only the abilities of Megas
    var megaOnly by mutableStateOf(false)
    var isLoading by mutableStateOf(true)
        private set
    var loadingFailed by mutableStateOf(false)
        private set
    var all by mutableStateOf(emptyList<AbilityHolders>())
        private set

    /** The abilities matching the query and the filters, in the order of the sort */
    val abilities: List<AbilityHolders> by derivedStateOf {
        val folded = foldName(query)
        val byName = compareBy<AbilityHolders> { it.ability.name.en }
        val byCriterion = when (sort.criterion) {
            PokedexAbilitySortCriterion.NAME -> byName
            // an ability of Megas counts its Megas
            PokedexAbilitySortCriterion.POKEMON -> compareBy { it.shown.size }
        }
        all.filter { holders ->
            val ability = holders.ability
            (!megaOnly || holders.isMegaAbility)
                && (folded.isEmpty() || listOfNotNull(ability.name.en, ability.name.fr, ability.shortDesc, ability.desc).any { folded in foldName(it) })
        }.sortedWith((if (sort.descending) byCriterion.reversed() else byCriterion).then(byName))
    }

    suspend fun load() {
        if (all.isNotEmpty()) return
        isLoading = true
        pokedexRepository.getPokedex()
            .onRight { pokedex ->
                all = pokedex.abilities.map { ability ->
                    val (megas, pokemons) = pokedex.withAbility(ability).filterNot { it.isBattleOnly }.partition { it.isMega }
                    AbilityHolders(ability, pokemons, megas)
                }
            }
            .onLeft { loadingFailed = true }
        isLoading = false
    }
}
