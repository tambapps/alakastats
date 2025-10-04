package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact


@Composable
fun <T> StatCard(
    title: String,
    data: Collection<T>,
    modifier: Modifier = Modifier,
    rowContent: @Composable (T) -> Unit
) {
    if (LocalIsCompact.current) {
        MobileStatCard(title, data, modifier, rowContent)
    } else {
        DesktopStatCard(title, data, modifier, rowContent)
    }
}

@Composable
internal fun <T> MobileStatCard(
    title: String,
    data: Collection<T>,
    modifier: Modifier = Modifier,
    rowContent: @Composable (T) -> Unit
) {
    ExpansionTile(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        content = {
            Row {
                data.forEach {
                    rowContent.invoke(it)
                }
            }
        }
    )
}

@Composable
internal fun <T> DesktopStatCard(
    title: String,
    data: Collection<T>,
    modifier: Modifier = Modifier,
    rowContent: @Composable (T) -> Unit
) {
    Card(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        Row {
            data.forEach {
                rowContent.invoke(it)
            }
        }
    }
}