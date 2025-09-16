package com.tambapps.pokemon.alakastats.domain.error

import kotlin.uuid.Uuid

/**
 * Sealed hierarchy representing all possible domain errors in the application
 * This replaces nullable returns and generic exceptions with typed error handling
 */
sealed interface DomainError {
    val message: String
    val cause: Throwable?
}

sealed interface GetTeamlyticsError : DomainError

data class TeamlyticsNotFound(val id: Uuid, override val cause: Throwable? = null) : GetTeamlyticsError {
    override val message: String = "Teamlytics with id $id not found"
}

data class StorageError(
    override val message: String,
    override val cause: Throwable? = null
) : GetTeamlyticsError

