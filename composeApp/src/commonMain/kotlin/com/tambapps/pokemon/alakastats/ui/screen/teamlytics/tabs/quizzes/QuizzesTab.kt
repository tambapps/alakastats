package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.quizzes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.elevatedCardGradientColors
import com.tambapps.pokemon.alakastats.ui.screen.quizzes.SpeedInteractionsTeamSetupScreen

@Composable
fun QuizzesTab(viewModel: QuizzesTabViewModel) {
    Column(Modifier.fillMaxWidth().safeContentPadding().padding(16.dp)) {
        val navigator = LocalNavigator.currentOrThrow
        MyCard(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            onClick = { navigator.push(SpeedInteractionsTeamSetupScreen(viewModel.team.id)) },
            gradientBackgroundColors = elevatedCardGradientColors
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Speed Interactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Guess whether your Pokemon outspeeds the meta — using their real EVs, nature and item.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
