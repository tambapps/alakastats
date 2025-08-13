package com.tambapps.pokemon.alakastats.util

fun String.titlecase() =
    if (isEmpty()) this
    else this[0].uppercase() + this.substring(1).lowercase()

