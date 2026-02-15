package com.tambapps.pokemon.alakastats.ui.screen.home

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.alakastats
import alakastats.composeapp.generated.resources.alakastats_dark
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
internal fun HomeScreenMobile(viewModel: HomeViewModel) {
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .safeContentPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(if (isDarkThemeEnabled()) Res.drawable.alakastats_dark else Res.drawable.alakastats),
                    contentDescription = "Alakastats logo",
                    modifier = Modifier.size(80.dp).align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
                AboutButton(Modifier.align(Alignment.TopEnd))
            }
            Text("Alakastats", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            CatchPhrase(skipLine = true, textAlign = TextAlign.Center)

            Spacer(Modifier.height(16.dp))

            NewTeamButton(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                ImportTeamButton(viewModel, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                SampleTeamButton(viewModel, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            for (team in viewModel.teamlyticsList) {
                TeamCard(viewModel, team, Modifier.padding(all = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
