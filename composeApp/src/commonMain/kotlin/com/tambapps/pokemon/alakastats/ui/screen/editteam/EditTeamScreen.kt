package com.tambapps.pokemon.alakastats.ui.screen.editteam

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.PokePasteInput
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import com.tambapps.pokemon.alakastats.util.isSdNameValid
import org.jetbrains.compose.resources.painterResource

data class EditTeamScreen(val teamlytics: Teamlytics? = null) : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<EditTeamViewModel>()
        LaunchedEffect(Unit) {
            if (teamlytics != null) {
                viewModel.prepareEdition(teamlytics)
            }
        }
        val navigator = LocalNavigator.currentOrThrow
        val isCompact = LocalIsCompact.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (teamlytics != null) "Edit Team" else "Create Team") },
                    navigationIcon = {
                        BackIconButton(navigator)
                    }
                )
            }
        ) { scaffoldPadding ->
            val paddingValues = if (isCompact) PaddingValues(start = 16.dp, end = 16.dp)
            else PaddingValues(16.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TeamNameInput(viewModel)

                ShowdownNamesInput(viewModel)

                PokePasteInput(viewModel)

                Spacer(modifier = Modifier.weight(1f))

                ButtonBar(navigator, viewModel, teamlytics != null)
            }
        }

        if (viewModel.showNewSdNameDialog) {
            ShowdownNameDialog(viewModel)
        }
    }
}

@Composable
private fun TeamNameInput(viewModel: EditTeamViewModel) {
    Text(
        text = "Team Name",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    OutlinedTextField(
        value = viewModel.teamName,
        onValueChange = viewModel::updateTeamName,
        placeholder = { Text("Enter a name for your team") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}


@Composable
private fun ShowdownNamesInput(viewModel: EditTeamViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),

            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Showdown Names (${viewModel.sdNames.size})",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(16.dp))
            OutlinedIconButton(onClick = { viewModel.showAddNameDialog() }) {
                Icon(
                    painter = painterResource(Res.drawable.add),
                    contentDescription = "Add Name",
                    tint = MaterialTheme.colorScheme.defaultIconColor
                )
            }
        }

        if (viewModel.sdNames.isEmpty()) {
            Text(
                text = "No Showdown names added yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                viewModel.sdNames.forEach { name ->
                    FilterChip(
                        onClick = { viewModel.removeShowdownName(name) },
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
        }
    }
}

@Composable
private fun ButtonBar(
    navigator: Navigator,
    viewModel: EditTeamViewModel,
    isEditing: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { navigator.pop() },
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancel")
        }

        val snackBar = LocalSnackBar.current
        Button(
            onClick = { viewModel.saveTeam(navigator, snackBar) },
            modifier = Modifier.weight(1f),
            enabled = viewModel.isFormValid,
        ) {
            Text(
                if (isEditing) "Update Team" else "Create Team",
                // important
                color = LocalContentColor.current
            )
        }
    }
}

@Composable
private fun ShowdownNameDialog(viewModel: EditTeamViewModel) {
    val newName = viewModel.newSdNameInput
    val isSdNameValid = isSdNameValid(newName)
    AlertDialog(
        onDismissRequest = { viewModel.hideAddNameDialog() },
        title = { Text("Add Showdown Name") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = viewModel::updateNewNameInput,
                    label = { Text("Name") },
                    placeholder = { Text("Enter your Showdown name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !isSdNameValid && !newName.isEmpty(),
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
                onClick = { viewModel.addShowdownName() },
                enabled = isSdNameValid
            ) {
                Text(
                    "OK",
                    color = if (isSdNameValid) Color.Unspecified else Color.LightGray
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.hideAddNameDialog() }
            ) {
                Text("Cancel")
            }
        }
    )
}