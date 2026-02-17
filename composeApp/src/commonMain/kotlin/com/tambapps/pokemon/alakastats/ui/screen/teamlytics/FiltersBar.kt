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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.domain.usecase.ManageReplayFiltersUseCase
import com.tambapps.pokemon.alakastats.ui.composables.PokemonFilterChip
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import com.tambapps.pokemon.alakastats.ui.model.ReplayFilters
import com.tambapps.pokemon.alakastats.ui.service.PokemonImageService
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource


private data class PokemonsFilter(
    val title: String,
    val pokemons: List<PokemonFilter>,
    val allowLead: Boolean,
    val max: Int
)

@Composable
fun FiltersBar(parentViewModel: TeamlyticsFiltersTabViewModel) {
    val filters = parentViewModel.filters
    val viewModel = remember(filters) { FiltersViewModel2(parentViewModel.useCase, parentViewModel.pokemonImageService) }
   val opponentTeamFilter = remember(filters) { PokemonsFilter("Oponent's Team", filters.opponentTeam, allowLead = false, max = 6) }
    val pokemonsFilters = remember { listOf(opponentTeamFilter) }
    var showOpponentTeamDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow {
                PokemonFilterButton(
                    viewModel,
                    opponentTeamFilter,
                    onClick = {
                        if (filters.opponentTeam.isNotEmpty()) {
                            viewModel.applyFilters(filters.copy(opponentTeam = emptyList()))
                        } else {
                            showOpponentTeamDialog = true
                        }
                    }
                )


                FilterButton("Opp. Username", onClick = {})


            }
            Spacer(Modifier.height(16.dp))

        }
    }

    if (showOpponentTeamDialog) {
        SetPokemonFilterDialog(
            filter = opponentTeamFilter,
            pokemonImageService = viewModel.pokemonImageService,
            onComplete = { viewModel.applyFilters(filters.copy(opponentTeam = it)) },
            onDismiss = { showOpponentTeamDialog = false }
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
    viewModel: FiltersViewModel2,
    filter: PokemonsFilter,
    onClick: () -> Unit
) {
    val pokemons = filter.pokemons
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = if (pokemons.isEmpty()) ButtonDefaults.ContentPadding else PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(filter.title)

        if (pokemons.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                (pokemons + pokemons+ pokemons +pokemons+ pokemons+ pokemons).chunked(3).forEach { pokemonChunk ->
                    Row {
                        pokemonChunk.forEach { p ->
                            viewModel.pokemonImageService.PokemonSprite(p.name, Modifier.size(32.dp), disableTooltip = true)
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
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

class FiltersViewModel2(
    private val useCase: ManageReplayFiltersUseCase,
    val pokemonImageService: PokemonImageService
) {

    val opponentTeamFilters = useCase.filters.opponentTeam.toMutableStateList()
    val opponentSelectionFilters = useCase.filters.opponentSelection.toMutableStateList()
    val opponentUsernamesFilters = mutableStateSetOf<UserName>().apply {
        addAll(useCase.filters.opponentUsernames)
    }
    val yourSelectionFilters = useCase.filters.yourSelection.toMutableStateList()

    fun clearFilters() {
        opponentTeamFilters.clear()
        opponentSelectionFilters.clear()
        opponentUsernamesFilters.clear()
        yourSelectionFilters.clear()
    }

    fun applyFilters(filters: ReplayFilters) = useCase.applyFilters(filters)
    fun closeFilters() = useCase.closeFilters()
}