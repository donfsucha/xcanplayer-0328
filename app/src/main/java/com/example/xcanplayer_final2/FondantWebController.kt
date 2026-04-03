package com.example.xcanplayer_final2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class FondantWebController(
    private val webView: WebView,
    private val fullscreenContainer: FrameLayout,
    private val onOrientationChangeNeeded: () -> Unit
) {
    var customView: View? = null
    var customViewCallback: WebChromeClient.CustomViewCallback? = null
    var currentSpeed: Float = 1.0f
    private var currentTargetUrl = ""
    private var isBibleMode = true

    inner class AndroidBotInterface {
        @JavascriptInterface
        fun requestPortrait() {
            val activity = webView.context as? Activity
            activity?.runOnUiThread {
                if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    onOrientationChangeNeeded()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            textZoom = 100
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.addJavascriptInterface(AndroidBotInterface(), "AndroidBot")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view; customViewCallback = callback
                fullscreenContainer.addView(view); fullscreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE; onOrientationChangeNeeded()
            }
            override fun onHideCustomView() {
                fullscreenContainer.removeView(customView); customView = null
                fullscreenContainer.visibility = View.GONE; webView.visibility = View.VISIBLE
                customViewCallback?.onCustomViewHidden(); onOrientationChangeNeeded()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                handleOrientationByUrl(url)
                applySpeed(); injectTouchMagic()
                if (url != null && (url.contains("youtube.com") || url.contains("youtu.be"))) injectYouTubeBot()
                else injectMasterBot()
            }
        }
    }

    private fun handleOrientationByUrl(url: String?) {
        val activity = webView.context as? Activity ?: return
        if (url == null) return
        if (url.contains("/series/") || url.contains("/watch") || url.contains("/play")) {
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                onOrientationChangeNeeded()
            }
        }
    }

    fun loadSmartUrl(url: String) {
        currentTargetUrl = url
        isBibleMode = url.contains("00090228-5db3-dc44-3c29-52bcaf0002ce") || url.contains("bible") || url.contains("통독")
        webView.loadUrl(url)
    }

    fun isVideoPage(): Boolean = webView.url?.let { it.contains("/series/") || it.contains("/watch") || it.contains("/play") } ?: false

    fun applySpeed() {
        val js = "javascript:(function() { window.androidSpeed = $currentSpeed; if(window.speedInt) clearInterval(window.speedInt); window.speedInt = setInterval(function() { var v = document.querySelector('video'); if(v && v.playbackRate != window.androidSpeed) v.playbackRate = window.androidSpeed; }, 1000); })();"
        webView.evaluateJavascript(js, null)
    }

    private fun injectTouchMagic() {
        val js = "javascript:(function() { document.addEventListener('touchstart', function(e) { if (e.touches.length > 0) { var touch = e.touches[0]; var event = new MouseEvent('mousemove', { 'view': window, 'bubbles': true, 'cancelable': true, 'clientX': touch.clientX, 'clientY': touch.clientY }); e.target.dispatchEvent(event); } }, {passive: true}); })();"
        webView.evaluateJavascript(js, null)
    }

    private fun injectYouTubeBot() {
        val js = "javascript:(function() { if(window.ytBotInt) clearInterval(window.ytBotInt); window.ytBotInt = setInterval(function() { var skip = document.querySelector('.ytp-ad-skip-button, .ytp-skip-ad-button'); if(skip) skip.click(); var fs = document.querySelector('.ytp-fullscreen-button'); if(fs && !(document.fullscreenElement)) fs.click(); var v = document.querySelector('video'); if(v && v.paused && v.currentTime < 1) v.play(); }, 1000); })();"
        webView.evaluateJavascript(js, null)
    }

    private fun injectMasterBot() {
        val js = """
            javascript:(function() {
              if(window.botInt) clearInterval(window.botInt);
              
              window.botInt = setInterval(function() {
                var v = document.querySelector('video');
                var u = window.location.href;
                var isBible = ${isBibleMode};

                if (v) {
                  if (window.androidSpeed && v.playbackRate !== window.androidSpeed) v.playbackRate = window.androidSpeed;
                  v.muted = false;
                  if (v.paused && v.currentTime < 1.0) {
                      var p = v.play();
                      if(p !== undefined) p.catch(function(e){});
                  }
                  return; 
                }

                var isHome = (u.split('?')[0].replace(/\/$/, '') === 'https://www.fondant.kr' || u.includes('/main'));
                
                // [홈 화면 로직]
                if (isHome) {
                    // 성경통독은 전용 URL이 있으므로 바로 이동
                    if (isBible) {
                        var targetUrl = '${currentTargetUrl}';
                        if (targetUrl && !u.includes(targetUrl)) {
                            window.location.href = targetUrl;
                            return;
                        }
                    } 
                    // 생명의삶은 홈 화면에서 배너를 찾아야 함
                    else {
                        var links = document.querySelectorAll('a');
                        for (var i = 0; i < links.length; i++) {
                            var img = links[i].querySelector('img');
                            var txt = (links[i].innerText || '') + (img ? img.alt : '');
                            if (txt.replace(/\s/g, '').includes('생명의삶')) {
                                if (links[i].href) { window.location.href = links[i].href; return; }
                            }
                        }
                    }
                }

                // [상세/재생 페이지 로직]
                if (u.includes('/series/') || u.includes('/watch') || u.includes('/play')) {
                    var links = document.querySelectorAll('a');
                    for (var i = 0; i < links.length; i++) {
                        var el = links[i];
                        var txt = (el.innerText || '').replace(/\s/g, '');
                        
                        if (txt.includes('로그인')) {
                            if(window.AndroidBot) window.AndroidBot.requestPortrait();
                            el.click(); return;
                        }

                        // 이어보기, 재생, 최신영상 등 발견 시 순간이동
                        if (txt.includes('이어') || txt.includes('계속') || txt.includes('최신') || txt.includes('재생') || txt.includes('오늘') || txt.includes('1편')) {
                            if (el.href && !el.href.includes('javascript')) {
                                window.location.href = el.href; return;
                            }
                        }
                    }
                }
              }, 2000);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}