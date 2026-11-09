package com.tambapps.pokemon.alakastats.ui.screen.teamlytics.tabs.replay

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tambapps.pokemon.alakastats.ui.composables.SortChip
import com.tambapps.pokemon.alakastats.ui.model.ReplaySort
import com.tambapps.pokemon.alakastats.ui.model.ReplaySortCriterion

/**
 * Chip displaying the order of the replays, opening a menu to pick the criterion and the direction. It is selected when
 * the order isn't the default one (newest first)
 */
@Composable
internal fun ReplaySortChip(
    sort: ReplaySort,
    onSortChange: (ReplaySort) -> Unit,
    modifier: Modifier = Modifier,
) = SortChip(
    criteria = ReplaySortCriterion.entries,
    criterion = sort.criterion,
    descending = sort.descending,
    isDefault = sort.isDefault,
    onSortChange = { criterion, descending -> onSortChange(ReplaySort(criterion, descending)) },
    onReset = { onSortChange(ReplaySort()) },
    modifier = modifier,
)
