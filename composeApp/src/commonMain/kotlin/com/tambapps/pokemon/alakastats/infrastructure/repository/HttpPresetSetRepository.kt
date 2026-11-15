package com.tambapps.pokemon.alakastats.infrastructure.repository

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.LoadPresetSetsError
import com.tambapps.pokemon.alakastats.domain.model.PresetSets
import com.tambapps.pokemon.alakastats.domain.repository.PresetSetRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

private const val PRESET_SETS_URL =
    "https://raw.githubusercontent.com/tambapps/alakastats/refs/heads/main/composeApp/src/commonMain/composeResources/$PRESET_SETS_FILE"

// reads the sets from the repository, to get the new ones without releasing a new version of the app
class HttpPresetSetRepository(
    private val json: Json,
    private val httpClient: HttpClient
): PresetSetRepository {

    override suspend fun get(): Either<LoadPresetSetsError, PresetSets> {
        return Either.catch {
            val response = httpClient.get(PRESET_SETS_URL)
            // the client doesn't expect success, and github answers a plain text 404 body
            check(response.status.isSuccess()) { "got status ${response.status}" }
            response.bodyAsText()
        }
            .map { json.decodeFromString<PresetSetsEntity>(it).toDomain() }
            .mapLeft { LoadPresetSetsError("Couldn't fetch the sets: ${it.message}", it) }
    }
}
