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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun HomeScreenDesktop(viewModel: HomeViewModel) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(all = 16.dp)
            .fillMaxSize(),
    ) {
        Row(Modifier.fillMaxWidth()) {
            AlakastatsLabel()
            Spacer(Modifier.weight(1f))
            AboutButton()
        }
        CatchPhrase()
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ButtonBarContent(viewModel)
        }
        TeamCardGrid(viewModel, 3)
    }
}

@Composable
fun AlakastatsLabel() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(if (isDarkThemeEnabled()) Res.drawable.alakastats_dark else Res.drawable.alakastats),
            contentDescription = "Alakastats logo",
            modifier = Modifier.size(80.dp),
            contentScale = ContentScale.Fit
        )
        var fontSize by remember { mutableStateOf(TextUnit.Unspecified) }
        Text(
            "Alakastats",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            fontSize = fontSize,
            overflow = TextOverflow.Clip,
            onTextLayout = { result ->
                // useful for the use in AboutScreen for mobile
                if (result.hasVisualOverflow) {
                    val currentSize = if (fontSize == TextUnit.Unspecified)
                        result.layoutInput.style.fontSize
                    else fontSize
                    fontSize = (currentSize.value * 0.9f).sp
                }
            }
        )

    }
}


@Composable
private fun ButtonBarContent(viewModel: HomeViewModel) {
    NewTeamButton()
    ImportTeamButton(viewModel)
    SampleTeamButton(viewModel)
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
