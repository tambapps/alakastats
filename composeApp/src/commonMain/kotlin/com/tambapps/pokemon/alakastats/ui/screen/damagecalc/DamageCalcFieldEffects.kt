package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.terrain_electric
import alakastats.composeapp.generated.resources.terrain_grassy
import alakastats.composeapp.generated.resources.terrain_misty
import alakastats.composeapp.generated.resources.terrain_psychic
import alakastats.composeapp.generated.resources.weather_rain
import alakastats.composeapp.generated.resources.weather_sand
import alakastats.composeapp.generated.resources.weather_snow
import alakastats.composeapp.generated.resources.weather_sun
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.alakastats.ui.theme.isDarkThemeEnabled
import com.tambapps.pokemon.champions.engine.Battlefield
import com.tambapps.pokemon.champions.engine.Terrain
import com.tambapps.pokemon.champions.engine.Weather
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/*
 * The weather and the terrain of the field, drawn behind the calc. The weather crosses the cards (falling rain and
 * snow, blown sand, rising embers) with a glow from the top, the terrain lives in place (flashing sparks, floating
 * leaves, drifting mist, spreading ripples) with a glow from the bottom corners, so that both read apart when on
 * together
 */

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
 * The color of a terrain, like [weatherColor]. Null without terrain
 */
@Composable
private fun terrainColor(terrain: Terrain): Color? {
    val dark = isDarkThemeEnabled()
    return when (terrain) {
        Terrain.ELECTRIC -> if (dark) Color(0xFFFFEE58) else Color(0xFFF9A825)
        Terrain.GRASSY -> if (dark) Color(0xFF81C784) else Color(0xFF43A047)
        Terrain.MISTY -> if (dark) Color(0xFFF48FB1) else Color(0xFFEC407A)
        Terrain.PSYCHIC -> if (dark) Color(0xFFCE93D8) else Color(0xFF8E24AA)
        Terrain.NONE -> null
    }
}

/**
 * The weather's icon, in the color of its effect. Nothing without weather
 */
@Composable
internal fun WeatherIcon(weather: Weather, modifier: Modifier = Modifier) {
    val icon = when (weather) {
        Weather.SUN -> Res.drawable.weather_sun
        Weather.RAIN -> Res.drawable.weather_rain
        Weather.SNOW -> Res.drawable.weather_snow
        Weather.SAND -> Res.drawable.weather_sand
        Weather.NONE, Weather.HAIL -> return
    }
    FieldEffectIcon(icon, weatherColor(weather), modifier)
}

/**
 * The terrain's icon, in the color of its effect. Nothing without terrain
 */
@Composable
internal fun TerrainIcon(terrain: Terrain, modifier: Modifier = Modifier) {
    val icon = when (terrain) {
        Terrain.ELECTRIC -> Res.drawable.terrain_electric
        Terrain.GRASSY -> Res.drawable.terrain_grassy
        Terrain.MISTY -> Res.drawable.terrain_misty
        Terrain.PSYCHIC -> Res.drawable.terrain_psychic
        Terrain.NONE -> return
    }
    FieldEffectIcon(icon, terrainColor(terrain), modifier)
}

@Composable
private fun FieldEffectIcon(icon: DrawableResource, color: Color?, modifier: Modifier) {
    // the text next to it names it
    Icon(painterResource(icon), contentDescription = null, tint = color ?: LocalContentColor.current, modifier = modifier)
}

/**
 * The border of a card of the calc on [field]: the weather's color from the top, the terrain's from the bottom. Null
 * without weather nor terrain
 */
@Composable
internal fun fieldEffectsBorder(field: Battlefield): BorderStroke? {
    val weather = weatherColor(field.weather)
    val terrain = terrainColor(field.terrain)
    val colors = when {
        weather != null && terrain != null -> listOf(
            weather.copy(alpha = 0.9f), weather.copy(alpha = 0.2f), terrain.copy(alpha = 0.2f), terrain.copy(alpha = 0.9f)
        )
        weather != null -> listOf(weather.copy(alpha = 0.9f), weather.copy(alpha = 0.15f))
        terrain != null -> listOf(terrain.copy(alpha = 0.15f), terrain.copy(alpha = 0.9f))
        else -> return null
    }
    return BorderStroke(1.5.dp, Brush.verticalGradient(colors))
}

/**
 * The field effects shared by the elements drawing them: one clock and the same particles, so that an element drawing
 * part of the scene ([fieldEffectsWindow]) continues the one behind it ([fieldEffectsScene]) without a seam
 */
@Stable
internal class FieldEffectsScene(val time: State<Float>) {
    // different particles for each layer, so that they don't line up
    internal val weatherParticles = particles(seed = 17)
    internal val terrainParticles = particles(seed = 4241)
    // of the element drawing the whole scene, for the windows onto it
    var size by mutableStateOf(Size.Zero)
    var positionInRoot by mutableStateOf(Offset.Zero)
}

/**
 * The scene a screen draws behind all of the calc (e.g. its top bar too), for the calc not to draw one of its own
 */
internal val LocalFieldEffectsScene = staticCompositionLocalOf<FieldEffectsScene?> { null }

/**
 * The scene of [field]'s effects, its clock only running with weather or terrain
 */
@Composable
internal fun rememberFieldEffectsScene(field: Battlefield): FieldEffectsScene {
    val framesPerSecond = when {
        // the rain's drops, much faster than the other particles, look choppy at a lower rate
        field.weather == Weather.RAIN -> FAST_FRAMES_PER_SECOND
        field.weather != Weather.NONE || field.terrain != Terrain.NONE -> FRAMES_PER_SECOND
        else -> null
    }
    val time = rememberElapsedSeconds(framesPerSecond)
    return remember { FieldEffectsScene(time) }
}

/**
 * The weather and the terrain of [field] drawn behind the content, with a scene of its own. Nothing without weather
 * nor terrain
 */
@Composable
internal fun Modifier.fieldEffectsBackground(field: Battlefield): Modifier =
    fieldEffectsScene(rememberFieldEffectsScene(field), field)

/**
 * The whole [scene] drawn behind the content, its size being the scene's
 */
@Composable
internal fun Modifier.fieldEffectsScene(scene: FieldEffectsScene, field: Battlefield): Modifier =
    onGloballyPositioned {
        scene.size = it.size.toSize()
        scene.positionInRoot = it.positionInRoot()
    }.drawFieldEffects(scene, field, sceneSize = { size }, offset = { Offset.Zero })

/**
 * The part of [scene] under this element drawn behind its content, for an opaque element over the scene (e.g. a pinned
 * header hiding what scrolls under it) to show the scene as if it were see-through
 */
@Composable
internal fun Modifier.fieldEffectsWindow(scene: FieldEffectsScene, field: Battlefield): Modifier {
    var positionInRoot by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { positionInRoot = it.positionInRoot() }
        .drawFieldEffects(scene, field, sceneSize = { scene.size }, offset = { positionInRoot - scene.positionInRoot })
}

/**
 * What doesn't move (gradients, shapes, the number of particles) is only built again when the scene's size changes,
 * each update only placing the particles
 *
 * @param sceneSize the size the effects are drawn for, read when building the cache
 * @param offset where this element is in the scene, read when drawing
 */
@Composable
private fun Modifier.drawFieldEffects(
    scene: FieldEffectsScene,
    field: Battlefield,
    sceneSize: CacheDrawScope.() -> Size,
    offset: () -> Offset,
): Modifier {
    val weather = field.weather
    val terrain = field.terrain
    val weatherColor = weatherColor(weather)
    val terrainColor = terrainColor(terrain)
    if (weatherColor == null && terrainColor == null) return this
    return drawWithCache {
        val size = sceneSize()
        if (size.isEmpty()) return@drawWithCache onDrawBehind {}
        val layerScope = LayerScope(size, this)
        // fewer particles in each layer when both are on, for the whole to stay readable
        val density = if (weatherColor != null && terrainColor != null) COMBINED_DENSITY else 1f
        val weatherLayer = weatherColor?.let { layerScope.weatherLayer(weather, it, scene.weatherParticles, density) }
        val terrainLayer = terrainColor?.let { layerScope.terrainLayer(terrain, it, scene.terrainParticles, density) }
        onDrawBehind {
            // read here, so that only the drawing is redone on each update or move
            val seconds = scene.time.value
            val (x, y) = offset()
            // in this element's bounds only, the drops and flakes starting above the top
            clipRect {
                translate(-x, -y) {
                    // drawn as for the whole scene
                    draw(this, layoutDirection, drawContext.canvas, size) {
                        terrainLayer?.invoke(this, seconds)
                        weatherLayer?.invoke(this, seconds)
                    }
                }
            }
        }
    }
}

/**
 * What the layers are built with: the size of the scene, which can differ from the one of the element drawing them
 */
private class LayerScope(val size: Size, density: Density) : Density by density

private const val COMBINED_DENSITY = 0.6f

/**
 * What to draw on each update, given the seconds since the effect started
 */
private typealias Layer = DrawScope.(seconds: Float) -> Unit

/**
 * A particle's random traits, each between 0 and 1, the layers turning them into positions, speeds and sizes
 */
internal class Particle(random: Random) {
    val x = random.nextFloat()
    val y = random.nextFloat()
    val size = random.nextFloat()
    val speed = random.nextFloat()
    val phase = random.nextFloat()
}

private fun particles(seed: Int) = List(MAX_PARTICLES) { Particle(Random(seed * 7919 + it)) }

private const val MAX_PARTICLES = 120

/**
 * The seconds since the effect started, updated about [framesPerSecond] times per second: less work than updating it on
 * every frame, especially on 120 Hz screens. Wrapping every hour to keep the float's precision
 */
@Composable
private fun rememberElapsedSeconds(framesPerSecond: Int?): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    // a bit less than a frame's duration, the wait for the next frame making up the rest
    val delayMillis = framesPerSecond?.let { (1000L / it - 8).coerceAtLeast(0) }
    LaunchedEffect(delayMillis) {
        // stopped without anything to animate
        if (delayMillis == null) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            // no frame is asked for while waiting, the screen staying still in between
            delay(delayMillis)
            withFrameNanos { time.floatValue = ((it - start) / 1_000_000L % 3_600_000L) / 1000f }
        }
    }
    return time
}

// enough for the slow particles (embers, flakes, grains, the terrains')
private const val FRAMES_PER_SECOND = 30
private const val FAST_FRAMES_PER_SECOND = 60

/**
 * How many particles for the size, [perArea] being the side (in dp) of the square one particle has, so that big and
 * small cards look as busy
 */
private fun LayerScope.particleCount(perArea: Float, max: Int, density: Float): Int {
    val side = perArea.dp.toPx()
    return (size.width * size.height / (side * side) * density).toInt().coerceIn(MIN_PARTICLES, max)
}

private const val MIN_PARTICLES = 8

// a position moving at [speed] px per second from [start], wrapping over [length]
private fun wrap(start: Float, speed: Float, seconds: Float, length: Float): Float {
    val value = (start + speed * seconds) % length
    return if (value < 0) value + length else value
}

// a pseudo-random value between 0 and 1 for [value] and [n], e.g. a new position on each of a spark's flashes
private fun hash(value: Float, n: Int): Float {
    val v = sin(value * 127.1f + n * 311.7f) * 43758.547f
    return v - floor(v)
}

// between 0 and 1, going back and forth over [period] seconds
private fun pulse(seconds: Float, period: Float, phase: Float): Float =
    (sin((seconds / period + phase) * 2 * PI).toFloat() + 1f) / 2f

private fun LayerScope.topGlow(color: Color, alpha: Float, heightFraction: Float) = Brush.verticalGradient(
    listOf(color.copy(alpha = alpha), Color.Transparent),
    startY = 0f,
    endY = size.height * heightFraction,
)

// ---------------------------------------------------------------------------------------------------- weathers

private fun LayerScope.weatherLayer(weather: Weather, color: Color, particles: List<Particle>, density: Float): Layer? =
    when (weather) {
        Weather.SUN -> sun(color, particles, density)
        Weather.RAIN -> rain(color, particles, density)
        Weather.SNOW -> snow(color, particles, density)
        Weather.SAND -> sand(color, particles, density)
        Weather.NONE, Weather.HAIL -> null
    }

/**
 * A warm glow from the top, slowly turning rays and embers rising
 */
private fun LayerScope.sun(color: Color, particles: List<Particle>, density: Float): Layer {
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
    val count = particleCount(perArea = 110f, max = 50, density)
    val sway = 10 * dp
    val height = size.height + 20 * dp
    return { seconds ->
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

private const val SUN_RAYS = 14
private const val SUN_RAY_WIDTH_DEGREES = 7.0
private const val SUN_RAY_DEGREES_PER_SECOND = 2f

/**
 * Drops falling fast, slightly slanted
 */
private fun LayerScope.rain(color: Color, particles: List<Particle>, density: Float): Layer {
    val dp = 1.dp.toPx()
    val glow = topGlow(color, alpha = 0.12f, heightFraction = 0.5f)
    val count = particleCount(perArea = 70f, max = MAX_PARTICLES, density)
    val stroke = 1.2f * dp
    return { seconds ->
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

private const val RAIN_SLANT = 0.25f

/**
 * Flakes of various sizes falling slowly and drifting from side to side
 */
private fun LayerScope.snow(color: Color, particles: List<Particle>, density: Float): Layer {
    val dp = 1.dp.toPx()
    val glow = topGlow(color, alpha = 0.08f, heightFraction = 0.5f)
    val count = particleCount(perArea = 85f, max = 80, density)
    val sway = 14 * dp
    return { seconds ->
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

/**
 * Grains blown fast from left to right, wavering a little, in an ochre haze
 */
private fun LayerScope.sand(color: Color, particles: List<Particle>, density: Float): Layer {
    val dp = 1.dp.toPx()
    val haze = color.copy(alpha = 0.06f)
    val glow = topGlow(color, alpha = 0.10f, heightFraction = 0.6f)
    val count = particleCount(perArea = 80f, max = 90, density)
    val waver = 4 * dp
    val stroke = 1.5f * dp
    return { seconds ->
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

// ---------------------------------------------------------------------------------------------------- terrains

private fun LayerScope.terrainLayer(terrain: Terrain, color: Color, particles: List<Particle>, density: Float): Layer? {
    val glow = bottomCornersGlow(color)
    val layer = when (terrain) {
        Terrain.ELECTRIC -> electric(color, particles, density)
        Terrain.GRASSY -> grassy(color, particles, density)
        Terrain.MISTY -> misty(color, particles, density)
        Terrain.PSYCHIC -> psychic(color, particles, density)
        Terrain.NONE -> return null
    }
    return { seconds ->
        glow(this)
        layer(this, seconds)
    }
}

/**
 * The terrain's glow, from the two bottom corners
 */
private fun LayerScope.bottomCornersGlow(color: Color): DrawScope.() -> Unit {
    val radius = size.width * 0.7f
    val colors = listOf(color.copy(alpha = 0.16f), Color.Transparent)
    val left = Brush.radialGradient(colors, Offset(0f, size.height), radius)
    val right = Brush.radialGradient(colors, Offset(size.width, size.height), radius)
    return {
        drawRect(left)
        drawRect(right)
    }
}

/**
 * Small zigzag arcs, each flashing for an instant at a random place, then somewhere else a bit later
 */
private fun LayerScope.electric(color: Color, particles: List<Particle>, density: Float): Layer {
    val dp = 1.dp.toPx()
    val count = particleCount(perArea = 120f, max = 40, density)
    val stroke = 1.5f * dp
    return { seconds ->
        for (i in 0 until count) {
            val p = particles[i]
            val period = 1.5f + p.speed * 2.5f
            val cycle = (seconds + p.phase * period) / period
            val progress = cycle - floor(cycle)
            if (progress >= SPARK_DURATION) continue
            // bright at once, fading quickly
            val alpha = (1f - progress / SPARK_DURATION) * 0.8f
            val n = cycle.toInt()
            val startX = hash(p.x, n) * size.width
            val startY = hash(p.y, n) * size.height
            val angle = hash(p.size, n) * 2 * PI.toFloat()
            val segment = (5 + p.size * 4) * dp
            val zig = 3.5f * dp
            // along the arc's direction, alternating from one side to the other
            val dx = cos(angle)
            val dy = sin(angle)
            var previous = Offset(startX, startY)
            for (s in 1..SPARK_SEGMENTS) {
                val side = if (s == SPARK_SEGMENTS) 0f else if (s % 2 == 0) zig else -zig
                val next = Offset(startX + dx * segment * s - dy * side, startY + dy * segment * s + dx * side)
                drawLine(color.copy(alpha = alpha), previous, next, strokeWidth = stroke, cap = StrokeCap.Round)
                previous = next
            }
        }
    }
}

// the part of its period a spark is seen
private const val SPARK_DURATION = 0.12f
private const val SPARK_SEGMENTS = 4

/**
 * Leaves floating and twirling slowly, with some pollen rising
 */
private fun LayerScope.grassy(color: Color, particles: List<Particle>, density: Float): Layer {
    val dp = 1.dp.toPx()
    val leaves = particleCount(perArea = 130f, max = 35, density)
    val pollen = particleCount(perArea = 140f, max = 30, density)
    val sway = 12 * dp
    return { seconds ->
        for (i in 0 until leaves) {
            val p = particles[i]
            val leaf = Size((7 + p.size * 6) * dp, (3.5f + p.size * 2.5f) * dp)
            val width = size.width + leaf.width * 2
            // drifting to the right and slightly down
            val x = wrap(p.x * width, (8 + p.speed * 14) * dp, seconds, width) - leaf.width
            val height = size.height + leaf.width * 2
            val y = wrap(p.y * height, (3 + p.speed * 5) * dp, seconds, height) - leaf.width +
                    sin((seconds / (5 + p.phase * 3) + p.phase) * 2 * PI).toFloat() * sway
            val degrees = (p.phase * 360f + seconds * (20 + p.speed * 40)) % 360f
            rotate(degrees, pivot = Offset(x, y)) {
                drawOval(
                    color.copy(alpha = 0.3f + p.size * 0.25f),
                    topLeft = Offset(x - leaf.width / 2, y - leaf.height / 2),
                    size = leaf,
                )
            }
        }
        // the pollen, among the other particles than the leaves', rising slowly
        for (i in leaves until (leaves + pollen).coerceAtMost(particles.size)) {
            val p = particles[i]
            val height = size.height + 10 * dp
            val y = height - wrap(p.y * height, (6 + p.speed * 8) * dp, seconds, height)
            val x = p.x * size.width + sin((seconds / (4 + p.phase * 2) + p.phase) * 2 * PI).toFloat() * 6 * dp
            drawCircle(color.copy(alpha = 0.25f + 0.3f * pulse(seconds, 3f, p.phase)), radius = 1.2f * dp, center = Offset(x, y))
        }
    }
}

/**
 * Soft patches of mist drifting slowly, with some twinkling glitters
 */
private fun LayerScope.misty(color: Color, particles: List<Particle>, density: Float): Layer {
    val dp = 1.dp.toPx()
    // one gradient for all the patches, scaled to each one's size
    val patchRadius = 100 * dp
    val patchBrush = Brush.radialGradient(listOf(color.copy(alpha = 0.12f), Color.Transparent), Offset.Zero, patchRadius)
    val patches = particleCount(perArea = 220f, max = 12, density)
    val glitters = particleCount(perArea = 120f, max = 35, density)
    return { seconds ->
        for (i in 0 until patches) {
            val p = particles[i]
            val radius = (70 + p.size * 90) * dp
            val width = size.width + radius * 2
            val x = wrap(p.x * width, (6 + p.speed * 10) * dp, seconds, width) - radius
            val y = p.y * size.height + sin((seconds / (8 + p.phase * 4) + p.phase) * 2 * PI).toFloat() * 10 * dp
            translate(x, y) {
                scale(radius / patchRadius, pivot = Offset.Zero) {
                    drawCircle(patchBrush, radius = patchRadius, center = Offset.Zero)
                }
            }
        }
        for (i in patches until (patches + glitters).coerceAtMost(particles.size)) {
            val p = particles[i]
            val twinkle = pulse(seconds, 2f + p.speed * 2f, p.phase)
            drawCircle(
                color.copy(alpha = 0.6f * twinkle * twinkle),
                radius = (1f + p.size * 1.5f) * dp,
                center = Offset(p.x * size.width, p.y * size.height),
            )
        }
    }
}

/**
 * Rings spreading from random places, each fading as it grows, with some motes pulsing in place
 */
private fun LayerScope.psychic(color: Color, particles: List<Particle>, density: Float): Layer {
    val dp = 1.dp.toPx()
    val ripples = particleCount(perArea = 170f, max = 16, density)
    val motes = particleCount(perArea = 130f, max = 30, density)
    val stroke = Stroke(width = 1.5f * dp)
    return { seconds ->
        for (i in 0 until ripples) {
            val p = particles[i]
            val period = 3f + p.speed * 2f
            val cycle = (seconds + p.phase * period) / period
            val n = cycle.toInt()
            val center = Offset(hash(p.x, n) * size.width, hash(p.y, n) * size.height)
            val maxRadius = (40 + p.size * 40) * dp
            // two rings, the second one a bit behind, both gone at the end of the cycle
            for (lag in RIPPLE_LAGS) {
                val progress = (cycle - floor(cycle) - lag) / (1f - lag)
                if (progress <= 0f) continue
                drawCircle(
                    color.copy(alpha = (1f - progress) * 0.4f),
                    radius = progress * maxRadius,
                    center = center,
                    style = stroke,
                )
            }
        }
        for (i in ripples until (ripples + motes).coerceAtMost(particles.size)) {
            val p = particles[i]
            val glow = pulse(seconds, 2.5f + p.speed * 2f, p.phase)
            val y = p.y * size.height + sin((seconds / (6 + p.phase * 3) + p.phase) * 2 * PI).toFloat() * 6 * dp
            drawCircle(
                color.copy(alpha = 0.15f + 0.45f * glow),
                radius = (1.2f + glow * 1.5f) * dp,
                center = Offset(p.x * size.width, y),
            )
        }
    }
}

private val RIPPLE_LAGS = floatArrayOf(0f, 0.25f)
