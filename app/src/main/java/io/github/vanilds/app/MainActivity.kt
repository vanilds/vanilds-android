package io.github.vanilds.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible

/**
 * The whole app: one WebView that shows https://vanilds.github.io.
 *
 * - The portfolio (GitHub Pages) and the CreditDesk demo (micro frontends, API and
 *   Keycloak login on creditdesk.duckdns.org) stay inside the app, so the OIDC
 *   redirect returns to the WebView with its session storage intact.
 * - Any other link (LinkedIn, GitHub, mailto, S3 downloads) opens outside the app.
 */
class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var offline: View
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val pickFiles =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            fileCallback?.onReceiveValue(uris.toTypedArray())
            fileCallback = null
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true // OIDC state + tokens live in sessionStorage
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = Client()
            webChromeClient = Chrome()
            setDownloadListener { url, _, _, _, _ -> openOutside(Uri.parse(url)) }
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            // The shell on github.io calls the cluster cross-site; Keycloak keeps its own
            // cookies first-party during the login navigation, but allow third-party ones
            // so the demo behaves like a desktop browser.
            setAcceptThirdPartyCookies(webView, true)
        }

        offline = buildOfflineView()
        val root = android.widget.FrameLayout(this).apply {
            addView(webView)
            addView(offline)
        }
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(START_URL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private fun isInternal(uri: Uri): Boolean =
        uri.scheme == "https" && uri.host?.lowercase() in INTERNAL_HOSTS

    private fun openOutside(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            // No app can open it (e.g. an unknown scheme): ignore, like a browser would.
        }
    }

    private inner class Client : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (isInternal(request.url)) return false
            openOutside(request.url)
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            offline.isVisible = false
        }

        override fun onReceivedError(
            view: WebView, request: WebResourceRequest, error: WebResourceError
        ) {
            // Only a failed page load means "offline". A failed sub-request (for instance the
            // demo cluster being stopped at night) is handled by the site's own "Demo offline" page.
            if (request.isForMainFrame) offline.isVisible = true
        }
    }

    private inner class Chrome : WebChromeClient() {
        // The demo's documents panel uses <input type="file"> (PDF/JPEG/PNG).
        override fun onShowFileChooser(
            view: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams
        ): Boolean {
            fileCallback?.onReceiveValue(null)
            fileCallback = callback
            val types = params.acceptTypes.filter { it.isNotBlank() }
                .flatMap { it.split(",") }
                .map { it.trim() }
                .map { if (it.startsWith(".")) MIME_BY_EXTENSION[it] ?: "*/*" else it }
                .ifEmpty { listOf("*/*") }
            pickFiles.launch(types.toTypedArray())
            return true
        }
    }

    private fun buildOfflineView(): View {
        val pad = (24 * resources.displayMetrics.density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(getColor(R.color.navy))
            isVisible = false
            addView(TextView(context).apply {
                setText(R.string.offline_title)
                setTextColor(getColor(R.color.white))
                textSize = 22f
            })
            addView(TextView(context).apply {
                setText(R.string.offline_message)
                setTextColor(getColor(R.color.white))
                setPadding(0, pad / 2, 0, pad)
                gravity = android.view.Gravity.CENTER
            })
            addView(Button(context).apply {
                setText(R.string.retry)
                setOnClickListener { webView.reload() }
            })
        }
    }

    private companion object {
        const val START_URL = "https://vanilds.github.io/"
        val INTERNAL_HOSTS = setOf("vanilds.github.io", "creditdesk.duckdns.org")
        val MIME_BY_EXTENSION = mapOf(
            ".pdf" to "application/pdf",
            ".jpg" to "image/jpeg",
            ".jpeg" to "image/jpeg",
            ".png" to "image/png",
        )
    }
}
