@file:OptIn(ExperimentalMaterial3Api::class)

package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.edit

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import arrow.core.Either
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.composables.PokePasteInput
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource

class MatchupNotesEditScreen(
    private val onSuccess: suspend (MatchupNotes) -> Either<DomainError, Unit>,
    private val matchupNotes: MatchupNotes? = null
) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<MatchupNotesEditViewModel>()
        LaunchedEffect(Unit) {
            if (matchupNotes != null) {
                viewModel.prepareEdition(matchupNotes)
            }
        }

        val isCompact = LocalIsCompact.current
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (matchupNotes != null) "Edit Matchup" else "Create Matchup") },
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

                SectionTitle("Matchup Name")

                NameInput(viewModel)

                PokePasteInput(viewModel)

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Game Plans")
                    Spacer(Modifier.width(16.dp))
                    AddButton(onClick = { viewModel.createGamePlan() }, contentDescription = "Add Game Plan")
                }

                viewModel.gamePlanStates.forEachIndexed { index, gamePlanState ->

                    Text("Game Plan ${index + 1}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = gamePlanState.description,
                        onValueChange = gamePlanState::updateDescription,
                        placeholder = { Text("Type your game plan") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        singleLine = false,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Composition",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(16.dp))
                        AddButton(onClick = {  }, contentDescription = "Add Pokemon to Composition")
                    }
                    if (gamePlanState.composition.isEmpty()) {
                        Text("Which pokemon to bring into battle")
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.headlineSmall,
    fontWeight = FontWeight.Bold
)


@Composable
private fun NameInput(viewModel: MatchupNotesEditViewModel) {
    OutlinedTextField(
        value = viewModel.name,
        onValueChange = viewModel::updateName,
        placeholder = { Text("Enter a name for your matchup") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}


@Composable
private fun AddButton(onClick: () -> Unit, contentDescription: String?) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.add),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.defaultIconColor
        )
    }
}
