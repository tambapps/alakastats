package com.tambapps.pokemon.alakastats.infrastructure.repository

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.CommonFilters
import com.tambapps.pokemon.alakastats.domain.model.FormatData
import com.tambapps.pokemon.alakastats.domain.model.PopularTeam
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import kotlinx.serialization.Serializable

@Serializable
data class CommonFiltersEntity(
    val opponentTeam: List<List<String>> = emptyList()
)

@Serializable
data class PopularTeamEntity(
    val name: String = "",
    val pokemons: List<String>
)

@Serializable
data class FormatDataEntity(
    val popularPokemons: List<String>,
    val commonFilters: CommonFiltersEntity = CommonFiltersEntity(),
    val popularTeams: List<PopularTeamEntity> = emptyList()
)

internal fun FormatDataEntity.toDomain() = FormatData(
    popularPokemons = popularPokemons.map { PokemonName(it) },
    commonFilters = CommonFilters(
        opponentTeamFilters = commonFilters.opponentTeam.map { team ->
            team.map { PokemonFilter(PokemonName(it), asLead = false) }
        }
    ),
    // format files may hold an empty team as a template to fill in
    popularTeams = popularTeams
        .filter { it.pokemons.isNotEmpty() }
        .map { team -> PopularTeam(team.name, team.pokemons.map { PokemonName(it) }) }
)
