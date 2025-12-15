package com.tambapps.pokemon.alakastats.ui.service

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.move_bug
import alakastats.composeapp.generated.resources.move_dark
import alakastats.composeapp.generated.resources.move_dragon
import alakastats.composeapp.generated.resources.move_electric
import alakastats.composeapp.generated.resources.move_fairy
import alakastats.composeapp.generated.resources.move_fighting
import alakastats.composeapp.generated.resources.move_fire
import alakastats.composeapp.generated.resources.move_flying
import alakastats.composeapp.generated.resources.move_ghost
import alakastats.composeapp.generated.resources.move_grass
import alakastats.composeapp.generated.resources.move_ground
import alakastats.composeapp.generated.resources.move_ice
import alakastats.composeapp.generated.resources.move_normal
import alakastats.composeapp.generated.resources.move_poison
import alakastats.composeapp.generated.resources.move_psychic
import alakastats.composeapp.generated.resources.move_rock
import alakastats.composeapp.generated.resources.move_steel
import alakastats.composeapp.generated.resources.move_water
import alakastats.composeapp.generated.resources.pokeball
import alakastats.composeapp.generated.resources.tera_type_bug
import alakastats.composeapp.generated.resources.tera_type_dark
import alakastats.composeapp.generated.resources.tera_type_dragon
import alakastats.composeapp.generated.resources.tera_type_electric
import alakastats.composeapp.generated.resources.tera_type_fairy
import alakastats.composeapp.generated.resources.tera_type_fighting
import alakastats.composeapp.generated.resources.tera_type_fire
import alakastats.composeapp.generated.resources.tera_type_flying
import alakastats.composeapp.generated.resources.tera_type_ghost
import alakastats.composeapp.generated.resources.tera_type_grass
import alakastats.composeapp.generated.resources.tera_type_ground
import alakastats.composeapp.generated.resources.tera_type_ice
import alakastats.composeapp.generated.resources.tera_type_normal
import alakastats.composeapp.generated.resources.tera_type_poison
import alakastats.composeapp.generated.resources.tera_type_psychic
import alakastats.composeapp.generated.resources.tera_type_rock
import alakastats.composeapp.generated.resources.tera_type_steel
import alakastats.composeapp.generated.resources.tera_type_stellar
import alakastats.composeapp.generated.resources.tera_type_water
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.PokemonNormalizer
import com.tambapps.pokemon.TeraType
import com.tambapps.pokemon.alakastats.ui.composables.TooltipIfEnabled
import com.tambapps.pokemon.alakastats.util.titlecase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource

private val placeHolderDrawable = Res.drawable.pokeball

enum class FacingDirection {
    LEFT, RIGHT
}
interface PokemonImageService {

    fun listAvailableNames(): List<PokemonName>

    @Composable
    fun PokemonSprite(name: PokemonName, modifier: Modifier = Modifier, disableTooltip: Boolean = false, facingDirection: FacingDirection = FacingDirection.LEFT)

    @Composable
    fun PokemonArtwork(
        name: PokemonName,
        modifier: Modifier = Modifier,
        disableTooltip: Boolean = false,
        facingDirection: FacingDirection = FacingDirection.LEFT
    )

    @Composable
    fun TeraTypeImage(type: TeraType, disableTooltip: Boolean = false, modifier: Modifier = Modifier)

    @Composable
    fun MoveTypeImage(type: PokeType, disableTooltip: Boolean = false, modifier: Modifier = Modifier)

    @Composable
    fun MoveSpecImages(move: String, iconModifier: Modifier = Modifier)

    @Composable
    fun ItemImage(item: String, modifier: Modifier = Modifier, disableTooltip: Boolean = false)
}

abstract class AbstractPokemonImageService(
    protected val json: Json
): PokemonImageService {
    protected val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val movesData = mutableStateMapOf<String, MoveData>()
    private val pokemonImages = mutableStateMapOf<String, PokemonImages>()
    private val availableNames = mutableStateListOf<PokemonName>()

    init {
        coroutineScope.launch {
            val map: Map<String, PokemonImages> = readMappingFile(json, "pokemon-sprites.json")
            withContext(Dispatchers.Main) {
                pokemonImages.putAll(map)
                availableNames.addAll(
                    map.keys.asSequence()
                        .sorted()
                        .map { PokemonName(it) }
                )
            }
        }
    }

    // needs to be @Composable to listen to the map changes
    @Composable
    protected fun getPokemonImageData(name: PokemonName, type: ImageType) = when (type) {
        ImageType.SPRITE -> pokemonImages[name.normalized.value]?.sprite
        ImageType.ARTWORK -> pokemonImages[name.normalized.value]?.artwork
    }

    override fun listAvailableNames() = availableNames

    @Composable
    override fun TeraTypeImage(type: TeraType, disableTooltip: Boolean, modifier: Modifier) {
        val resource = when(type) {
            TeraType.STEEL -> Res.drawable.tera_type_steel
            TeraType.FIGHTING -> Res.drawable.tera_type_fighting
            TeraType.DRAGON -> Res.drawable.tera_type_dragon
            TeraType.FIRE -> Res.drawable.tera_type_fire
            TeraType.ICE -> Res.drawable.tera_type_ice
            TeraType.NORMAL -> Res.drawable.tera_type_normal
            TeraType.WATER -> Res.drawable.tera_type_water
            TeraType.GRASS -> Res.drawable.tera_type_grass
            TeraType.ELECTRIC -> Res.drawable.tera_type_electric
            TeraType.FAIRY -> Res.drawable.tera_type_fairy
            TeraType.POISON -> Res.drawable.tera_type_poison
            TeraType.PSY -> Res.drawable.tera_type_psychic
            TeraType.ROCK -> Res.drawable.tera_type_rock
            TeraType.GHOST -> Res.drawable.tera_type_ghost
            TeraType.DARK -> Res.drawable.tera_type_dark
            TeraType.GROUND -> Res.drawable.tera_type_ground
            TeraType.FLYING -> Res.drawable.tera_type_flying
            TeraType.PSYCHIC -> Res.drawable.tera_type_psychic
            TeraType.BUG -> Res.drawable.tera_type_bug
            TeraType.STELLAR -> Res.drawable.tera_type_stellar
        }
        TooltipIfEnabled(disableTooltip,
            "Tera " + type.name.titlecase(), modifier) { mod ->
            Image(
                painter = painterResource(resource),
                contentDescription = "Tera $type",
                modifier = mod,
                contentScale = ContentScale.Fit
            )
        }
    }

    @Composable
    override fun MoveTypeImage(type: PokeType, disableTooltip: Boolean, modifier: Modifier) {
        val resource = when(type) {
            PokeType.STEEL -> Res.drawable.move_steel
            PokeType.FIGHTING -> Res.drawable.move_fighting
            PokeType.DRAGON -> Res.drawable.move_dragon
            PokeType.FIRE -> Res.drawable.move_fire
            PokeType.ICE -> Res.drawable.move_ice
            PokeType.NORMAL -> Res.drawable.move_normal
            PokeType.WATER -> Res.drawable.move_water
            PokeType.GRASS -> Res.drawable.move_grass
            PokeType.ELECTRIC -> Res.drawable.move_electric
            PokeType.FAIRY -> Res.drawable.move_fairy
            PokeType.POISON -> Res.drawable.move_poison
            PokeType.PSY -> Res.drawable.move_psychic
            PokeType.ROCK -> Res.drawable.move_rock
            PokeType.GHOST -> Res.drawable.move_ghost
            PokeType.DARK -> Res.drawable.move_dark
            PokeType.GROUND -> Res.drawable.move_ground
            PokeType.FLYING -> Res.drawable.move_flying
            PokeType.PSYCHIC -> Res.drawable.move_psychic
            PokeType.BUG -> Res.drawable.move_bug
        }
        TooltipIfEnabled(disableTooltip, type.name.titlecase(), modifier) { mod ->
            Image(
                painter = painterResource(resource),
                contentDescription = "$type",
                modifier = mod,
                contentScale = ContentScale.Fit
            )
        }
    }

    @Composable
    override fun MoveSpecImages(move: String, iconModifier: Modifier) {
        // lazy loading
        if (movesData.isEmpty()) {
            loadMoves()
            DefaultIcon(modifier = iconModifier)
            return
        }
        val data = movesData[PokemonNormalizer.normalize(move)]
        if (data == null) {
            DefaultIcon(modifier = iconModifier)
            return
        }
        MoveTypeImage(data.type, modifier = iconModifier, disableTooltip = true)
        /*
          val categoryRes = when(category.lowercase()) {
            "physical" -> Res.drawable.move_physical
            else -> Res.drawable.move_special
        }
        Spacer(Modifier.width(8.dp))
        Image(
            painter = painterResource(categoryRes),
            contentDescription = category,
            modifier = iconModifier,
            contentScale = ContentScale.Fit,
        )

         */
    }

    @Composable
    protected fun DefaultIcon(contentDescription: String = "", modifier: Modifier = Modifier) {
        Icon(
            painter = painterResource(placeHolderDrawable),
            contentDescription = contentDescription,
            modifier = modifier
        )
    }


    private fun loadMoves() {
        coroutineScope.launch {
            val map: Map<String, MoveData> = readMappingFile(json, "moves.json")
            withContext(Dispatchers.Main) {
                movesData.clear()
                movesData.putAll(map)
            }
        }
    }
}

class PokemonLocalUrlImageService(
    json: Json,
    private val baseUrl: String
): AbstractPokemonImageService(json) {
    @Composable
    override fun PokemonSprite(
        name: PokemonName,
        modifier: Modifier,
        disableTooltip: Boolean,
        facingDirection: FacingDirection
    ) = WebPokemonImage(ImageType.SPRITE, name, modifier, facingDirection, disableTooltip)

    @Composable
    override fun PokemonArtwork(
        name: PokemonName,
        modifier: Modifier,
        disableTooltip: Boolean,
        facingDirection: FacingDirection
    ) = WebPokemonImage(
        ImageType.ARTWORK, name, modifier,
        facingDirection,
        disableTooltip = disableTooltip
    )

    @Composable
    override fun ItemImage(
        item: String,
        modifier: Modifier,
        disableTooltip: Boolean
    ) {
        val formattedItemName = PokemonNormalizer.normalize(item)
        TooltipIfEnabled(disableTooltip, item, modifier) { mod ->
            MyImage(url = "$baseUrl/images/items/$formattedItemName.png",
                contentDescription = item,
                modifier = modifier,
            )
        }
    }

    @Composable
    private fun WebPokemonImage(
        type: ImageType,
        name: PokemonName,
        modifier: Modifier,
        facingDirection: FacingDirection,
        disableTooltip: Boolean
    ) {
        val imageFacingDirection = getPokemonImageData(name, type)?.direction
        val displayedName = name.pretty
        TooltipIfEnabled(disableTooltip, displayedName, modifier) { mod ->
            MyImage(url = "$baseUrl/images/pokemons/${type.name.lowercase()}/${name.normalized.value}.png",
                contentDescription = displayedName,
                modifier = mod.flipXIfNecessary(facingDirection, imageFacingDirection),
            )
        }
    }
}

class PokemonUrlMappingImageService(json: Json) : AbstractPokemonImageService(json) {
    private val itemsData = mutableStateMapOf<String, ItemData>()

    @Composable
    override fun PokemonSprite(name: PokemonName, modifier: Modifier, disableTooltip: Boolean, facingDirection: FacingDirection) = PokemonImage(
        name,
        ImageType.SPRITE,
        modifier,
        disableTooltip = disableTooltip,
        facingDirection = facingDirection)

    @Composable
    override fun PokemonArtwork(
        name: PokemonName,
        modifier: Modifier,
        disableTooltip: Boolean,
        facingDirection: FacingDirection
    ) = PokemonImage(
        name,
        ImageType.ARTWORK,
        modifier, disableTooltip = disableTooltip,
        facingDirection = facingDirection)

    @Composable
    private fun PokemonImage(pokemonName: PokemonName, imageType: ImageType, modifier: Modifier, disableTooltip: Boolean = false, facingDirection: FacingDirection) {
        val imageData = getPokemonImageData(pokemonName, imageType)
        val prettyName = pokemonName.pretty
        TooltipIfEnabled(disableTooltip, prettyName, modifier) { mod ->
            val imageUrl = imageData?.url
            if (imageUrl != null) {
                MyImage(url = imageUrl,
                    contentDescription = prettyName,
                    modifier = mod.flipXIfNecessary(facingDirection, imageData.direction),
                )
            } else {
                DefaultIcon(
                    contentDescription = prettyName,
                    modifier = mod
                )
            }
        }
    }

    @Composable
    override fun ItemImage(item: String, modifier: Modifier, disableTooltip: Boolean) {
        TooltipIfEnabled(disableTooltip, item, modifier) { mod ->
            // lazy loading
            if (itemsData.isEmpty()) {
                loadItems()
                DefaultIcon()
                return@TooltipIfEnabled
            }
            val data = itemsData[PokemonNormalizer.normalize(item)]
            if (data == null) {
                DefaultIcon()
                return@TooltipIfEnabled
            }

            MyImage(
                url = data.spriteUrl,
                contentDescription = data.name,
                modifier = mod,
                )
        }
    }

    private fun loadItems() {
        coroutineScope.launch {
            val map: Map<String, ItemData> = readMappingFile(json, "items-mapping.json")
            withContext(Dispatchers.Main) {
                itemsData.clear()
                itemsData.putAll(map)
            }
        }
    }
}

@Composable
private fun MyImage(url: String, contentDescription: String = "", modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = painterResource(placeHolderDrawable),
        error = painterResource(placeHolderDrawable),
        )
}

private suspend inline fun <reified T> readMappingFile(json: Json, filename: String): T {
    val text = Res.readBytes("files/$filename").decodeToString()
    return json.decodeFromString<T>(text)
}

// these below class need to be public in order for state listening to work properly
@Serializable
data class ItemData(
    val name: String,
    val spriteUrl: String
)


@Serializable
data class MoveData(
    val name: String,
    val category: String,
    val type: PokeType
)


@Serializable
data class PokemonImages(
    val name: String,
    val sprite: PokemonImageData,
    val artwork: PokemonImageData,
)

@Serializable
data class PokemonImageData(
    val url: String,
    val direction: FacingDirection = FacingDirection.LEFT
)

enum class ImageType {
    SPRITE, ARTWORK
}

private fun Modifier.flipXIfNecessary(wantedFacingDirection: FacingDirection?, pokemonFacingDirection: FacingDirection?) =
    if (wantedFacingDirection != null && wantedFacingDirection != pokemonFacingDirection) scale(scaleX = - 1f, scaleY = 1f)
    else this
