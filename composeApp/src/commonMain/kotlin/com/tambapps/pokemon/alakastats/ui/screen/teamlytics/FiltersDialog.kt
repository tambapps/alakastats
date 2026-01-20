package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
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
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.PokemonFilterChip
import com.tambapps.pokemon.alakastats.ui.composables.PokemonNameTextField
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import com.tambapps.pokemon.alakastats.ui.model.ReplayFilters
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamViewModel
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import com.tambapps.pokemon.alakastats.util.isSdNameValid
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
                    Text("Filter replays to view stats for specific matchups/criteria", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))

                    OpponentFilters(viewModel)
                    Spacer(Modifier.height(22.dp))
                    YouFilters(viewModel)
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
fun OpponentFilters(viewModel: FiltersViewModel) {
    Text(
        text = "Opponent's",
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(Modifier.height(12.dp))

    Column(Modifier.padding(horizontal = 16.dp)) {
        PokemonsFiltersTile(
            title = "Team",
            viewModel = viewModel,
            pokemons = viewModel.opponentTeamFilters,
            max = 6,
            preventLeadOption = true
        )
        Spacer(Modifier.height(12.dp))

        PokemonsFiltersTile(
            title = "Selection",
            viewModel = viewModel,
            pokemons = viewModel.opponentSelectionFilters,
            max = 4,
        )

        Spacer(Modifier.height(12.dp))

        OpponentUsernamesFilter(viewModel)
    }
}

@Composable
fun YouFilters(viewModel: FiltersViewModel) {
    Text(
        text = "Your",
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(Modifier.height(12.dp))

    Column(Modifier.padding(horizontal = 16.dp)) {
        PokemonsFiltersTile(
            title = "Selection",
            viewModel = viewModel,
            pokemons = viewModel.yourSelectionFilters,
            max = 4,
        )
    }
}

@Composable
private fun OpponentUsernamesFilter(viewModel: FiltersViewModel) {
    val opponentUsernamesFilters = viewModel.opponentUsernamesFilters
    var showAddDialog by remember { mutableStateOf(false) }
    ExpansionTile(
        title = {
            BadgedBox(
                badge = {
                    if (opponentUsernamesFilters.isNotEmpty()) {
                        Badge()
                    }
                }
            ) {
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "Username",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    opponentUsernamesFilters.forEach { name ->
                        FilterChip(
                            onClick = { opponentUsernamesFilters.remove(name) },
                            label = { Text(name) },
                            selected = false,
                            trailingIcon = {
                                Text(
                                    text = "×",
                                    style = MaterialTheme.typography.labelLarge
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
                        contentDescription = "Add Username",
                        tint = MaterialTheme.colorScheme.defaultIconColor
                    )
                    Text("Add Username")
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    )

    if (showAddDialog) {
        ShowdownNameDialog(
            onAdd = { opponentUsernamesFilters.add(it) },
            onDismissRequest = { showAddDialog = false }
        )
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
                    modifier = Modifier.padding(start = 8.dp),
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
                        PokemonFilterChip(
                            pokemonName = pokemonFilter.name,
                            pokemonImageService = viewModel.pokemonImageService,
                            onClick = { pokemons.remove(pokemonFilter) },
                            asLead = pokemonFilter.asLead
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
private fun ShowdownNameDialog(
    onDismissRequest: () -> Unit,
    onAdd: (String) -> Unit
) {
    var newName by mutableStateOf("")
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add Showdown Name") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    placeholder = { Text("Enter a Showdown username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !isSdNameValid(newName) && !newName.isEmpty(),
                    supportingText = {
                        when {
                            newName.contains('/') -> Text("Name cannot contain slash characters")
                            newName.length > 30 -> Text("Name must be 30 characters or less")
                            newName.isNotEmpty() && newName.isBlank() -> Text("Name cannot be empty")
                            else -> null
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(newName); onDismissRequest.invoke() },
                enabled = isSdNameValid(newName)
            ) {
                Text(
                    "OK",
                    color = if (isSdNameValid(newName)) Color.Unspecified else Color.LightGray
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text("Cancel")
            }
        }
    )
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
    val opponentUsernamesFilters = mutableStateSetOf<String>().apply {
        addAll(useCase.filters.opponentUsernames)
    }
    val yourSelectionFilters = useCase.filters.yourSelection.toMutableStateList()

    fun clearFilters() {
        opponentTeamFilters.clear()
        opponentSelectionFilters.clear()
        opponentUsernamesFilters.clear()
        yourSelectionFilters.clear()
    }

    fun applyFilters() = useCase.applyFilters(
        ReplayFilters(
            opponentTeam = opponentTeamFilters.toList(),
            opponentSelection = opponentSelectionFilters.toList(),
            opponentUsernames = opponentUsernamesFilters.toSet(),
            yourSelection = yourSelectionFilters.toList()
        )
    )
    fun closeFilters() = useCase.closeFilters()
}