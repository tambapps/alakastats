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


data class ReplayAnalytics(
    val replay: SdReplay,
)