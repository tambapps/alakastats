package com.tambapps.pokemon.alakastats

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform