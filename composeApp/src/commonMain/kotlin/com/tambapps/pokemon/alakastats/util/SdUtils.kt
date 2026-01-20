package com.tambapps.pokemon.alakastats.util


fun isSdNameValid(name: String) = name.isNotBlank() &&
        !name.contains('/') &&
        name.length <= 30