package com.tambapps.pokemon.alakastats.domain.model

import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.sd.replay.parser.SdReplay
import kotlinx.serialization.Serializable

@Serializable
data class Teamlytics(
    val pokePaste: PokePaste,
    val replays: List<ReplayAnalytics>,
    val sdNames: List<String>
)

data class ReplayAnalytics(
    val replay: SdReplay
)