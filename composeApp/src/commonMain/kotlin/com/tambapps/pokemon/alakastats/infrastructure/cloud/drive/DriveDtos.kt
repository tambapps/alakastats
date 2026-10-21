package com.tambapps.pokemon.alakastats.infrastructure.cloud.drive

import kotlinx.serialization.Serializable

@Serializable
internal data class DriveFileDto(
    val id: String,
    val name: String = "",
    val modifiedTime: String? = null,
    val appProperties: Map<String, String>? = null,
)

@Serializable
internal data class DriveFileListDto(
    val files: List<DriveFileDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
internal data class DriveFileMetadataDto(
    val name: String,
    val appProperties: Map<String, String>,
    // only allowed on creation
    val parents: List<String>? = null,
)

@Serializable
internal data class GoogleErrorResponseDto(
    val error: GoogleErrorDto? = null,
)

@Serializable
internal data class GoogleErrorDto(
    val code: Int? = null,
    val message: String? = null,
    val errors: List<GoogleErrorReasonDto> = emptyList(),
)

@Serializable
internal data class GoogleErrorReasonDto(
    val reason: String? = null,
    val message: String? = null,
)
