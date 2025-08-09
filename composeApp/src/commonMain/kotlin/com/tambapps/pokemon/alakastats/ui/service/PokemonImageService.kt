package com.tambapps.pokemon.alakastats.ui.service

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.catching_pokemon
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource

class PokemonImageService(
    private val json: Json
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val sprites = mutableStateMapOf<String, PokemonSpriteData>()

    init {
        coroutineScope.launch {
            sprites.putAll(readMappingFile("pokemon-sprites.json"))
        }
    }

    private suspend inline fun <reified T> readMappingFile(filename: String): T {
        val text = Res.readBytes("files/$filename").decodeToString()
        return json.decodeFromString<T>(text)
    }

    @Composable
    fun PokemonSprite(name: String, modifier: Modifier) {
        val pokemonSpriteData = sprites[name]
        if (pokemonSpriteData != null) {
            KamelImage({ asyncPainterResource(data = pokemonSpriteData.sprite) },
                contentDescription = pokemonSpriteData.name,
                modifier = modifier
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.catching_pokemon),
                contentDescription = name,
                modifier = modifier
            )
        }
    }
}


@Serializable
data class PokemonSpriteData(
    val name: String,
    val sprite: String,
    val artwork: String
)

