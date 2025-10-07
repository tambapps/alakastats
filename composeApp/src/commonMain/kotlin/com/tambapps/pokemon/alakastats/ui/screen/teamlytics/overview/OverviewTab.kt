package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.more_vert
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.domain.model.computeWinRatePercentage
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamScreen
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource

@Composable
fun OverviewTab(viewModel: OverviewViewModel) {
    val isCompact = LocalIsCompact.current
    if (isCompact) {
        OverviewTabMobile(viewModel)
    } else {
        OverviewTabDesktop(viewModel)
    }
}

@Composable
internal fun TeamName(team: Teamlytics, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = team.name,
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun Header(team: Teamlytics) {
    val replaysCount = remember { team.replays.size }
    val textStyle = MaterialTheme.typography.titleLarge
    if (replaysCount == 0) {
        Text(
            "${team.replays.size} replays",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
        return
    }
    Row(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Spacer(Modifier.weight(1f))
        Text("$replaysCount replays", style = textStyle)
        Spacer(Modifier.width(32.dp))
        val winRate = remember { team.computeWinRatePercentage() }
        Text("$winRate% winrate", style = textStyle)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun PokePasteTitle() {
    Text(
        text = "PokePaste",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun EditButton(team: Teamlytics) {
    val navigator = LocalNavigator.currentOrThrow

    OutlinedButton(onClick = { navigator.push(EditTeamScreen(team))}) {
        Text("Edit")
    }
}

@Composable
internal fun MoreActionsButton(viewModel: OverviewViewModel) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val snackbar = LocalSnackBar.current

    IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
        Icon(
            modifier = if (LocalIsCompact.current) Modifier else Modifier.size(40.dp),
            painter = painterResource(Res.drawable.more_vert),
            contentDescription = "More",
            tint = MaterialTheme.colorScheme.defaultIconColor
        )
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Export") },
                onClick = {
                    isMenuExpanded = false
                    snackbar.show("TODO (not implemented yet)")
                }
            )

            val alreadyHasNotes = false
            DropdownMenuItem(
                text = { Text(
                    if (!alreadyHasNotes) "Add notes" else "Edit notes"
                ) },
                onClick = {
                    viewModel.editNotes()
                    isMenuExpanded = false
                }
            )

            if (alreadyHasNotes) {
                DropdownMenuItem(
                    text = { Text("Remove notes") },
                    onClick = {
                        viewModel.removeNotes()
                        isMenuExpanded = false
                    }
                )
            }

        }
    }
}
