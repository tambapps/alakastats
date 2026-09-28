package com.tambapps.pokemon.alakastats.ui.service

import com.tambapps.pokemon.PokemonName

// Champions dex names megas "Mega Charizard X", images are named "charizard-mega-x"
private val CHAMPIONS_MEGA_NAME = Regex("^mega-(.+?)(-[xyz])?$")

// forms (from the Champions dex) that have no image of their own, and use their base form's image
private val IMAGE_NAME_ALIASES = mapOf(
    "aegislash-shield" to "aegislash",
    "lycanroc-midday" to "lycanroc",
    "gourgeist-average" to "gourgeist",
    "gourgeist-small" to "gourgeist",
    "gourgeist-large" to "gourgeist",
    "gourgeist-super" to "gourgeist",
    "tauros-paldea-combat" to "tauros",
    "tauros-paldea-aqua" to "tauros",
    "tauros-paldea-blaze" to "tauros",
    "meowstic-f" to "meowstic",
    "morpeko-hangry" to "morpeko",
    "persian-alola" to "persian",
    "sirfetch'd" to "sirfetch-d",
)

/**
 * The name of the image files of a pokemon. Mostly its normalized name, except for names coming from
 * the Champions dex that don't follow the images' naming
 */
val PokemonName.imageName: String
    get() {
        val normalizedName = normalized.value
        val megaMatch = CHAMPIONS_MEGA_NAME.matchEntire(normalizedName)
        if (megaMatch != null) {
            val (baseName, variant) = megaMatch.destructured
            return "$baseName-mega$variant"
        }
        return IMAGE_NAME_ALIASES[normalizedName] ?: normalizedName
    }
