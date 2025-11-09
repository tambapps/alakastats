package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.usecase.ReplayFiltersUseCase
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.PokemonNameTextField
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource

@Composable
fun FiltersDialog(viewModel: FiltersViewModel) {
    Dialog(onDismissRequest = { viewModel.closeFilters() }) {
        Card(Modifier.fillMaxSize()) {
            Column(Modifier.padding(vertical = 16.dp, horizontal = 8.dp)) {
                Column(Modifier.weight(1f)
                    .verticalScroll(rememberScrollState()),
                ) {

                    Text("Replay Filters", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Filter replays to view stats for specific matchups", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
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
    val filters = viewModel.filters.opponentTeam
    var showAddDialog by remember { mutableStateOf(false) }
    ExpansionTile(
        title = {
            BadgedBox(
                badge = {
                    if (filters.pokemons.isNotEmpty()) {
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
                    filters.pokemons.forEach { pokemonFilter ->
                        val height = 64.dp
                        FilterChip(
                            modifier = Modifier.height(height),
                            onClick = { filters.pokemons.remove(pokemonFilter) },
                            leadingIcon = {
                                viewModel.pokemonImageService.PokemonSprite(
                                    pokemonFilter.name,
                                    disableTooltip = true,
                                    modifier = Modifier.size(height)
                                )
                            },
                            label = { Text(
                                text = pokemonFilter.name.pretty + (if (pokemonFilter.asLead) "\nas lead" else ""),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineSmall
                            ) },
                            selected = false,
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
            containsValidator = { pName -> filters.pokemons.isNotEmpty() && filters.pokemons.any { it.name.matches(pName) } },
            onDismissRequest = { showAddDialog = false },
            placeholder = "Pokemon ${filters.pokemons.size + 1}",
            proposeLeadOption = filters.pokemons.count { it.asLead } < 2,
            onAdd = { filters.pokemons.add(it) }
            )
    }
}

@Composable
private fun AddPokemonNameDialog(
    containsValidator: (PokemonName) -> Boolean,
    placeholder: String,
    proposeLeadOption: Boolean,
    onAdd: (PokemonFilter) -> Unit,
    onDismissRequest: () -> Unit
) {
    var pokemonName by remember { mutableStateOf(PokemonName("")) }
    var asLead by remember { mutableStateOf(false) }

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
                    isError = alreadyContains,
                    supportingText = if (alreadyContains) ({
                        Text("${pokemonName.pretty} was already added")
                    }) else null,
                    onValueChange = { pokemonName = it }
                )
                if (proposeLeadOption) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.clickable { asLead = !asLead },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = asLead,
                            onCheckedChange = { asLead = it}
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("as lead", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd.invoke(PokemonFilter(pokemonName, asLead))
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