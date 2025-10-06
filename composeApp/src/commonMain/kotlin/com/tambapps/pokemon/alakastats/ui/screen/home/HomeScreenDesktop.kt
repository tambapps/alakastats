package com.tambapps.pokemon.alakastats.ui.screen.home

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.alakastats
import alakastats.composeapp.generated.resources.alakastats_dark
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun HomeScreenDesktop(isDarkTheme: Boolean, viewModel: HomeViewModel) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(all = 16.dp)
            .fillMaxSize(),
    ) {
        AlakastatsLabel(isDarkTheme)
        CatchPhrase()
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ButtonBarContent()
        }
        TeamCardGrid(viewModel, 3)
    }
}

@Composable
private fun AlakastatsLabel(isDarkTheme: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(if (isDarkTheme) Res.drawable.alakastats_dark else Res.drawable.alakastats),
            contentDescription = "Alakastats logo",
            modifier = Modifier.size(80.dp),
            contentScale = ContentScale.Fit
        )
        Text("Alakastats", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

    }
}


@Composable
private fun ButtonBarContent() {
    NewTeamButton()
    ImportTeamButton()
    SampleTeamButton()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TeamCardGrid(
    viewModel: HomeViewModel,
    columns: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            viewModel.teamlyticsList.forEach { team ->
                item(key = team.id) {
                    TeamCard(viewModel = viewModel, team = team)
                }
            }
        }
    }
}
