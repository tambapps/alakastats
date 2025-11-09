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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.usecase.ManageReplayFiltersUseCase
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.PokemonNameTextField
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import com.tambapps.pokemon.alakastats.ui.model.ReplayFilters
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource

@Composable
fun FiltersDialog(viewModel: FiltersViewModel) {
    Dialog(onDismissRequest = { viewModel.closeFilters() }) {
        Card(Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.8f)) {
            Column(Modifier.padding(vertical = 16.dp, horizontal = 8.dp)) {
                Column(Modifier.weight(1f)
                    .verticalScroll(rememberScrollState()),
                ) {

                    Text("Replay Filters", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Filter replays to view stats for specific matchups", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))

                    PokemonsFiltersTile(
                        title = "Opponent's team",
                        viewModel = viewModel,
                        pokemons = viewModel.opponentTeamFilters,
                        max = 6,
                        preventLeadOption = true
                        )
                    Spacer(Modifier.height(16.dp))

                    PokemonsFiltersTile(
                        title = "Opponent's selection",
                        viewModel = viewModel,
                        pokemons = viewModel.opponentSelectionFilters,
                        max = 4,
                        )
                    Spacer(Modifier.height(16.dp))

                    PokemonsFiltersTile(
                        title = "Your selection",
                        viewModel = viewModel,
                        pokemons = viewModel.yourSelectionFilters,
                        max = 4,
                        )
                    Spacer(Modifier.height(16.dp))
                }
                Row(Modifier.padding(horizontal = 8.dp)) {
                    TextButton(onClick = { viewModel.closeFilters() }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text("Clear")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.applyFilters() }) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun PokemonsFiltersTile(
    title: String,
    viewModel: FiltersViewModel,
    pokemons: SnapshotStateList<PokemonFilter>,
    max: Int,
    preventLeadOption: Boolean = false
) {
    var showAddDialog by remember { mutableStateOf(false) }
    ExpansionTile(
        title = {
            BadgedBox(
                badge = {
                    if (pokemons.isNotEmpty()) {
                        Badge()
                    }
                }
            ) {
                Text(
                    text = title,
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
                    pokemons.forEach { pokemonFilter ->
                        val height = 70.dp
                        FilterChip(
                            modifier = Modifier.height(height).padding(vertical = 4.dp),
                            onClick = { pokemons.remove(pokemonFilter) },
                            leadingIcon = {
                                viewModel.pokemonImageService.PokemonSprite(
                                    pokemonFilter.name,
                                    disableTooltip = true,
                                    modifier = Modifier.size(height).padding(bottom = 8.dp)
                                )
                            },
                            label = { Text(
                                text = pokemonFilter.name.pretty + (if (pokemonFilter.asLead) "\nas lead" else ""),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineSmall
                            ) },
                            selected = pokemonFilter.asLead,
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
                if (pokemons.size < max) {
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
        }
    )

    if (showAddDialog) {
        AddPokemonNameDialog(
            pokemonImageService = viewModel.pokemonImageService,
            containsValidator = { pName -> pokemons.isNotEmpty() && pokemons.any { it.name.matches(pName) } },
            onDismissRequest = { showAddDialog = false },
            proposeLeadOption = !preventLeadOption && pokemons.count { it.asLead } < 2,
            onAdd = { pokemons.add(it) }
            )
    }
}

@Composable
private fun AddPokemonNameDialog(
    pokemonImageService: PokemonImageService,
    containsValidator: (PokemonName) -> Boolean,
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
                    placeholder = "Pokemon Name",
                    isError = alreadyContains,
                    supportingText = if (alreadyContains) ({
                        Text("${pokemonName.pretty} was already added")
                    }) else null,
                    onValueChange = { pokemonName = it },
                    pokemonImageService = pokemonImageService,
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
    private val useCase: ManageReplayFiltersUseCase,
    val pokemonImageService: PokemonImageService
) {

    val opponentTeamFilters = useCase.filters.opponentTeam.toMutableStateList()
    val opponentSelectionFilters = useCase.filters.opponentSelection.toMutableStateList()
    val yourSelectionFilters = useCase.filters.yourSelection.toMutableStateList()

    fun clearFilters() {
        opponentTeamFilters.clear()
        opponentSelectionFilters.clear()
        yourSelectionFilters.clear()
    }

    fun applyFilters() = useCase.applyFilters(
        ReplayFilters(
            opponentTeam = opponentTeamFilters.toList(),
            opponentSelection = opponentSelectionFilters.toList(),
            yourSelection = yourSelectionFilters.toList()
        )
    )
    fun closeFilters() = useCase.closeFilters()
}