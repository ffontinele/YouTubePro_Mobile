package com.github.libretube.ui.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.github.libretube.R

class WebViewPlayerActivity : AppCompatActivity() {

    private lateinit var webView: WebView

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
        val btnClose = findViewById<ImageButton>(R.id.btn_close)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // URL do embed (funciona melhor pra legendas)
        val url = "https://www.youtube.com/embed/$videoId?autoplay=1&rel=0"
        webView.loadUrl(url)

        btnClose.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
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
