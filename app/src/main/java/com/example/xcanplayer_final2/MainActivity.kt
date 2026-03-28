package com.example.xcanplayer_final2

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var webView: android.webkit.WebView
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var btnSpeed: TextView
    private lateinit var btnSchedule: ImageButton
    private lateinit var btnRotate: ImageButton

    private lateinit var immersiveHelper: ImmersiveModeHelper
    private lateinit var webController: FondantWebController
    private lateinit var scheduleUiController: ScheduleUiController
    private lateinit var localStore: LocalStore

    private var scheduleList = mutableListOf<ScheduleItem>()
    private val scheduleCheckHandler = Handler(Looper.getMainLooper())
    private var lastExecutedTime = ""

    // ★ [핵심 추가] 팝업창에서 스케줄을 건드렸는지 감시하는 센서
    private var isScheduleModified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        webView = findViewById(R.id.webView)
        fullscreenContainer = findViewById(R.id.fullscreen_container)
        btnSpeed = findViewById(R.id.btnSpeed)
        btnSchedule = findViewById(R.id.btnSchedule)
        btnRotate = findViewById(R.id.btnRotate)

        immersiveHelper = ImmersiveModeHelper(window, window.decorView)
        localStore = LocalStore(this)
        scheduleList = localStore.loadSchedule()

        scheduleUiController = ScheduleUiController(this, localStore) { updatedList ->
            scheduleList.clear()
            scheduleList.addAll(updatedList)
            isScheduleModified = true // 스케줄이 추가/삭제되면 센서 작동!
        }

        webController = FondantWebController(webView, fullscreenContainer) {
            checkAndApplyOrientation()
        }

        webController.setupWebView()
        setupButtons()

        val initialUrl = getUrlForCurrentTime()
        webController.loadSmartUrl(initialUrl)

        startScheduleChecker()
    }

    private fun checkAndApplyOrientation() {
        if (webController.isVideoPage() || webController.customView != null) {
            if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            immersiveHelper.applyImmersiveMode(true)
        } else {
            if (resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            immersiveHelper.applyImmersiveMode(false)
        }
    }

    private fun setupButtons() {
        btnSpeed.setOnClickListener {
            webController.currentSpeed = if (webController.currentSpeed >= 2.0f) 1.0f else webController.currentSpeed + 0.25f
            btnSpeed.text = "${webController.currentSpeed}x"
            webController.applySpeed()
        }
        btnRotate.setOnClickListener {
            requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }

        btnSchedule.setOnClickListener {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            immersiveHelper.applyImmersiveMode(false)
            isScheduleModified = false // 팝업 열 때는 센서 초기화

            scheduleUiController.showScheduleDialog(scheduleList) {
                checkAndApplyOrientation()
                // ★ [핵심 해결] 팝업 닫을 때, 스케줄이 지워지거나 추가됐으면 화면을 '즉시' 새 스케줄에 맞춰 이동!
                if (isScheduleModified) {
                    val newUrl = getUrlForCurrentTime()
                    webController.loadSmartUrl(newUrl)
                    Toast.makeText(this@MainActivity, "변경된 스케줄에 맞춰 화면을 이동합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getUrlForCurrentTime(): String {
        if (scheduleList.isEmpty()) return FondantDefaults.QT_URL
        val now = Calendar.getInstance()
        val curMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        var matchedUrl = scheduleList.last().url
        for (item in scheduleList) {
            if (item.hour * 60 + item.minute <= curMinutes) matchedUrl = item.url else break
        }
        return matchedUrl
    }

    private fun startScheduleChecker() {
        scheduleCheckHandler.postDelayed(object : Runnable {
            override fun run() {
                val now = Calendar.getInstance()
                val curHour = now.get(Calendar.HOUR_OF_DAY)
                val curMin = now.get(Calendar.MINUTE)

                for (item in scheduleList) {
                    if (item.hour == curHour && item.minute == curMin) {
                        val timeKey = String.format(Locale.getDefault(), "%02d:%02d", curHour, curMin)
                        if (timeKey != lastExecutedTime) {
                            lastExecutedTime = timeKey
                            Toast.makeText(this@MainActivity, "${item.title} 스케줄 강제 실행!", Toast.LENGTH_LONG).show()
                            webController.loadSmartUrl(item.url)
                        }
                        break
                    }
                }
                scheduleCheckHandler.postDelayed(this, 10000)
            }
        }, 5000)
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onBackPressed() {
        if (webController.customView != null) webController.customViewCallback?.onCustomViewHidden()
        else if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}