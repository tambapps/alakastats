package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import com.tambapps.pokemon.champions.engine.Weather
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The colors of a weather: its particles and glow, and the border of the cards
 */
private class WeatherColors(val main: Color)

@Composable
private fun weatherColors(weather: Weather): WeatherColors? {
    val dark = isDarkThemeEnabled()
    val color = when (weather) {
        Weather.SUN -> if (dark) Color(0xFFFFB74D) else Color(0xFFFB8C00)
        Weather.RAIN -> if (dark) Color(0xFF90CAF9) else Color(0xFF1E88E5)
        Weather.SNOW -> if (dark) Color(0xFFFFFFFF) else Color(0xFF78909C)
        Weather.SAND -> if (dark) Color(0xFFD7A86E) else Color(0xFFB07A3B)
        Weather.NONE, Weather.HAIL -> return null
    }
    return WeatherColors(color)
}

/**
 * The border of a card of the calc under [weather], fading from the top. Null without weather
 */
@Composable
internal fun weatherBorder(weather: Weather): BorderStroke? {
    val colors = weatherColors(weather) ?: return null
    return BorderStroke(
        1.5.dp,
        Brush.verticalGradient(listOf(colors.main.copy(alpha = 0.9f), colors.main.copy(alpha = 0.15f))),
    )
}

/**
 * The [weather] drawn behind the content: a glow from the top and animated particles (sun rays and embers, rain drops,
 * snowflakes, sand grains). Nothing without weather
 */
@Composable
internal fun Modifier.weatherBackground(weather: Weather): Modifier {
    val colors = weatherColors(weather) ?: return this
    val particles = remember { List(MAX_PARTICLES) { Particle(Random(it * 7919 + 17)) } }
    val time = rememberElapsedSeconds()
    return drawBehind {
        clipRect {
            // read here, so that only the drawing is redone on each frame
            val seconds = time.value
            when (weather) {
                Weather.SUN -> drawSun(colors.main, particles, seconds)
                Weather.RAIN -> drawRain(colors.main, particles, seconds)
                Weather.SNOW -> drawSnow(colors.main, particles, seconds)
                Weather.SAND -> drawSand(colors.main, particles, seconds)
                Weather.NONE, Weather.HAIL -> {}
            }
        }
    }
}

/**
 * A particle's random traits, each between 0 and 1, the weathers turning them into positions, speeds and sizes
 */
private class Particle(random: Random) {
    val x = random.nextFloat()
    val y = random.nextFloat()
    val size = random.nextFloat()
    val speed = random.nextFloat()
    val phase = random.nextFloat()
}

private const val MAX_PARTICLES = 120

// the seconds since the effect started, on each frame. Wrapping every hour to keep the float's precision
@Composable
private fun rememberElapsedSeconds(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { time.floatValue = ((it - start) / 1_000_000L % 3_600_000L) / 1000f }
        }
    }
    return time
}

/**
 * How many particles for the size, [perArea] being the area (in dp) of one particle, so that big and small cards look
 * as busy
 */
private fun DrawScope.particleCount(perArea: Float, max: Int): Int {
    val side = perArea.dp.toPx()
    return (size.width * size.height / (side * side)).toInt().coerceIn(MIN_PARTICLES, max)
}

private const val MIN_PARTICLES = 12

// a position moving at [speed] px per second from [start], wrapping over [length]
private fun wrap(start: Float, speed: Float, seconds: Float, length: Float): Float {
    val value = (start + speed * seconds) % length
    return if (value < 0) value + length else value
}

private fun DrawScope.drawTopGlow(color: Color, alpha: Float, heightFraction: Float) {
    drawRect(
        Brush.verticalGradient(
            listOf(color.copy(alpha = alpha), Color.Transparent),
            startY = 0f,
            endY = size.height * heightFraction,
        )
    )
}

/**
 * A warm glow from the top, slowly turning rays and embers rising
 */
private fun DrawScope.drawSun(color: Color, particles: List<Particle>, seconds: Float) {
    val center = Offset(size.width / 2f, -size.width * 0.1f)
    val radius = maxOf(size.width, size.height) * 0.9f
    drawRect(Brush.radialGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent), center, size.width * 0.9f))
    // the rays, all around the sun above the card (only the ones going down being seen), turning a few degrees per second
    val rayBrush = Brush.radialGradient(listOf(color.copy(alpha = 0.10f), Color.Transparent), center, radius)
    val rotation = (seconds * SUN_RAY_DEGREES_PER_SECOND * PI / 180).toFloat()
    val halfWidth = (SUN_RAY_WIDTH_DEGREES / 2 * PI / 180).toFloat()
    repeat(SUN_RAYS) { index ->
        val angle = (2 * PI / SUN_RAYS * index).toFloat() + rotation
        val path = Path().apply {
            moveTo(center.x, center.y)
            lineTo(center.x + cos(angle - halfWidth) * radius, center.y + sin(angle - halfWidth) * radius)
            lineTo(center.x + cos(angle + halfWidth) * radius, center.y + sin(angle + halfWidth) * radius)
            close()
        }
        drawPath(path, rayBrush)
    }
    // embers rising and swaying, fading in at the bottom and out at the top
    val count = particleCount(perArea = 110f, max = 50)
    val sway = 10.dp.toPx()
    particles.take(count).forEach { p ->
        val speed = (18 + p.speed * 30).dp.toPx()
        val height = size.height + 20f
        val y = height - wrap(p.y * height, speed, seconds, height)
        val x = p.x * size.width + sin((seconds / (3 + p.phase * 3) + p.phase) * 2 * PI).toFloat() * sway
        val progress = y / size.height
        val fade = (minOf(progress, 1f - progress) * 4f).coerceIn(0f, 1f)
        drawCircle(color.copy(alpha = 0.55f * fade), radius = (1.2f + p.size * 2f).dp.toPx(), center = Offset(x, y))
    }
}

private const val SUN_RAYS = 14
private const val SUN_RAY_WIDTH_DEGREES = 7.0
private const val SUN_RAY_DEGREES_PER_SECOND = 2.0

/**
 * Drops falling fast, slightly slanted
 */
private fun DrawScope.drawRain(color: Color, particles: List<Particle>, seconds: Float) {
    drawTopGlow(color, alpha = 0.12f, heightFraction = 0.5f)
    val count = particleCount(perArea = 70f, max = MAX_PARTICLES)
    val stroke = 1.2.dp.toPx()
    particles.take(count).forEach { p ->
        val length = (14 + p.size * 12).dp.toPx()
        val speed = (520 + p.speed * 260).dp.toPx()
        val height = size.height + length
        val y = wrap(p.y * height, speed, seconds, height) - length
        // falling a bit to the left, the drop drawn along its way
        val slant = length * RAIN_SLANT
        val x = wrap(p.x * (size.width + slant) - (y + length) * RAIN_SLANT, 0f, 0f, size.width + slant)
        drawLine(
            color.copy(alpha = 0.25f + p.size * 0.2f),
            start = Offset(x, y),
            end = Offset(x - slant, y + length),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private const val RAIN_SLANT = 0.25f

/**
 * Flakes of various sizes falling slowly and drifting from side to side
 */
private fun DrawScope.drawSnow(color: Color, particles: List<Particle>, seconds: Float) {
    drawTopGlow(color, alpha = 0.08f, heightFraction = 0.5f)
    val count = particleCount(perArea = 85f, max = 80)
    val sway = 14.dp.toPx()
    particles.take(count).forEach { p ->
        val radius = (1.5f + p.size * 2.5f).dp.toPx()
        // the big flakes, closer, fall faster
        val speed = (22 + p.size * 30 + p.speed * 10).dp.toPx()
        val height = size.height + radius * 2
        val y = wrap(p.y * height, speed, seconds, height) - radius
        val x = p.x * size.width + sin((seconds / (4 + p.phase * 3) + p.phase) * 2 * PI).toFloat() * sway
        drawCircle(color.copy(alpha = 0.35f + p.size * 0.35f), radius = radius, center = Offset(x, y))
    }
}

/**
 * Grains blown fast from left to right, wavering a little, in an ochre haze
 */
private fun DrawScope.drawSand(color: Color, particles: List<Particle>, seconds: Float) {
    drawRect(color.copy(alpha = 0.06f))
    drawTopGlow(color, alpha = 0.10f, heightFraction = 0.6f)
    val count = particleCount(perArea = 80f, max = 90)
    val waver = 4.dp.toPx()
    val stroke = 1.5.dp.toPx()
    particles.take(count).forEach { p ->
        val length = (3 + p.size * 7).dp.toPx()
        val speed = (160 + p.speed * 180).dp.toPx()
        val width = size.width + length
        val x = wrap(p.x * width, speed, seconds, width) - length
        val y = p.y * size.height + sin((seconds / (1 + p.phase) + p.phase) * 2 * PI).toFloat() * waver
        drawLine(
            color.copy(alpha = 0.3f + p.size * 0.3f),
            start = Offset(x, y),
            end = Offset(x + length, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
