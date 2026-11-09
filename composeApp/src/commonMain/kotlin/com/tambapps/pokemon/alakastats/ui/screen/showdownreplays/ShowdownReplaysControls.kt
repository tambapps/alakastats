package com.tambapps.pokemon.alakastats.ui.screen.showdownreplays

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.delete
import alakastats.composeapp.generated.resources.download
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayFilters
import com.tambapps.pokemon.alakastats.ui.composables.FormatSelector
import com.tambapps.pokemon.alakastats.ui.composables.SortChip
import com.tambapps.pokemon.alakastats.ui.model.PokemonFilter
import com.tambapps.pokemon.alakastats.ui.model.ShowdownReplaySort
import com.tambapps.pokemon.alakastats.ui.model.ShowdownReplaySortCriterion
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.ClearIcon
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.FilterBarButton
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.PokemonFilterButton
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.PokemonsFilter
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.SetPokemonFilterDialog
import com.tambapps.pokemon.alakastats.ui.screen.teamlytics.ShowdownNameDialog
import org.jetbrains.compose.resources.painterResource

private const val TEAM_FILTER_MAX = 6
private const val SELECTION_FILTER_MAX = 4

/** E.g. 10,000 */
private fun formatCount(count: Int) = count.toString().reversed().chunked(3).joinToString(",").reversed()

/** E.g. "Regulation M-C Bo3" */
private val ShowdownReplaysViewModel.formatLabel get() = format.displayedName + if (bestOf3) " Bo3" else " Bo1"

/**
 * What the screen shows the replays of, and the actions on them: loading more, deleting those shown. Below it, how the
 * loading goes
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShowdownReplaysToolbar(viewModel: ShowdownReplaysViewModel, modifier: Modifier = Modifier) {
    val enabled = !viewModel.isBusy
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FormatSelector(
                format = viewModel.format,
                onFormatChange = viewModel::selectFormat,
                formats = viewModel.formats,
                enabled = enabled,
                modifier = Modifier.width(240.dp),
            )
            FilterChip(selected = !viewModel.bestOf3, onClick = { viewModel.selectBestOf3(false) }, label = { Text("Bo1") }, enabled = enabled)
            FilterChip(selected = viewModel.bestOf3, onClick = { viewModel.selectBestOf3(true) }, label = { Text("Bo3") }, enabled = enabled)
            FilledTonalButton(onClick = { viewModel.showLoadDialog = true }, enabled = enabled) {
                Icon(painterResource(Res.drawable.download), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(8.dp))
                Text("Load replays")
            }
            IconButton(onClick = viewModel::askDeletion, enabled = enabled && viewModel.shownReplays.isNotEmpty()) {
                Icon(painterResource(Res.drawable.delete), contentDescription = "Delete the replays shown")
            }
        }
        LoadStatus(viewModel)
    }
}

@Composable
private fun LoadStatus(viewModel: ShowdownReplaysViewModel) {
    when (val load = viewModel.load) {
        ShowdownReplaysLoad.Idle, is ShowdownReplaysLoad.Listed, ShowdownReplaysLoad.TooMany -> Unit
        is ShowdownReplaysLoad.Listing -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Listing the replays… ${load.found} found", Modifier.weight(1f, fill = false))
            TextButton(onClick = viewModel::cancel) { Text("Cancel") }
        }
        is ShowdownReplaysLoad.Downloading -> Column {
            LinearProgressIndicator(
                progress = { if (load.total == 0) 0f else load.done.toFloat() / load.total },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Downloading ${load.done} / ${load.total}" + if (load.failed > 0) " (${load.failed} failed)" else "",
                    Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::cancel) { Text("Cancel") }
            }
        }
        is ShowdownReplaysLoad.Done -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildString {
                    append(if (load.saved == 1) "1 replay saved" else "${load.saved} replays saved")
                    if (load.failed > 0) append(", ${load.failed} couldn't be downloaded")
                },
                Modifier.weight(1f),
            )
            TextButton(onClick = viewModel::dismissResult) { Text("OK") }
        }
        is ShowdownReplaysLoad.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(load.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            TextButton(onClick = viewModel::dismissResult) { Text("OK") }
        }
    }
}

/** Where the replays will be, the first time: how to get some */
@Composable
internal fun NoSavedReplays(viewModel: ShowdownReplaysViewModel, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "No replay of ${viewModel.formatLabel} on this device yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            "Load the public replays of the last days from Showdown, then filter them by Pokémon, player or Elo",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** What to load: the period and the minimum Elo, the format being the one of the screen */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoadDialog(viewModel: ShowdownReplaysViewModel) {
    if (!viewModel.showLoadDialog) return
    val dismiss = { viewModel.showLoadDialog = false }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Load replays") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("The public replays of ${viewModel.formatLabel} uploaded on Showdown during")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (days in SHOWDOWN_REPLAYS_PERIODS) {
                        FilterChip(
                            selected = viewModel.days == days,
                            onClick = { viewModel.days = days },
                            label = { Text(if (days == 1) "Last day" else "Last $days days") },
                        )
                    }
                }
                OutlinedTextField(
                    value = viewModel.minRatingText,
                    onValueChange = { text -> viewModel.minRatingText = text.filter(Char::isDigit).take(4) },
                    label = { Text("Minimum Elo (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    "You'll see how many there are before they're downloaded, up to ${formatCount(MAX_REPLAYS_PER_LOAD)} " +
                        "at once. Those already saved are skipped",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.listReplays() }) { Text("Find replays") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun LoadConfirmationDialog(viewModel: ShowdownReplaysViewModel) {
    if (viewModel.load == ShowdownReplaysLoad.TooMany) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = { Text("Too many replays") },
            text = {
                Text(
                    "There are more than ${formatCount(MAX_REPLAYS_PER_LOAD)} new replays in this period, more than a " +
                        "load can download. Choose a shorter period or a minimum Elo."
                )
            },
            confirmButton = { TextButton(onClick = viewModel::narrowLoad) { Text("Change") } },
            dismissButton = { TextButton(onClick = viewModel::dismissResult) { Text("Cancel") } },
        )
        return
    }
    val listed = viewModel.load as? ShowdownReplaysLoad.Listed ?: return
    val count = listed.toDownload.size
    val skipped = if (listed.alreadySaved > 0) " ${listed.alreadySaved} already saved are skipped." else ""
    if (count == 0) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = { Text("No new replay") },
            text = { Text("There's no replay to download for this period.$skipped") },
            confirmButton = { TextButton(onClick = viewModel::dismissResult) { Text("OK") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = viewModel::cancel,
        title = { Text("Download replays") },
        text = {
            Text(
                (if (count == 1) "1 replay" else "$count replays") +
                    " will be downloaded and saved on this device.$skipped"
            )
        },
        confirmButton = { TextButton(onClick = viewModel::download) { Text("Download") } },
        dismissButton = { TextButton(onClick = viewModel::cancel) { Text("Cancel") } },
    )
}

/** Deletes the replays shown: all the saved ones of the format, or those the filters keep */
@Composable
internal fun DeletionConfirmationDialog(viewModel: ShowdownReplaysViewModel) {
    val replays = viewModel.replaysToDelete ?: return
    val count = if (replays.size == 1) "1 saved replay" else "${replays.size} saved replays"
    val which = if (viewModel.filters.isEmpty) "All the $count of ${viewModel.formatLabel}"
    else "The $count of ${viewModel.formatLabel} the filters show"
    AlertDialog(
        onDismissRequest = viewModel::dismissDeletion,
        title = { Text("Delete replays") },
        text = { Text("$which will be deleted from this device. They can be downloaded again.") },
        confirmButton = {
            TextButton(onClick = viewModel::confirmDeletion) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissDeletion) { Text("Cancel") } },
    )
}

/**
 * The filters and the order of the saved replays, as the team replays' filters bar
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShowdownReplayFiltersCard(viewModel: ShowdownReplaysViewModel, modifier: Modifier = Modifier) {
    val filters = viewModel.filters
    val apply = { newFilters: ShowdownReplayFilters -> viewModel.filters = newFilters }
    val teamFilter = remember(filters) {
        PokemonsFilter("Team", "Pokémon in a Team", filters.teamPokemons.map { PokemonFilter(it, asLead = false) }, allowLead = false, max = TEAM_FILTER_MAX)
    }
    val selectionFilter = remember(filters) {
        PokemonsFilter("Selection", "Pokémon Brought", filters.broughtPokemons, allowLead = true, max = SELECTION_FILTER_MAX)
    }
    val dialogState = remember { mutableStateOf<PokemonsFilter?>(null) }

    ElevatedCard(modifier) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp).weight(1f))
                if (!filters.isEmpty) {
                    TextButton(onClick = { apply(ShowdownReplayFilters()) }) { Text("Clear all") }
                }
                val sort = viewModel.sort
                SortChip(
                    criteria = ShowdownReplaySortCriterion.entries,
                    criterion = sort.criterion,
                    descending = sort.descending,
                    isDefault = sort.isDefault,
                    onSortChange = { criterion, descending -> viewModel.sort = ShowdownReplaySort(criterion, descending) },
                    onReset = { viewModel.sort = ShowdownReplaySort() },
                )
            }
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                PokemonFilterButton(
                    onClear = { apply(filters.copy(teamPokemons = emptyList())) },
                    dialogState = dialogState,
                    filter = teamFilter,
                )
                PokemonFilterButton(
                    onClear = { apply(filters.copy(broughtPokemons = emptyList())) },
                    dialogState = dialogState,
                    filter = selectionFilter,
                )
                UsernameFilterButton(filters.userName) { apply(filters.copy(userName = it)) }
                MinEloFilterButton(filters.minRating) { apply(filters.copy(minRating = it)) }
                FilterChip(
                    selected = filters.winnerOnly,
                    onClick = { apply(filters.copy(winnerOnly = !filters.winnerOnly)) },
                    label = { Text("Winner only") },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }

    dialogState.value?.let { filter ->
        SetPokemonFilterDialog(
            filter = filter,
            onComplete = { pokemons ->
                when (filter) {
                    teamFilter -> apply(filters.copy(teamPokemons = pokemons.map { it.name }))
                    selectionFilter -> apply(filters.copy(broughtPokemons = pokemons))
                }
            },
            onDismiss = { dialogState.value = null },
        )
    }
}

@Composable
private fun UsernameFilterButton(userName: String?, onChange: (String?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    FilterBarButton(onClick = { showDialog = true }) {
        if (userName.isNullOrBlank()) {
            Text("Username")
        } else {
            Text("Username: $userName")
            Spacer(Modifier.width(18.dp))
            ClearIcon(onClick = { onChange(null) })
        }
    }
    if (showDialog) {
        ShowdownNameDialog(
            onDismissRequest = { showDialog = false },
            initialValue = userName.orEmpty(),
            onAdd = { onChange(it.ifBlank { null }) },
        )
    }
}

@Composable
private fun MinEloFilterButton(minRating: Int?, onChange: (Int?) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    FilterBarButton(onClick = { showDialog = true }) {
        if (minRating == null) {
            Text("Min Elo")
        } else {
            Text("Elo ≥ $minRating")
            Spacer(Modifier.width(18.dp))
            ClearIcon(onClick = { onChange(null) })
        }
    }
    if (showDialog) {
        var text by remember { mutableStateOf(minRating?.toString().orEmpty()) }
        val dismiss = { showDialog = false }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("Minimum Elo") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(4) },
                    label = { Text("Elo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(onClick = { onChange(text.toIntOrNull()); dismiss() }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
        )
    }
}
