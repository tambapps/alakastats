package com.tambapps.pokemon.alakastats.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HtmlView(html: String, baseUrl: String, bridge: HtmlBridge, modifier: Modifier, obscured: Boolean) {
    // menus and dialogs are drawn over the web view
    // update also runs on recompositions that don't change the page, which mustn't reload it
    val loaded = remember { LoadedHtml() }
    UIKitView(
        modifier = modifier,
        factory = {
            val configuration = WKWebViewConfiguration()
            configuration.userContentController.addScriptMessageHandler(MessageHandler(bridge), IOS_BRIDGE_NAME)
            WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = configuration).also { webView ->
                bridge.evaluator = { script -> webView.evaluateJavaScript(script, completionHandler = null) }
            }
        },
        update = { webView ->
            if (loaded.html != html) {
                loaded.html = html
                webView.loadHTMLString(html, NSURL.URLWithString(baseUrl))
            }
        },
        onRelease = { webView ->
            bridge.evaluator = null
            // the content controller keeps its handlers, and the handler the bridge
            webView.configuration.userContentController.removeScriptMessageHandlerForName(IOS_BRIDGE_NAME)
        },
    )
}

private class LoadedHtml {
    var html: String? = null
}

private class MessageHandler(private val bridge: HtmlBridge) : NSObject(), WKScriptMessageHandlerProtocol {
    // called on the main thread
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        (didReceiveScriptMessage.body as? String)?.let(bridge.onMessage)
    }
}
