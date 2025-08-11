package com.tambapps.pokemon.alakastats.ui.screen.home

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.add
import alakastats.composeapp.generated.resources.more_horiz
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.domain.model.TeamlyticsPreview
import com.tambapps.pokemon.alakastats.ui.screen.createteam.CreateTeamScreen
import com.tambapps.pokemon.alakastats.ui.theme.isCompact
import org.jetbrains.compose.resources.painterResource

object HomeScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<HomeViewModel>()
        LaunchedEffect(Unit) {
            viewModel.loadTeams()
        }
        val isDarkTheme = isSystemInDarkTheme()
        BoxWithConstraints {
            val isCompact = isCompact()
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .safeContentPadding()
                    .fillMaxSize(),
            ) {
                if (isCompact) {
                    HomeScreenMobile(isDarkTheme, viewModel)
                } else {
                    HomeScreenDesktop(isDarkTheme, viewModel)
                }
            }
        }
    }
}

@Composable
internal fun CatchPhrase(skipLine: Boolean = false, textAlign: TextAlign = TextAlign.Unspecified) {
    val text = buildString {
        append("Think like Alakazam.")
        append(if (skipLine) '\n' else ' ')
        append("Play like a pro.")
    }
    Text(text, style = MaterialTheme.typography.headlineMedium, textAlign = textAlign)
}

@Composable
fun TeamCard(viewModel: HomeViewModel, team: TeamlyticsPreview, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
              //  modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(team.name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                IconButton(
                    modifier = Modifier.offset(y = (-12).dp),
                    onClick = { /* TODO */ }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.more_horiz),
                        contentDescription = "More"
                    )
                }
            }

            Row {
                for (pokemon in team.pokemons) {
                    viewModel.imageService.PokemonSprite(pokemon, modifier = Modifier.weight(1f))
                }
            }
            HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
            Row {
                Text("${team.nbReplays} replays")
                Spacer(Modifier.weight(1f))
                Text("${team.winrate}% winrate")
            }
        }
    }
}

@Composable
internal fun NewTeamButton(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    Button(
        modifier = modifier,
        onClick = { navigator.push(CreateTeamScreen) }
    ) {
        Icon(
            painter = painterResource(Res.drawable.add),
            contentDescription = "Add",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.width(8.dp))
        Text("New Team", style = MaterialTheme.typography.labelLarge.copy(
            color = LocalContentColor.current
        ))
    }
}


internal val buttonTextStyle  @Composable get() = MaterialTheme.typography.labelLarge.copy(
    fontWeight = FontWeight.Bold
)

@Composable
internal fun ImportTeamButton(modifier: Modifier = Modifier) {
    OutlinedButton(onClick = {  }, modifier = modifier) {
        Text("Import", style = buttonTextStyle)
    }
}
@Composable
internal fun SampleTeamButton(modifier: Modifier = Modifier) {
    OutlinedButton(onClick = {  }, modifier = modifier) {
        Text("Sample", style = buttonTextStyle)
    }
}
