package com.tambapps.pokemon.alakastats.ui.composables

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.arrow_back
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tambapps.pokemon.alakastats.ui.navigation.AppNavigator
import com.tambapps.pokemon.alakastats.domain.model.GameOutcome
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import org.jetbrains.compose.resources.painterResource

@Composable
fun BackIconButton(navigator: AppNavigator, modifier: Modifier = Modifier) = BackIconButton(modifier=modifier) { navigator.pop() }

@Composable
fun BackIconButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter = painterResource(Res.drawable.arrow_back),
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.defaultIconColor
        )
    }
}

val WIN_COLOR = Color(0xFF4CAF50)
val LOOSE_COLOR = Color(0xFFF44336)

/** the color telling the outcome, e.g. of a replay's card */
val GameOutcome.color: Color
    @Composable get() = when (this) {
        GameOutcome.WIN -> WIN_COLOR
        GameOutcome.LOOSE -> LOOSE_COLOR
        GameOutcome.UNKNOWN -> if (isDarkThemeEnabled()) Color.LightGray else Color.DarkGray
    }

val GameOutcome.displayName: String
    get() = when (this) {
        GameOutcome.WIN -> "Win"
        GameOutcome.LOOSE -> "Loss"
        GameOutcome.UNKNOWN -> "Unknown"
    }

/**
 * @param letter what the card tells, the outcome's initial by default
 * @param small to go along a line of text, e.g. before a player's name
 */
@Composable
fun GameOutcomeCard(output: GameOutcome, letter: Char = output.name[0], small: Boolean = false) {
    val color = output.color
    val isCompact = LocalIsCompact.current

    val (size, textSize) = when {
        small -> 24.dp to 14.sp
        isCompact -> 42.dp to 22.sp
        else -> 52.dp to 30.sp
    }
    Card(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(if (small) 6.dp else 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                modifier = if (small) Modifier else Modifier.padding(all = 8.dp).fillMaxSize(),
                text = letter.toString(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDarkThemeEnabled()) Color(0xFF222222) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = textSize
            )
        }
    }
}