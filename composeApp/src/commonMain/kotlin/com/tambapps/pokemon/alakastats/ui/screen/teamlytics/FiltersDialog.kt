package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.usecase.ReplayFiltersUseCase
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.PokemonNameTextField
import com.tambapps.pokemon.alakastats.ui.model.OpponentTeamFilters
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource
import kotlin.text.matches

@Composable
fun FiltersDialog(viewModel: FiltersViewModel) {
    Dialog(onDismissRequest = { viewModel.closeFilters() }) {
        Card(Modifier.fillMaxSize()) {
            Column(Modifier.padding(vertical = 16.dp, horizontal = 8.dp)) {
                Column(Modifier.weight(1f)
                    .verticalScroll(rememberScrollState()),
                ) {
                    OpponentTeamFiltersTile(viewModel)
                }
                Row(Modifier.padding(horizontal = 8.dp)) {
                    TextButton(onClick = { viewModel.closeFilters() }) {
                        Text("Close")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.closeFilters() }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.closeFilters() }) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}


@Composable
private fun OpponentTeamFiltersTile(viewModel: FiltersViewModel) {
    val filters = viewModel.filters.opponentTeamFilters
    var showAddDialog by remember { mutableStateOf(false) }
    ExpansionTile(
        title = {
            BadgedBox(
                badge = {
                    if (filters.team.isNotEmpty()) {
                        Badge()
                    }
                }
            ) {
                Text(
                    text = "Opponent's team",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        content = {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filters.team.forEach { pokemon ->
                        val height = 64.dp
                        FilterChip(
                            modifier = Modifier.height(height),
                            onClick = { filters.team.remove(pokemon) },
                            leadingIcon = {
                                viewModel.pokemonImageService.PokemonSprite(
                                    pokemon.name,
                                    disableTooltip = true,
                                    modifier = Modifier.size(height)
                                )
                            },
                            label = { Text(
                                pokemon.name.pretty,
                                style = MaterialTheme.typography.headlineSmall
                            ) },
                            selected = true,
                            trailingIcon = {
                                Text(
                                    text = "×",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.add),
                        contentDescription = "Add Pokemon",
                        tint = MaterialTheme.colorScheme.defaultIconColor
                    )
                    Text("Add Pokemon")
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    )

    if (showAddDialog) {
        AddPokemonNameDialog(
            containsValidator = { pName -> filters.team.isNotEmpty() && filters.team.any { it.name.matches(pName) } },
            onDismissRequest = { showAddDialog = false },
            placeholder = "Pokemon ${filters.team.size + 1}",
            onAdd = { filters.team.add(PokemonFilter(it)) }
            )
    }
}

@Composable
private fun AddPokemonNameDialog(
    containsValidator: (PokemonName) -> Boolean,
    placeholder: String,
    onAdd: (PokemonName) -> Unit,
    onDismissRequest: () -> Unit
) {
    var pokemonName by remember { mutableStateOf(PokemonName("")) }

    val alreadyContains = remember(pokemonName) { containsValidator.invoke(pokemonName) }
    val isValid = pokemonName.value.isNotBlank() && !alreadyContains
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add Pokemon Filter") },
        text = {
            Column {
                PokemonNameTextField(
                    value = pokemonName,
                    placeholder = placeholder,
                    isError = !alreadyContains,
                    supportingText = if (alreadyContains) ({
                        Text("${pokemonName.pretty} was already added")
                    }) else null,
                    onValueChange = { pokemonName = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd.invoke(pokemonName)
                    onDismissRequest.invoke()
                },
                enabled = isValid
            ) {
                Text(
                    "Add",
                    color = if (isValid) Color.Unspecified else Color.LightGray
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}


class FiltersViewModel(
    private val useCase: ReplayFiltersUseCase,
    val pokemonImageService: PokemonImageService
) {
    val filters = useCase.filters

    fun closeFilters() = useCase.closeFilters()
}