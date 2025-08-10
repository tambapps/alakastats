package com.tambapps.pokemon.alakastats.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.database.SdReplayRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.ExperimentalTime

// TODO delete this
@OptIn(ExperimentalTime::class)
@Composable
fun DatabaseTestScreen() {
    val repository: SdReplayRepository = koinInject()
    val scope = rememberCoroutineScope()
    
    var replays by remember { mutableStateOf(listOf<com.tambapps.pokemon.alakastats.database.SdReplay>()) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        loadReplays(repository) { newReplays ->
            replays = newReplays
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SQLDelight Database Test",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (message.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {

                            val currentTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
                            repository.insertReplay(
                                url = "https://replay.pokemonshowdown.us/gen9ou-${currentTime}",
                                uploadTime = currentTime,
                                format = "gen9ou",
                                rating = 1500L,
                                winner = "Player1"
                            )
                            message = "Replay inserted successfully!"
                            loadReplays(repository) { newReplays ->
                                replays = newReplays
                            }
                        } catch (e: Exception) {
                            message = "Error inserting replay: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Add Test Replay")
                }
            }
            
            Button(
                onClick = {
                    scope.launch {
                        loadReplays(repository) { newReplays ->
                            replays = newReplays
                        }
                    }
                }
            ) {
                Text("Refresh")
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Stored Replays (${replays.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (replays.isEmpty()) {
                    Text(
                        text = "No replays found. Click 'Add Test Replay' to create one.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(replays) { replay ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = replay.url,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Format: ${replay.format} | Rating: ${replay.rating ?: "N/A"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (replay.winner != null) {
                                        Text(
                                            text = "Winner: ${replay.winner}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun loadReplays(
    repository: SdReplayRepository,
    onLoaded: (List<com.tambapps.pokemon.alakastats.database.SdReplay>) -> Unit
) {
    try {
        val replays = repository.getAllReplays()
        onLoaded(replays)
    } catch (e: Exception) {
        println("Error loading replays: ${e.message}")
        onLoaded(emptyList())
    }
}