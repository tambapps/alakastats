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
import alakastats.composeapp.generated.resources.move_physical
import alakastats.composeapp.generated.resources.move_poison
import alakastats.composeapp.generated.resources.move_psychic
import alakastats.composeapp.generated.resources.move_rock
import alakastats.composeapp.generated.resources.move_special
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.PlatformType
import com.tambapps.pokemon.alakastats.getPlatform
import com.tambapps.pokemon.alakastats.ui.composables.TooltipIfEnabled
import com.tambapps.pokemon.alakastats.util.PokemonNormalizer
import com.tambapps.pokemon.alakastats.util.titlecase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource

private val placeHolderDrawable = Res.drawable.pokeball

interface IPokemonImageService {
    @Composable
    fun PokemonSprite(name: String, modifier: Modifier = Modifier, disableTooltip: Boolean = false)

    @Composable
    fun PokemonArtwork(name: String, modifier: Modifier = Modifier, disableTooltip: Boolean = false)

    @Composable
    fun TeraTypeImage(type: PokeType, disableTooltip: Boolean = false, modifier: Modifier = Modifier)

    @Composable
    fun MoveTypeImage(type: PokeType, disableTooltip: Boolean = false, modifier: Modifier = Modifier)

    @Composable
    fun MoveSpecImages(move: String, iconModifier: Modifier = Modifier)

    @Composable
    fun ItemImage(item: String, modifier: Modifier = Modifier, disableTooltip: Boolean = false)
}

abstract class AbstractPokemonImageService(
    protected val json: Json
): IPokemonImageService {
    protected val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val movesData = mutableStateMapOf<String, MoveData>()

    @Composable
    override fun TeraTypeImage(type: PokeType, disableTooltip: Boolean, modifier: Modifier) {
        val resource = when(type) {
            PokeType.STEEL -> Res.drawable.tera_type_steel
            PokeType.FIGHTING -> Res.drawable.tera_type_fighting
            PokeType.DRAGON -> Res.drawable.tera_type_dragon
            PokeType.FIRE -> Res.drawable.tera_type_fire
            PokeType.ICE -> Res.drawable.tera_type_ice
            PokeType.NORMAL -> Res.drawable.tera_type_normal
            PokeType.WATER -> Res.drawable.tera_type_water
            PokeType.GRASS -> Res.drawable.tera_type_grass
            PokeType.ELECTRIC -> Res.drawable.tera_type_electric
            PokeType.FAIRY -> Res.drawable.tera_type_fairy
            PokeType.POISON -> Res.drawable.tera_type_poison
            PokeType.PSY -> Res.drawable.tera_type_psychic
            PokeType.ROCK -> Res.drawable.tera_type_rock
            PokeType.GHOST -> Res.drawable.tera_type_ghost
            PokeType.DARK -> Res.drawable.tera_type_dark
            PokeType.GROUND -> Res.drawable.tera_type_ground
            PokeType.FLYING -> Res.drawable.tera_type_flying
            PokeType.PSYCHIC -> Res.drawable.tera_type_psychic
            PokeType.BUG -> Res.drawable.tera_type_bug
            PokeType.STELLAR -> Res.drawable.tera_type_stellar
            PokeType.UNKNOWN -> Res.drawable.tera_type_normal
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
            PokeType.UNKNOWN, PokeType.STELLAR -> Res.drawable.move_normal
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
        val (_, category, type) = data

        val categoryRes = when(category.lowercase()) {
            "physical" -> Res.drawable.move_physical
            else -> Res.drawable.move_special
        }

        MoveTypeImage(type, modifier = iconModifier, disableTooltip = true)
        Spacer(Modifier.width(8.dp))
        Image(
            painter = painterResource(categoryRes),
            contentDescription = category,
            modifier = iconModifier,
            contentScale = ContentScale.Fit,
        )
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

class PokemonUrlImageService(
    json: Json,
    private val baseUrl: String
): AbstractPokemonImageService(json) {
    @Composable
    override fun PokemonSprite(
        name: String,
        modifier: Modifier,
        disableTooltip: Boolean
    ) = WebPokemonImage("sprite", name, modifier)

    @Composable
    override fun PokemonArtwork(
        name: String,
        modifier: Modifier,
        disableTooltip: Boolean
    ) = WebPokemonImage("artwork", name, modifier)

    @Composable
    override fun ItemImage(
        item: String,
        modifier: Modifier,
        disableTooltip: Boolean
    ) {
        // TODO
        MyImage(url = "http://localhost:8080/images/pokemons/artowrk/abomasnow.png",
            contentDescription = item,
            modifier = modifier,
        )
    }

    @Composable
    private fun WebPokemonImage(type: String, name: String, modifier: Modifier) {
        MyImage(url = "$baseUrl/images/pokemons/$type/$name.png",
            contentDescription = name,
            modifier = modifier,
        )
    }
}

class PokemonImageService(json: Json) : AbstractPokemonImageService(json) {
    private val pokemonImages = mutableStateMapOf<String, PokemonSpriteData>()
    private val itemsData = mutableStateMapOf<String, ItemData>()

    init {
        coroutineScope.launch {
            val map: Map<String, PokemonSpriteData> = readMappingFile(json, "pokemon-sprites.json")
            withContext(Dispatchers.Main) {
                pokemonImages.putAll(map)
            }
        }
    }

    @Composable
    override fun PokemonSprite(name: String, modifier: Modifier, disableTooltip: Boolean) = PokemonImage(name, modifier, disableTooltip) { it.sprite }
    @Composable
    override fun PokemonArtwork(name: String, modifier: Modifier, disableTooltip: Boolean) = PokemonImage(name, modifier, disableTooltip) { it.artwork }

    @Composable
    private inline fun PokemonImage(name: String, modifier: Modifier, disableTooltip: Boolean = false, imageUrlSupplier: (PokemonSpriteData) -> String) {
        val pokemonSpriteData = pokemonImages[PokemonNormalizer.normalizeToBase(name)]
        val imageUrl = pokemonSpriteData?.let(imageUrlSupplier)


        TooltipIfEnabled(disableTooltip, name, modifier) { mod ->
            if (imageUrl != null) {
                MyImage(url = imageUrl,
                    contentDescription = pokemonSpriteData.name,
                    modifier = mod,
                )
            } else {
                DefaultIcon(
                    contentDescription = name,
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
    )
}

private suspend inline fun <reified T> readMappingFile(json: Json, filename: String): T {
    val text = Res.readBytes("files/$filename").decodeToString()
    return json.decodeFromString<T>(text)
}

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
data class PokemonSpriteData(
    val name: String,
    val sprite: String,
    val artwork: String
)

