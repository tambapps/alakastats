package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.tambapps.pokemon.alakastats.PlatformType
import com.tambapps.pokemon.alakastats.platform

/**
 * For a screen with text fields: pads the content with the scaffold padding and keeps it above the
 * virtual keyboard (which would otherwise hide the bottom of the screen, its buttons included), and a
 * tap outside a text field closes the keyboard (a tap on a button stays the button's)
 */
@Composable
fun Modifier.keyboardAwarePadding(scaffoldPadding: PaddingValues): Modifier = this
    .padding(scaffoldPadding)
    // so that imePadding doesn't add the bottom inset the scaffold padding already has
    .consumeWindowInsets(scaffoldPadding)
    .imePadding()
    .hideKeyboardOnTap()

/** A tap outside a text field closes the keyboard (a tap on a button stays the button's) */
@Composable
fun Modifier.hideKeyboardOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return pointerInput(focusManager) { detectTapGestures { focusManager.clearFocus() } }
}

/** For a search field, whose search is done while typing: the return key, a magnifier, closes the keyboard */
val searchKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)

@Composable
fun searchKeyboardActions(): KeyboardActions {
    val focusManager = LocalFocusManager.current
    return remember(focusManager) { KeyboardActions(onSearch = { focusManager.clearFocus() }) }
}

/** For a single line field: the return key closes the keyboard */
val doneKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

/**
 * For the single line field of a dialog: the return key does the dialog's action when it can, else it closes
 * the keyboard
 */
fun doneKeyboardActions(canConfirm: Boolean, onConfirm: () -> Unit) = KeyboardActions(onDone = {
    if (canConfirm) onConfirm() else defaultKeyboardAction(ImeAction.Done)
})

/**
 * Closes the keyboard, shown on iOS only while the keyboard is open: some iOS keyboards have no key to
 * close them, a multiline field's return key making a new line. Android has its back button for that
 *
 * @param floating whether it's drawn over the content, which then needs a filled button to stay readable
 */
@Composable
fun HideKeyboardButton(modifier: Modifier = Modifier, floating: Boolean = false) {
    if (platform.type != PlatformType.Ios || WindowInsets.ime.getBottom(LocalDensity.current) == 0) return
    val focusManager = LocalFocusManager.current
    if (floating) {
        FilledTonalButton(onClick = { focusManager.clearFocus() }, modifier = modifier) {
            Text("Hide Keyboard")
        }
    } else {
        TextButton(onClick = { focusManager.clearFocus() }, modifier = modifier) {
            Text("Hide Keyboard")
        }
    }
}
