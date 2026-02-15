package com.tambapps.pokemon.alakastats.ui.screen.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.tambapps.pokemon.alakastats.ui.screen.home.AlakastatsLabel
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact


object AboutScreen : Screen {
    @Composable
    override fun Content() {
        val isCompact = LocalIsCompact.current
        Column(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (isCompact) Modifier.safeContentPadding().padding(horizontal = 8.dp)
                    else Modifier.padding(all = 16.dp)
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AlakastatsLabel()
            VerticalSpacer()
            Text("About me", style = MaterialTheme.typography.headlineMedium)
            Text("I am a french Pokemon VGC player and developer, known by the pseudos JarMan and Tambapps",
                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun VerticalSpacer(height: Dp = 8.dp) = Spacer(Modifier.height(height))