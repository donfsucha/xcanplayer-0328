package com.example.xcanplayer_final2;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private float currentSpeed = 1.0f;

    private TextView btnSpeed;
    private ImageButton btnSchedule, btnRotate;

    private List<ScheduleItem> scheduleList = new ArrayList<>();
    private Handler scheduleCheckHandler = new Handler(Looper.getMainLooper());

    private static final String PRESET1_URL = "https://www.youtube.com/channel/UC9XJt_V-t6p9oO-iJjVw5_w";
    private static final String PRESET2_URL = "https://www.youtube.com/results?search_query=생명의삶";
    private static final String PRESET3_URL = "https://www.fondant.kr/series/00090228-5db3-dc44-3c29-52bcaf0002ce";

    private String currentTargetUrl = PRESET3_URL;
    private boolean wasOnLoginPage = false;

    private static final String JS_SPEED = "javascript:(function() { window.androidSpeed = 1.0; setInterval(function() { var v = document.querySelector('video'); if(v && v.playbackRate != window.androidSpeed) v.playbackRate = window.androidSpeed; }, 1000); })();";

    private static final String JS_MOUSE_HOVER = "javascript:(function() { " +
            "document.addEventListener('touchstart', function(e) { " +
            "  if (e.touches.length > 0) { " +
            "    var touch = e.touches[0]; " +
            "    var event = new MouseEvent('mousemove', { " +
            "      'view': window, " +
            "      'bubbles': true, " +
            "      'cancelable': true, " +
            "      'clientX': touch.clientX, " +
            "      'clientY': touch.clientY " +
            "    }); " +
            "    e.target.dispatchEvent(event); " +
            "  } " +
            "}, true); " +
            "})();";

    // 유튜브 자동 클릭 & 풀화면 팽창 기능이 추가된 봇
    private static final String JS_MASTER_BOT = "javascript:(function() { " +
            "if(window.masterBotInterval) clearInterval(window.masterBotInterval); " +
            "window.nextBtnSeenTime = 0; " +
            "window.lastPlayClickTime = 0; " +
            "window.cssFsApplied = false; " +
            "window.currentVidUrl = ''; " +
            "window.masterBotInterval = setInterval(function() { " +
            "  var v = document.querySelector('video'); " +
            "  var now = Date.now(); " +
            "  var url = window.location.href; " +

            "  if (v && v.src && v.dataset.autoUnmutedSrc !== v.src) { " +
            "    v.muted = false; " +
            "    v.volume = 1.0; " +
            "    v.dataset.autoUnmutedSrc = v.src; " +
            "    window.cssFsApplied = false; " +
            "  } " +

            "  function triggerClick(el) { " +
            "    if(!el) return; " +
            "    try { " +
            "      el.click(); " +
            "      var ev = new MouseEvent('click', {bubbles: true, cancelable: true, view: window}); " +
            "      el.dispatchEvent(ev); " +
            "    } catch(e){} " +
            "  } " +

            "  /* 유튜브 전용: 첫 영상 클릭 및 100% 풀화면 강제 팽창 */ " +
            "  if (url.includes('youtube.com')) { " +
            "    if (url.includes('results')) { " +
            "      var links = document.querySelectorAll('ytd-video-renderer a#video-title, a.ytm-compact-video-renderer'); " +
            "      if (links.length > 0 && (now - window.lastPlayClickTime > 4000)) { " +
            "        triggerClick(links[0]); " +
            "        window.lastPlayClickTime = now; " +
            "      } " +
            "    } else if (url.includes('watch')) { " +
            "      if (v && v.paused && (now - window.lastPlayClickTime > 3000)) { " +
            "        var p = v.play(); if(p) p.catch(function(){}); " +
            "        window.lastPlayClickTime = now; " +
            "      } " +
            "      if (window.currentVidUrl !== url) { " +
            "        window.cssFsApplied = false; " +
            "        window.currentVidUrl = url; " +
            "      } " +
            "      if (!window.cssFsApplied && v && v.currentTime > 0.5) { " +
            "        var s = document.createElement('style'); " +
            "        s.innerHTML = 'ytd-masthead, #masthead-container, #secondary, #below, #comments, ytd-engagement-panel-section-list-renderer, ytm-header-bar, ytm-item-section-renderer, ytm-single-column-watch-next-results-renderer, .watch-below-the-player { display: none !important; } ' + " +
            "                      'body, html, ytd-app, #page-manager, ytm-app { padding: 0 !important; margin: 0 !important; overflow: hidden !important; background: black !important; } ' + " +
            "                      'ytd-watch-flexy, #columns, #primary, #primary-inner, #player, #player-container-outer, #player-container-inner, .html5-video-player, #player-container-id, .player-size, .player-container, ytm-custom-control { ' + " +
            "                      '  position: fixed !important; top: 0 !important; left: 0 !important; right: 0 !important; bottom: 0 !important; ' + " +
            "                      '  width: 100vw !important; height: 100vh !important; ' + " +
            "                      '  max-width: 100vw !important; max-height: 100vh !important; ' + " +
            "                      '  z-index: 999999 !important; background: black !important; margin: 0 !important; padding: 0 !important; ' + " +
            "                      '} ' + " +
            "                      'video { width: 100vw !important; height: 100vh !important; object-fit: contain !important; z-index: 999999 !important; margin: 0 !important; padding: 0 !important; }'; " +
            "        document.head.appendChild(s); " +
            "        window.cssFsApplied = true; " +
            "      } " +
            "    } " +
            "    return; " +
            "  } " +

            "  /* 퐁당 전용 로직 */ " +
            "  var tags = document.querySelectorAll('button, a, div, span, p'); " +
            "  var nextTags = []; " +
            "  var playTags = []; " +

            "  for (var i = 0; i < tags.length; i++) { " +
            "    var txt = tags[i].innerText; " +
            "    if (txt && txt.length < 30 && !txt.includes('구독')) { " +
            "      var ct = txt.replace(/\\s/g, ''); " +
            "      if (ct.includes('다음회차')) { " +
            "        nextTags.push(tags[i]); " +
            "      } else if (ct.includes('이어보기') || ct.includes('재생하기') || ct.includes('편재생') || ct.includes('첫화보기')) { " +
            "        playTags.push(tags[i]); " +
            "      } " +
            "    } " +
            "  } " +

            "  if (nextTags.length > 0) { " +
            "    if (window.nextBtnSeenTime === 0) { " +
            "      window.nextBtnSeenTime = now; " +
            "    } else if (now - window.nextBtnSeenTime >= 15000) { " +
            "      for(var j = nextTags.length - 1; j >= 0; j--) { triggerClick(nextTags[j]); } " +
            "      window.nextBtnSeenTime = 0; " +
            "      window.lastPlayClickTime = now; " +
            "    } " +
            "    return; " +
            "  } else { " +
            "    window.nextBtnSeenTime = 0; " +
            "  } " +

            "  if (playTags.length > 0) { " +
            "    if (now - window.lastPlayClickTime > 4000) { " +
            "      for(var k = playTags.length - 1; k >= 0; k--) { triggerClick(playTags[k]); } " +
            "      window.lastPlayClickTime = now; " +
            "    } " +
            "  } else if (v && v.paused && v.currentTime < 1.0) { " +
            "    if (now - window.lastPlayClickTime > 4000) { " +
            "      var p = v.play(); if(p !== undefined) p.catch(function(e){}); " +
            "      window.lastPlayClickTime = now; " +
            "    } " +
            "  } " +
            "}, 1000); " +
            "})();";

    public static class ScheduleItem {
        int hour, minute;
        String title;
        String url;

        public ScheduleItem(int hour, int minute, String title, String url) {
            this.hour = hour;
            this.minute = minute;
            this.title = title;
            this.url = url;
        }
    }

    private boolean isLoginPage(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("login") || lowerUrl.contains("member")
                || lowerUrl.contains("oauth") || lowerUrl.contains("account")
                || lowerUrl.contains("nid.naver.com") || lowerUrl.contains("accounts.google.com")
                || lowerUrl.contains("auth") || lowerUrl.contains("join")
                || lowerUrl.contains("kakaocorp") || lowerUrl.contains("kauth");
    }

    private boolean isVideoPage(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("series") || lowerUrl.contains("watch")
                || lowerUrl.contains("youtube") || lowerUrl.contains("results")
                || lowerUrl.contains("play") || lowerUrl.contains("vod") || lowerUrl.contains("content");
    }

    private boolean isHomePage(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("login") || lowerUrl.contains("oauth") || lowerUrl.contains("auth")) return false;

        String cleanUrl = lowerUrl.split("\\?")[0].replaceAll("/$", "");
        return cleanUrl.equals("https://www.fondant.kr") ||
                cleanUrl.equals("https://fondant.kr") ||
                cleanUrl.endsWith("fondant.kr/main");
    }

    private void setImmersiveMode(boolean enable) {
        View decorView = getWindow().getDecorView();
        if (enable) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        } else {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_VISIBLE
            );
        }
    }

    private void checkAndApplyOrientation(String url) {
        if (url == null) return;
        if (isVideoPage(url)) {
            if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            setImmersiveMode(true);
        } else {
            if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            setImmersiveMode(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        webView = findViewById(R.id.webView);
        fullscreenContainer = findViewById(R.id.fullscreen_container);
        btnSpeed = findViewById(R.id.btnSpeed);
        btnSchedule = findViewById(R.id.btnSchedule);
        btnRotate = findViewById(R.id.btnRotate);

        if (btnRotate != null && btnRotate.getParent() != null) {
            ((View) btnRotate.getParent()).bringToFront();
        }

        loadScheduleData();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(125);

        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        setImmersiveMode(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                customView = view;
                customViewCallback = callback;
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                fullscreenContainer.addView(view);
                fullscreenContainer.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                setImmersiveMode(true);

                if (btnRotate != null && btnRotate.getParent() != null) {
                    ((View) btnRotate.getParent()).bringToFront();
                }
            }

            @Override
            public void onHideCustomView() {
                fullscreenContainer.removeView(customView);
                customView = null;
                fullscreenContainer.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();

                if (webView != null) {
                    checkAndApplyOrientation(webView.getUrl());
                }
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                WebView newWebView = new WebView(MainActivity.this);
                WebSettings newSettings = newWebView.getSettings();
                newSettings.setJavaScriptEnabled(true);
                newSettings.setDomStorageEnabled(true);
                newSettings.setDatabaseEnabled(true);
                newSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                newSettings.setSupportMultipleWindows(true);
                newSettings.setUserAgentString(view.getSettings().getUserAgentString());

                CookieManager.getInstance().setAcceptThirdPartyCookies(newWebView, true);

                final android.app.Dialog popupDialog = new android.app.Dialog(MainActivity.this, android.R.style.Theme_Light_NoTitleBar_Fullscreen);
                popupDialog.setContentView(newWebView);

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                setImmersiveMode(false);
                popupDialog.show();

                newWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        popupDialog.dismiss();
                        CookieManager.getInstance().flush();

                        if (MainActivity.this.webView != null) {
                            if (MainActivity.this.wasOnLoginPage && MainActivity.this.currentTargetUrl != null) {
                                MainActivity.this.wasOnLoginPage = false;
                                MainActivity.this.webView.loadUrl(MainActivity.this.currentTargetUrl);
                            }
                            checkAndApplyOrientation(MainActivity.this.webView.getUrl());
                        }
                    }
                });

                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request.getUrl().toString();
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            try {
                                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                                if (intent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(intent);
                                    return true;
                                } else {
                                    String packageName = intent.getPackage();
                                    if (packageName != null) {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
                                        return true;
                                    }
                                }
                            } catch (Exception e) {}
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        if (url != null && (url.equals("https://www.fondant.kr/") || url.equals("https://fondant.kr/") || url.endsWith("fondant.kr/main"))) {
                            CookieManager.getInstance().flush();
                            popupDialog.dismiss();
                            if (MainActivity.this.webView != null) {
                                MainActivity.this.wasOnLoginPage = false;
                                MainActivity.this.webView.loadUrl(currentTargetUrl);
                            }
                        }
                    }
                });

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                        return true;
                    } else {
                        String packageName = intent.getPackage();
                        if (packageName != null) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
                            return true;
                        }
                    }
                } catch (Exception e) {}
                return true;
            }

            private void handleRouting(WebView view, String url) {
                if (url == null) return;
                if (isLoginPage(url)) {
                    wasOnLoginPage = true;
                } else if (isHomePage(url)) {
                    if (wasOnLoginPage) {
                        wasOnLoginPage = false;
                        if (currentTargetUrl != null && !currentTargetUrl.isEmpty() && isVideoPage(currentTargetUrl)) {
                            view.loadUrl(currentTargetUrl);
                        }
                    }
                } else if (isVideoPage(url)) {
                    wasOnLoginPage = false;
                    currentTargetUrl = url;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                checkAndApplyOrientation(url);
                handleRouting(view, url);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                checkAndApplyOrientation(url);
                handleRouting(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                webView.evaluateJavascript(JS_SPEED, null);
                webView.evaluateJavascript(JS_MOUSE_HOVER, null);

                if (url != null && isVideoPage(url)) {
                    webView.evaluateJavascript(JS_MASTER_BOT, null);
                }
            }
        });

        btnSpeed.setOnClickListener(v -> {
            if (currentSpeed >= 2.0f) currentSpeed = 1.0f;
            else currentSpeed += 0.25f;
            btnSpeed.setText(currentSpeed + "x");
            Toast.makeText(this, "속도: " + currentSpeed + "x", Toast.LENGTH_SHORT).show();
            webView.evaluateJavascript("window.androidSpeed = " + currentSpeed + ";", null);
        });

        btnSchedule.setOnClickListener(v -> showScheduleDialog());

        btnRotate.setOnClickListener(v -> {
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                setImmersiveMode(false);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                setImmersiveMode(true);
            }
        });

        currentTargetUrl = PRESET3_URL;
        webView.loadUrl(currentTargetUrl);
        startScheduleChecker();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            checkAndApplyOrientation(webView.getUrl());
        }
    }

    private void showScheduleDialog() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setImmersiveMode(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        layout.setBackgroundColor(Color.WHITE);

        TextView titleView = new TextView(this);
        titleView.setText("스케줄 관리");
        titleView.setTextSize(24);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextColor(Color.BLACK);
        layout.addView(titleView);
        layout.addView(createSpace(30));

        LinearLayout presetLayout = new LinearLayout(this);
        presetLayout.setOrientation(LinearLayout.HORIZONTAL);

        final EditText inputTitle = new EditText(this);
        final EditText inputUrl = new EditText(this);
        final Button[] btnTimeSelect = {null};
        final int[] selectedTime = { -1, -1 };

        presetLayout.addView(createStyledButton("🌅 새벽(5시)", "#FFA726", v -> {
            inputTitle.setText("새벽예배");
            inputUrl.setText(PRESET1_URL);
            selectedTime[0] = 5; selectedTime[1] = 0;
            if(btnTimeSelect[0] != null) btnTimeSelect[0].setText("05:00");
        }));
        presetLayout.addView(createSpaceHorizontal(15));

        presetLayout.addView(createStyledButton("🌿 생명(6시)", "#66BB6A", v -> {
            inputTitle.setText("생명의삶");
            inputUrl.setText(PRESET2_URL);
            selectedTime[0] = 6; selectedTime[1] = 0;
            if(btnTimeSelect[0] != null) btnTimeSelect[0].setText("06:00");
        }));
        presetLayout.addView(createSpaceHorizontal(15));

        presetLayout.addView(createStyledButton("📖 통독(8시)", "#42A5F5", v -> {
            inputTitle.setText("성경통독");
            inputUrl.setText(PRESET3_URL);
            selectedTime[0] = 8; selectedTime[1] = 0;
            if(btnTimeSelect[0] != null) btnTimeSelect[0].setText("08:00");
        }));

        layout.addView(presetLayout);
        layout.addView(createSpace(30));

        inputTitle.setHint("제목 입력");
        inputUrl.setHint("URL (https://...) 붙여넣기");

        inputTitle.setTextColor(Color.BLACK);
        inputTitle.setHintTextColor(Color.GRAY);
        inputUrl.setTextColor(Color.BLACK);
        inputUrl.setHintTextColor(Color.GRAY);

        layout.addView(inputTitle);
        layout.addView(createSpace(15));
        layout.addView(inputUrl);
        layout.addView(createSpace(20));

        LinearLayout inputActionLayout = new LinearLayout(this);
        inputActionLayout.setOrientation(LinearLayout.HORIZONTAL);

        btnTimeSelect[0] = new Button(this);
        btnTimeSelect[0].setText("시간 선택 (터치)");
        btnTimeSelect[0].setBackgroundColor(Color.parseColor("#EEEEEE"));
        btnTimeSelect[0].setTextColor(Color.BLACK);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f);
        btnTimeSelect[0].setLayoutParams(timeParams);
        btnTimeSelect[0].setOnClickListener(v -> {
            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker = new TimePickerDialog(this, (timePicker, selectedHour, selectedMinute) -> {
                selectedTime[0] = selectedHour;
                selectedTime[1] = selectedMinute;
                btnTimeSelect[0].setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
            }, hour, minute, false);
            mTimePicker.setTitle("시간 선택");
            mTimePicker.show();
        });

        Button btnAdd = new Button(this);
        btnAdd.setText("➕ 추가");
        btnAdd.setBackgroundColor(Color.parseColor("#2979FF"));
        btnAdd.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        addParams.setMargins(20, 0, 0, 0);
        btnAdd.setLayoutParams(addParams);

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        btnAdd.setOnClickListener(v -> {
            if(selectedTime[0] == -1 || inputTitle.getText().toString().isEmpty() || inputUrl.getText().toString().isEmpty()) {
                Toast.makeText(this, "시간, 제목, URL을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            scheduleList.add(new ScheduleItem(selectedTime[0], selectedTime[1], inputTitle.getText().toString(), inputUrl.getText().toString()));
            sortScheduleList();
            saveScheduleData();

            inputTitle.setText(""); inputUrl.setText("");
            btnTimeSelect[0].setText("시간 선택 (터치)");
            selectedTime[0] = -1; selectedTime[1] = -1;

            refreshScheduleList(listContainer);
            Toast.makeText(this, "추가되었습니다.", Toast.LENGTH_SHORT).show();
        });

        inputActionLayout.addView(btnTimeSelect[0]);
        inputActionLayout.addView(btnAdd);
        layout.addView(inputActionLayout);
        layout.addView(createSpace(30));

        TextView listHeader = new TextView(this);
        listHeader.setText("▼ 스케줄 목록");
        listHeader.setTypeface(null, Typeface.BOLD);
        listHeader.setTextSize(16);
        listHeader.setTextColor(Color.DKGRAY);
        layout.addView(listHeader);
        layout.addView(createSpace(10));

        layout.addView(listContainer);
        refreshScheduleList(listContainer);

        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("닫기", null);

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> {
            if (webView != null) {
                checkAndApplyOrientation(webView.getUrl());
            }
        });
        dialog.show();
    }

    private void refreshScheduleList(LinearLayout container) {
        container.removeAllViews();
        for (int i = 0; i < scheduleList.size(); i++) {
            final int index = i;
            ScheduleItem item = scheduleList.get(i);

            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setPadding(0, 15, 0, 15);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            itemRow.setBackgroundColor(Color.parseColor("#FAFAFA"));

            TextView itemText = new TextView(this);
            itemText.setText(String.format(Locale.getDefault(), "⏰ %02d:%02d | %s", item.hour, item.minute, item.title));
            itemText.setTextColor(Color.BLACK);
            itemText.setTextSize(15);
            itemText.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            itemText.setLayoutParams(textParams);

            Button btnDelete = new Button(this);
            btnDelete.setText("삭제");
            btnDelete.setTextSize(13);
            btnDelete.setTextColor(Color.RED);
            btnDelete.setBackgroundColor(Color.parseColor("#FFEBEE"));
            btnDelete.setPadding(20, 0, 20, 0);

            btnDelete.setOnClickListener(v -> {
                scheduleList.remove(index);
                saveScheduleData();
                refreshScheduleList(container);
            });

            itemRow.addView(itemText);
            itemRow.addView(btnDelete);
            container.addView(itemRow);

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(Color.LTGRAY);
            container.addView(divider);
        }

        if (scheduleList.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("목록이 없습니다. 앱을 재실행하면 기본값이 복구됩니다.");
            emptyView.setTextColor(Color.GRAY);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 20, 0, 20);
            container.addView(emptyView);
        }
    }

    private Button createStyledButton(String text, String colorHex, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setSingleLine(true);
        btn.setEllipsize(TextUtils.TruncateAt.END);
        btn.setPadding(5, 0, 5, 0);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        p.setMargins(5, 0, 5, 0);
        btn.setLayoutParams(p);
        btn.setOnClickListener(listener);
        return btn;
    }

    private View createSpace(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        return v;
    }
    private View createSpaceHorizontal(int width) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        return v;
    }

    private void saveScheduleData() {
        SharedPreferences prefs = getSharedPreferences("SchedulerPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray jsonArray = new JSONArray();
        for(ScheduleItem item : scheduleList) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("h", item.hour);
                obj.put("m", item.minute);
                obj.put("t", item.title);
                obj.put("u", item.url);
                jsonArray.put(obj);
            } catch (Exception e) {}
        }
        editor.putString("schedule_json", jsonArray.toString());
        editor.apply();
    }

    private void loadScheduleData() {
        SharedPreferences prefs = getSharedPreferences("SchedulerPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("schedule_json", "[]");
        scheduleList.clear();
        try {
            JSONArray jsonArray = new JSONArray(json);
            if (jsonArray.length() == 0) {
                scheduleList.add(new ScheduleItem(5, 0, "새벽예배", PRESET1_URL));
                scheduleList.add(new ScheduleItem(6, 0, "생명의삶", PRESET2_URL));
                scheduleList.add(new ScheduleItem(8, 0, "성경통독", PRESET3_URL));
                saveScheduleData();
            } else {
                for(int i=0; i<jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    scheduleList.add(new ScheduleItem(obj.getInt("h"), obj.getInt("m"), obj.getString("t"), obj.getString("u")));
                }
            }
        } catch (Exception e) {
            scheduleList.add(new ScheduleItem(5, 0, "새벽예배", PRESET1_URL));
            scheduleList.add(new ScheduleItem(6, 0, "생명의삶", PRESET2_URL));
            scheduleList.add(new ScheduleItem(8, 0, "성경통독", PRESET3_URL));
        }
        sortScheduleList();
    }

    private void sortScheduleList() {
        Collections.sort(scheduleList, new Comparator<ScheduleItem>() {
            @Override
            public int compare(ScheduleItem o1, ScheduleItem o2) {
                if(o1.hour != o2.hour) return o1.hour - o2.hour;
                return o1.minute - o2.minute;
            }
        });
    }

    private void startScheduleChecker() {
        scheduleCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();
                int curHour = now.get(Calendar.HOUR_OF_DAY);
                int curMin = now.get(Calendar.MINUTE);
                for(ScheduleItem item : scheduleList) {
                    if(item.hour == curHour && item.minute == curMin) {
                        if(!webView.getUrl().equals(item.url)) {
                            Toast.makeText(MainActivity.this, "스케줄 실행: " + item.title, Toast.LENGTH_LONG).show();
                            currentTargetUrl = item.url;
                            webView.loadUrl(item.url);
                        }
                    }
                }
                scheduleCheckHandler.postDelayed(this, 30000);
            }
        }, 5000);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (btnRotate != null && btnRotate.getParent() != null) {
            ((View) btnRotate.getParent()).bringToFront();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scheduleCheckHandler.removeCallbacksAndMessages(null);
        CookieManager.getInstance().flush();
    }

    @Override
    public void onBackPressed() {
        if (customView != null) webView.getWebChromeClient().onHideCustomView();
        else if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}