package com.tambapps.pokemon.alakastats.infrastructure.repository

import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.LoadFormatDataError
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.FormatData
import com.tambapps.pokemon.alakastats.domain.repository.FormatDataRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

private const val FORMATS_URL =
    "https://raw.githubusercontent.com/tambapps/alakastats/refs/heads/main/composeApp/src/commonMain/composeResources/files/formats"

// reads the format data from the repository, to get updates without releasing a new version of the app
class HttpFormatDataRepository(
    private val json: Json,
    private val httpClient: HttpClient
): FormatDataRepository {

    override suspend fun get(format: Format): Either<LoadFormatDataError, FormatData> {
        return Either.catch {
            val response = httpClient.get("$FORMATS_URL/${format.name}.json")
            // the client doesn't expect success, and github answers a plain text 404 body
            check(response.status.isSuccess()) { "got status ${response.status}" }
            response.bodyAsText()
        }
            .map { json.decodeFromString<FormatDataEntity>(it) }
            .map { it.toDomain() }
            .mapLeft { LoadFormatDataError("Couldn't fetch format data: ${it.message}") }
    }
}
