package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.alakastats.ui.theme.fabPadding

@Composable
fun FabLayout(
    fab: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content.invoke()
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(fabPadding)) {
            fab.invoke()
        }
    }
}
