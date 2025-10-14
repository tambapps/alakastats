package com.tambapps.pokemon.alakastats.ui.screen.home

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import alakastats.composeapp.generated.resources.more_horiz
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.PokemonTeamPreview
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamScreen
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.painterResource

object HomeScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<HomeViewModel>()
        LaunchedEffect(Unit) {
            viewModel.loadTeams()
        }
        val isDarkTheme = isSystemInDarkTheme()
        val isCompact = LocalIsCompact.current

        if (isCompact) {
            HomeScreenMobile(isDarkTheme, viewModel)
        } else {
            HomeScreenDesktop(isDarkTheme, viewModel)
        }

        if (viewModel.isLoading) {
            Dialog(
                onDismissRequest = {},
                content = {
                    CircularProgressIndicator()
                }
            )
        }
        viewModel.teamToImport?.let { teamToImport ->
            TeamActionDialog(
                viewModel = viewModel,
                teamName = teamToImport.name,
                pokemons = remember { teamToImport.pokePaste.pokemons.map { it.name } },
                winRatePercentage = remember { teamToImport.winRate },
                nbReplays = teamToImport.replays.size,
                title = "Load Team",
                onDismissRequest = { viewModel.dismissImportTeamDialog() },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmImport() }
                    ) {
                        Text("Load")
                    }
                }
            )

        }
        viewModel.teamToDelete?.let { teamToDelete ->
            TeamActionDialog(
                viewModel = viewModel,
                teamName = teamToDelete.name,
                pokemons = teamToDelete.pokemons,
                winRatePercentage = teamToDelete.winrate,
                nbReplays = teamToDelete.nbReplays,
                title = "Delete Team",
                text = "This action cannot be undone.",
                onDismissRequest = { viewModel.dismissDeleteDialog() },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmDelete() }
                    ) {
                        Text("Delete", color = Color.Red)
                    }
                }
            )
        }
        viewModel.samplesToShow?.let { samples ->
            SamplesDialog(viewModel, samples)
        }
    }
}

@Composable
internal fun CatchPhrase(skipLine: Boolean = false, textAlign: TextAlign = TextAlign.Unspecified) {
    val text = buildString {
        append("Think like Alakazam.")
        append(if (skipLine) '\n' else ' ')
        append("Play like a pro.")
    }
    Text(text, style = MaterialTheme.typography.headlineMedium, textAlign = textAlign)
}

@Composable
internal fun TeamCard(viewModel: HomeViewModel, team: TeamlyticsPreview, modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(),
        onClick = { viewModel.consultTeam(team, navigator) }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Text(team.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.offset(y = (-12).dp)) {
                    IconButton(
                        onClick = { viewModel.showMenu(team.id) }
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.more_horiz),
                            contentDescription = "More"
                        )
                    }
                    DropdownMenu(
                        expanded = viewModel.expandedMenuTeamId == team.id,
                        onDismissRequest = { viewModel.hideMenu() }
                    ) {
                        val snackBar = LocalSnackBar.current
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { viewModel.editTeam(team, navigator, snackBar) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { viewModel.deleteTeamDialog(team) }
                        )
                    }
                }
            }

            PokemonTeamPreview(viewModel.imageService, team.pokemons, fillWidth = true)
            HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
            Row {
                Text("${team.nbReplays} replays")
                Spacer(Modifier.weight(1f))
                Text("${team.winrate}% winrate")
            }
        }
    }
}

@Composable
internal fun NewTeamButton(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    Button(
        modifier = modifier,
        onClick = { navigator.push(EditTeamScreen()) }
    ) {
        Icon(
            painter = painterResource(Res.drawable.add),
            contentDescription = "Add",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.width(8.dp))
        Text("New Team", style = buttonTextStyle.copy(
            color = LocalContentColor.current
        ))
    }
}


internal val buttonTextStyle  @Composable get() = MaterialTheme.typography.labelLarge.copy(
    fontWeight = FontWeight.Bold
)

@Composable
internal fun ImportTeamButton(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val snackbar = LocalSnackBar.current
    OutlinedButton(onClick = { viewModel.importTeam(snackbar) }, modifier = modifier) {
        Text("Import", style = buttonTextStyle)
    }
}
@Composable
internal fun SampleTeamButton(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = { viewModel.showSamplesDialog() }, modifier = modifier) {
        Text("Sample", style = buttonTextStyle)
    }
}

@Composable
private fun SamplesDialog(viewModel: HomeViewModel, samplePreviews: List<TeamlyticsPreview>) {
    val snackBar = LocalSnackBar.current
    AlertDialog(
        onDismissRequest = { viewModel.hideSamplesDialog() },
        title = {
            Text("Select Sample")
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                samplePreviews.forEachIndexed { index, preview ->
                    if (index > 0) {
                        HorizontalDivider(Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 16.dp), thickness = 2.dp)
                    }
                    Column(
                        Modifier.clickable(onClick = {
                            viewModel.importSample(snackBar, preview)
                            viewModel.hideSamplesDialog()
                        })
                    ) {
                        Text(preview.name, style = MaterialTheme.typography.titleLarge)
                        PokemonTeamPreview(viewModel.imageService, preview.pokemons, fillWidth = true)
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Text("${preview.nbReplays} replays")
                            Spacer(Modifier.weight(1f))
                            Text("${preview.winrate}% winrate")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = { viewModel.hideSamplesDialog() }
            ) {
                Text("Cancel")
            }
        }

    )
}

@Composable
private fun TeamActionDialog(
    viewModel: HomeViewModel,
    pokemons: List<PokemonName>,
    nbReplays: Int,
    winRatePercentage: Int,
    teamName: String,
    title: String,
    text: String? = null,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    ) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Column {
            text?.let {
                Text(it)
                Spacer(Modifier.height(16.dp))
            }

            Text(teamName, style = MaterialTheme.typography.titleMedium)
            PokemonTeamPreview(viewModel.imageService, pokemons, fillWidth = true)
            Spacer(Modifier.height(4.dp))
            Row {
                Text("$nbReplays replays")
                Spacer(Modifier.weight(1f))
                Text("$winRatePercentage% winrate")
            }
        } },
        confirmButton = confirmButton,
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text("Cancel")
            }
        }
    )
}
