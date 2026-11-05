package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.Nature
import com.tambapps.pokemon.PokeStats
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.Stat
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.domain.model.usesLegacySystem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// the order of the games, clockwise from the top
private val RADAR_STATS = listOf(Stat.HP, Stat.ATTACK, Stat.DEFENSE, Stat.SPEED, Stat.SPECIAL_DEFENSE, Stat.SPECIAL_ATTACK)
private const val TOP_INDEX = 0
private const val BOTTOM_INDEX = 3
private val RIGHT_INDEXES = listOf(1, 2)

// a fixed scale keeps the shapes comparable from one pokemon to another
private const val MAX_STAT_AT_LEVEL_50 = 220f
private const val MAX_IV = 31

@Composable
fun PokemonStatsRadar(
    pokemon: Pokemon,
    baseStats: PokeStats,
    format: Format,
    modifier: Modifier = Modifier,
) {
    val usesLegacySystem = remember(format, pokemon.evs) { format.usesLegacySystem(pokemon.evs) }
    val finalStats = remember(pokemon, baseStats, usesLegacySystem) {
        PokeStats.compute(
            baseStats = baseStats,
            evs = pokemon.evs,
            ivs = pokemon.ivs,
            nature = pokemon.nature ?: Nature.QUIRKY,
            level = pokemon.level,
            legacySystem = usesLegacySystem,
        )
    }
    val noInvestmentStats = remember(pokemon, baseStats, usesLegacySystem) {
        PokeStats.compute(
            baseStats = baseStats,
            evs = PokeStats.default(0),
            ivs = pokemon.ivs,
            nature = Nature.QUIRKY,
            level = pokemon.level,
            legacySystem = usesLegacySystem,
        )
    }
    val investmentUnit = if (usesLegacySystem) "EV" else "SP"
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        RadarCanvas(
            pokemon = pokemon,
            finalStats = finalStats,
            noInvestmentStats = noInvestmentStats,
            usesLegacySystem = usesLegacySystem,
            investmentUnit = investmentUnit,
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().height(320.dp),
        )
        Spacer(Modifier.height(8.dp))
        RadarLegend()
    }
}

@Composable
private fun RadarCanvas(
    pokemon: Pokemon,
    finalStats: PokeStats,
    noInvestmentStats: PokeStats,
    usesLegacySystem: Boolean,
    investmentUnit: String,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val gridColor = colors.outlineVariant
    val statsColor = colors.primary
    val noInvestmentColor = colors.onSurfaceVariant

    val maxStat = MAX_STAT_AT_LEVEL_50 * pokemon.level.coerceAtLeast(1) / 50f
    // animated so that the shape morphs when switching to the mega form
    val finalFractions = RADAR_STATS.map { stat ->
        animateFloatAsState((finalStats[stat] / maxStat).coerceIn(0f, 1f), label = "finalStat")
    }
    val noInvestmentFractions = RADAR_STATS.map { stat ->
        animateFloatAsState((noInvestmentStats[stat] / maxStat).coerceIn(0f, 1f), label = "noInvestmentStat")
    }

    val titleStyle = MaterialTheme.typography.titleSmall
    val detailStyle = MaterialTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
    val defaultStatColor = colors.onSurface
    val statColors = RADAR_STATS.map { stat -> natureStatColor(pokemon.nature, stat).takeOrElse(defaultStatColor) }
    val textMeasurer = rememberTextMeasurer()
    val labels: List<List<TextLayoutResult>> = remember(pokemon, finalStats, usesLegacySystem, titleStyle, detailStyle, statColors) {
        RADAR_STATS.mapIndexed { index, stat ->
            buildList {
                add(textMeasurer.measure("${stat.shortName} ${finalStats[stat]}", titleStyle.copy(color = statColors[index])))
                val investment = pokemon.evs[stat]
                val prefix = if (investment > 0) "+" else ""
                add(textMeasurer.measure("$prefix$investment $investmentUnit", detailStyle))
                if (usesLegacySystem && pokemon.ivs[stat] != MAX_IV) {
                    add(textMeasurer.measure("${pokemon.ivs[stat]} IV", detailStyle))
                }
            }
        }
    }

    val description = RADAR_STATS.joinToString(prefix = "Stats radar: ") { "${it.shortName} ${finalStats[it]}" }
    Canvas(modifier.semantics { contentDescription = description }) {
        val gap = 8.dp.toPx()
        val topHeight = labels[TOP_INDEX].sumOf { it.size.height }.toFloat()
        val bottomHeight = labels[BOTTOM_INDEX].sumOf { it.size.height }.toFloat()
        val sideWidth = labels.filterIndexed { index, _ -> index != TOP_INDEX && index != BOTTOM_INDEX }
            .maxOf { lines -> lines.maxOf { it.size.width } }
        val availableHeight = size.height - topHeight - bottomHeight - 2 * gap
        val radius = minOf(size.width / 2 - sideWidth - gap, availableHeight / 2).coerceAtLeast(0f)
        val center = Offset(size.width / 2, topHeight + gap + availableHeight / 2)

        val gridStroke = Stroke(width = 1.dp.toPx())
        for (level in 1..3) {
            drawPath(polygon(center) { radius * level / 3f }, gridColor, style = gridStroke)
        }
        RADAR_STATS.indices.forEach { index ->
            drawLine(gridColor, center, vertex(center, radius, index), strokeWidth = gridStroke.width)
        }

        val finalPath = polygon(center) { index -> radius * finalFractions[index].value }
        drawPath(finalPath, statsColor.copy(alpha = 0.35f))
        drawPath(finalPath, statsColor, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
        drawPath(
            polygon(center) { index -> radius * noInvestmentFractions[index].value },
            noInvestmentColor,
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
            ),
        )
        RADAR_STATS.indices.forEach { index ->
            drawCircle(statsColor, 3.5.dp.toPx(), vertex(center, radius * finalFractions[index].value, index))
        }

        labels.forEachIndexed { index, lines ->
            drawLabel(lines, index, vertex(center, radius, index), gap)
        }
    }
}

private fun DrawScope.drawLabel(lines: List<TextLayoutResult>, index: Int, vertex: Offset, gap: Float) {
    val blockHeight = lines.sumOf { it.size.height }.toFloat()
    var y = when (index) {
        TOP_INDEX -> vertex.y - gap - blockHeight
        BOTTOM_INDEX -> vertex.y + gap
        else -> vertex.y - blockHeight / 2
    }
    for (line in lines) {
        val x = when (index) {
            TOP_INDEX, BOTTOM_INDEX -> vertex.x - line.size.width / 2f
            in RIGHT_INDEXES -> vertex.x + gap
            else -> vertex.x - gap - line.size.width
        }
        drawText(line, topLeft = Offset(x, y))
        y += line.size.height
    }
}

private fun polygon(center: Offset, radiusOf: (Int) -> Float) = Path().apply {
    RADAR_STATS.indices.forEach { index ->
        val point = vertex(center, radiusOf(index), index)
        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
    }
    close()
}

private fun vertex(center: Offset, radius: Float, index: Int): Offset {
    val angle = -PI / 2 + index * 2 * PI / RADAR_STATS.size
    return Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
}

@Composable
private fun RadarLegend() {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodySmall
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp, 10.dp).background(colors.primary, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(6.dp))
            Text("Final stats", style = textStyle, color = colors.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(16.dp, 10.dp)) {
                drawLine(
                    colors.onSurfaceVariant,
                    Offset(0f, size.height / 2),
                    Offset(size.width, size.height / 2),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("No investment", style = textStyle, color = colors.onSurfaceVariant)
        }
    }
}

private fun Color.takeOrElse(other: Color) = if (this == Color.Unspecified) other else this
