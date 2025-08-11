package com.tambapps.pokemon.alakastats.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PokemonNormalizerTest {

    @Test
    fun testNormalize() {
        // Test lowercase conversion
        assertEquals("pikachu", PokemonNormalizer.normalize("Pikachu"))
        assertEquals("pikachu", PokemonNormalizer.normalize("PIKACHU"))
        assertEquals("pikachu", PokemonNormalizer.normalize("pikachu"))
        
        // Test space replacement with dashes
        assertEquals("tapu-koko", PokemonNormalizer.normalize("Tapu Koko"))
        assertEquals("tapu-lele", PokemonNormalizer.normalize("TAPU LELE"))
        assertEquals("mr-mime", PokemonNormalizer.normalize("Mr Mime"))
        
        // Test combined transformations
        assertEquals("ho-oh", PokemonNormalizer.normalize("Ho Oh"))
        assertEquals("porygon-z", PokemonNormalizer.normalize("Porygon Z"))
        
        // Test edge cases
        assertEquals("", PokemonNormalizer.normalize(""))
        assertEquals("a", PokemonNormalizer.normalize("A"))
        assertEquals("-", PokemonNormalizer.normalize(" "))
    }

    @Test
    fun testNormalizeToBase_PrefixMatching() {
        // Test Urshifu forms - should all return "urshifu"
        assertEquals("urshifu", PokemonNormalizer.normalizeToBase("Urshifu"))
        assertEquals("urshifu", PokemonNormalizer.normalizeToBase("Urshifu-Rapid-Strike"))
        assertEquals("urshifu", PokemonNormalizer.normalizeToBase("Urshifu-Single-Strike"))
        assertEquals("urshifu", PokemonNormalizer.normalizeToBase("URSHIFU-GMAX"))

        // Test Ogerpon forms - should all return "ogerpon"
        assertEquals("ogerpon", PokemonNormalizer.normalizeToBase("Ogerpon"))
        assertEquals("ogerpon", PokemonNormalizer.normalizeToBase("Ogerpon-Hearthflame"))
        assertEquals("ogerpon", PokemonNormalizer.normalizeToBase("Ogerpon-Wellspring"))
        assertEquals("ogerpon", PokemonNormalizer.normalizeToBase("Ogerpon-Cornerstone"))

        // Test Ursaluna forms - should all return "ursaluna"
        assertEquals("ursaluna", PokemonNormalizer.normalizeToBase("Ursaluna"))
        assertEquals("ursaluna", PokemonNormalizer.normalizeToBase("Ursaluna-Bloodmoon"))

        // Test Rotom forms - should all return "rotom"
        assertEquals("rotom", PokemonNormalizer.normalizeToBase("Rotom"))
        assertEquals("rotom", PokemonNormalizer.normalizeToBase("Rotom-Heat"))
        assertEquals("rotom", PokemonNormalizer.normalizeToBase("Rotom-Wash"))
        assertEquals("rotom", PokemonNormalizer.normalizeToBase("Rotom-Frost"))
        assertEquals("rotom", PokemonNormalizer.normalizeToBase("Rotom-Fan"))
        assertEquals("rotom", PokemonNormalizer.normalizeToBase("Rotom-Mow"))

        // Test Legendary forms
        assertEquals("terapagos", PokemonNormalizer.normalizeToBase("Terapagos-Stellar"))
        assertEquals("zamazenta", PokemonNormalizer.normalizeToBase("Zamazenta-Crowned"))
        assertEquals("zacian", PokemonNormalizer.normalizeToBase("Zacian-Crowned"))
        assertEquals("necrozma", PokemonNormalizer.normalizeToBase("Necrozma-Dusk-Mane"))
        assertEquals("calyrex", PokemonNormalizer.normalizeToBase("Calyrex-Ice-Rider"))
        assertEquals("kyurem", PokemonNormalizer.normalizeToBase("Kyurem-Black"))
    }

    @Test
    fun testNormalizeToBase_SuffixMatching() {
        // Test Galar forms
        assertEquals("ponyta", PokemonNormalizer.normalizeToBase("Ponyta-Galar"))
        assertEquals("rapidash", PokemonNormalizer.normalizeToBase("Rapidash-Galar"))
        assertEquals("weezing", PokemonNormalizer.normalizeToBase("Weezing-Galar"))

        // Test Alola forms
        assertEquals("raichu", PokemonNormalizer.normalizeToBase("Raichu-Alola"))
        assertEquals("sandshrew", PokemonNormalizer.normalizeToBase("Sandshrew-Alola"))
        assertEquals("vulpix", PokemonNormalizer.normalizeToBase("Vulpix-Alola"))

        // Test Paldea forms (special case - stops at first "-paldea")
        assertEquals("tauros", PokemonNormalizer.normalizeToBase("Tauros-Paldea-Combat"))
        assertEquals("tauros", PokemonNormalizer.normalizeToBase("Tauros-Paldea-Blaze"))
        assertEquals("tauros", PokemonNormalizer.normalizeToBase("Tauros-Paldea-Aqua"))

        // Test Incarnate forms
        assertEquals("tornadus", PokemonNormalizer.normalizeToBase("Tornadus-Incarnate"))
        assertEquals("thundurus", PokemonNormalizer.normalizeToBase("Thundurus-Incarnate"))
        assertEquals("landorus", PokemonNormalizer.normalizeToBase("Landorus-Incarnate"))
    }

    @Test
    fun testNormalizeToBase_NoMatching() {
        // Test regular Pokemon that shouldn't be transformed
        assertEquals("pikachu", PokemonNormalizer.normalizeToBase("Pikachu"))
        assertEquals("charizard", PokemonNormalizer.normalizeToBase("Charizard"))
        assertEquals("blastoise", PokemonNormalizer.normalizeToBase("Blastoise"))
        assertEquals("venusaur", PokemonNormalizer.normalizeToBase("Venusaur"))
        
        // Test Pokemon with forms that don't match our patterns
        assertEquals("deoxys-attack", PokemonNormalizer.normalizeToBase("Deoxys-Attack"))
        assertEquals("shaymin-sky", PokemonNormalizer.normalizeToBase("Shaymin-Sky"))
        assertEquals("giratina-origin", PokemonNormalizer.normalizeToBase("Giratina-Origin"))
    }

    @Test
    fun testNormalizeToBase_EdgeCases() {
        // Test empty string
        assertEquals("", PokemonNormalizer.normalizeToBase(""))
        
        // Test single character
        assertEquals("a", PokemonNormalizer.normalizeToBase("a"))
        
        // Test strings that start with prefix but are actually different
        assertEquals("rotate", PokemonNormalizer.normalizeToBase("Rotate"))
        
        // Test case sensitivity
        assertEquals("urshifu", PokemonNormalizer.normalizeToBase("URSHIFU-RAPID-STRIKE"))
        assertEquals("pikachu", PokemonNormalizer.normalizeToBase("PIKACHU-GALAR")) // No such form exists
    }

    @Test
    fun testNormalizeToBase_CombinedTransformations() {
        // Test that normalization (spaces to dashes, lowercase) works with base conversion
        assertEquals("urshifu", PokemonNormalizer.normalizeToBase("Urshifu Rapid Strike"))
        assertEquals("tapu-koko", PokemonNormalizer.normalizeToBase("Tapu Koko"))
        assertEquals("ponyta", PokemonNormalizer.normalizeToBase("Ponyta Galar")) // "Ponyta Galar" → "ponyta-galar" → "ponyta"
    }

    @Test
    fun testNormalizeToBase_LongestPrefixMatch() {
        // Test that the trie returns the longest matching prefix
        // If we had both "ur" and "urshifu" in the trie, it should return "urshifu"
        assertEquals("urshifu", PokemonNormalizer.normalizeToBase("Urshifu-Something-Else"))
        assertEquals("ursaluna", PokemonNormalizer.normalizeToBase("Ursaluna-Something-Else"))
        
        // Test that shorter prefixes don't interfere
        assertEquals("rotom", PokemonNormalizer.normalizeToBase("Rotom-Heat"))
        assertEquals("rotom", PokemonNormalizer.normalizeToBase("Rotom-Something-New"))
    }

    @Test
    fun testNormalizeToBase_PriorityOrder() {
        // Test that prefix matching takes priority over suffix matching
        // A Pokemon like "Urshifu-Galar" should match prefix "urshifu", not suffix "-galar"
        assertEquals("urshifu", PokemonNormalizer.normalizeToBase("Urshifu-Galar"))
        assertEquals("rotom", PokemonNormalizer.normalizeToBase("Rotom-Alola"))
        
        // But if no prefix matches, suffix should work
        assertEquals("pikachu", PokemonNormalizer.normalizeToBase("Pikachu-Galar"))
    }
}