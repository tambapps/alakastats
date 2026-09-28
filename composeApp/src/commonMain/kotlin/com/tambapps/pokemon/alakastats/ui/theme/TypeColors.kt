package com.tambapps.pokemon.alakastats.ui.theme

import androidx.compose.ui.graphics.Color
import com.tambapps.pokemon.PokeType

/** The usual color of each type, as in the games' type badges */
val PokeType.color: Color
    get() = when (this) {
        PokeType.NORMAL -> Color(0xFFA8A77A)
        PokeType.FIRE -> Color(0xFFEE8130)
        PokeType.WATER -> Color(0xFF6390F0)
        PokeType.ELECTRIC -> Color(0xFFF7D02C)
        PokeType.GRASS -> Color(0xFF7AC74C)
        PokeType.ICE -> Color(0xFF96D9D6)
        PokeType.FIGHTING -> Color(0xFFC22E28)
        PokeType.POISON -> Color(0xFFA33EA1)
        PokeType.GROUND -> Color(0xFFE2BF65)
        PokeType.FLYING -> Color(0xFFA98FF3)
        PokeType.PSYCHIC -> Color(0xFFF95587)
        PokeType.BUG -> Color(0xFFA6B91A)
        PokeType.ROCK -> Color(0xFFB6A136)
        PokeType.GHOST -> Color(0xFF735797)
        PokeType.DRAGON -> Color(0xFF6F35FC)
        PokeType.DARK -> Color(0xFF705746)
        PokeType.STEEL -> Color(0xFFB7B7CE)
        PokeType.FAIRY -> Color(0xFFD685AD)
    }
