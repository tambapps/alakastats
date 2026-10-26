package com.tambapps.pokemon.alakastats.ui.composables

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun HtmlView(html: String, baseUrl: String, bridge: HtmlBridge, modifier: Modifier, obscured: Boolean) {
    // menus and dialogs are windows of their own, over the web view
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // without it, the page's alert() and confirm() do nothing
                webChromeClient = WebChromeClient()
                val webView = this
                addJavascriptInterface(object {
                    // called on a background thread
                    @JavascriptInterface
                    fun postMessage(message: String) {
                        webView.post { bridge.onMessage(message) }
                    }
                }, ANDROID_BRIDGE_NAME)
                bridge.evaluator = { script -> evaluateJavascript(script, null) }
            }
        },
        update = { webView ->
            // update also runs on recompositions that don't change the page, which mustn't reload it
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null)
            }
        },
        onRelease = {
            bridge.evaluator = null
            it.destroy()
        },
    )
}
