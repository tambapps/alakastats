package com.tambapps.pokemon.alakastats.domain.transformer

import com.tambapps.pokemon.AbilityName
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.MoveName
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.TeraType
import com.tambapps.pokemon.alakastats.domain.model.OpenTeamSheet
import com.tambapps.pokemon.alakastats.domain.model.OtsPokemon
import com.tambapps.pokemon.sd.replay.log.visitor.OtsPokemon as SdOtsPokemon
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamPreview
import com.tambapps.pokemon.alakastats.domain.model.TeamPreviewPokemon
import com.tambapps.pokemon.alakastats.domain.model.Terastallization
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ReplayAnalyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PlayerEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamPreviewPokemonEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.OpenTeamSheetEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.OtsPokemonEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TerastallizationEntity

fun ReplayAnalytics.toEntity() = ReplayAnalyticsEntity(
    players = players.map { it.toEntity() },
    uploadTime = uploadTime,
    format = format,
    rating = rating,
    version = version,
    winner = winner?.value,
    url = url,
    reference = reference,
    nextBattleRef = nextBattleRef,
    notes = notes
)

fun ReplayAnalyticsEntity.toDomain() = ReplayAnalytics(
    players = players.map { it.toDomain() },
    uploadTime = uploadTime,
    format = format,
    rating = rating,
    version = version,
    winner = winner?.let(::UserName),
    url = url,
    reference = reference,
    nextBattleRef = nextBattleRef,
    notes = notes
)

fun Player.toEntity() = PlayerEntity(
    name = name.value,
    teamPreview = teamPreview.toEntity(),
    selection = selection.map { it.value },
    beforeElo = beforeElo,
    afterElo = afterElo,
    terastallization = terastallization?.toEntity(),
    ots = ots?.toEntity(),
    movesUsage = movesUsage.mapKeys { (k, _) -> k.value }
)

fun PlayerEntity.toDomain() = Player(
    name = UserName(name),
    teamPreview = teamPreview.toDomain(),
    selection = selection.map { PokemonName(it) },
    beforeElo = beforeElo,
    afterElo = afterElo,
    terastallization = terastallization?.toDomain(),
    ots = ots?.toDomain(),
    movesUsage = movesUsage.mapKeys { (k, _) -> PokemonName(k) }
)

fun TeamPreview.toEntity() = TeamPreviewEntity(
    pokemons = pokemons.map { it.toEntity() }
)

fun TeamPreviewEntity.toDomain() = TeamPreview(
    pokemons = pokemons.map { it.toDomain() }
)

fun TeamPreviewPokemon.toEntity() = TeamPreviewPokemonEntity(
    name = name.value,
    level = level
)

fun TeamPreviewPokemonEntity.toDomain() = TeamPreviewPokemon(
    name = PokemonName(name),
    level = level
)

fun OpenTeamSheet.toEntity() = OpenTeamSheetEntity(
    pokemons = pokemons.map { it.toEntity() }
)

fun OpenTeamSheetEntity.toDomain() = OpenTeamSheet(
    pokemons = pokemons.map { it.toDomain() }
)

fun OtsPokemon.toEntity() = OtsPokemonEntity(
    name = name.value,
    item = item.value,
    ability = ability.value,
    moves = moves.map { it.value },
    level = level,
    teraType = teraType?.name
)

fun SdOtsPokemon.toEntity() = OtsPokemonEntity(
    name = name.value,
    item = item,
    ability = ability,
    moves = moves,
    level = level,
    teraType = teraType?.name
)

fun OtsPokemonEntity.toDomain() = OtsPokemon(
    name = PokemonName(name),
    item = ItemName(item),
    ability = AbilityName(ability),
    moves = moves.map(::MoveName),
    level = level,
    teraType = teraType?.let(TeraType::valueOf)
)

fun Terastallization.toEntity() = TerastallizationEntity(
    pokemon = pokemon.value,
    type = type.name
)

fun TerastallizationEntity.toDomain() = Terastallization(
    pokemon = PokemonName(pokemon),
    type = TeraType.valueOf(type)
)
