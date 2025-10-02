package com.tambapps.pokemon.alakastats.util

import kotlinx.browser.window

fun getCurrentBaseUrl(): String {
    val location = window.location
    return "${location.protocol}//${location.host}"
}