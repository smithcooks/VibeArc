package com.vibearc.app

import com.vibearc.app.GlassButton as Button
import com.vibearc.app.GlassTextButton as OutlinedButton
import com.vibearc.app.GlassTextButton as TextButton
import com.vibearc.app.GlassButton as FilledTonalButton
import com.vibearc.app.GlassIconButton as IconButton
import com.vibearc.app.GlassFilledIconButton as FilledIconButton

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YouTubeLoginSheet(onDismiss: () -> Unit, onConnected: () -> Unit) {
    var progress by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val connected by rememberUpdatedState(onConnected)
    fun checkSession(url: String?) {
        if (!completed && runCatching { java.net.URI(url.orEmpty()).host }.getOrNull() == "music.youtube.com" &&
            YouTubeWebSession.isAuthenticated()) {
            completed = true
            CookieManager.getInstance().flush()
            connected()
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.82f).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("YouTube Music", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss) { Text("Close") }
            }
            if (progress < 100) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            if (error) Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Page could not load. Check your connection.", Modifier.weight(1f))
                TextButton(onClick = { error = false; webView?.reload() }) { Text("Retry") }
            }
            AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { context ->
                WebView(context).apply web@ {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.setGeolocationEnabled(false)
                    settings.safeBrowsingEnabled = true
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(this@web, true)
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            progress = newProgress
                            checkSession(view.url)
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                            !isTrustedYouTubeLoginUrl(request.url.toString())
                        override fun onPageCommitVisible(view: WebView, url: String) = checkSession(url)
                        override fun onPageFinished(view: WebView, url: String) = checkSession(url)
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                            if (request.isForMainFrame) error = true
                        }
                    }
                    webView = this
                    loadUrl("https://music.youtube.com/")
                }
            }, onRelease = { view ->
                webView = null
                view.stopLoading()
                view.destroy()
            })
        }
    }
}
