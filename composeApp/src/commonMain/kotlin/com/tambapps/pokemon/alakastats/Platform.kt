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
}

expect fun getPlatform(): Platform