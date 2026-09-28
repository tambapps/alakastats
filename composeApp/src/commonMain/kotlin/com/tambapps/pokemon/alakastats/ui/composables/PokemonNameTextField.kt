package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.service.availablePokemonNames

@Composable
fun PokemonNameTextField(
    value: PokemonName,
    placeholder: String = "Pokemon",
    onValueChange: (PokemonName) -> Unit,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) = SuggestionTextField(
    value = value.value,
    onValueChange = { onValueChange(PokemonName(it)) },
    suggestions = availablePokemonNames(),
    suggestionText = { it.pretty },
    label = placeholder,
    modifier = modifier,
    isError = isError,
    supportingText = supportingText,
    suggestionLeadingContent = { PokemonSprite(it, Modifier.size(40.dp)) },
)
