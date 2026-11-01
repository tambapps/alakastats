package com.tambapps.pokemon.alakastats.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import arrow.core.Either
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.error.NetworkError
import com.tambapps.pokemon.pokepaste.parser.PokePaste
import com.tambapps.pokemon.pokepaste.parser.PokePasteParseException
import com.tambapps.pokemon.pokepaste.parser.PokepasteParser
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The text of a PokePaste, from its page's URL
 */
suspend fun HttpClient.fetchPokepaste(url: String): Either<DomainError, String> = Either.catch {
    var normalizedUrl = url.trim()
    if (!normalizedUrl.endsWith("/raw")) {
        if (!normalizedUrl.endsWith("/")) {
            normalizedUrl = "$normalizedUrl/"
        }
        normalizedUrl += "raw"
    }
    val response = get(normalizedUrl)
    // e.g. a 404 for a paste that doesn't exist, rather than an empty paste
    check(response.status.isSuccess()) { "the server answered ${response.status}" }
    response.bodyAsText()
}.mapLeft { error -> NetworkError(error.message ?: "unknown error", error) }

/**
 * The paste shown as an OTS if none of its Pokémon has EVs. The parser only sees an OTS in a paste without any
 * EVs, IVs or Nature line, so e.g. an OTS copied from a replay (whose IVs are all 0) would show stats all at 0
 */
fun PokePaste.asOtsIfNoStats(): PokePaste =
    if (!isOts && pokemons.all { it.evs == PokeStats.default(PokepasteParser.DEFAULT_EV_VALUE) }) copy(isOts = true)
    else this

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

    /**
     * The paste as last applied from the paste dialog, the one shown and saved
     */
    var appliedPokepaste by mutableStateOf<PokePaste?>(null)
        private set

    var isEditingPokepaste by mutableStateOf(false)
        private set
    private var pokepasteBeforeEdition = ""

    val canApplyPokepaste: Boolean get() = pokepaste.isBlank() || pokepasteError == null

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
                httpClient.fetchPokepaste(pokePasteUrlInput).fold(
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

    /**
     * Shows the given paste, e.g. the one of what is edited. It is kept as is, so that it's saved unchanged if the
     * user doesn't touch it
     */
    protected fun loadPokepaste(paste: PokePaste?) {
        appliedPokepaste = paste
        pokepaste = paste?.toPokePasteString() ?: ""
        validPokepaste()
    }

    fun startPokepasteEdition() {
        pokepasteBeforeEdition = pokepaste
        isEditingPokepaste = true
    }

    fun cancelPokepasteEdition() {
        updatePokepaste(pokepasteBeforeEdition)
        isEditingPokepaste = false
    }

    /**
     * Applies the paste being edited. Does nothing if it doesn't parse
     */
    fun applyPokepasteEdition() {
        if (pokepaste.isBlank()) {
            appliedPokepaste = null
        } else {
            appliedPokepaste = validPokepaste() ?: return
        }
        isEditingPokepaste = false
    }

    protected fun validPokepaste(): PokePaste? {
        if (pokepaste.isBlank()) {
            // not the error of a paste that has since been cleared
            pokepasteError = null
            return null
        }
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