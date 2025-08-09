package com.tambapps.pokemon.alakastats

enum class PlatformType {
    Ios,
    Jvm,
    Web
}
interface Platform {
    val name: String
    val type: PlatformType
}

expect fun getPlatform(): Platform