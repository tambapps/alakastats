package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.sd.replay.parser.SdReplay
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

data class Teamlytics(
    val id: Uuid,
    val name: String,
    val pokePaste: PokePaste?,
    val replays: List<ReplayAnalytics>,
    val sdNames: List<String>
)

@Serializable
data class TeamlyticsPreview(
    val id: Uuid,
    val name: String,
    val pokemons: List<String>,
    val nbReplays: Int,
    val winrate: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TeamlyticsPreview

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class ReplayAnalytics(
    val replay: SdReplay,
)