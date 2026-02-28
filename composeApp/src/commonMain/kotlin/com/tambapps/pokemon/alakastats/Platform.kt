package com.tambapps.pokemon.alakastats

enum class PlatformType {
    Ios,
    Android,
    Web
}
interface Platform {
    val name: String
    val type: PlatformType
}

expect fun getPlatform(): Platform