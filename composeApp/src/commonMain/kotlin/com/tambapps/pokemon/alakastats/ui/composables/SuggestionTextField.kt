package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

private const val DEFAULT_MAX_SUGGESTIONS = 20
// suggestions only show up once the input is specific enough, to avoid a huge list on the first letter
private const val MIN_CHARS_TO_SUGGEST = 3

/**
 * Text field suggesting values while typing. Suggestions contain the typed text (case insensitive),
 * the ones starting with it first. Picking a suggestion fills the text field with it.
 *
 * @param suggestionText the text of a suggestion, used both to match the input and to fill the text field
 * @param suggestionLeadingContent optional content displayed before a suggestion's text (e.g. a sprite)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SuggestionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<T>,
    suggestionText: (T) -> String,
    label: String,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    maxSuggestions: Int = DEFAULT_MAX_SUGGESTIONS,
    suggestionLeadingContent: (@Composable (T) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    val filteredSuggestions = remember(value, suggestions) {
        filterSuggestions(value, suggestions, suggestionText, maxSuggestions)
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredSuggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                expanded = newValue.text.trim().length >= MIN_CHARS_TO_SUGGEST
                onValueChange(newValue.text)
            },
            isError = isError,
            supportingText = supportingText,
            singleLine = true,
            label = { Text(label) },
            modifier = textFieldModifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded && filteredSuggestions.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            filteredSuggestions.forEach { suggestion ->
                val text = suggestionText(suggestion)
                DropdownMenuItem(
                    text = { Text(text) },
                    leadingIcon = suggestionLeadingContent?.let { { it(suggestion) } },
                    onClick = {
                        textFieldValue = TextFieldValue(text = text, selection = TextRange(text.length))
                        onValueChange(text)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun <T> filterSuggestions(
    value: String,
    suggestions: List<T>,
    suggestionText: (T) -> String,
    maxSuggestions: Int,
): List<T> {
    val input = value.trim()
    if (input.isEmpty()) return emptyList()
    return suggestions
        .filter { suggestionText(it).contains(input, ignoreCase = true) }
        // suggestions starting with the input first
        .sortedBy { !suggestionText(it).startsWith(input, ignoreCase = true) }
        .take(maxSuggestions)
}
