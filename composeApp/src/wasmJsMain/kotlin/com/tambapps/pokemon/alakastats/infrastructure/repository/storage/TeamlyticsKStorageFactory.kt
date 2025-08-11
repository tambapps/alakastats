package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.pokepaste.parser.PokepasteParser

actual fun createTeamlyticsKStorage(pokepasteParser: PokepasteParser): AbstractTeamlyticsKStorage {
    return WasmTeamlyticsKStorage(pokepasteParser)
}