package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.matchup

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.FabLayout
import com.tambapps.pokemon.alakastats.ui.screen.home.buttonTextStyle
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import org.jetbrains.compose.resources.painterResource


@Composable
fun MatchupNotesTab(viewModel: MatchupNotesViewModel) {
    FabLayout(
        fab = {

        }
    ) {
        if (!viewModel.hasMatchups) {
            NoNotes()
        } else if (LocalIsCompact.current)  {
            MatchupNotesTabMobile(viewModel)
        } else {
            MatchupNotesTabDesktop(viewModel)
        }
    }
}

@Composable
fun NoNotes() {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.Center).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Add your gameplay per matchup to remember how to handle the meta with your team",
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {  }) {
                Icon(
                    painter = painterResource(Res.drawable.add),
                    contentDescription = "Add",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(8.dp))
                Text("New Matchup", style = buttonTextStyle.copy(
                    color = LocalContentColor.current
                ))
            }
        }
    }
}