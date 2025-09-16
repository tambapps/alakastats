package com.tambapps.pokemon.alakastats.ui.composables

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.arrow_back
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import com.tambapps.pokemon.alakastats.domain.model.GameOutput
import com.tambapps.pokemon.alakastats.ui.theme.defaultIconColor
import org.jetbrains.compose.resources.painterResource


@Composable
fun BackIconButton(navigator: Navigator) {
    IconButton(onClick = { navigator.pop() }) {
        Icon(
            painter = painterResource(Res.drawable.arrow_back),
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.defaultIconColor
        )
    }

}


private val WIN_COLOR = Color(0xFF4CAF50)
private val LOOSE_COLOR = Color(0xFFF44336)
private val UNKNOWN_COLOR = Color.DarkGray

@Composable
fun GameOutputCard(output: GameOutput) {
    val color = when (output) {
        GameOutput.WIN -> WIN_COLOR
        GameOutput.LOOSE -> LOOSE_COLOR
        GameOutput.UNKNOWN -> UNKNOWN_COLOR
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
    ) {
        Text(
            modifier = Modifier.padding(all = 8.dp),
            text = output.name[0].toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
    }
}