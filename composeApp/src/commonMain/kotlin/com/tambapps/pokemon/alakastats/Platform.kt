package com.tambapps.pokemon.alakastats

enum class PlatformType {
    Ios,
    Android,
    Web
}

enum class DeviceType {
    Android, Ios, Desktop
}

interface Platform {
    val name: String
    val type: PlatformType
    val deviceType: DeviceType
    /** the host the web app is served from, null for the native apps */
    val webHost: String? get() = null
}

expect val platform: Platform
