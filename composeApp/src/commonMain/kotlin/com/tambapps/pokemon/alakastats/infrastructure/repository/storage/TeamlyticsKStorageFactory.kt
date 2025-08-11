package com.tambapps.pokemon.alakastats.infrastructure.repository.storage

import com.tambapps.pokemon.pokepaste.parser.PokepasteParser

expect fun createTeamlyticsKStorage(pokepasteParser: PokepasteParser): AbstractTeamlyticsKStorage