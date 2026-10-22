package com.tambapps.pokemon.alakastats.infrastructure.cloud

import arrow.core.Either

/**
 * Lets the user pick, in their cloud storage, the files of teams shared with them. Picking a file gives the app the
 * access to it. Only bound on the platforms having one (the web for now)
 */
interface TeamFilePicker {

    /**
     * @param accessToken a token with the sharing scopes
     * @return the ids of the picked files, empty if the user cancelled
     */
    suspend fun pick(accessToken: String): Either<CloudError, List<String>>
}
