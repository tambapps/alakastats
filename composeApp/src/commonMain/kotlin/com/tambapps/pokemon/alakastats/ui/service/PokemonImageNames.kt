package com.tambapps.pokemon.alakastats.ui.service

import com.tambapps.pokemon.PokemonName

// Champions dex names megas "Mega Charizard X", images are named "charizard-mega-x"
private val CHAMPIONS_MEGA_NAME = Regex("^mega-(.+?)(-[xyz])?$")

// names whose image is named otherwise, the Pokémon looking the same. A form looking otherwise (e.g. Tauros-Paldea-Aqua)
// has an image of its own: aliasing it to its base form's would show another Pokémon
private val IMAGE_NAME_ALIASES = mapOf(
    // the default forms
    "aegislash-shield" to "aegislash",
    "lycanroc-midday" to "lycanroc",
    "gourgeist-average" to "gourgeist",
    // the sizes, hardly visible on an image
    "gourgeist-small" to "gourgeist",
    "gourgeist-large" to "gourgeist",
    "gourgeist-super" to "gourgeist",
    // Mega Meowstic looks the same whatever its gender
    "meowstic-m-mega" to "meowstic-mega",
    "meowstic-f-mega" to "meowstic-mega",
    // the apostrophes: Showdown's names have a typographic one
    "farfetch’d" to "farfetch'd",
    "sirfetch'd" to "sirfetch-d",
    "sirfetch’d" to "sirfetch-d",
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
