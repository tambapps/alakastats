package com.tambapps.pokemon.alakastats.ui.model

import com.tambapps.pokemon.alakastats.domain.model.GameEndReason
import com.tambapps.pokemon.alakastats.domain.model.Player
import com.tambapps.pokemon.alakastats.domain.model.ReplayAnalytics
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * What a replay's card tells below its title, each when known: the Pokémon left on each side, how the game ended,
 * its length, its date and its place in its series. E.g. "2–0 · 9 turns · Oct 12"
 *
 * @param showScore false when the team's player isn't known, as the score is told from their side
 * @param inSeries shown in its series' card, which tells the date and the series
 */
fun replayDetails(
    replay: ReplayAnalytics,
    youPlayer: Player,
    opponentPlayer: Player,
    showScore: Boolean,
    inSeries: Boolean,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    now: Instant = Clock.System.now(),
): String = buildList {
    val youRemaining = replay.remainingPokemons(youPlayer)
    val opponentRemaining = replay.remainingPokemons(opponentPlayer)
    if (showScore && youRemaining != null && opponentRemaining != null) {
        add("$youRemaining–$opponentRemaining")
    }
    when (replay.endReason) {
        GameEndReason.FORFEIT -> add("Forfeit")
        GameEndReason.TIMER -> add("Timeout")
        null -> {}
    }
    replay.turns?.takeIf { it > 0 }?.let { add(if (it == 1) "1 turn" else "$it turns") }
    if (!inSeries) {
        formatReplayDate(replay.uploadTime, timeZone, now)?.let(::add)
        replay.bestOf?.let { add("Game ${it.game} of ${it.of}") }
    }
}.joinToString(" · ")

/**
 * The showdown replays loaded before the app parsed their turns and KOs: they don't show them until reloaded
 */
val Teamlytics.outdatedReplays: List<ReplayAnalytics>
    get() = replays.filter { it.showdownUrl != null && it.turns == null }

/**
 * E.g. "Oct 12", with the year when not the current one. Null for a replay without upload time
 */
fun formatReplayDate(
    uploadTime: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    now: Instant = Clock.System.now(),
): String? {
    if (uploadTime <= 0) return null
    val date = Instant.fromEpochSeconds(uploadTime).toLocalDateTime(timeZone).date
    val monthAndDay = "${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${date.day}"
    return if (date.year == now.toLocalDateTime(timeZone).year) monthAndDay
    else "$monthAndDay, ${date.year}"
}
