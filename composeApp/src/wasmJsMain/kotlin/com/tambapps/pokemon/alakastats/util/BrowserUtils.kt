package com.tambapps.pokemon.alakastats.util

import kotlinx.browser.window

fun getCurrentBaseUrl(): String {
    val base = window.location.href.substringBeforeLast('/')
    return if (base.endsWith("index.html")) {
        base.substringBeforeLast('/')
    } else {
        base
    }
}