package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.model.withComputedElo
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.util.insertInChain
import kotlin.uuid.Uuid

/**
 * Adds replays to a team from outside of it, e.g. public Showdown replays
 */
class AddReplaysToTeamUseCase(private val repository: TeamlyticsRepository) {

    suspend fun listTeams(): List<TeamlyticsPreview> = repository.listPreviews()

    /**
     * Adds the replays the team doesn't have yet, as the team's screen does: the games of a series next to each other
     *
     * @return how many were added, the others being in the team already
     */
    suspend fun add(teamId: Uuid, replays: List<ReplayAnalytics>): Either<DomainError, Int> = either {
        val team = repository.get(teamId).bind()
        val newReplays = replays.distinctBy { it.reference }
            .filter { replay -> team.replays.none { it.reference == replay.reference } }
        if (newReplays.isEmpty()) return@either 0
        val teamReplays = team.replays.toMutableList()
        teamReplays.insertInChain(newReplays, reference = { it.reference }, nextReference = { it.nextBattleRef })
        repository.save(team.copy(replays = teamReplays.withComputedElo()), base = team).bind()
        newReplays.size
    }
}
