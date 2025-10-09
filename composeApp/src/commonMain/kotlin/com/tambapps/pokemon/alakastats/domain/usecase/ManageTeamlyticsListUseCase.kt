package com.tambapps.pokemon.alakastats.domain.usecase

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.GetTeamlyticsError
import com.tambapps.pokemon.alakastats.domain.error.LoadTeamError
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.domain.repository.TeamlyticsRepository
import com.tambapps.pokemon.alakastats.infrastructure.service.TeamlyticsSerializer
import kotlin.uuid.Uuid

class ManageTeamlyticsListUseCase(
    private val repository: TeamlyticsRepository,
    private val serializer: TeamlyticsSerializer
) {

    suspend fun list(): List<TeamlyticsPreview> = repository.listPreviews()

    suspend fun get(id: Uuid): Either<GetTeamlyticsError, Teamlytics> = repository.get(id)

    suspend fun delete(id: Uuid) = repository.delete(id)

    suspend fun loadTeam(byteArray: ByteArray): Either<LoadTeamError, Teamlytics> =
        serializer.loadTeam(byteArray)

    suspend fun saveNewTeam(team: Teamlytics) = repository.save(team.copy(id = Uuid.random()))

}
