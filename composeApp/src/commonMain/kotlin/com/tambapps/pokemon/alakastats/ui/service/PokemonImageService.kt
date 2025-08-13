package com.tambapps.pokemon.alakastats.ui.service

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.catching_pokemon
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.tambapps.pokemon.PokeType
import com.tambapps.pokemon.alakastats.PlatformType
import com.tambapps.pokemon.alakastats.getPlatform
import com.tambapps.pokemon.alakastats.ui.composables.Tooltip
import com.tambapps.pokemon.alakastats.ui.composables.TooltipIfEnabled
import com.tambapps.pokemon.alakastats.util.PokemonNormalizer
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource

class PokemonImageService(
    private val json: Json
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val pokemonImages = mutableStateMapOf<String, PokemonSpriteData>()
    private val movesData = mutableStateMapOf<String, MoveData>()
    private val itemsData = mutableStateMapOf<String, ItemData>()

    init {
        coroutineScope.launch {
            val map: Map<String, PokemonSpriteData> = readMappingFile("pokemon-sprites.json")
            withContext(Dispatchers.Main) {
                pokemonImages.putAll(map)
            }
        }
    }

    private suspend inline fun <reified T> readMappingFile(filename: String): T {
        val text = Res.readBytes("files/$filename").decodeToString()
        return json.decodeFromString<T>(text)
    }

    @Composable
    fun PokemonSprite(name: String, modifier: Modifier = Modifier, disableTooltip: Boolean = false) = PokemonImage(name, modifier, disableTooltip) { it.sprite }
    @Composable
    fun PokemonArtwork(name: String, modifier: Modifier = Modifier, disableTooltip: Boolean = false) = PokemonImage(name, modifier, disableTooltip) { it.artwork }

    @Composable
    private inline fun PokemonImage(name: String, modifier: Modifier, disableTooltip: Boolean = false, imageUrlSupplier: (PokemonSpriteData) -> String) {
        val pokemonSpriteData = pokemonImages[PokemonNormalizer.normalizeToBase(name)]
        val imageUrl = pokemonSpriteData?.let(imageUrlSupplier)


        TooltipIfEnabled(disableTooltip, name, modifier) { mod ->
            if (imageUrl != null) {
                // TODO the web part is BAD. This is a hack to avoid CORS, because kmp web rendering uses a canvas instead of a <img>
                val url = if (getPlatform().type == PlatformType.Web)  "https://api.allorigins.win/raw?url=${imageUrl.replace("#", "%23").replace("&", "%26").replace("?", "%3F")}"
                else imageUrl
                KamelImage({ asyncPainterResource(data = url) },
                    contentDescription = pokemonSpriteData.name,
                    modifier = mod
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
    fun TeraTypeImage(type: PokeType, disableTooltip: Boolean = false) {
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
            "Tera " + type.name.let { it[0] + it.substring(1).lowercase() }, Modifier) { modifier ->
            Image(
                painter = painterResource(resource),
                contentDescription = "Tera $type",
                modifier = modifier,
                contentScale = ContentScale.Fit
            )
        }
    }

    @Composable
    fun MoveTypeImage(type: PokeType, disableTooltip: Boolean = false) {
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
        TooltipIfEnabled(disableTooltip, type.name.let { it[0] + it.substring(1).lowercase() }, Modifier) { modifier ->
            Image(
                painter = painterResource(resource),
                contentDescription = "$type",
                modifier = Modifier,
                contentScale = ContentScale.Fit
            )
        }
    }

    @Composable
    fun MoveSpecImages(move: String) {
        // lazy loading
        if (movesData.isEmpty()) {
            loadMoves()
            DefaultIcon()
            return
        }
        val data = movesData[PokemonNormalizer.normalize(move)]
        if (data == null) {
            DefaultIcon()
            return
        }
        val (_, category, type) = data

        val categoryRes = when(category.lowercase()) {
            "physical" -> Res.drawable.move_physical
            else -> Res.drawable.move_special
        }

        MoveTypeImage(type)
        Image(
            painter = painterResource(categoryRes),
            contentDescription = category,
            modifier = Modifier,
            contentScale = ContentScale.Fit
        )
    }

    @Composable
    fun ItemImage(item: String, modifier: Modifier = Modifier, disableTooltip: Boolean = false) {
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

            KamelImage({ asyncPainterResource(data = data.spriteUrl) },
                contentDescription = data.name,
                modifier = mod
            )
        }
    }

    @Composable
    private fun DefaultIcon(contentDescription: String = "", modifier: Modifier = Modifier) {
        Icon(
            painter = painterResource(Res.drawable.catching_pokemon),
            contentDescription = contentDescription,
            modifier = modifier
        )
    }

    private fun loadMoves() {
        coroutineScope.launch {
            val map: Map<String, MoveData> = readMappingFile("moves.json")
            withContext(Dispatchers.Main) {
                movesData.clear()
                movesData.putAll(map)
            }
        }
    }

    private fun loadItems() {
        coroutineScope.launch {
            val map: Map<String, ItemData> = readMappingFile("items-mapping.json")
            withContext(Dispatchers.Main) {
                itemsData.clear()
                itemsData.putAll(map)
            }
        }
    }
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

