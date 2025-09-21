package com.tambapps.pokemon.alakastats.domain.transformer

import com.tambapps.pokemon.alakastats.domain.model.OpenTeamSheet
import com.tambapps.pokemon.alakastats.domain.model.OtsPokemon
import com.tambapps.pokemon.sd.replay.log.visitor.OtsPokemon as SdOtsPokemon
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamPreview
import com.tambapps.pokemon.alakastats.domain.model.TeamPreviewPokemon
import com.tambapps.pokemon.alakastats.domain.model.Terastallization
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.ReplayAnalyticsEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.PlayerEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamPreviewEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TeamPreviewPokemonEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.OpenTeamSheetEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.OtsPokemonEntity
import com.tambapps.pokemon.alakastats.infrastructure.repository.storage.entity.TerastallizationEntity

class ReplayAnalyticsTransformer(
    private val playerTransformer: PlayerTransformer
) {
    
    fun toEntity(domain: ReplayAnalytics): ReplayAnalyticsEntity {
        return ReplayAnalyticsEntity(
            players = domain.players.map { playerTransformer.toEntity(it) },
            uploadTime = domain.uploadTime,
            format = domain.format,
            rating = domain.rating,
            version = domain.version,
            winner = domain.winner,
            url = domain.url,
            reference = domain.reference,
            nextBattleRef = domain.nextBattleRef
        )
    }
    
    fun toDomain(entity: ReplayAnalyticsEntity): ReplayAnalytics {
        return ReplayAnalytics(
            players = entity.players.map { playerTransformer.toDomain(it) },
            uploadTime = entity.uploadTime,
            format = entity.format,
            rating = entity.rating,
            version = entity.version,
            winner = entity.winner,
            url = entity.url,
            reference = entity.reference,
            nextBattleRef = entity.nextBattleRef
        )
    }
}

class PlayerTransformer(
    private val teamPreviewTransformer: TeamPreviewTransformer,
    private val terastallizationTransformer: TerastallizationTransformer,
    private val openTeamSheetTransformer: OpenTeamSheetTransformer
) {
    
    fun toEntity(domain: Player): PlayerEntity {
        return PlayerEntity(
            name = domain.name,
            teamPreview = teamPreviewTransformer.toEntity(domain.teamPreview),
            selection = domain.selection,
            beforeElo = domain.beforeElo,
            afterElo = domain.afterElo,
            terastallization = domain.terastallization?.let { terastallizationTransformer.toEntity(it) },
            ots = domain.ots?.let { openTeamSheetTransformer.toEntity(it) },
            movesUsage = domain.movesUsage
        )
    }
    
    fun toDomain(entity: PlayerEntity): Player {
        return Player(
            name = entity.name,
            teamPreview = teamPreviewTransformer.toDomain(entity.teamPreview),
            selection = entity.selection,
            beforeElo = entity.beforeElo,
            afterElo = entity.afterElo,
            terastallization = entity.terastallization?.let { terastallizationTransformer.toDomain(it) },
            ots = entity.ots?.let { openTeamSheetTransformer.toDomain(it) },
            movesUsage = entity.movesUsage
        )
    }
}

class TeamPreviewTransformer(
    private val teamPreviewPokemonTransformer: TeamPreviewPokemonTransformer
) {
    
    fun toEntity(domain: TeamPreview): TeamPreviewEntity {
        return TeamPreviewEntity(
            pokemons = domain.pokemons.map { teamPreviewPokemonTransformer.toEntity(it) }
        )
    }
    
    fun toDomain(entity: TeamPreviewEntity): TeamPreview {
        return TeamPreview(
            pokemons = entity.pokemons.map { teamPreviewPokemonTransformer.toDomain(it) }
        )
    }
}

class TeamPreviewPokemonTransformer {
    
    fun toEntity(domain: TeamPreviewPokemon): TeamPreviewPokemonEntity {
        return TeamPreviewPokemonEntity(
            name = domain.name,
            level = domain.level
        )
    }
    
    fun toDomain(entity: TeamPreviewPokemonEntity): TeamPreviewPokemon {
        return TeamPreviewPokemon(
            name = entity.name,
            level = entity.level
        )
    }
}

class OpenTeamSheetTransformer(
    private val otsPokemonTransformer: OtsPokemonTransformer
) {
    
    fun toEntity(domain: OpenTeamSheet): OpenTeamSheetEntity {
        return OpenTeamSheetEntity(
            pokemons = domain.pokemons.map { otsPokemonTransformer.toEntity(it) }
        )
    }

    fun toDomain(entity: OpenTeamSheetEntity): OpenTeamSheet {
        return OpenTeamSheet(
            pokemons = entity.pokemons.map { otsPokemonTransformer.toDomain(it) }
        )
    }
}

class OtsPokemonTransformer {

    fun toEntity(domain: OtsPokemon): OtsPokemonEntity {
        return OtsPokemonEntity(
            name = domain.name,
            item = domain.item,
            ability = domain.ability,
            moves = domain.moves,
            level = domain.level,
            teraType = domain.teraType
        )
    }

    fun toEntity(domain: SdOtsPokemon): OtsPokemonEntity {
        return OtsPokemonEntity(
            name = domain.name,
            item = domain.item,
            ability = domain.ability,
            moves = domain.moves,
            level = domain.level,
            teraType = domain.teraType
        )
    }

    fun toDomain(entity: OtsPokemonEntity): OtsPokemon {
        return OtsPokemon(
            name = entity.name,
            item = entity.item,
            ability = entity.ability,
            moves = entity.moves,
            level = entity.level,
            teraType = entity.teraType
        )
    }
}

class TerastallizationTransformer {
    
    fun toEntity(domain: Terastallization): TerastallizationEntity {
        return TerastallizationEntity(
            pokemon = domain.pokemon,
            type = domain.type
        )
    }
    
    fun toDomain(entity: TerastallizationEntity): Terastallization {
        return Terastallization(
            pokemon = entity.pokemon,
            type = entity.type
        )
    }
}