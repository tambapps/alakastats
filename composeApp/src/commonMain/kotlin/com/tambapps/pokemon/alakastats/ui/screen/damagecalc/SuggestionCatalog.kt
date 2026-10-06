package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.champions.data.Ability
import com.tambapps.pokemon.champions.data.ChampionsDex
import com.tambapps.pokemon.champions.data.Item

/**
 * The values a text field suggests and accepts, with how they're shown, shared by the desktop layout's text fields and
 * the mobile layout's dialogs so that both behave and look the same
 *
 * @param allowBlank whether an empty value can be applied (e.g. no item)
 * @param suggestionLeadingContent what's shown before a suggested value (e.g. a move's type icon)
 */
internal class SuggestionCatalog(
    val values: List<String>,
    val allowBlank: Boolean = false,
    val suggestionLeadingContent: (@Composable (String) -> Unit)? = null,
) {

    /**
     * The value [input] stands for, with the catalog's spelling (e.g. "Moonblast" for "moonblast "), "" for a blank
     * input if [allowBlank], null if it's none of [values]
     */
    fun match(input: String): String? =
        if (input.isBlank()) "".takeIf { allowBlank }
        else values.firstOrNull { it.equals(input.trim(), ignoreCase = true) }
}

// the species, forms being selected separately
internal val POKEMON_CATALOG by lazy {
    SuggestionCatalog(
        values = PICKABLE_POKEMON_NAMES.map { it.value },
        suggestionLeadingContent = { PokemonSprite(PokemonName(it), Modifier.size(32.dp)) },
    )
}

internal val ABILITY_CATALOG by lazy {
    SuggestionCatalog(Ability.entries.filter { it != Ability.NO_ABILITY }.map { it.displayName })
}

// no item is allowed
internal val ITEM_CATALOG by lazy { SuggestionCatalog(Item.entries.map { it.displayName }, allowBlank = true) }

// an empty move slot is allowed
internal val MOVE_CATALOG by lazy {
    SuggestionCatalog(
        values = ChampionsDex.allMoves.map { it.name.value }.sorted(),
        allowBlank = true,
        suggestionLeadingContent = { MoveSuggestionTypeIcon(it) },
    )
}
