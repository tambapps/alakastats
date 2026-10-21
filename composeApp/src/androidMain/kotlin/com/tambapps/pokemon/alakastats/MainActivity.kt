package com.tambapps.pokemon.alakastats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tambapps.pokemon.alakastats.infrastructure.cloud.AndroidAuthorizationBridge
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

        setContent {
            AndroidApp()
        }
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