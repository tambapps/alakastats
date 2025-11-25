@file:OptIn(ExperimentalMaterial3Api::class)

package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import arrow.core.Either
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.ui.composables.BackIconButton
import com.tambapps.pokemon.alakastats.ui.screen.editteam.EditTeamViewModel
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

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

            }
        }
    }

}
