package com.tambapps.pokemon.alakastats.ui.composables

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.arrow_back
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
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