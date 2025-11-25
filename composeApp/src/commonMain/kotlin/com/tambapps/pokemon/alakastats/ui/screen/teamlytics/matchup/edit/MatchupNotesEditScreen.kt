package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import arrow.core.Either
import cafe.adriel.voyager.core.screen.Screen
import com.tambapps.pokemon.alakastats.domain.error.DomainError
import com.tambapps.pokemon.alakastats.domain.model.MatchupNotes
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

class MatchupNotesEditScreen(
    private val onSuccess: suspend (MatchupNotes) -> Either<DomainError, Unit>
): Screen {
    @Composable
    override fun Content() {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .safeContentPadding()
        ) {
            if (LocalIsCompact.current) {
                MatchupNotesEditTabMobile(onSuccess)
            } else {
                MatchupNotesEditTabDesktop(onSuccess)
            }
        }
    }

}
