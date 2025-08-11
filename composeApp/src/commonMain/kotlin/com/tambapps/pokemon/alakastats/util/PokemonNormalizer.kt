package com.tambapps.pokemon.alakastats.util

object PokemonNormalizer {
    
    fun normalize(s: String): String = s.lowercase().replace(' ', '-')
    
    fun normalizeToBase(input: String): String {
        val s = normalize(input)
        // TODO handle all special forms
        // TODO could be optimized using a tree search?
        return when {
            s.startsWith("urshifu") -> "urshifu"
            s.startsWith("ogerpon") -> "ogerpon"
            s.endsWith("-galar") -> s.substring(0, s.length - 6)
            s.endsWith("-alola") -> s.substring(0, s.length - 6)
            s.endsWith("-paldea") -> {
                // because of Tauros-Paldea-Aqua
                s.substring(0, s.indexOf("-paldea"))
            }
            s.endsWith("-incarnate") -> s.substring(0, s.length - 10)
            s.startsWith("ursaluna") -> "ursaluna"
            s.startsWith("rotom") -> "rotom"
            s.startsWith("terapagos") -> "terapagos"
            s.startsWith("zamazenta") -> "zamazenta"
            s.startsWith("zacian") -> "zacian"
            s.startsWith("necrozma") -> "necrozma"
            s.startsWith("calyrex") -> "calyrex"
            s.startsWith("kyurem") -> "kyurem"
            else -> s
        }
    }
}

