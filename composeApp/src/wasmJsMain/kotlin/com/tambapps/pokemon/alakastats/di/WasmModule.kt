package com.tambapps.pokemon.alakastats.di

import com.tambapps.pokemon.alakastats.infrastructure.cloud.GisTokenProvider
import com.tambapps.pokemon.alakastats.infrastructure.cloud.GooglePicker
import com.tambapps.pokemon.alakastats.infrastructure.cloud.TeamFilePicker
import com.tambapps.pokemon.alakastats.infrastructure.cloud.auth.OAuthTokenProvider
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.service.PokemonLocalUrlImageService
import com.tambapps.pokemon.alakastats.util.getCurrentBaseUrl
import org.koin.dsl.module

// Web OAuth client of the Google Cloud project. Not a secret: it's public in any web app using it
private const val GOOGLE_WEB_CLIENT_ID = "737779642859-7a2gn9bfpkihf3a9npm3algdmqopn9vr.apps.googleusercontent.com"
// Google Picker's browser key. Not a secret, so it's fine that it's in this public repository (GitHub's secret
// scanning flags it anyway): a browser key is sent by the browser to Google, so anyone can read it in the deployed
// site whatever we do. What protects it are its restrictions in the Google Cloud console: only the app's origin
// (tambapps.github.io) and only the Picker API. It gives access to no user data either: the Picker also needs the
// user's own OAuth token. At worst, someone forging the origin could use the Picker's free quota
private const val GOOGLE_PICKER_API_KEY = "AIzaSyD6mnU4vbwIiLMNA4Vlp9acWbZme2xirxg"
// the Google Cloud project number
private const val GOOGLE_APP_ID = "737779642859"

val wasmModule = module {
    single<PokemonImageService> { PokemonLocalUrlImageService(get(), getCurrentBaseUrl()) }
    single<OAuthTokenProvider> { GisTokenProvider(GOOGLE_WEB_CLIENT_ID) }
    single<TeamFilePicker> { GooglePicker(GOOGLE_PICKER_API_KEY, GOOGLE_APP_ID) }
}
