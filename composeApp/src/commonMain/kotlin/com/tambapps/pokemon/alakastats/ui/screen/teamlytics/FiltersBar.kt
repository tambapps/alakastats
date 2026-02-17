package com.tambapps.pokemon.alakastats.ui.screen.teamlytics

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.usecase.ManageReplayFiltersUseCase
import com.tambapps.pokemon.alakastats.ui.composables.PokemonFilterChip
import com.tambapps.pokemon.alakastats.ui.composables.PokemonNameTextField
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import com.tambapps.pokemon.alakastats.ui.model.ReplayFilters
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource
import kotlin.jvm.JvmInline

private class FiltersViewModel(
    private val useCase: ManageReplayFiltersUseCase,
    val pokemonImageService: PokemonImageService
) {
    fun applyFilters(filters: ReplayFilters) = useCase.applyFilters(filters)
}

@JvmInline
private value class AddPokemonDialogState(val asLead: Boolean)

private data class PokemonsFilter(
    val shortTitle: String,
    val title: String,
    val pokemons: List<PokemonFilter>,
    val allowLead: Boolean,
    val max: Int,
)

@Composable
fun FiltersBar(parentViewModel: TeamlyticsFiltersTabViewModel) {
    val filters = parentViewModel.filters
    val viewModel = remember(filters) { FiltersViewModel(parentViewModel.useCase, parentViewModel.pokemonImageService) }
    val opponentTeamFilter = remember(filters) {
        PokemonsFilter("Opp. Team", "Opponent's Team", filters.opponentTeam, allowLead = false, max = 6)
    }
    val opponentSelectionFilter = remember(filters) { PokemonsFilter("Opp. Selection", "Opponent's Selection", filters.opponentSelection, allowLead = true, max = 4) }
    val yourSelectionFilter = remember(filters) { PokemonsFilter("Your Selection", "Your Selection", filters.yourSelection, allowLead = true, max = 4) }
    val showPokemonDialogFilterState = remember { mutableStateOf<PokemonsFilter?>(null) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow {
                PokemonFilterButton(
                    pokemonImageService = viewModel.pokemonImageService,
                    filter = opponentTeamFilter,
                    dialogState = showPokemonDialogFilterState,
                    onClear = { viewModel.applyFilters(filters.copy(opponentTeam = emptyList())) }
                )

                PokemonFilterButton(
                    pokemonImageService = viewModel.pokemonImageService,
                    filter = opponentSelectionFilter,
                    dialogState = showPokemonDialogFilterState,
                    onClear = { viewModel.applyFilters(filters.copy(opponentSelection = emptyList())) }
                )

                FilterButton("Opp. Username", onClick = {})

                PokemonFilterButton(
                    pokemonImageService = viewModel.pokemonImageService,
                    filter = yourSelectionFilter,
                    dialogState = showPokemonDialogFilterState,
                    onClear = { viewModel.applyFilters(filters.copy(yourSelection = emptyList())) }
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    showPokemonDialogFilterState.value?.let { filter ->
        SetPokemonFilterDialog(
            filter = filter,
            pokemonImageService = viewModel.pokemonImageService,
            onComplete = {
                when(filter) {
                    opponentTeamFilter -> viewModel.applyFilters(filters.copy(opponentTeam = it))
                    opponentSelectionFilter -> viewModel.applyFilters(filters.copy(opponentSelection = it))
                    yourSelectionFilter -> viewModel.applyFilters(filters.copy(yourSelection = it))
                }
            },
            onDismiss = { showPokemonDialogFilterState.value = null }
        )
    }
}

@Composable
private fun SetPokemonFilterDialog(
    filter: PokemonsFilter,
    pokemonImageService: PokemonImageService,
    onComplete: (List<PokemonFilter>) -> Unit,
    onDismiss: () -> Unit
) {
    val pokemons = remember { mutableStateListOf<PokemonFilter>().apply { addAll(filter.pokemons) } }
    var addPokemonDialogState by remember { mutableStateOf<AddPokemonDialogState?>(null) }

    AlertDialog(
        title = {
            Text(filter.title)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pokemons.forEach { pokemonFilter ->
                        PokemonFilterChip(
                            pokemonName = pokemonFilter.name,
                            pokemonImageService = pokemonImageService,
                            onClick = { pokemons.remove(pokemonFilter) },
                            asLead = pokemonFilter.asLead
                        )
                    }
                }

                if (filter.allowLead && pokemons.count { it.asLead } < 2) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { addPokemonDialogState = AddPokemonDialogState(asLead = true) },
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.add),
                            contentDescription = "Add Lead Pokemon",
                            tint = MaterialTheme.colorScheme.defaultIconColor
                        )
                        Text("Add Lead Pokemon")
                        Spacer(Modifier.width(8.dp))
                    }
                }

                if (pokemons.size < filter.max) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { addPokemonDialogState = AddPokemonDialogState(asLead = false) },
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
        },
        confirmButton = {
            TextButton(
                onClick = { onComplete(pokemons); onDismiss() }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }

            TextButton(
                onClick = { pokemons.clear() }
            ) {
                Text("Clear")
            }
        },
        onDismissRequest = onDismiss
    )

    addPokemonDialogState?.let { state ->
        AddPokemonNameDialog(
            pokemonImageService = pokemonImageService,
            containsValidator = { pName -> pokemons.isNotEmpty() && pokemons.any { it.name.matches(pName) } },
            onDismissRequest = { addPokemonDialogState = null },
            asLead = state.asLead,
            onAdd = { pokemons.add(it) }
        )
    }
}

@Composable
private fun PokemonFilterButton(
    pokemonImageService: PokemonImageService,
    onClear: () -> Unit,
    dialogState: MutableState<PokemonsFilter?>,
    filter: PokemonsFilter,
) {
    val pokemons = filter.pokemons
    val isCompact = LocalIsCompact.current
    OutlinedButton(
        onClick = {
            if (filter.pokemons.isNotEmpty()) {
                onClear.invoke()
            } else {
                dialogState.value = filter
            }
        },
        shape = RoundedCornerShape(8.dp),
        contentPadding = if (!isCompact || pokemons.isEmpty()) ButtonDefaults.ContentPadding else PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = Modifier.padding(horizontal = if (isCompact) 4.dp else 16.dp)) {
        Text(filter.shortTitle)

        if (pokemons.isNotEmpty()) {
            Spacer(Modifier.width(if (isCompact) 4.dp else 8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                pokemons.chunked(3).forEach { pokemonChunk ->
                    Row {
                        pokemonChunk.forEach { p ->
                            pokemonImageService.PokemonSprite(p.name, Modifier.size(if (isCompact) 32.dp else 40.dp)
                                , disableTooltip = true)
                        }
                    }
                }
            }
            Spacer(Modifier.width(if (isCompact) 8.dp else 16.dp))
            Text("x", style = MaterialTheme.typography.titleLarge)
        }
    }

}
@Composable
private fun FilterButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick, shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(text)
    }
}

@Composable
private fun AddPokemonNameDialog(
    pokemonImageService: PokemonImageService,
    containsValidator: (PokemonName) -> Boolean,
    asLead: Boolean,
    onAdd: (PokemonFilter) -> Unit,
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
                    placeholder = "Pokemon Name",
                    isError = alreadyContains,
                    supportingText = if (alreadyContains) ({
                        Text("${pokemonName.pretty} was already added")
                    }) else null,
                    onValueChange = { pokemonName = it },
                    pokemonImageService = pokemonImageService,
                )
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
