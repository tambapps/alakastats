package com.tambapps.pokemon.alakastats.ui.composables

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.arrow_back
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import org.jetbrains.compose.resources.painterResource

@Composable
fun BackIconButton(navigator: Navigator, modifier: Modifier = Modifier) = BackIconButton(modifier=modifier) { navigator.pop() }

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

@Composable
fun GameOutputCard(output: GameOutput) {
    val color = when (output) {
        GameOutput.WIN -> WIN_COLOR
        GameOutput.LOOSE -> LOOSE_COLOR
        GameOutput.UNKNOWN -> if (isDarkThemeEnabled()) Color.LightGray else Color.DarkGray
    }
    val isCompact = LocalIsCompact.current

    val (size, textSize) = if (isCompact) 42.dp to 22.sp else 52.dp to 30.sp
    Card(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
    ) {
        Text(
            modifier = Modifier.padding(all = 8.dp).fillMaxSize(),
            text = output.name[0].toString(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDarkThemeEnabled()) Color(0xFF222222) else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = textSize
        )
    }
}