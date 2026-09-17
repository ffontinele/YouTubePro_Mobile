package com.github.libretube.ui.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.github.libretube.R

class WebViewPlayerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var btnClose: ImageButton
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_player)

        val videoId = intent.getStringExtra("video_id")
        if (videoId.isNullOrEmpty()) {
            finish()
            return
        }

        webView = findViewById(R.id.webview)
        btnClose = findViewById(R.id.btn_close)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setCookie(".youtube.com", "CONSENT=YES+cb")
        cookieManager.setCookie(".youtube.com", "SOCS=CAI")

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view == null) return
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                val root = window.decorView as FrameLayout
                root.addView(view, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT))
                webView.visibility = View.GONE
                btnClose.visibility = View.GONE
            }

            override fun onHideCustomView() {
                val view = customView ?: return
                val root = window.decorView as FrameLayout
                root.removeView(view)
                webView.visibility = View.VISIBLE
                btnClose.visibility = View.VISIBLE
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }
        }

        // Pagina normal do YouTube (nao embed): sem Erro 153
        webView.loadUrl("https://www.youtube.com/watch?v=$videoId")

        btnClose.setOnClickListener { finish() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (customView != null) {
            webView.webChromeClient?.onHideCustomView()
            return
        }
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
