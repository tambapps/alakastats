package com.tambapps.pokemon.alakastats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import alakastats.composeapp.generated.resources.alakastats
import alakastats.composeapp.generated.resources.alakastats_dark
import alakastats.composeapp.generated.resources.compose_multiplatform
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.theme.AppTheme

@Composable
@Preview
fun App() {
    val isDarkTheme = isSystemInDarkTheme()

    AppTheme(darkTheme = isDarkTheme) {
        BoxWithConstraints {
            val isCompact = maxWidth < 600.dp
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .safeContentPadding()
                    .fillMaxSize(),
            ) {
                if (isCompact) {
                    MobileScreen(isDarkTheme)
                } else {
                    LargeScreen(isDarkTheme)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.MobileScreen(isDarkTheme: Boolean) {
    AlakastatsLabel(isDarkTheme, Modifier.align(Alignment.CenterHorizontally))
    var showContent by remember { mutableStateOf(false) }

    /*
    Button(onClick = { showContent = !showContent }) {
        Text("Click me!")
    }
    AnimatedVisibility(showContent) {
        val greeting = remember { Greeting().greet() }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(Res.drawable.compose_multiplatform), null)
            Text("Compose: $greeting")
        }
    }

     */
}

@Composable
private fun ColumnScope.LargeScreen(isDarkTheme: Boolean) {
    AlakastatsLabel(isDarkTheme)
    Text("Think like Alakazam. Play like a pro.", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ButtonBarContent()
    }
    TeamCardGrid(emptyList(), 3)
}

@Composable
private fun ButtonBarContent() {
    val textStyle = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Bold
    )
    Button(onClick = {  }) {
        Icon(
            painter = painterResource(Res.drawable.add),
            contentDescription = "Add",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.width(8.dp))
        Text("New Team", style = MaterialTheme.typography.labelLarge.copy(
            color = LocalContentColor.current // important for the color to be opposite
        ))
    }

    OutlinedButton(onClick = {  }) {
        Text("Import", style = textStyle)
    }

    OutlinedButton(onClick = {  }) {
        Text("Sample", style = textStyle)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeamCardGrid(
    teams: List<Teamlytics>,
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
            teams.forEach { team ->
                item(key = team) {
                    TeamCard(team = team)
                }
            }
            // TODO delete me
            repeat(25) {
                item {
                    TeamCard()
                }

            }
        }
    }
}

@Composable
private fun TeamCard(team: Teamlytics? = null) {
    Card(
        modifier = Modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Chuppa Cross Five", style = MaterialTheme.typography.titleMedium)
            // Add your Pokémon sprite row, stats, etc.
            HorizontalDivider(thickness = 2.dp)
            Text("25 replays")
            Text("60% winrate")
        }
    }
}
@Composable
private fun AlakastatsLabel(isDarkTheme: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
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