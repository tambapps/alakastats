package com.tambapps.pokemon.alakastats.infrastructure.cloud.drive

import kotlinx.serialization.Serializable

@Serializable
internal data class DriveFileDto(
    val id: String,
    val name: String = "",
    val modifiedTime: String? = null,
    val appProperties: Map<String, String>? = null,
    // only asked for shared files. version is an int64, given as a string
    val version: String? = null,
    val headRevisionId: String? = null,
    val trashed: Boolean = false,
    val ownedByMe: Boolean = false,
    val owners: List<DriveUserDto> = emptyList(),
    val capabilities: DriveCapabilitiesDto? = null,
)

@Serializable
internal data class DriveUserDto(
    val emailAddress: String? = null,
    val displayName: String? = null,
    val me: Boolean = false,
)

@Serializable
internal data class DriveAboutDto(
    val user: DriveUserDto? = null,
)

@Serializable
internal data class DriveCapabilitiesDto(
    val canEdit: Boolean = false,
    val canShare: Boolean = false,
)

@Serializable
internal data class DriveFolderDto(
    val name: String,
    val mimeType: String,
)

@Serializable
internal data class DrivePermissionDto(
    val id: String = "",
    val type: String = "user",
    val role: String,
    val emailAddress: String? = null,
    val displayName: String? = null,
)

@Serializable
internal data class DrivePermissionListDto(
    val permissions: List<DrivePermissionDto> = emptyList(),
)

@Serializable
internal data class DriveRevisionDto(
    val id: String,
    val modifiedTime: String? = null,
    val lastModifyingUser: DriveUserDto? = null,
)

@Serializable
internal data class DriveRevisionListDto(
    val revisions: List<DriveRevisionDto> = emptyList(),
    val nextPageToken: String? = null,
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
