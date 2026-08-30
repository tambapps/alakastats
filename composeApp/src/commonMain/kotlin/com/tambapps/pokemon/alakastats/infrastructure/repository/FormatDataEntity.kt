package com.tambapps.pokemon.alakastats.infrastructure.repository

import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.CommonFilters
import com.tambapps.pokemon.alakastats.domain.model.FormatData
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import kotlinx.serialization.Serializable

@Serializable
data class CommonFiltersEntity(
    val opponentTeam: List<List<String>> = emptyList()
)

@Serializable
data class FormatDataEntity(
    val popularPokemons: List<String>,
    val commonFilters: CommonFiltersEntity = CommonFiltersEntity()
)

internal fun FormatDataEntity.toDomain() = FormatData(
    popularPokemons = popularPokemons.map { PokemonName(it) },
    commonFilters = CommonFilters(
        opponentTeamFilters = commonFilters.opponentTeam.map { team ->
            team.map { PokemonFilter(PokemonName(it), asLead = false) }
        }
    )
)
