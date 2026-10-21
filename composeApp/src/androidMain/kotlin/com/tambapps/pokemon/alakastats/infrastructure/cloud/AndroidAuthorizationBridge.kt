package com.tambapps.pokemon.alakastats.infrastructure.cloud

import android.app.PendingIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

/**
 * Lets the token provider, which lives outside of any activity, show Google's consent screen.
 * The launcher must be registered by the activity in onCreate.
 */
class AndroidAuthorizationBridge {

    private var activity: ComponentActivity? = null
    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var pendingResult: CompletableDeferred<ActivityResult>? = null

    fun attach(activity: ComponentActivity) {
        this.activity = activity
        launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            pendingResult?.complete(result)
            pendingResult = null
        }
    }

    fun detach(activity: ComponentActivity) {
        if (this.activity !== activity) return
        this.activity = null
        launcher = null
    }

    /**
     * @return null if no activity is there to show the UI
     */
    suspend fun launch(pendingIntent: PendingIntent): ActivityResult? {
        val launcher = launcher ?: return null
        pendingResult?.cancel()
        val deferred = CompletableDeferred<ActivityResult>()
        pendingResult = deferred
        launcher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        return deferred.await()
    }
}
