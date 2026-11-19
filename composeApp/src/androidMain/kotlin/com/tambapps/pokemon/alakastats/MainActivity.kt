package com.tambapps.pokemon.alakastats

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tambapps.pokemon.alakastats.infrastructure.cloud.AndroidAuthorizationBridge
import com.tambapps.pokemon.alakastats.ui.navigation.AppShortcut
import com.tambapps.pokemon.alakastats.ui.navigation.AppShortcuts
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val authorizationBridge: AndroidAuthorizationBridge by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        FileKit.init(this)
        authorizationBridge.attach(this)

        // not when recreated (e.g. rotated): the intent is still the shortcut's, whose screen was already shown
        if (savedInstanceState == null) openShortcut(intent)

        setContent {
            AndroidApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openShortcut(intent)
    }

    /** The launcher shortcuts of res/xml/shortcuts.xml (androidApp), whose extra is the id of an [AppShortcut] */
    private fun openShortcut(intent: Intent?) {
        AppShortcuts.open(intent?.getStringExtra(SHORTCUT_EXTRA))
    }

    private companion object {
        const val SHORTCUT_EXTRA = "shortcut"
    }

    override fun onDestroy() {
        authorizationBridge.detach(this)
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}