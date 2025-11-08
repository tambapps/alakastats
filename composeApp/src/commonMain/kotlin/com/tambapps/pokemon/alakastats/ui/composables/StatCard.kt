package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact


@Composable
fun <T> StatCard(
    title: String,
    data: Collection<T>,
    modifier: Modifier = Modifier,
    rowContent: @Composable RowScope.(T) -> Unit
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
    rowContent: @Composable RowScope.(T) -> Unit
) {
    ExpansionTile(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        content = {
            Column {
                data.forEach {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowContent.invoke(this, it)
                    }
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
    rowContent: @Composable RowScope.(T) -> Unit
) {
    MyCard(
        modifier = modifier.verticalScroll(rememberScrollState()),
        gradientBackgroundColors = cardGradientColors,
        border = BorderStroke(
            width = 2.dp,
            color = MaterialTheme.colorScheme.outline
        ),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        data.forEach {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowContent.invoke(this, it)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}