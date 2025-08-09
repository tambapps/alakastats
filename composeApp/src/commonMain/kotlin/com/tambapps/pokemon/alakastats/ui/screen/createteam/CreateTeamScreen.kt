package com.tambapps.pokemon.alakastats.ui.screen.createteam

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import alakastats.composeapp.generated.resources.arrow_back
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.painterResource

object CreateTeamScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<CreateTeamViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Create Team") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                painter = painterResource(Res.drawable.arrow_back),
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .safeContentPadding()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Team Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = viewModel.teamName,
                    onValueChange = viewModel::updateTeamName,
                    label = { Text("Team Name") },
                    placeholder = { Text("Enter a name for your team") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                
                Text(
                    text = "Pokepaste",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedTextField(
                    value = viewModel.pokepaste,
                    onValueChange = viewModel::updatePokepaste,
                    placeholder = { Text("The pokepaste content (not the URL)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    minLines = 8,
                    maxLines = 12,
                    isError = viewModel.pokepasteError != null,
                    supportingText = viewModel.pokepasteError?.let { error ->
                        { Text(text = error) }
                    }
                )
                
                Text(
                    text = "Showdown Names",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Names (${viewModel.showdownNames.size})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            IconButton(
                                onClick = { viewModel.showAddNameDialog() }
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.add),
                                    contentDescription = "Add Name"
                                )
                            }
                        }
                        
                        if (viewModel.showdownNames.isEmpty()) {
                            Text(
                                text = "No Showdown names added yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.height(120.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(viewModel.showdownNames) { name ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        
                                        IconButton(
                                            onClick = { viewModel.removeShowdownName(name) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text(
                                                text = "×",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { viewModel.createTeam(navigator) },
                        modifier = Modifier.weight(1f),
                        enabled = viewModel.isFormValid,
                    ) {
                        Text(
                            "Create Team",
                            // important
                            color = LocalContentColor.current
                        )
                    }
                }
            }
        }
        
        if (viewModel.showAddNameDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideAddNameDialog() },
                title = { Text("Add Showdown Name") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = viewModel.newNameInput,
                            onValueChange = viewModel::updateNewNameInput,
                            label = { Text("Name") },
                            placeholder = { Text("Enter a Showdown name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = !viewModel.isNewNameValid && viewModel.newNameInput.isNotBlank(),
                            supportingText = {
                                when {
                                    viewModel.newNameInput.contains('/') -> Text("Name cannot contain slash characters")
                                    viewModel.newNameInput.length > 30 -> Text("Name must be 30 characters or less")
                                    viewModel.newNameInput.isBlank() -> Text("Name cannot be empty")
                                    else -> null
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.addShowdownName() },
                        enabled = viewModel.isNewNameValid
                    ) {
                        Text("OK")
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
    }
}