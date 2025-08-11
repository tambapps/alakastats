package com.tambapps.pokemon.alakastats.util

object PokemonNormalizer {
    
    private class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var value: String? = null
    }
    
    private class PrefixTrie {
        private val root = TrieNode()
        
        fun insert(prefix: String, value: String) {
            var current = root
            for (char in prefix) {
                current = current.children.getOrPut(char) { TrieNode() }
            }
            current.value = value
        }
        
        fun findLongestPrefixMatch(input: String): String? {
            var current = root
            var lastMatch: String? = null
            
            for (char in input) {
                current = current.children[char] ?: break
                if (current.value != null) {
                    lastMatch = current.value
                }
            }
            
            return lastMatch
        }
    }
    
    // Initialize the prefix trie with Pokemon forms that need prefix matching
    private val prefixTrie = PrefixTrie().apply {
        insert("urshifu", "urshifu")
        insert("ogerpon", "ogerpon") 
        insert("ursaluna", "ursaluna")
        insert("rotom", "rotom")
        insert("terapagos", "terapagos")
        insert("zamazenta", "zamazenta")
        insert("zacian", "zacian")
        insert("necrozma", "necrozma")
        insert("calyrex", "calyrex")
        insert("kyurem", "kyurem")
    }
    
    fun normalize(s: String): String = s.lowercase().replace(' ', '-')
    
    fun normalizeToBase(input: String): String {
        val s = normalize(input)
        
        // First, try prefix matching using the trie (O(m) where m is input length)
        val prefixMatch = prefixTrie.findLongestPrefixMatch(s)
        if (prefixMatch != null) {
            return prefixMatch
        }
        
        // Then handle suffix patterns (still O(1) constant time checks)
        return when {
            !s.contains("-") -> s
            s.endsWith("-galar") -> s.substring(0, s.length - 6)
            s.endsWith("-alola") -> s.substring(0, s.length - 6)
            s.endsWith("-incarnate") -> s.substring(0, s.length - 10)
            s.contains("-paldea") -> {
                // because of Tauros-Paldea-Aqua
                s.substring(0, s.indexOf("-paldea"))
            }
            else -> s
        }
    }
}

