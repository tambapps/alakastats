package com.tambapps.pokemon.alakastats.infrastructure.cloud

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Google Picker, showing the JSON files shared with the user
 * @param apiKey browser key restricted to the app's origins and to the Picker API: public by nature
 * @param appId the Google Cloud project number
 */
class GooglePicker(
    private val apiKey: String,
    private val appId: String,
) : TeamFilePicker {

    override suspend fun pick(accessToken: String): Either<CloudError, List<String>> {
        val result = showPicker(accessToken, apiKey, appId).await<JsString>().toString()
        if (result == LOAD_ERROR) {
            return CloudError.Network("Couldn't open Google's file picker. Are you offline?").left()
        }
        if (result.startsWith(ERROR_PREFIX)) {
            return CloudError.Unknown("Couldn't open Google's file picker (${result.removePrefix(ERROR_PREFIX)})").left()
        }
        return result.split(',').filter { it.isNotBlank() }.right()
    }

    private companion object {
        const val ERROR_PREFIX = "error:"
        const val LOAD_ERROR = "error:load"
    }
}

/**
 * Loads the Picker (once) and shows it. Never rejects: resolves to the picked ids joined by commas, to an empty
 * string if cancelled, or to "error:..."
 */
private fun showPicker(token: String, apiKey: String, appId: String): Promise<JsString> = js("""
    new Promise((resolve) => {
        const build = () => {
            // an error here would leave the promise pending forever
            try {
                const view = new google.picker.DocsView(google.picker.ViewId.DOCS)
                    .setOwnedByMe(false)
                    .setMimeTypes('application/json');
                new google.picker.PickerBuilder()
                    .setAppId(appId)
                    .setOAuthToken(token)
                    .setDeveloperKey(apiKey)
                    .addView(view)
                    .enableFeature(google.picker.Feature.MULTISELECT_ENABLED)
                    .setTitle('Pick the shared teams to join')
                    .setCallback((data) => {
                        if (data.action === google.picker.Action.PICKED) resolve(data.docs.map((doc) => doc.id).join(','));
                        else if (data.action === google.picker.Action.CANCEL) resolve('');
                    })
                    .build()
                    .setVisible(true);
            } catch (e) {
                resolve('error:' + e);
            }
        };
        if (window.google && window.google.picker) { build(); return; }
        const load = () => gapi.load('picker', { callback: build, onerror: () => resolve('error:load') });
        if (window.gapi) { load(); return; }
        const script = document.createElement('script');
        script.src = 'https://apis.google.com/js/api.js';
        script.async = true;
        script.onload = load;
        script.onerror = () => resolve('error:load');
        document.head.appendChild(script);
    })
""")
