package com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity

import kotlinx.serialization.Serializable


@Serializable
data class PssTeamlytics(
    val saveName: String,
    val teamNotes: String? = null,
    val sdNames: List<String>,
    )

@Serializable
data class PssReplay(
    val uri: String,
    val notes: String? = null
)

@Serializable
data class PssPokepaste(
  val pokemons: List<PssPokepastePokemon>
)

@Serializable
data class PssPokepastePokemon(
    val name: String,
    val gender: String? = null,
    val item: String? = null,
    val ability: String,
    val teraType: String? = null,
    val nature: String? = null,
    val level: Int? = null,
    val moves: List<String>,
    val ivs: PssStats? = null,
    val evs: PssStats? = null
)


@Serializable
data class PssStats(
    val hp: Int,
    val speed: Int,
    val attack: Int,
    val specialAttack: Int,
    val defense: Int,
    val specialDefense: Int,
)