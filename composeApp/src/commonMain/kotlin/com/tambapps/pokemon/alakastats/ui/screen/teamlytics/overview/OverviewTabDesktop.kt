package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.overview

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.more_vert
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.composables.Pokepaste
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource


@Composable
internal fun OverviewTabDesktop(viewModel: OverviewViewModel) {
    val team = viewModel.team
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamName(team, modifier = Modifier.weight(1f))
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewModel.isEditingNotes) {

                 // TODO add save and cancel button
                }
                Spacer(Modifier.width(8.dp))
                MoreActionsButton(viewModel)
            }
        }
        Header(team)

        PokePasteTitle()
        Pokepaste(team.pokePaste, viewModel.pokemonImageService)
    }
}
