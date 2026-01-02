package com.tambapps.pokemon.alakastats.infrastructure.service

import com.tambapps.pokemon.alakastats.domain.transformer.OtsPokemonTransformer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayAnalyticsBuilderVisitorTest {

    private val otsTransformer = OtsPokemonTransformer()
    private val playerNames = listOf("player1", "player2")

    @Test
    fun testSelectionWithSpecialForm() = runTest {
        val visitor = ReplayAnalyticsBuilderVisitor(otsTransformer, playerNames).apply {
            visitLogs(LOGS_SELECTION_WITH_SPECIAL_FORM)
        }
        val player = visitor.players.last()
        assertEquals(listOf("flutter-mane", "ogerpon-wellspring", "arcanine-hisui", "raging-bolt"), player.selection)
    }
}



private const val LOGS_SELECTION_WITH_SPECIAL_FORM = """
|j|☆tailwinderuption
|j|☆lazarett
|html|<table width="100%"><tr><td align="left">tailwinderuption</td><td align="right">lazarett</tr><tr><td align="left"><i class="fa fa-circle-o"></i> <i class="fa fa-circle-o"></i> </td><td align="right"><i class="fa fa-circle-o"></i> <i class="fa fa-circle"></i> </tr></table>
|uhtml|bestof|<h2><strong>Game 2</strong> of <a href="/game-bestof3-gen9vgc2026regfbo3-2510361883-7dcdp0t9d15hl26dmuxfplbcrdlw5iepw">a best-of-3</a></h2>
|t:|1767308061
|gametype|doubles
|player|p1|tailwinderuption|102|1365
|player|p2|lazarett|riley|1471
|gen|9
|tier|[Gen 9] VGC 2026 Reg F (Bo3)
|rated|
|rule|Species Clause: Limit one of each Pokémon
|rule|Item Clause: Limit 1 of each item
|clearpoke
|poke|p1|Walking Wake, L50|
|poke|p1|Flutter Mane, L50|
|poke|p1|Torkoal, L50, F|
|poke|p1|Incineroar, L50, F|
|poke|p1|Iron Treads, L50|
|poke|p1|Farigiraf, L50, F|
|poke|p2|Flutter Mane, L50|
|poke|p2|Arcanine-Hisui, L50, F|
|poke|p2|Raging Bolt, L50|
|poke|p2|Ogerpon-Wellspring, L50, F|
|poke|p2|Moltres-Galar, L50|
|poke|p2|Sneasler, L50, F|
|teampreview|4
|showteam|p1|Walking Wake||LifeOrb|Protosynthesis|HydroSteam,DracoMeteor,Flamethrower,Protect||||||50|,,,,,Fire]Flutter Mane||ChoiceSpecs|Protosynthesis|Moonblast,ShadowBall,DazzlingGleam,PowerGem||||||50|,,,,,Fairy]Torkoal||HeatRock|Drought|Eruption,HeatWave,HelpingHand,Protect|||F|||50|,,,,,Grass]Incineroar||SafetyGoggles|Intimidate|FakeOut,KnockOff,FlareBlitz,PartingShot|||F|||50|,,,,,Grass]Iron Treads||BoosterEnergy|QuarkDrive|IronHead,HighHorsepower,IceSpinner,Protect||||||50|,,,,,Ghost]Farigiraf||ThroatSpray|ArmorTail|HyperVoice,PsychicNoise,TrickRoom,Protect|||F|||50|,,,,,Fairy
|showteam|p2|Flutter Mane||BoosterEnergy|Protosynthesis|Moonblast,IcyWind,ThunderWave,Taunt||||||50|,,,,,Fairy]Arcanine-Hisui||ChoiceBand|Intimidate|FlareBlitz,RockSlide,HeadSmash,ExtremeSpeed|||F|||50|,,,,,Grass]Raging Bolt||AssaultVest|Protosynthesis|Thunderbolt,DragonPulse,Thunderclap,Electroweb||||||50|,,,,,Electric]Ogerpon-Wellspring||WellspringMask|WaterAbsorb|IvyCudgel,HornLeech,FollowMe,SpikyShield|||F|||50|,,,,,Water]Moltres-Galar||ChoiceSpecs|Berserk|FieryWrath,AirSlash,Snarl,Hex||||||50|,,,,,Ghost]Sneasler||WhiteHerb|Unburden|CloseCombat,GunkShot,FakeOut,Protect|||F|||50|,,,,,Stellar
|inactive|Battle timer is ON: inactive players will automatically lose when time's up. (requested by tailwinderuption)
|inactive|tailwinderuption has 60 seconds left.
|inactive|lazarett has 60 seconds left.
|
|t:|1767308118
|teamsize|p1|4
|teamsize|p2|4
|start
|switch|p1a: Iron Treads|Iron Treads, L50|100/100
|switch|p1b: Incineroar|Incineroar, L50, F|100/100
|switch|p2a: Flutter Mane|Flutter Mane, L50|100/100
|switch|p2b: Ogerpon|Ogerpon-Wellspring, L50, F|100/100
|-ability|p1b: Incineroar|Intimidate|boost
|-unboost|p2a: Flutter Mane|atk|1
|-unboost|p2b: Ogerpon|atk|1
|-enditem|p1a: Iron Treads|Booster Energy
|-activate|p1a: Iron Treads|ability: Quark Drive|[fromitem]
|-start|p1a: Iron Treads|quarkdrivespe
|-enditem|p2a: Flutter Mane|Booster Energy
|-activate|p2a: Flutter Mane|ability: Protosynthesis|[fromitem]
|-start|p2a: Flutter Mane|protosynthesisspe
|turn|1
|inactive|lazarett has 30 seconds left.
|inactive|tailwinderuption has 30 seconds left.
|inactive|tailwinderuption has 20 seconds left.
|
|t:|1767308163
|switch|p2b: Arcanine|Arcanine-Hisui, L50, F|100/100
|-ability|p2b: Arcanine|Intimidate|boost
|-unboost|p1a: Iron Treads|atk|1
|-unboost|p1b: Incineroar|atk|1
|move|p1b: Incineroar|Fake Out|p2b: Arcanine
|-resisted|p2b: Arcanine
|-crit|p2b: Arcanine
|-damage|p2b: Arcanine|91/100
|move|p1a: Iron Treads|High Horsepower|p2a: Flutter Mane
|-damage|p2a: Flutter Mane|64/100
|move|p2a: Flutter Mane|Icy Wind|p1b: Incineroar|[spread] p1a,p1b
|-resisted|p1b: Incineroar
|-damage|p1a: Iron Treads|83/100
|-damage|p1b: Incineroar|95/100
|-unboost|p1a: Iron Treads|spe|1
|-unboost|p1b: Incineroar|spe|1
|
|upkeep
|turn|2
|inactive|tailwinderuption has 30 seconds left.
|inactive|lazarett has 30 seconds left.
|inactive|tailwinderuption has 20 seconds left.
|inactive|tailwinderuption has 15 seconds left.
|inactive|tailwinderuption has 10 seconds left.
|
|t:|1767308209
|-terastallize|p1b: Incineroar|Grass
|move|p2a: Flutter Mane|Moonblast|p1b: Incineroar
|-damage|p1b: Incineroar|57/100
|move|p1a: Iron Treads|High Horsepower|p2b: Arcanine
|-supereffective|p2b: Arcanine
|-damage|p2b: Arcanine|0 fnt
|faint|p2b: Arcanine
|move|p1b: Incineroar|Parting Shot|p2a: Flutter Mane
|-unboost|p2a: Flutter Mane|atk|1
|-unboost|p2a: Flutter Mane|spa|1
|
|t:|1767308223
|switch|p1b: Flutter Mane|Flutter Mane, L50|100/100|[from] Parting Shot
|
|upkeep
|
|t:|1767308228
|switch|p2b: Ogerpon|Ogerpon-Wellspring, L50, F|100/100
|turn|3
|
|t:|1767308247
|-end|p1b: Flutter Mane|Protosynthesis|[silent]
|switch|p1b: Incineroar|Incineroar, L50, F, tera:Grass|57/100
|-ability|p1b: Incineroar|Intimidate|boost
|-unboost|p2a: Flutter Mane|atk|1
|-unboost|p2b: Ogerpon|atk|1
|-terastallize|p2b: Ogerpon|Water
|detailschange|p2b: Ogerpon|Ogerpon-Wellspring-Tera, L50, F, tera:Water
|-ability|p2b: Ogerpon|Embody Aspect (Wellspring)|boost
|-boost|p2b: Ogerpon|spd|1
|move|p1a: Iron Treads|Protect|p1a: Iron Treads
|-singleturn|p1a: Iron Treads|Protect
|move|p2b: Ogerpon|Follow Me|p2b: Ogerpon
|-singleturn|p2b: Ogerpon|move: Follow Me
|move|p2a: Flutter Mane|Thunder Wave|p1b: Incineroar
|-status|p1b: Incineroar|par
|
|upkeep
|turn|4
|
|t:|1767308268
|move|p1b: Incineroar|Fake Out|p2b: Ogerpon
|-damage|p2b: Ogerpon|92/100
|move|p2a: Flutter Mane|Icy Wind|p1a: Iron Treads|[spread] p1a,p1b
|-supereffective|p1b: Incineroar
|-damage|p1a: Iron Treads|71/100
|-damage|p1b: Incineroar|44/100 par
|-unboost|p1a: Iron Treads|spe|1
|-unboost|p1b: Incineroar|spe|1
|cant|p2b: Ogerpon|flinch
|move|p1a: Iron Treads|Iron Head|p2a: Flutter Mane
|-supereffective|p2a: Flutter Mane
|-damage|p2a: Flutter Mane|4/100
|
|upkeep
|turn|5
|inactive|tailwinderuption has 30 seconds left.
|inactive|tailwinderuption has 20 seconds left.
|inactive|tailwinderuption has 15 seconds left.
|
|t:|1767308310
|switch|p1b: Farigiraf|Farigiraf, L50, F|100/100
|move|p2a: Flutter Mane|Moonblast|p1b: Farigiraf
|-damage|p1b: Farigiraf|75/100
|-unboost|p1b: Farigiraf|spa|1
|move|p2b: Ogerpon|Ivy Cudgel|p1a: Iron Treads|[anim] Ivy Cudgel Water
|-supereffective|p1a: Iron Treads
|-damage|p1a: Iron Treads|0 fnt
|faint|p1a: Iron Treads
|-end|p1a: Iron Treads|Quark Drive|[silent]
|
|upkeep
|
|t:|1767308320
|switch|p1a: Incineroar|Incineroar, L50, F, tera:Grass|44/100 par
|-ability|p1a: Incineroar|Intimidate|boost
|-unboost|p2a: Flutter Mane|atk|1
|-unboost|p2b: Ogerpon|atk|1
|turn|6
|
|t:|1767308338
|switch|p2b: Raging Bolt|Raging Bolt, L50|100/100
|move|p2a: Flutter Mane|Taunt|p1b: Farigiraf
|-start|p1b: Farigiraf|move: Taunt
|move|p1b: Farigiraf|Psychic Noise|p2a: Flutter Mane
|-damage|p2a: Flutter Mane|0 fnt
|faint|p2a: Flutter Mane
|-end|p2a: Flutter Mane|Protosynthesis|[silent]
|-enditem|p1b: Farigiraf|Throat Spray
|-boost|p1b: Farigiraf|spa|1|[from] item: Throat Spray
|move|p1a: Incineroar|Knock Off|p2b: Raging Bolt
|-damage|p2b: Raging Bolt|71/100
|-enditem|p2b: Raging Bolt|Assault Vest|[from] move: Knock Off|[of] p1a: Incineroar
|
|upkeep
|
|t:|1767308353
|switch|p2a: Ogerpon|Ogerpon-Wellspring-Tera, L50, F, tera:Water|92/100
|-ability|p2a: Ogerpon|Embody Aspect (Wellspring)|boost
|-boost|p2a: Ogerpon|spd|1
|turn|7
|inactive|lazarett has 30 seconds left.
|inactive|lazarett has 20 seconds left.
|inactive|lazarett has 15 seconds left.
|inactive|lazarett has 10 seconds left.
|
|t:|1767308399
|move|p2a: Ogerpon|Ivy Cudgel|p1a: Incineroar|[anim] Ivy Cudgel Water
|-resisted|p1a: Incineroar
|-damage|p1a: Incineroar|22/100 par
|move|p2b: Raging Bolt|Thunderbolt|p1a: Incineroar
|-resisted|p1a: Incineroar
|-damage|p1a: Incineroar|1/100 par
|move|p1b: Farigiraf|Hyper Voice|p2a: Ogerpon|[spread] p2a,p2b
|-damage|p2a: Ogerpon|74/100
|-damage|p2b: Raging Bolt|46/100
|move|p1a: Incineroar|Parting Shot|p2a: Ogerpon
|-unboost|p2a: Ogerpon|atk|1
|-unboost|p2a: Ogerpon|spa|1
|
|t:|1767308410
|switch|p1a: Flutter Mane|Flutter Mane, L50|100/100|[from] Parting Shot
|
|upkeep
|turn|8
|
|t:|1767308424
|switch|p1b: Incineroar|Incineroar, L50, F, tera:Grass|1/100 par
|-ability|p1b: Incineroar|Intimidate|boost
|-unboost|p2a: Ogerpon|atk|1
|-unboost|p2b: Raging Bolt|atk|1
|move|p2b: Raging Bolt|Thunderclap|p1a: Flutter Mane
|-damage|p1a: Flutter Mane|62/100
|move|p1a: Flutter Mane|Dazzling Gleam|p2a: Ogerpon|[spread] p2a,p2b
|-supereffective|p2b: Raging Bolt
|-damage|p2a: Ogerpon|45/100
|-damage|p2b: Raging Bolt|0 fnt
|faint|p2b: Raging Bolt
|-end|p2b: Raging Bolt|Protosynthesis|[silent]
|move|p2a: Ogerpon|Ivy Cudgel|p1a: Flutter Mane|[anim] Ivy Cudgel Water
|-crit|p1a: Flutter Mane
|-damage|p1a: Flutter Mane|0 fnt
|faint|p1a: Flutter Mane
|-end|p1a: Flutter Mane|Protosynthesis|[silent]
|
|upkeep
|
|t:|1767308438
|switch|p1a: Farigiraf|Farigiraf, L50, F|75/100
|turn|9
|
|t:|1767308458
|move|p2a: Ogerpon|Horn Leech|p1b: Incineroar
|-resisted|p1b: Incineroar
|-damage|p1b: Incineroar|0 fnt
|-heal|p2a: Ogerpon|46/100|[from] drain|[of] p1b: Incineroar
|faint|p1b: Incineroar
|move|p1a: Farigiraf|Hyper Voice|p2a: Ogerpon
|-damage|p2a: Ogerpon|19/100
|
|upkeep
|turn|10
|
|t:|1767308466
|move|p2a: Ogerpon|Horn Leech|p1a: Farigiraf
|-damage|p1a: Farigiraf|57/100
|-heal|p2a: Ogerpon|29/100|[from] drain|[of] p1a: Farigiraf
|move|p1a: Farigiraf|Psychic Noise|p2a: Ogerpon
|-damage|p2a: Ogerpon|8/100
|-start|p2a: Ogerpon|move: Heal Block
|
|upkeep
|turn|11
|
|t:|1767308476
|move|p2a: Ogerpon|Ivy Cudgel|p1a: Farigiraf|[anim] Ivy Cudgel Water
|-damage|p1a: Farigiraf|22/100
|move|p1a: Farigiraf|Hyper Voice|p2a: Ogerpon
|-damage|p2a: Ogerpon|0 fnt
|faint|p2a: Ogerpon
|detailschange|p2: Ogerpon|Ogerpon-Wellspring, L50, F|[silent]
|
|win|tailwinderuption
||lazarett is ready for game 3.
||tailwinderuption is ready for game 3.
|uhtml|next|Next: <a href="/battle-gen9vgc2026regfbo3-2510367203-3esia59724qt9tq3i3cohfu2t6lh2nepw"><strong>Game 3 of 3</strong></a>
|l|☆lazarett
|player|p2|
"""