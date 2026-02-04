package com.example.xcanplayer_final2;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI
    private WebView webview;
    private LinearLayout controlPanel;
    private Button btnRotate, btnLogin, btnSetting;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideRunnable = () -> {
        if (controlPanel != null && controlPanel.getVisibility() == View.VISIBLE) {
            AlphaAnimation fade = new AlphaAnimation(1.0f, 0.0f);
            fade.setDuration(180);
            controlPanel.startAnimation(fade);
            controlPanel.setVisibility(View.GONE);
        }
    };

    private boolean isLandscape = true;
    private boolean isLoginFlow = false;
    private boolean isLoggedIn = false;

    private long lastHoverInjectMs = 0L;
    private static final long HOVER_THROTTLE_MS = 40;

    private static final String PREFS_NAME = "BibleAppPrefs";
    private static final String KEY_SCHEDULE = "schedule_json";
    private static final String KEY_LOGGED_IN = "logged_in";

    private static final String BASE_URL = "https://www.fondant.kr";
    private static final String FONDANT_LOGIN_URL = "https://www.fondant.kr/sign/in";
    private static final String YOUTUBE_LIVE_URL = "https://www.youtube.com/@suwondongbu/live";

    // Presets
    private static final int PRESET1_H = 5; private static final String PRESET1_T = "새벽예배"; private static final String PRESET1_U = "https://www.fondant.kr/series/00090200-0000-0000-0000-000000001088";
    private static final int PRESET2_H = 6; private static final String PRESET2_T = "생명의삶"; private static final String PRESET2_U = "https://www.fondant.kr/series/00090200-0000-0000-0000-00000000071b";
    private static final int PRESET3_H = 8; private static final String PRESET3_T = "성경통독"; private static final String PRESET3_U = "https://www.fondant.kr/series/00090228-5db3-dc44-3c29-52bcaf0002ce";

    static class ScheduleItem {
        int hour; String title; String url;
        ScheduleItem(int hour, String title, String url) { this.hour = hour; this.title = title; this.url = url; }
    }

    // 로그인 확인 스크립트
    private static final String JS_GUESS_LOGGED_IN =
            "(function(){try{"
                    + "var txt=(document.body && (document.body.innerText||''))||'';"
                    + "if(txt.indexOf('로그아웃')>-1 || txt.indexOf('마이페이지')>-1 || txt.indexOf('My')>-1 || txt.indexOf('프로필')>-1 || txt.indexOf('내 정보')>-1) return true;"
                    + "var ck=(document.cookie||'').toLowerCase();"
                    + "if(ck.indexOf('token')>-1||ck.indexOf('session')>-1||ck.indexOf('auth')>-1) return true;"
                    + "return false;"
                    + "}catch(e){return false;}})()";

    // 자동재생 엔진
    private static final String JS_AUTO_MONITOR =
            "(function(){"
                    + "if(window.__xcan_monitor) return;"
                    + "window.__xcan_monitor = true;"
                    + "var style = document.createElement('style');"
                    + "style.innerHTML = 'html, body { background: black !important; } video { width: 100vw !important; height: 100vh !important; object-fit: contain !important; } .app-banner { display: none !important; }';"
                    + "document.head.appendChild(style);"
                    + "setInterval(function(){"
                    + "   var v = document.querySelector('video');"
                    + "   if(!v) {"
                    + "       var btns = document.querySelectorAll('button, a, div[role=\"button\"]');"
                    + "       for(var i=0; i<btns.length; i++){"
                    + "           var t = (btns[i].innerText || '').trim();"
                    + "           if(/재생|시청|이어보기|Play|Continue/.test(t)) { btns[i].click(); return; }"
                    + "       }"
                    + "       return;"
                    + "   }"
                    + "   if (v.ended || (v.duration > 0 && v.currentTime >= v.duration - 2)) {"
                    + "       var all = document.querySelectorAll('button, a, span, div[role=\"button\"]');"
                    + "       for (var i = 0; i < all.length; i++) {"
                    + "           var txt = (all[i].innerText || '').trim();"
                    + "           if (txt.indexOf('다음') > -1 || txt.indexOf('Next') > -1 || txt.indexOf('Play next') > -1) {"
                    + "               if(all[i].href) { location.href = all[i].href; } else { all[i].click(); }"
                    + "               return;"
                    + "           }"
                    + "       }"
                    + "   }"
                    + "   if(v.paused && v.currentTime < 2) {"
                    + "       v.muted = true;"
                    + "       var p = v.play();"
                    + "       if(p && p.then) {"
                    + "           p.then(function(){ v.muted = false; }).catch(function(){ v.play(); });"
                    + "       } else { v.muted = false; }"
                    + "   }"
                    + "}, 1000);"
                    + "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        setContentView(R.layout.activity_main);

        webview = findViewById(R.id.webview);
        controlPanel = findViewById(R.id.controlPanel);
        btnRotate = findViewById(R.id.btnRotate);
        btnLogin = findViewById(R.id.btnLogin);
        btnSetting = findViewById(R.id.btnSetting);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false);

        enableTouchAsMouseHover();
        setupWebView();

        List<ScheduleItem> list = loadScheduleFromPrefs();
        ensureDefaultPresets(list);
        saveScheduleToPrefs(list);

        loadTargetVideoFromSchedule();

        btnRotate.setOnClickListener(v -> toggleRotation());

        // 설정 버튼: 클릭 시 세로모드 -> 다이얼로그 표시
        btnSetting.setOnClickListener(v -> {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            isLandscape = false;
            handler.postDelayed(this::showAdminDialog, 200);
        });

        btnLogin.setOnClickListener(v -> onLoginClicked());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            long last = 0L;
            @Override
            public void handleOnBackPressed() {
                if (controlPanel != null && controlPanel.getVisibility() == View.VISIBLE) {
                    controlPanel.setVisibility(View.GONE);
                    return;
                }
                if (webview != null && webview.canGoBack()) {
                    webview.goBack();
                    return;
                }
                long now = System.currentTimeMillis();
                if (now - last < 2000) finish();
                else {
                    last = now;
                    Toast.makeText(MainActivity.this, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        showPanelForce();
    }

    private void onLoginClicked() {
        if (isLoggedIn) {
            Toast.makeText(this, "이미 로그인 상태입니다. (설정값)", Toast.LENGTH_SHORT).show();
            webview.evaluateJavascript(JS_GUESS_LOGGED_IN, value -> {});
            return;
        }

        webview.evaluateJavascript(JS_GUESS_LOGGED_IN, value -> {
            if ("true".equalsIgnoreCase(value)) {
                setLoggedIn(true);
                Toast.makeText(this, "이미 로그인되어 있습니다.", Toast.LENGTH_SHORT).show();
            } else {
                isLoginFlow = true;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                isLandscape = false;
                webview.loadUrl(FONDANT_LOGIN_URL);
            }
        });
    }

    private void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    private boolean isLoginUrl(String url) {
        if (url == null) return false;
        return url.contains("/sign/in") || url.contains("/login") || url.contains("kakao") || url.contains("naver");
    }

    private boolean isHomeUrl(String url) {
        if (url == null) return false;
        return url.equals(BASE_URL) || url.equals(BASE_URL + "/") || url.contains("fondant.kr/main");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webview.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webview, true);

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return url.startsWith("intent:") || url.contains("play.google.com");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (isLoginUrl(url)) {
                    isLoginFlow = true;
                    if (isLandscape) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        isLandscape = false;
                    }
                    return;
                }
                if (isLoginFlow && isHomeUrl(url)) {
                    isLoginFlow = false;
                    setLoggedIn(true);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    isLandscape = true;
                    Toast.makeText(MainActivity.this, "로그인 완료", Toast.LENGTH_SHORT).show();
                    loadTargetVideoFromSchedule();
                    return;
                }
                if (isLoginFlow) {
                    isLoginFlow = false;
                    setLoggedIn(true);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    isLandscape = true;
                    loadTargetVideoFromSchedule();
                    return;
                }
                if (isHomeUrl(url)) {
                    loadTargetVideoFromSchedule();
                    return;
                }
                if (url.contains("/series/") || url.contains("episode") || url.contains("category=episode")) {
                    view.evaluateJavascript(JS_AUTO_MONITOR, null);
                }
            }
        });
    }

    private void loadTargetVideoFromSchedule() {
        String target = buildTargetUrlForNow();
        if (TextUtils.isEmpty(target)) {
            Toast.makeText(this, "스케줄이 없습니다. 설정에서 추가해주세요.", Toast.LENGTH_LONG).show();
            showAdminDialog();
            return;
        }
        webview.loadUrl(target);
    }

    private String buildTargetUrlForNow() {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_WEEK);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (day == Calendar.SUNDAY && (hour >= 10 && hour <= 12)) {
            return YOUTUBE_LIVE_URL;
        }

        List<ScheduleItem> list = loadScheduleFromPrefs();
        if (list.isEmpty()) return null;

        list.sort(Comparator.comparingInt(o -> o.hour));
        ScheduleItem target = list.get(list.size() - 1);
        for (ScheduleItem i : list) if (i.hour <= hour) target = i;

        String u = target.url;
        if (u != null && !u.contains("category=episode")) {
            u += (u.contains("?") ? "&" : "?") + "category=episode";
        }
        return u;
    }

    private void ensureDefaultPresets(List<ScheduleItem> list) {
        boolean has5 = false, has6 = false, has8 = false;
        for (ScheduleItem s : list) {
            if (s.hour == PRESET1_H) has5 = true;
            if (s.hour == PRESET2_H) has6 = true;
            if (s.hour == PRESET3_H) has8 = true;
        }
        if (!has5) list.add(new ScheduleItem(PRESET1_H, PRESET1_T, PRESET1_U));
        if (!has6) list.add(new ScheduleItem(PRESET2_H, PRESET2_T, PRESET2_U));
        if (!has8) list.add(new ScheduleItem(PRESET3_H, PRESET3_T, PRESET3_U));
    }

    private List<ScheduleItem> loadScheduleFromPrefs() {
        SharedPreferences p = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String j = p.getString(KEY_SCHEDULE, "");
        List<ScheduleItem> l = new ArrayList<>();
        try {
            if (!TextUtils.isEmpty(j)) {
                JSONArray a = new JSONArray(j);
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    l.add(new ScheduleItem(o.getInt("hour"), o.getString("title"), o.getString("url")));
                }
            }
        } catch (Exception ignored) {}
        return l;
    }

    private void saveScheduleToPrefs(List<ScheduleItem> l) {
        try {
            JSONArray a = new JSONArray();
            for (ScheduleItem i : l) {
                JSONObject o = new JSONObject();
                o.put("hour", i.hour);
                o.put("title", i.title);
                o.put("url", i.url);
                a.put(o);
            }
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putString(KEY_SCHEDULE, a.toString())
                    .apply();
        } catch (Exception ignored) {}
    }

    private void showAdminDialog() {
        final List<ScheduleItem> temp = loadScheduleFromPrefs();
        ensureDefaultPresets(temp);

        // 전체 화면 레이아웃
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);

        // 1. 프리셋 버튼
        LinearLayout presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        presetRow.setGravity(Gravity.CENTER_HORIZONTAL);
        presetRow.setPadding(0, 0, 0, 10);

        final EditText etHour = new EditText(this);
        etHour.setHint("시(0~23)");
        etHour.setInputType(InputType.TYPE_CLASS_NUMBER);
        etHour.setTextSize(14);
        etHour.setGravity(Gravity.CENTER);

        final EditText etTitle = new EditText(this);
        etTitle.setHint("제목");
        etTitle.setTextSize(14);

        final EditText etUrl = new EditText(this);
        etUrl.setHint("URL (https://...)");
        etUrl.setTextSize(14);

        Button b1 = makeSmallPresetBtn("🌅 새벽", 14);
        b1.setOnClickListener(v -> { etHour.setText(String.valueOf(PRESET1_H)); etTitle.setText(PRESET1_T); etUrl.setText(PRESET1_U); });
        Button b2 = makeSmallPresetBtn("🌿 생명", 14);
        b2.setOnClickListener(v -> { etHour.setText(String.valueOf(PRESET2_H)); etTitle.setText(PRESET2_T); etUrl.setText(PRESET2_U); });
        Button b3 = makeSmallPresetBtn("📖 통독", 14);
        b3.setOnClickListener(v -> { etHour.setText(String.valueOf(PRESET3_H)); etTitle.setText(PRESET3_T); etUrl.setText(PRESET3_U); });

        presetRow.addView(b1); presetRow.addView(space(10));
        presetRow.addView(b2); presetRow.addView(space(10));
        presetRow.addView(b3);
        root.addView(presetRow);

        // 2. 입력 영역
        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams hourParams = new LinearLayout.LayoutParams(dpToPx(100), LinearLayout.LayoutParams.WRAP_CONTENT);
        etHour.setLayoutParams(hourParams);

        Button add = new Button(this);
        add.setText("추가 / 수정");
        add.setTextSize(14);

        inputRow.addView(etHour);
        inputRow.addView(space(20));
        inputRow.addView(add);
        root.addView(inputRow);

        root.addView(etTitle);
        root.addView(etUrl);

        // 3. 리스트 영역 (화면 남은 공간 꽉 채우기)
        TextView listHeader = new TextView(this);
        listHeader.setText("▼ 현재 스케줄 목록 (시간순)");
        listHeader.setPadding(0, 20, 0, 10);
        root.addView(listHeader);

        ScrollView sv = new ScrollView(this);
        LinearLayout.LayoutParams svParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        svParams.weight = 1; // [핵심] 남은 공간 모두 차지
        sv.setLayoutParams(svParams);

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        sv.addView(listContainer);
        root.addView(sv);

        final Runnable[] updateList = new Runnable[1];
        updateList[0] = () -> {
            listContainer.removeAllViews();
            temp.sort(Comparator.comparingInt(o -> o.hour));
            for (ScheduleItem item : temp) {
                final ScheduleItem cur = item;

                LinearLayout r = new LinearLayout(this);
                r.setOrientation(LinearLayout.HORIZONTAL);
                r.setGravity(Gravity.CENTER_VERTICAL);
                r.setPadding(0, 10, 0, 10);

                TextView tv = new TextView(this);
                tv.setText(String.format(Locale.KOREA, "[%02d시] %s", cur.hour, cur.title));
                tv.setTextSize(16);
                tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                Button del = new Button(this);
                del.setText("삭제");
                del.setTextSize(12);
                del.setOnClickListener(v -> { temp.remove(cur); updateList[0].run(); });

                r.addView(tv);
                r.addView(del);
                listContainer.addView(r);
            }
        };
        updateList[0].run();

        add.setOnClickListener(v -> {
            int h = parseHour(etHour.getText().toString().trim());
            String t = etTitle.getText().toString().trim();
            String u = etUrl.getText().toString().trim();

            if (h < 0 || h > 23) { Toast.makeText(this, "시간은 0~23 사이 입력", Toast.LENGTH_SHORT).show(); return; }
            if (TextUtils.isEmpty(t) || TextUtils.isEmpty(u)) { Toast.makeText(this, "제목과 URL을 입력하세요", Toast.LENGTH_SHORT).show(); return; }

            for (int i = temp.size() - 1; i >= 0; i--) if (temp.get(i).hour == h) temp.remove(i);
            temp.add(new ScheduleItem(h, t, u));
            updateList[0].run();
            Toast.makeText(this, "추가되었습니다.", Toast.LENGTH_SHORT).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(root)
                .setNegativeButton("닫기 (취소)", null)
                .setPositiveButton("저장 및 적용", (d, w) -> {
                    saveScheduleToPrefs(temp);
                    loadTargetVideoFromSchedule();
                })
                .create();

        dialog.setOnDismissListener(d -> {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            isLandscape = true;
        });

        dialog.show();

        // [중요] 다이얼로그 크기를 전체 화면으로 확장
        try {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            }
        } catch (Exception ignored) {}
    }

    private int parseHour(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return -1; } }

    private View space(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(dp), 1));
        return v;
    }

    private int dpToPx(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }

    private Button makeSmallPresetBtn(String text, int sp) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextSize(sp);
        b.setPadding(10, 5, 10, 5);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        return b;
    }

    private void showPanelForce() {
        if (controlPanel == null) return;
        handler.removeCallbacks(hideRunnable);
        controlPanel.bringToFront();
        controlPanel.setVisibility(View.VISIBLE);

        AlphaAnimation fade = new AlphaAnimation(0.0f, 1.0f);
        fade.setDuration(160);
        controlPanel.startAnimation(fade);

        handler.postDelayed(hideRunnable, 4500);
    }

    private void toggleRotation() {
        if (isLandscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            isLandscape = false;
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            isLandscape = true;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void enableTouchAsMouseHover() {
        if (webview == null) return;

        webview.setOnTouchListener((v, ev) -> {
            int a = ev.getActionMasked();

            if (a == MotionEvent.ACTION_DOWN) {
                showPanelForce();
            }

            if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_MOVE) {
                long now = SystemClock.uptimeMillis();
                if (now - lastHoverInjectMs < HOVER_THROTTLE_MS) return false;
                lastHoverInjectMs = now;

                float s = webview.getScale();
                int x = Math.round(ev.getX() / (s <= 0 ? 1f : s));
                int y = Math.round(ev.getY() / (s <= 0 ? 1f : s));

                String js =
                        "(function(){"
                                + "var x=" + x + ",y=" + y + ";"
                                + "var el=document.elementFromPoint(x,y);"
                                + "var ts=[el,document,window].filter(Boolean);"
                                + "function f(t,e){try{t.dispatchEvent(e);}catch(_){}}"
                                + "try{var pe=new PointerEvent('pointermove',{bubbles:true,clientX:x,clientY:y,pointerId:1,pointerType:'mouse',isPrimary:true});"
                                + "ts.forEach(function(t){f(t,pe);});}catch(e){}"
                                + "var mm=new MouseEvent('mousemove',{bubbles:true,clientX:x,clientY:y});"
                                + "var mo=new MouseEvent('mouseover',{bubbles:true,clientX:x,clientY:y});"
                                + "ts.forEach(function(t){f(t,mm);f(t,mo);});"
                                + "})();";
                webview.evaluateJavascript(js, null);
            }
            return false;
        });
    }
}