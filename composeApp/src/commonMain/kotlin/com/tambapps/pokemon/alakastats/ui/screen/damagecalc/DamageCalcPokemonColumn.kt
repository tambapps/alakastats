package com.tambapps.pokemon.alakastats.ui.screen.damagecalc

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.content_paste
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tambapps.pokemon.Pokemon
import com.tambapps.pokemon.PokemonName
import com.tambapps.pokemon.alakastats.domain.model.Teamlytics
import com.tambapps.pokemon.alakastats.ui.composables.DropdownField
import com.tambapps.pokemon.alakastats.ui.composables.MegaSwitch
import com.tambapps.pokemon.alakastats.ui.composables.MyCard
import com.tambapps.pokemon.alakastats.ui.composables.SelectPokemonDialog
import com.tambapps.pokemon.alakastats.ui.composables.SuggestionTextField
import com.tambapps.pokemon.alakastats.ui.composables.cardGradientColors
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.service.MoveTypeImage
import com.tambapps.pokemon.alakastats.ui.service.PokemonSprite
import com.tambapps.pokemon.champions.data.Ability
import com.tambapps.pokemon.champions.data.ChampionsDex
import com.tambapps.pokemon.champions.engine.Battlefield
import com.tambapps.pokemon.champions.engine.Status
import com.tambapps.pokemon.champions.engine.returnsDefenderMove
import org.jetbrains.compose.resources.painterResource


private val TYPE_TINT_LENGTH = 320.dp

/**
 * A pokemon of the desktop layout's damage calc, with text fields suggesting values instead of dialogs
 *
 * @param opponent the other pokemon of the calc, whose moves Counter-like moves return
 * @param field the field of the calc, for the final speed
 */
@Composable
internal fun DamageCalcPokemonColumn(
    state: DamageCalcPokemonState,
    side: DamageCalcSide,
    team: Teamlytics?,
    opponent: DamageCalcPokemonState,
    field: Battlefield,
    modifier: Modifier = Modifier,
) {
    var showPasteDialog by remember { mutableStateOf(false) }
    MyCard(modifier = modifier, gradientBackgroundColors = cardGradientColors) {
        Column(
            Modifier.typeTint(state, side, diagonalLength = TYPE_TINT_LENGTH).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // the defender's mirrors the attacker's, the sprite on the right
            val isAttacker = side == DamageCalcSide.ATTACKER
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isAttacker) Arrangement.Start else Arrangement.End,
            ) {
                val sprite = @Composable {
                    // the attacker faces right, towards its target
                    DamageCalcPokemonSprite(
                        state,
                        facingDirection = if (isAttacker) FacingDirection.RIGHT else FacingDirection.LEFT,
                        modifier = Modifier.size(80.dp),
                    )
                }
                // a pokemon holding its mega stone has a mega switch instead of a form selection, in the top corner:
                // right for the attacker, left for the defender
                val megaSwitch = @Composable {
                    if (state.megaFormOfItem != null) {
                        Box(Modifier.align(Alignment.Top)) {
                            MegaSwitch(state.isMegaEvolved, onCheckedChange = { state.setMegaEvolved(it) })
                        }
                    }
                }
                if (isAttacker) {
                    sprite()
                    Spacer(Modifier.width(8.dp))
                } else {
                    megaSwitch()
                    Spacer(Modifier.weight(1f))
                }
                PokemonHeaderTitle(state, side)
                if (!isAttacker) {
                    Spacer(Modifier.width(8.dp))
                    sprite()
                } else {
                    Spacer(Modifier.weight(1f))
                    megaSwitch()
                }
            }
            // the team next to the set's fields, to switch between its pokemon in one click. On the side of the field
            // (right of the attacker, left of the defender), mirrored like the header. The stats and the HP are next to
            // it too, the team being taller than the set's fields alone
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (team != null && !isAttacker) {
                    TeamPokemonButtons(team, state, side)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SetFields(state, onPasteClick = { showPasteDialog = true })
                    // separating the stats from the set's fields
                    Spacer(Modifier.height(8.dp))
                    StatPointsHeader(state)
                    val finalSpeed = state.finalSpeed(field, side)
                    STATS.forEach { StatPointsSlider(state, it, finalSpeed, isDesktop = true) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            CurrentHpSlider(state)
                        }
                        CurrentHpField(state)
                    }
                    ResetBattleStateButton(state, Modifier.align(Alignment.End))
                }
                if (team != null && isAttacker) {
                    TeamPokemonButtons(team, state, side)
                }
            }
            Text("Moves", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            state.moves.forEachIndexed { index, move ->
                MoveRow(state, index, move)
                // Counter-like moves return one of the opponent's moves, picked like in the source calculator
                if (state.championsMove(index)?.returnsDefenderMove == true) {
                    DropdownField(
                        label = "Returns",
                        selected = state.counteredMoveIndex(index),
                        options = opponent.moves.indices.toList(),
                        optionText = { opponent.moves.getOrNull(it)?.ifBlank { null } ?: "-" },
                        onSelect = { state.setCounteredMoveIndex(index, it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    if (showPasteDialog) {
        // the same as the mobile layout's: a pokemon name or a pokepaste
        SelectPokemonDialog(
            onSelect = { state.pickSpecies(it) },
            onDismissRequest = { showPasteDialog = false },
            title = "Paste ${side.desktopName}",
            confirmButtonText = "Fill",
            allPokemons = PICKABLE_POKEMON_NAMES,
            onPokepasteSelect = { state.fillFrom(it) },
            // the pokemon name is typed in the column's text field
            label = "Pokepaste",
        )
    }
}

/**
 * The fields of the pokemon's set: species (or a pokepaste), form, ability, item, nature, status
 */
@Composable
private fun SetFields(state: DamageCalcPokemonState, onPasteClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CatalogTextField(
                label = "Pokémon",
                value = ChampionsDex.speciesOrNull(state.name)?.name?.value ?: state.name.value,
                catalog = POKEMON_CATALOG,
                onValueSelected = { state.pickSpecies(PokemonName(it)) },
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onPasteClick, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(painterResource(Res.drawable.content_paste), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("From Paste", maxLines = 1, softWrap = false)
            }
        }
        // a grid of 2 columns, a field alone on its row taking the whole width
        FieldPairRow(
            first = if (state.availableForms.size > 1 && state.megaFormOfItem == null) {
                { modifier ->
                    DropdownField(
                        label = "Form",
                        selected = state.form,
                        options = state.availableForms,
                        optionText = { it.pretty },
                        onSelect = { state.selectForm(it) },
                        modifier = modifier,
                    )
                }
            } else null,
            // only for the species having preset sets
            second = if (state.presetSets.isNotEmpty()) {
                { modifier ->
                    DropdownField(
                        label = "Set",
                        selected = state.setChoice,
                        options = state.setChoices,
                        optionText = { it.text },
                        onSelect = { state.chooseSet(it) },
                        modifier = modifier,
                    )
                }
            } else null,
        )
        FieldPairRow(
            first = { modifier -> AbilityField(state, modifier) },
            second = { modifier ->
                CatalogTextField(
                    label = "Item",
                    value = state.item,
                    catalog = ITEM_CATALOG,
                    onValueSelected = { state.item = it },
                    leadingIcon = state.item.ifBlank { null }
                        ?.let { { DamageCalcItemIcon(it, Modifier.size(ITEM_ICON_SIZE)) } },
                    modifier = modifier,
                )
            },
        )
        FieldPairRow(
            first = { modifier ->
                NatureField(state.nature, onSelect = { state.nature = it }, modifier = modifier)
            },
            second = { modifier ->
                DropdownField(
                    label = "Status",
                    selected = state.status,
                    options = Status.entries,
                    optionText = { it.displayName },
                    onSelect = { state.status = it },
                    modifier = modifier,
                )
            },
        )
        // the settings shown for some abilities and statuses only
        FieldPairRow(
            // the ability settings the source calculator shows for these abilities only
            first = when (state.resolvedAbility) {
                Ability.RIVALRY -> { modifier ->
                    DropdownField(
                        label = "Rivalry",
                        selected = state.rivalry,
                        options = RivalryRelation.entries,
                        optionText = { it.displayName },
                        onSelect = { state.rivalry = it },
                        modifier = modifier,
                    )
                }
                Ability.SUPREME_OVERLORD -> { modifier ->
                    DropdownField(
                        label = "Fainted Allies",
                        selected = state.faintedAllyCount,
                        options = FAINTED_ALLY_COUNTS.toList(),
                        optionText = ::faintedAlliesText,
                        onSelect = { state.faintedAllyCount = it },
                        modifier = modifier,
                    )
                }
                else -> null
            },
            second = if (state.status == Status.BADLY_POISONED) {
                { modifier ->
                    DropdownField(
                        label = "Toxic Damage",
                        selected = state.toxicCounter,
                        options = TOXIC_COUNTERS.toList(),
                        optionText = ::toxicCounterText,
                        onSelect = { state.toxicCounter = it },
                        modifier = modifier,
                    )
                }
            } else null,
        )
    }
}

/**
 * The side (Pokémon 1/2) as an overline, the pokemon's form and its types. Aligned on the sprite's side
 */
@Composable
private fun PokemonHeaderTitle(state: DamageCalcPokemonState, side: DamageCalcSide) {
    val horizontalAlignment = if (side == DamageCalcSide.ATTACKER) Alignment.Start else Alignment.End
    Column(horizontalAlignment = horizontalAlignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            side.desktopName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PokemonFormTitle(state, horizontalAlignment)
    }
}

/**
 * The ability, with its "ability on" toggle for the abilities the source calculator has one for
 */
@Composable
private fun AbilityField(state: DamageCalcPokemonState, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CatalogTextField(
            label = "Ability",
            value = state.ability,
            catalog = ABILITY_CATALOG,
            onValueSelected = { state.ability = it },
            modifier = Modifier.weight(1f),
        )
        if (state.hasAbilityToggle) {
            FilterChip(
                selected = state.isAbilityActive,
                onClick = { state.isAbilityActive = !state.isAbilityActive },
                label = { Text("Active", maxLines = 1, softWrap = false) },
                // the default selected color barely stands out on the card
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

/**
 * A row of the set's grid: two fields sharing the width, a single one taking all of it, nothing without fields. Each
 * field is given the modifier to apply
 */
@Composable
internal fun FieldPairRow(
    first: (@Composable (Modifier) -> Unit)?,
    second: (@Composable (Modifier) -> Unit)?,
) {
    if (first == null && second == null) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        first?.invoke(Modifier.weight(1f))
        second?.invoke(Modifier.weight(1f))
    }
}

/**
 * One button per pokemon of the team, filling this pokemon with its set
 */
@Composable
private fun TeamPokemonButtons(team: Teamlytics, state: DamageCalcPokemonState, side: DamageCalcSide) {
    Column(verticalArrangement = Arrangement.spacedBy(TEAM_BUTTONS_SPACING)) {
        team.pokePaste.pokemons.forEach { pokemon ->
            TeamPokemonButton(pokemon, state, side, TEAM_SPRITE_SIZE, Modifier.size(TEAM_SPRITE_SIZE + 8.dp))
        }
    }
}

/**
 * A button filling [state] with the set of [pokemon], a pokemon of the team. Highlighted while [state] still has its set
 */
@Composable
internal fun TeamPokemonButton(
    pokemon: Pokemon,
    state: DamageCalcPokemonState,
    side: DamageCalcSide,
    spriteSize: Dp,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = { state.fillFrom(pokemon) },
        border = if (state.hasSetOf(pokemon)) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else CardDefaults.outlinedCardBorder(),
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // like the header's sprite, the attacker faces right, towards its target
            PokemonSprite(
                pokemon.name,
                Modifier.size(spriteSize),
                facingDirection = if (side == DamageCalcSide.ATTACKER) FacingDirection.RIGHT else FacingDirection.LEFT,
            )
        }
    }
}

private val TEAM_SPRITE_SIZE = 64.dp
private val TEAM_BUTTONS_SPACING = 8.dp

@Composable
private fun StatPointsHeader(state: DamageCalcPokemonState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Stat Points", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        if (state.exceedsMaxTotalStatPoints) {
            Text(
                "${-state.remainingStatPoints} over",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text("${state.remainingStatPoints} left", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * A move, with its settings (crit, 2x BP...) next to it
 */
@Composable
private fun MoveRow(state: DamageCalcPokemonState, index: Int, move: String) {
    val championsMove = state.championsMove(index)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CatalogTextField(
            label = "Move ${index + 1}",
            value = move,
            catalog = MOVE_CATALOG,
            onValueSelected = { state.setMove(index, it) },
            leadingIcon = championsMove?.let { { MoveTypeImage(it.type, Modifier.size(MOVE_TYPE_ICON_SIZE)) } },
            modifier = Modifier.weight(1f),
        )
        if (move.isNotBlank()) {
            CritChip(
                isCritical = state.isCritical(index),
                alwaysCrits = state.alwaysCrits(index),
                onCriticalChange = { state.setCritical(index, it) },
            )
            if (championsMove?.canBePowerDoubled == true) {
                FilterChip(
                    selected = state.isPowerDoubled(index),
                    onClick = { state.setPowerDoubled(index, !state.isPowerDoubled(index)) },
                    label = { Text("2x BP", maxLines = 1, softWrap = false) },
                )
            }
            if (championsMove?.hasStackingPower == true) {
                StackCountChip(
                    moveName = championsMove.name.value,
                    count = state.stackCount(index),
                    onCountSelected = { state.setStackCount(index, it) },
                    useMenu = true,
                )
            }
            state.selectableHitCounts(index)?.let {
                HitCountChip(
                    hits = state.hitCount(index),
                    selectableHitCounts = it,
                    onHitCountSelected = { hits -> state.selectHitCount(index, hits) },
                    useMenu = true,
                )
            }
        }
    }
}

/**
 * Field to type the exact current HP points
 */
@Composable
private fun CurrentHpField(state: DamageCalcPokemonState) {
    val currentHp = state.currentHp ?: return
    val maxHp = state.maxHp ?: return
    var text by remember(currentHp) { mutableStateOf(currentHp.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText.filter { it.isDigit() }.take(4)
            text.toIntOrNull()?.let(state::setCurrentHp)
        },
        label = { Text("HP") },
        singleLine = true,
        isError = text.toIntOrNull()?.let { it > maxHp } ?: true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier.width(96.dp),
    )
}

/**
 * Text field suggesting the values of [catalog]. A value is only applied once it's one of [catalog] (with its
 * spelling, see [SuggestionCatalog.match]), the field being in error until then
 */
@Composable
private fun CatalogTextField(
    label: String,
    value: String,
    catalog: SuggestionCatalog,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    var text by remember(value) { mutableStateOf(value) }
    SuggestionTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            val match = catalog.match(newText)
            if (match != null && match != value) onValueSelected(match)
        },
        suggestions = catalog.values,
        suggestionText = { it },
        label = label,
        modifier = modifier,
        isError = catalog.match(text) == null,
        suggestionLeadingContent = catalog.suggestionLeadingContent,
        leadingIcon = leadingIcon,
    )
}
