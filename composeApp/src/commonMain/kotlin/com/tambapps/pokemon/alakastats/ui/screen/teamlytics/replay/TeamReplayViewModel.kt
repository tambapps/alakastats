package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.replay

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import io.ktor.http.Url

class TeamReplayViewModel(
    val pokemonImageService: PokemonImageService,
    private val teamState: MutableState<Teamlytics?>,
    val team: Teamlytics,
) {

    private companion object {
        val REPLAYS_SEPARATOR_REGEX = Regex("[,\\s]+")
    }
    var showAddReplayDialog by mutableStateOf(false)
        private set
    
    var replayUrlsText by mutableStateOf("")
        private set
    
    fun showAddReplayDialog() {
        showAddReplayDialog = true
        replayUrlsText = ""
    }
    
    fun hideAddReplayDialog() {
        showAddReplayDialog = false
        replayUrlsText = ""
    }
    
    fun updateReplayUrlsText(text: String) {
        replayUrlsText = text
    }
    
    fun addReplays() {
        val urls = parseReplayUrls(replayUrlsText)
        // TODO: Process the URLs - add them to the team
        // For now, just close the dialog
        hideAddReplayDialog()
    }
    
    private fun parseReplayUrls(text: String): List<String> {
        return text
            .split(REPLAYS_SEPARATOR_REGEX)
            .map { it.trim() }
            .filter { it.isNotBlank() && isValidReplayUrl(it) }
    }
    
    private fun isValidReplayUrl(url: String): Boolean {
        return isValidUrl(url) && url.contains("replay.pokemonshowdown.com")
    }

    private fun isValidUrl(s: String) = try {
        Url(s) // will throw IllegalArgumentException if invalid
        true
    } catch (_: IllegalArgumentException) {
        false
    }
    fun getValidationMessage(): String? {
        if (replayUrlsText.isBlank()) return null
        
        val urls = parseReplayUrls(replayUrlsText)
        val totalUrls = replayUrlsText
            .split(REPLAYS_SEPARATOR_REGEX)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .size
        
        return when {
            urls.isEmpty() && totalUrls > 0 -> "No valid replay URLs found"
            urls.size < totalUrls -> "${urls.size}/${totalUrls} valid URLs found"
            urls.isNotEmpty() -> "${urls.size} valid URLs found"
            else -> null
        }
    }
}