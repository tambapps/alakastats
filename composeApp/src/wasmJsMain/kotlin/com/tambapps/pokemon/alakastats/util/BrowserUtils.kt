package com.tambapps.pokemon.alakastats.util

import kotlinx.browser.document

/**
 * The URL the app's files are relative to. It's the document's base URI, not the page's URL, as a page can set another
 * base (e.g. the damage-calc page, in a subfolder, uses the app's root)
 */
fun getCurrentBaseUrl(): String {
    val base = document.baseURI.substringBeforeLast('/')
    return if (base.endsWith("index.html")) {
        base.substringBeforeLast('/')
    } else {
        base
    }
}