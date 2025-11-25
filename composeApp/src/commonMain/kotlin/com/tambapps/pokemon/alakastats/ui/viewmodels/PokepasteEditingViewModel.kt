package com.tambapps.pokemon.alakastats.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.Either
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.error.NetworkError
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.pokepaste.parser.PokePasteParseException
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class PokepasteEditingViewModel(
    protected val pokepasteParser: PokepasteParser,
    private val httpClient: HttpClient
) {

    var pokepaste by mutableStateOf("")
        protected set
    var pokepasteError by mutableStateOf<String?>(null)
        private set

    var showPokePasteUrlDialog by mutableStateOf(false)
        private set
    var pokePasteUrlInput by mutableStateOf("")
        private set

    var isLoadingPokepasteUrl by mutableStateOf(false)
        private set
    var pokepasteUrlError by mutableStateOf<String?>(null)
        private set

    val isPokepasteUrlValid: Boolean
        get() = isValidUrl(pokePasteUrlInput)

    protected val scope = CoroutineScope(Dispatchers.Default)

    private fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val trimmedUrl = url.trim()
            trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }

    fun loadPokepasteFromUrl() {
        if (isPokepasteUrlValid && !isLoadingPokepasteUrl) {
            isLoadingPokepasteUrl = true
            pokepasteUrlError = null

            scope.launch {
                fetchUrlContent(pokePasteUrlInput).fold(
                    ifLeft = { error ->
                        pokepasteUrlError = "Failed to load URL: ${error.message}"
                    },
                    ifRight = { content ->
                        withContext(Dispatchers.Main) {
                            updatePokepaste(content)
                            hidePokepasteUrlDialog()
                        }
                    }
                )
                isLoadingPokepasteUrl = false
            }
        }
    }

    private suspend fun fetchUrlContent(url: String): Either<DomainError, String> = Either.catch {
        var normalizedUrl = url
        if (!normalizedUrl.endsWith("/raw")) {
            if (!normalizedUrl.endsWith("/")) {
                normalizedUrl = "$normalizedUrl/"
            }
            normalizedUrl += "raw"
        }
        val response = httpClient.get(normalizedUrl)
        response.bodyAsText()
    }.mapLeft { error -> NetworkError(error.message ?: "unknown error", error) }


    fun showPokepasteUrlDialog() {
        showPokePasteUrlDialog = true
        pokePasteUrlInput = ""
        pokepasteUrlError = null
    }

    fun hidePokepasteUrlDialog() {
        showPokePasteUrlDialog = false
        pokePasteUrlInput = ""
        pokepasteUrlError = null
        isLoadingPokepasteUrl = false
    }

    fun updatePokepasteUrlInput(input: String) {
        pokePasteUrlInput = input
    }

    fun updatePokepaste(paste: String) {
        pokepaste = paste
        validPokepaste()
    }

    protected fun validPokepaste(): PokePaste? {
        if (pokepaste.isBlank()) return null
        try {
            val p = pokepasteParser.parse(pokepaste)
            pokepasteError = null
            return p
        } catch (e: PokePasteParseException) {
            pokepasteError = e.message
            return null
        }
    }
}