package com.tambapps.pokemon.alakastats.ui.composables

import alakastats.composeapp.generated.resources.Res
import alakastats.composeapp.generated.resources.content_paste
import alakastats.composeapp.generated.resources.edit
import alakastats.composeapp.generated.resources.visibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.tambapps.pokemon.alakastats.domain.model.Format
import com.tambapps.pokemon.alakastats.ui.LocalSnackBar
import com.tambapps.pokemon.alakastats.ui.service.FacingDirection
import com.tambapps.pokemon.alakastats.ui.viewmodels.PokepasteEditingViewModel
import com.tambapps.pokemon.alakastats.ui.viewmodels.asOtsIfNoStats
import com.tambapps.pokemon.alakastats.util.copyToClipboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * The paste being edited, as its Pokémon, with a Details dialog showing the whole paste and an Edit dialog to change
 * it, rather than a big text field
 *
 * @param title the card's title, e.g. "Opponent Team"
 * @param emptyText what the card tells when there's no paste yet
 * @param teamName the name of the team, titling the Details dialog
 */
@Composable
fun PokepasteCard(
    viewModel: PokepasteEditingViewModel,
    format: Format,
    title: String,
    emptyText: String,
    teamName: String,
    modifier: Modifier = Modifier,
) {
    var showDetails by remember { mutableStateOf(false) }
    MyCard(gradientBackgroundColors = cardGradientColors, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            val pokePaste = viewModel.appliedPokepaste
            if (pokePaste == null) {
                Text(emptyText, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = viewModel::startPokepasteEdition) {
                    ButtonIcon(painterResource(Res.drawable.content_paste))
                    Text("Add PokéPaste")
                }
            } else {
                PokemonTeamPreview(pokePaste, fillWidth = true, facingDirection = FacingDirection.LEFT)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showDetails = true }) {
                        ButtonIcon(painterResource(Res.drawable.visibility))
                        Text("Details")
                    }
                    OutlinedButton(onClick = viewModel::startPokepasteEdition) {
                        ButtonIcon(painterResource(Res.drawable.edit))
                        Text("Edit")
                    }
                }
            }
        }
    }
    val pokePaste = viewModel.appliedPokepaste
    if (showDetails && pokePaste != null) {
        val snackbar = LocalSnackBar.current
        val clipboard = LocalClipboard.current
        val scope = rememberCoroutineScope()
        val name = teamName.ifBlank { title }
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text(name) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // vertical on desktop too, as a dialog doesn't take the whole screen width
                    VerticalPokepaste(remember(pokePaste) { pokePaste.asOtsIfNoStats() }, format)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDetails = false
                    scope.launch {
                        if (copyToClipboard(clipboard, label = name, text = pokePaste.toPokePasteString())) {
                            snackbar.show("Copied to clipboard")
                        } else {
                            snackbar.show("Copy to clipboard not supported")
                        }
                    }
                }) {
                    Text("COPY")
                }
            }
        )
    }
    if (viewModel.isEditingPokepaste) {
        EditPokepasteDialog(viewModel, title)
    }
}

@Composable
private fun EditPokepasteDialog(viewModel: PokepasteEditingViewModel, title: String) {
    AlertDialog(
        onDismissRequest = viewModel::cancelPokepasteEdition,
        // a click next to the dialog shouldn't lose a paste being typed
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(if (viewModel.appliedPokepaste == null) "Add $title" else "Edit $title") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PokePasteInput(viewModel)
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::applyPokepasteEdition,
                enabled = viewModel.canApplyPokepaste
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::cancelPokepasteEdition) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ButtonIcon(painter: Painter) {
    Icon(painter = painter, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(Modifier.width(8.dp))
}
