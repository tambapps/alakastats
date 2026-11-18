package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import com.tambapps.pokemon.champions.engine.Weather
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The color of a weather: its particles, its glow and the border of the cards. Null without weather
 */
@Composable
private fun weatherColor(weather: Weather): Color? {
    val dark = isDarkThemeEnabled()
    return when (weather) {
        Weather.SUN -> if (dark) Color(0xFFFFB74D) else Color(0xFFFB8C00)
        Weather.RAIN -> if (dark) Color(0xFF90CAF9) else Color(0xFF1E88E5)
        Weather.SNOW -> if (dark) Color(0xFFFFFFFF) else Color(0xFF78909C)
        Weather.SAND -> if (dark) Color(0xFFD7A86E) else Color(0xFFB07A3B)
        Weather.NONE, Weather.HAIL -> null
    }
}

/**
 * The border of a card of the calc under [weather], fading from the top. Null without weather
 */
@Composable
internal fun weatherBorder(weather: Weather): BorderStroke? {
    val color = weatherColor(weather) ?: return null
    return BorderStroke(
        1.5.dp,
        Brush.verticalGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.15f))),
    )
}

/**
 * The [weather] drawn behind the content: a glow from the top and animated particles (sun rays and embers, rain drops,
 * snowflakes, sand grains). Nothing without weather.
 *
 * What doesn't move (gradients, the rays' shape, the number of particles) is only built again when the size changes,
 * each frame only placing the particles
 */
@Composable
internal fun Modifier.weatherBackground(weather: Weather): Modifier {
    val color = weatherColor(weather) ?: return this
    val particles = remember { List(MAX_PARTICLES) { Particle(Random(it * 7919 + 17)) } }
    // the rain's drops, much faster than the other particles, look choppy at a lower rate
    val time = rememberElapsedSeconds(framesPerSecond = if (weather == Weather.RAIN) FAST_FRAMES_PER_SECOND else FRAMES_PER_SECOND)
    return drawWithCache {
        when (weather) {
            Weather.SUN -> sun(color, particles, time)
            Weather.RAIN -> rain(color, particles, time)
            Weather.SNOW -> snow(color, particles, time)
            Weather.SAND -> sand(color, particles, time)
            Weather.NONE, Weather.HAIL -> onDrawBehind {}
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

/**
 * The seconds since the effect started, updated about [framesPerSecond] times per second: less work than updating it on
 * every frame, especially on 120 Hz screens. Wrapping every hour to keep the float's precision
 */
@Composable
private fun rememberElapsedSeconds(framesPerSecond: Int): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    // a bit less than a frame's duration, the wait for the next frame making up the rest
    val delayMillis = (1000L / framesPerSecond - 8).coerceAtLeast(0)
    LaunchedEffect(delayMillis) {
        val start = withFrameNanos { it }
        while (true) {
            // no frame is asked for while waiting, the screen staying still in between
            delay(delayMillis)
            withFrameNanos { time.floatValue = ((it - start) / 1_000_000L % 3_600_000L) / 1000f }
        }
    }
    return time
}

// enough for the slow particles (embers, flakes, grains)
private const val FRAMES_PER_SECOND = 30
private const val FAST_FRAMES_PER_SECOND = 60

/**
 * How many particles for the size, [perArea] being the side (in dp) of the square one particle has, so that big and
 * small cards look as busy
 */
private fun CacheDrawScope.particleCount(perArea: Float, max: Int): Int {
    val side = perArea.dp.toPx()
    return (size.width * size.height / (side * side)).toInt().coerceIn(MIN_PARTICLES, max)
}

private const val MIN_PARTICLES = 12

// a position moving at [speed] px per second from [start], wrapping over [length]
private fun wrap(start: Float, speed: Float, seconds: Float, length: Float): Float {
    val value = (start + speed * seconds) % length
    return if (value < 0) value + length else value
}

private fun CacheDrawScope.topGlow(color: Color, alpha: Float, heightFraction: Float) = Brush.verticalGradient(
    listOf(color.copy(alpha = alpha), Color.Transparent),
    startY = 0f,
    endY = size.height * heightFraction,
)

// the particles drawn in the bounds only, the drops and flakes starting above the top
private inline fun DrawScope.clipped(block: DrawScope.() -> Unit) = clipRect(block = block)

/**
 * A warm glow from the top, slowly turning rays and embers rising
 */
private fun CacheDrawScope.sun(color: Color, particles: List<Particle>, time: State<Float>): DrawResult {
    val dp = 1.dp.toPx()
    val center = Offset(size.width / 2f, -size.width * 0.1f)
    val radius = maxOf(size.width, size.height) * 0.9f
    val glow = Brush.radialGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent), center, size.width * 0.9f)
    // the rays all around the sun above the card (only the ones going down being seen), around the origin to be turned
    val rayBrush = Brush.radialGradient(listOf(color.copy(alpha = 0.10f), Color.Transparent), Offset.Zero, radius)
    val halfWidth = (SUN_RAY_WIDTH_DEGREES / 2 * PI / 180).toFloat()
    val rays = Path().apply {
        repeat(SUN_RAYS) { index ->
            val angle = (2 * PI / SUN_RAYS * index).toFloat()
            moveTo(0f, 0f)
            lineTo(cos(angle - halfWidth) * radius, sin(angle - halfWidth) * radius)
            lineTo(cos(angle + halfWidth) * radius, sin(angle + halfWidth) * radius)
            close()
        }
    }
    val count = particleCount(perArea = 110f, max = 50)
    val sway = 10 * dp
    val height = size.height + 20 * dp
    return onDrawBehind {
        // read here, so that only the drawing is redone on each update
        val seconds = time.value
        clipped {
            drawRect(glow)
            translate(center.x, center.y) {
                rotate((seconds * SUN_RAY_DEGREES_PER_SECOND) % 360f, pivot = Offset.Zero) {
                    drawPath(rays, rayBrush)
                }
            }
            // embers rising and swaying, fading in at the bottom and out at the top
            for (i in 0 until count) {
                val p = particles[i]
                val y = height - wrap(p.y * height, (18 + p.speed * 30) * dp, seconds, height)
                val x = p.x * size.width + sin((seconds / (3 + p.phase * 3) + p.phase) * 2 * PI).toFloat() * sway
                val progress = y / size.height
                val fade = (minOf(progress, 1f - progress) * 4f).coerceIn(0f, 1f)
                drawCircle(color.copy(alpha = 0.55f * fade), radius = (1.2f + p.size * 2f) * dp, center = Offset(x, y))
            }
        }
    }
}

private const val SUN_RAYS = 14
private const val SUN_RAY_WIDTH_DEGREES = 7.0
private const val SUN_RAY_DEGREES_PER_SECOND = 2f

/**
 * Drops falling fast, slightly slanted
 */
private fun CacheDrawScope.rain(color: Color, particles: List<Particle>, time: State<Float>): DrawResult {
    val dp = 1.dp.toPx()
    val glow = topGlow(color, alpha = 0.12f, heightFraction = 0.5f)
    val count = particleCount(perArea = 70f, max = MAX_PARTICLES)
    val stroke = 1.2f * dp
    return onDrawBehind {
        val seconds = time.value
        clipped {
            drawRect(glow)
            for (i in 0 until count) {
                val p = particles[i]
                val length = (14 + p.size * 12) * dp
                val height = size.height + length
                val y = wrap(p.y * height, (520 + p.speed * 260) * dp, seconds, height) - length
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
    }
}

private const val RAIN_SLANT = 0.25f

/**
 * Flakes of various sizes falling slowly and drifting from side to side
 */
private fun CacheDrawScope.snow(color: Color, particles: List<Particle>, time: State<Float>): DrawResult {
    val dp = 1.dp.toPx()
    val glow = topGlow(color, alpha = 0.08f, heightFraction = 0.5f)
    val count = particleCount(perArea = 85f, max = 80)
    val sway = 14 * dp
    return onDrawBehind {
        val seconds = time.value
        clipped {
            drawRect(glow)
            for (i in 0 until count) {
                val p = particles[i]
                val radius = (1.5f + p.size * 2.5f) * dp
                val height = size.height + radius * 2
                // the big flakes, closer, fall faster
                val speed = (22 + p.size * 30 + p.speed * 10) * dp
                val y = wrap(p.y * height, speed, seconds, height) - radius
                val x = p.x * size.width + sin((seconds / (4 + p.phase * 3) + p.phase) * 2 * PI).toFloat() * sway
                drawCircle(color.copy(alpha = 0.35f + p.size * 0.35f), radius = radius, center = Offset(x, y))
            }
        }
    }
}

/**
 * Grains blown fast from left to right, wavering a little, in an ochre haze
 */
private fun CacheDrawScope.sand(color: Color, particles: List<Particle>, time: State<Float>): DrawResult {
    val dp = 1.dp.toPx()
    val haze = color.copy(alpha = 0.06f)
    val glow = topGlow(color, alpha = 0.10f, heightFraction = 0.6f)
    val count = particleCount(perArea = 80f, max = 90)
    val waver = 4 * dp
    val stroke = 1.5f * dp
    return onDrawBehind {
        val seconds = time.value
        clipped {
            drawRect(haze)
            drawRect(glow)
            for (i in 0 until count) {
                val p = particles[i]
                val length = (3 + p.size * 7) * dp
                val width = size.width + length
                val x = wrap(p.x * width, (160 + p.speed * 180) * dp, seconds, width) - length
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
    }
}
