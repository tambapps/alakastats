package com.tambapps.pokemon.alakastats.ui.screen.showdownreplays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.ItemName
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.GameOutcome
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplayPlayer
import com.tambapps.pokemon.alakastats.domain.model.ShowdownReplaySummary
import com.tambapps.pokemon.alakastats.domain.model.UserName
import com.tambapps.pokemon.alakastats.ui.composables.ExpansionTile
import com.tambapps.pokemon.alakastats.ui.composables.GameOutcomeCard
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.WIN_COLOR
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.composables.withMegaForms
import com.tambapps.pokemon.alakastats.ui.model.ShowdownReplayListItem
import com.tambapps.pokemon.alakastats.ui.model.formatReplayDate
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.ItemImage
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.alakastats.ui.theme.LocalIsCompact

// on desktop, a team is a row in its player's half
private val DESKTOP_TEAM_SPRITE_SIZE = 64.dp
// on a phone, a team is 3 by 2 under its player, for both to fit side by side
private const val MOBILE_TEAM_COLUMNS = 3
private val MOBILE_TEAM_SPRITE_SIZE = 48.dp
// a game's sprites take the room of their side, up to this size
private val MAX_GAME_SPRITE_SIZE = 56.dp
// a side of a game has room for the 4 Pokémon brought in VGC
private const val GAME_SIDE_SLOTS = 4
// the Pokémon of a team that weren't brought, faded for the brought ones to stand out
private const val NOT_BROUGHT_ALPHA = 0.35f
// under a lead's sprite
private val LEAD_MARK_WIDTH = 16.dp
private val LEAD_MARK_HEIGHT = 3.dp
// the loser's side, a bit faded: still readable
private const val LOSER_ALPHA = 0.8f
// the label of a game in its series, e.g. "G2"
private val GAME_LABEL_WIDTH = 36.dp

/**
 * A replay that isn't in a series, or not grouped with it: the players face each other, each with their team, what
 * they didn't bring faded and their leads marked
 */
@Composable
internal fun ShowdownReplayCard(
    replay: ShowdownReplaySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MyCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailsLine(
                formatReplayDate(replay.uploadTime),
                replay.rating?.let { "Rating $it" },
                replay.bestOf?.let { "Game ${it.game} of ${it.of}" },
            )
            PlayersHeader(replay.players, between = "vs", winner = replay.winner)
            Teams(replay.players, markSelection = true)
        }
    }
}

/**
 * Closed, the series takes the place of a replay: its players with its score between them, and their teams. Opened,
 * its games, a line each: the Pokémon each player brought, the winner's side standing out
 */
@Composable
internal fun ShowdownReplaySeriesCard(
    series: ShowdownReplayListItem.Series,
    onGameClick: (ShowdownReplaySummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCompact = LocalIsCompact.current
    ExpansionTile(
        modifier = modifier,
        // beside the expand button
        title = {
            // its games are played the same day: they don't repeat it
            val date = series.games.map { it.uploadTime }.filter { it > 0 }.minOrNull()?.let { formatReplayDate(it) }
            DetailsLine("Best-of-${series.bestOf}", "incomplete".takeIf { series.missingGames }, date)
        },
        // below it, on the whole width as the games, for the players and their teams to be centered as them
        subtitle = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayersHeader(series.players, between = series.wins.joinToString("–"), winner = series.winner)
                Teams(series.players)
            }
        },
        menu = null,
        gradientBackgroundColors = cardGradientColors,
        // its games have one, telling them apart
        bordered = false,
        // on desktop, a click between its games mustn't close it. On mobile, a tap anywhere does
        disableWhenOpened = !isCompact,
    ) {
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (game in series.games) {
                SeriesGameRow(game, series.players, onClick = { onGameClick(game) })
            }
            if (series.hiddenGames > 0) {
                Text(
                    text = if (series.hiddenGames == 1) "1 game hidden by the filters" else "${series.hiddenGames} games hidden by the filters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** E.g. "Nov 9 · Rating 1429", without what isn't known */
@Composable
private fun DetailsLine(vararg details: String?) {
    Text(
        details.filterNotNull().joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.alpha(0.8f),
    )
}

/**
 * The players at both ends, facing each other, with e.g. the score between them. A W tells the winner
 */
@Composable
private fun PlayersHeader(players: List<ShowdownReplayPlayer>, between: String, winner: UserName?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        players.getOrNull(0)?.let { PlayerName(it, hasWon = winner?.matches(it.name) == true, atEnd = false, Modifier.weight(1f)) }
        Text(
            between,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        players.getOrNull(1)?.let { PlayerName(it, hasWon = winner?.matches(it.name) == true, atEnd = true, Modifier.weight(1f)) }
    }
}

/** @param atEnd for the player on the right: aligned on the right, the W on the outside */
@Composable
private fun PlayerName(player: ShowdownReplayPlayer, hasWon: Boolean, atEnd: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = if (atEnd) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasWon && !atEnd) {
                GameOutcomeCard(GameOutcome.WIN, small = true)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                player.name.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (hasWon) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (hasWon && atEnd) {
                Spacer(Modifier.width(6.dp))
                GameOutcomeCard(GameOutcome.WIN, small = true)
            }
        }
        player.beforeElo?.let { elo ->
            Text("$elo", style = MaterialTheme.typography.labelMedium, modifier = Modifier.alpha(0.7f))
        }
    }
}

// the players' teams face each other, the first one being on the left
private fun facingDirectionOf(playerIndex: Int) = if (playerIndex == 0) FacingDirection.RIGHT else FacingDirection.LEFT

/**
 * Each player's team under their name: in a row on desktop, with their items. On a phone, 3 by 2, without items
 *
 * @param markSelection whether the Pokémon a player didn't bring are faded and their leads marked, e.g. for a single
 * game: a series' games don't all bring the same ones
 */
@Composable
private fun Teams(players: List<ShowdownReplayPlayer>, markSelection: Boolean = false) {
    val isCompact = LocalIsCompact.current
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        players.forEachIndexed { index, player ->
            TeamView(
                player = player,
                facingDirection = facingDirectionOf(index),
                // e.g. none brought in a game forfeited at the team preview
                markSelection = markSelection && player.brought.isNotEmpty(),
                isCompact = isCompact,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TeamView(
    player: ShowdownReplayPlayer,
    facingDirection: FacingDirection,
    markSelection: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    // the Mega Evolutions' sprites, the items on desktop only
    val shown = withMegaForms(player.team, player.items)
    val slots = shown.mapIndexed { index, (name, item) ->
        val teamPokemon = player.team[index]
        TeamSlot(
            name = name,
            item = item.takeIf { !isCompact },
            faded = markSelection && player.brought.none { it.baseMatches(teamPokemon) },
            lead = markSelection && player.lead.any { it.baseMatches(teamPokemon) },
        )
    }
    if (isCompact) {
        // centered under its player's half
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            for (line in slots.chunked(MOBILE_TEAM_COLUMNS)) {
                Row {
                    for (slot in line) {
                        TeamSlotView(slot, MOBILE_TEAM_SPRITE_SIZE, facingDirection, markSelection)
                    }
                }
            }
        }
    } else {
        Row(modifier) {
            for (slot in slots) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TeamSlotView(slot, DESKTOP_TEAM_SPRITE_SIZE, facingDirection, markSelection)
                }
            }
        }
    }
}

private data class TeamSlot(val name: PokemonName, val item: ItemName?, val faded: Boolean, val lead: Boolean)

/** @param markSelection whether there's room below the sprite for the lead's mark, for all the sprites to be aligned */
@Composable
private fun TeamSlotView(slot: TeamSlot, size: Dp, facingDirection: FacingDirection, markSelection: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size).alpha(if (slot.faded) NOT_BROUGHT_ALPHA else 1f)) {
            PokemonSprite(slot.name, Modifier.fillMaxSize(), facingDirection = facingDirection)
            slot.item?.let { item ->
                // overlapping the sprite a bit
                ItemImage(item, Modifier.align(Alignment.BottomEnd).fillMaxSize(0.4f))
            }
        }
        if (markSelection) {
            Box(
                Modifier.padding(top = 2.dp)
                    .size(width = LEAD_MARK_WIDTH, height = LEAD_MARK_HEIGHT)
                    .clip(RoundedCornerShape(2.dp))
                    .then(if (slot.lead) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier)
            )
        }
    }
}

/**
 * A game of a series in one line: its number, then what each player brought, their leads in the middle
 * facing each other. The winner's side is highlighted, the other one dimmed
 *
 * @param seriesPlayers the players in the order of the series' card: a game may list them the other way around
 */
@Composable
private fun SeriesGameRow(game: ShowdownReplaySummary, seriesPlayers: List<ShowdownReplayPlayer>, onClick: () -> Unit) {
    // on a phone, the margins go to the sprites, small enough already
    val isCompact = LocalIsCompact.current
    val sides = seriesPlayers.map { seriesPlayer -> game.players.find { it.name.matches(seriesPlayer.name) } ?: seriesPlayer }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = if (isCompact) 8.dp else 16.dp, vertical = if (isCompact) 6.dp else 8.dp),
    ) {
        // the players' Elo is in the series' header
        Text(
            "G${game.bestOf?.game ?: "?"}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(min = GAME_LABEL_WIDTH),
        )
        sides.forEachIndexed { index, player ->
            // the leads of both sides apart
            if (index == 1) Spacer(Modifier.width(if (isCompact) 8.dp else 16.dp))
            val hasWon = game.winner?.matches(player.name) == true
            GameSide(
                player = player,
                // the leads in the middle: on the right for the first player, on the left for the second
                atEnd = index == 0,
                hasWon = hasWon,
                dimmed = game.winner != null && !hasWon,
                facingDirection = facingDirectionOf(index),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GameSide(
    player: ShowdownReplayPlayer,
    atEnd: Boolean,
    hasWon: Boolean,
    dimmed: Boolean,
    facingDirection: FacingDirection,
    modifier: Modifier = Modifier,
) {
    // e.g. none brought in a game forfeited at the team preview
    val brought = player.brought.ifEmpty { player.team }
    val broughtItems = if (player.brought.isEmpty()) player.items else player.broughtItems
    // the Mega Evolutions' sprites, without the items
    val selection = withMegaForms(brought, broughtItems).map { it.first }
    val ordered = if (atEnd) selection.asReversed() else selection
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (hasWon) Modifier.background(WIN_COLOR.copy(alpha = 0.18f)) else Modifier)
            .alpha(if (dimmed) LOSER_ALPHA else 1f)
            // the sprites off the edges of the winner's highlight, on desktop
            .then(if (LocalIsCompact.current) Modifier else Modifier.padding(horizontal = 8.dp, vertical = 4.dp)),
        contentAlignment = if (atEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        val spriteSize = minOf(maxWidth / GAME_SIDE_SLOTS, MAX_GAME_SPRITE_SIZE)
        Row {
            for (pokemon in ordered) {
                PokemonSprite(pokemon, Modifier.size(spriteSize), facingDirection = facingDirection)
            }
        }
    }
}

